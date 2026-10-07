package com.bobux.vaz2109.client;

import com.bobux.vaz2109.network.Network;
import com.bobux.vaz2109.network.RoleC2S;
import com.bobux.vaz2109.role.Ability;
import com.bobux.vaz2109.role.Role;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/** Hold the class key: a wheel of the class's abilities. Point at one with the mouse and let go of the key to pick it. */
public class AbilityWheelScreen extends Screen {
   private static final float INNER = 26.0F;
   private static final float OUTER = 96.0F;
   private static final float ICON_RADIUS = 62.0F;
   private int hovered = -1;
   private boolean done;
   private float open;

   public AbilityWheelScreen() {
      super(Component.translatable("gui.vaz2109.wheel.title"));
   }

   public boolean isPauseScreen() {
      return false;
   }

   private List<Ability> abilities() {
      Role role = RoleClient.role();
      return role == null ? List.of() : Ability.of(role);
   }

   private int pick(double mx, double my) {
      int n = this.abilities().size();
      double dx = mx - this.width / 2.0;
      double dy = my - this.height / 2.0;
      if (n == 0 || dx * dx + dy * dy < (double)(INNER * INNER)) {
         return -1;
      } else {
         double turn = Math.atan2(dx, -dy) / (Math.PI * 2);
         return Math.floorMod((int)Math.floor(turn * n + 0.5), n);
      }
   }

   private void choose() {
      if (!this.done) {
         this.done = true;
         if (this.hovered >= 0 && RoleClient.isUnlocked(this.hovered)) {
            Network.CHANNEL.sendToServer(new RoleC2S(3, this.hovered));
         }

         this.onClose();
      }
   }

   public boolean keyReleased(int key, int scan, int modifiers) {
      if (ClientModEvents.CLASS.matches(key, scan)) {
         this.choose();
         return true;
      } else {
         return super.keyReleased(key, scan, modifiers);
      }
   }

   public boolean mouseClicked(double mx, double my, int button) {
      this.hovered = this.pick(mx, my);
      this.choose();
      return true;
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.open = Math.min(1.0F, this.open + partialTick * 0.25F + 0.05F);
      g.fill(0, 0, this.width, this.height, HudKit.alpha(0, 0.35F * this.open));
      List<Ability> list = this.abilities();
      Role role = RoleClient.role();
      int n = list.size();
      if (role != null && n != 0) {
         float cx = this.width / 2.0F;
         float cy = this.height / 2.0F;
         float scale = 0.8F + 0.2F * this.open;
         float inner = INNER * scale;
         float outer = OUTER * scale;
         this.hovered = this.pick(mouseX, mouseY);
         int accent = role.color;

         for (int i = 0; i < n; i++) {
            float from = ((float)i - 0.5F) / (float)n + 0.004F;
            float to = ((float)i + 0.5F) / (float)n - 0.004F;
            boolean hot = i == this.hovered;
            boolean sel = i == RoleClient.selectedIndex();
            int fill = hot ? HudKit.alpha(accent, 0.55F) : (sel ? HudKit.alpha(accent, 0.3F) : -1072820192);
            HudKit.ring(g, cx, cy, inner, outer + (hot ? 6.0F : 0.0F), from, to, fill);
            HudKit.ring(g, cx, cy, outer + (hot ? 6.0F : 0.0F), outer + (hot ? 8.0F : 2.0F), from, to, sel ? 0xFF000000 | accent : 1627389951);
         }

         for (int i = 0; i < n; i++) {
            Ability a = list.get(i);
            float ang = (float)i / (float)n * (float)(Math.PI * 2);
            float r = ICON_RADIUS * scale + (i == this.hovered ? 3.0F : 0.0F);
            int size = i == this.hovered ? 32 : 28;
            int ix = Math.round(cx + Mth.sin(ang) * r) - size / 2;
            int iy = Math.round(cy - Mth.cos(ang) * r) - size / 2;
            boolean open = RoleClient.isUnlocked(i);
            int cd = RoleClient.cooldown(i);
            HudKit.icon(g, HudKit.icon(a.id), ix, iy, size, open ? (cd > 0 ? 0.55F : 1.0F) : 0.25F, open ? 1.0F : 0.8F);
            if (open && cd > 0 && RoleClient.cooldownTotal(i) > 0) {
               float left = (float)cd / (float)RoleClient.cooldownTotal(i);
               HudKit.pie(g, ix + size / 2.0F, iy + size / 2.0F, size * 0.55F, 1.0F - left, 1.0F, -1476395008);
               HudKit.outlined(g, this.font, HudKit.seconds(cd), ix + size / 2, iy + size / 2 - 4, -1);
            } else if (!open) {
               int lx = ix + size / 2 - 3;
               int ly = iy + size / 2 - 4;
               g.fill(lx + 1, ly, lx + 6, ly + 1, -4671304);
               g.fill(lx + 1, ly, lx + 2, ly + 4, -4671304);
               g.fill(lx + 5, ly, lx + 6, ly + 4, -4671304);
               g.fill(lx, ly + 3, lx + 7, ly + 9, -2576320);
            }
         }

         int show = this.hovered >= 0 ? this.hovered : RoleClient.selectedIndex();
         if (show >= 0 && show < n) {
            Ability a = list.get(show);
            int ty = Math.round(cy + outer + 14.0F);
            HudKit.outlined(g, this.font, Component.translatable("ability.vaz2109." + a.id).getString(), Math.round(cx), ty, 0xFF000000 | accent);
            ty += 12;
            if (!RoleClient.isUnlocked(show)) {
               for (FormattedCharSequence line : this.font.split(a.requirement(), 220)) {
                  g.drawCenteredString(this.font, line, Math.round(cx), ty, -5209984);
                  ty += 10;
               }
            } else if (RoleClient.cooldown(show) > 0) {
               g.drawCenteredString(
                  this.font, Component.translatable("hud.vaz2109.role.cooldown", new Object[]{HudKit.duration(RoleClient.cooldown(show))}), Math.round(cx), ty, -6645094
               );
            } else {
               g.drawCenteredString(this.font, Component.translatable("hud.vaz2109.ready"), Math.round(cx), ty, -9379728);
            }
         }

         Component title = Component.translatable("role.vaz2109." + role.id);
         g.drawCenteredString(this.font, title, Math.round(cx), Math.round(cy) - 4, 0xFF000000 | accent);
         g.drawCenteredString(
            this.font, Component.translatable("gui.vaz2109.wheel.hint", new Object[]{ClientModEvents.CLASS.getTranslatedKeyMessage()}), Math.round(cx), Math.round(cy - outer - 22.0F), -3355444
         );
      }

      super.render(g, mouseX, mouseY, partialTick);
   }
}
