package com.bobux.vaz2109.client;

import com.bobux.vaz2109.ModRegistry;
import com.bobux.vaz2109.client.fx.CursedFxClient;
import com.bobux.vaz2109.network.Network;
import com.bobux.vaz2109.network.RoleC2S;
import com.bobux.vaz2109.network.RoleS2C;
import com.bobux.vaz2109.power.Power;
import com.bobux.vaz2109.role.Ability;
import com.bobux.vaz2109.role.Role;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(
   modid = "vaz2109",
   value = {Dist.CLIENT}
)
public final class RoleClient {
   private static boolean known;
   private static boolean held;
   private static int role = -1;
   private static int selected;
   private static int unlocked;
   private static int possession;
   private static int fingers;
   private static int charging = -1;
   private static int blue;
   private static int[] cooldowns = new int[0];
   private static int[] totals = new int[0];
   public static final int PANEL_W = 124;
   private static final int SLOT = 24;
   private static final float[] READY_AT = new float[]{-1000.0F, -1000.0F, -1000.0F, -1000.0F, -1000.0F, -1000.0F, -1000.0F, -1000.0F};
   private static final boolean[] COOLING = new boolean[8];
   private static boolean filtered;

   private RoleClient() {
   }

   @Nullable
   public static Role role() {
      return Role.byId(role);
   }

   public static void handle(RoleS2C msg) {
      known = true;
      role = msg.role;
      selected = msg.selected;
      unlocked = msg.unlocked;
      possession = msg.possession;
      fingers = msg.fingers;
      charging = msg.charging;
      blue = msg.blue;
      if (totals.length != msg.cooldowns.length) {
         totals = new int[msg.cooldowns.length];
      }

      for (int i = 0; i < msg.cooldowns.length; i++) {
         if (msg.cooldowns[i] > (cooldowns.length > i ? cooldowns[i] : 0)) {
            totals[i] = msg.cooldowns[i];
         }
      }

      cooldowns = msg.cooldowns;
      if (role >= 0 && Minecraft.getInstance().screen instanceof RoleScreen screen) {
         screen.onClose();
      }
   }

   public static void reset() {
      known = false;
      held = false;
      role = -1;
      cooldowns = new int[0];
      possession = 0;
      charging = -1;
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.START) {
         Minecraft mc = Minecraft.getInstance();

         for (int i = 0; i < cooldowns.length; i++) {
            if (cooldowns[i] > 0) {
               cooldowns[i]--;
            }
         }

         if (possession > 0) {
            possession--;
         }

         if (blue > 0) {
            blue--;
         }

         if (charging >= 0) {
            charging++;
         }

         if (mc.player != null && mc.level != null) {
            if (known && role < 0 && mc.screen == null) {
               mc.setScreen(new RoleScreen());
            }

            while (ClientModEvents.CLASS.consumeClick()) {
               if (mc.screen == null && role >= 0 && !held) {
                  held = true;
                  Network.CHANNEL.sendToServer(new RoleC2S(1, mc.player.isShiftKeyDown() ? 1 : 0));
               }
            }

            if (held && !ClientModEvents.CLASS.isDown()) {
               held = false;
               Network.CHANNEL.sendToServer(new RoleC2S(2, 0));
            }
         }
      }
   }

   public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         float now = (float)mc.player.tickCount + partialTick;
         if (mc.level != null
            && CursedFxClient.shrineEnd > mc.level.getGameTime()
            && mc.player.position().distanceTo(CursedFxClient.shrineCenter) < CursedFxClient.shrineRadius + 2.0) {
            float pulse = 0.5F + 0.5F * Mth.sin(now * 0.3F);
            g.fill(0, 0, width, height, (int)(40.0F + 30.0F * pulse) << 24 | 5898248);
         }

         Role r = Role.byId(role);
         if (!mc.options.hideGui && r != null && possession <= 0) {
            if (!filtered) {
               filtered = true;

               for (Ability a : Ability.values()) {
                  mc.getTextureManager().getTexture(HudKit.icon(a.id)).setFilter(true, false);
               }

               for (Role each : Role.values()) {
                  mc.getTextureManager().getTexture(HudKit.icon("role_" + each.id)).setFilter(true, false);
               }
            }

            Font font = mc.font;
            List<Ability> list = Ability.of(r);
            int accent = r.color;
            int sel = Mth.clamp(selected, 0, Math.max(0, list.size() - 1));
            Ability chosen = list.isEmpty() ? null : list.get(sel);
            boolean chosenOpen = (unlocked & 1 << sel) != 0;
            List<FormattedCharSequence> need = chosen != null && !chosenOpen ? font.split(chosen.requirement(), 149) : List.of();
            int h = 57 + need.size() * 7 + 14;
            int w = 124;
            int bottom = height - 4 - (Power.of(mc.player) != null ? 34 : 0);
            int x = width / 2 - 91 - 6 - w;
            int y = bottom - h;
            if (x < 2) {
               x = width / 2 - 91;
               y = height - 40 - h - (Power.of(mc.player) != null ? 34 : 0);
            }

            HudKit.panel(g, x, y, w, h, accent);
            HudKit.icon(g, HudKit.icon("role_" + r.id), x + 6, y + 4, 11, 1.0F, 1.0F);
            g.drawString(font, Component.translatable("role.vaz2109." + r.id), x + 20, y + 6, 0xFF000000 | accent, true);
            if (r == Role.VESSEL) {
               String f = fingers + "/20";
               int fx = x + w - 6 - font.width(f);
               g.drawString(font, f, fx, y + 6, fingers >= 15 ? -44976 : -3121056, true);
               g.pose().pushPose();
               g.pose().translate((float)(fx - 12), (float)(y + 4), 0.0F);
               g.pose().scale(0.65F, 0.65F, 1.0F);
               g.renderItem(new ItemStack((ItemLike)ModRegistry.SUKUNA_FINGER.get()), 0, 0);
               g.pose().popPose();
            }

            g.fill(x + 6, y + 17, x + w - 6, y + 18, 553648127);
            int sy = y + 22;

            for (int i = 0; i < list.size() && i < READY_AT.length; i++) {
               Ability a = list.get(i);
               int sx = x + 7 + i * 30;
               boolean open = (unlocked & 1 << i) != 0;
               boolean isSel = i == sel;
               int cd = i < cooldowns.length ? cooldowns[i] : 0;
               if (cd > 0) {
                  COOLING[i] = true;
               } else if (COOLING[i]) {
                  COOLING[i] = false;
                  READY_AT[i] = now;
               }

               boolean active = open && a == Ability.BLUE_FISTS && blue > 0;
               float flash = Math.max(0.0F, 1.0F - (now - READY_AT[i]) / 10.0F);
               float glow = isSel && open ? 0.55F + 0.25F * Mth.sin(now * 0.15F) : 0.0F;
               if (active) {
                  glow = 0.8F + 0.2F * Mth.sin(now * 0.5F);
               }

               HudKit.slot(g, sx, sy, 24, active ? 3832831 : accent, Math.max(glow, flash), isSel);
               HudKit.icon(g, HudKit.icon(a.id), sx + 2, sy + 2, 20, open ? (cd > 0 ? 0.55F : 1.0F) : 0.25F, open ? 1.0F : 0.8F);
               if (!open) {
                  int lx = sx + 24 - 9;
                  int ly = sy + 24 - 10;
                  g.fill(lx + 1, ly, lx + 6, ly + 1, -4671304);
                  g.fill(lx + 1, ly, lx + 2, ly + 4, -4671304);
                  g.fill(lx + 5, ly, lx + 6, ly + 4, -4671304);
                  g.fill(lx, ly + 3, lx + 7, ly + 9, -2576320);
                  g.fill(lx + 3, ly + 5, lx + 4, ly + 7, -12965360);
               } else if (cd > 0 && i < totals.length && totals[i] > 0) {
                  float left = (float)cd / (float)totals[i];
                  g.enableScissor(sx, sy, sx + 24, sy + 24);
                  HudKit.pie(g, (float)sx + 12.0F, (float)sy + 12.0F, 24.0F, 1.0F - left, 1.0F, -1476395008);
                  g.disableScissor();
                  HudKit.outlined(g, font, HudKit.seconds(cd), sx + 12, sy + 12 - 4, -1);
               }

               if (flash > 0.0F) {
                  g.fill(sx, sy, sx + 24, sy + 24, HudKit.alpha(16777215, flash * 0.6F));
               }

               if (active) {
                  float left = (float)blue / 300.0F;
                  g.fill(sx, sy + 24 + 2, sx + 24, sy + 24 + 4, -1073741824);
                  g.fill(sx, sy + 24 + 2, sx + Math.round(24.0F * left), sy + 24 + 4, -11891457);
               } else if (isSel) {
                  int px = sx + 12;
                  g.fill(px - 2, sy + 24 + 2, px + 3, sy + 24 + 3, 0xFF000000 | accent);
                  g.fill(px - 1, sy + 24 + 3, px + 2, sy + 24 + 4, 0xFF000000 | accent);
               }
            }

            int ty = sy + 24 + 6;
            if (chosen != null) {
               int cdx = sel < cooldowns.length ? cooldowns[sel] : 0;
               String name = font.plainSubstrByWidth(Component.translatable("ability.vaz2109." + chosen.id).getString(), w - 12);
               g.drawString(font, name, x + 6, ty, chosenOpen ? -1 : -7697782, true);
               ty += 11;
               if (!chosenOpen) {
                  for (FormattedCharSequence line : need) {
                     g.pose().pushPose();
                     g.pose().translate((float)(x + 6), (float)ty, 0.0F);
                     g.pose().scale(0.75F, 0.75F, 1.0F);
                     g.drawString(font, line, 0, 0, -5209984, false);
                     g.pose().popPose();
                     ty += 7;
                  }
               } else {
                  String state;
                  int color;
                  if (chosen == Ability.BLUE_FISTS && blue > 0) {
                     state = Component.translatable("hud.vaz2109.role.active", new Object[]{HudKit.duration(blue)}).getString();
                     color = -9787137;
                  } else if (cdx > 0) {
                     state = Component.translatable("hud.vaz2109.role.cooldown", new Object[]{HudKit.duration(cdx)}).getString();
                     color = -6645094;
                  } else {
                     state = Component.translatable("hud.vaz2109.ready").getString();
                     color = (int)now % 20 < 10 ? -9379728 : -11485104;
                  }

                  HudKit.text(g, font, state, (float)(x + 6), (float)(ty - 1), 0.75F, color, false);
               }
            }

            int ky = y + h - 13;
            Component key = ClientModEvents.CLASS.getTranslatedKeyMessage();
            int kx = x + 6;
            kx += HudKit.keyCap(g, font, key, kx, ky, held) + 3;
            String use = Component.translatable("hud.vaz2109.role.use").getString();
            HudKit.text(g, font, use, (float)kx, (float)(ky + 2), 0.75F, -5197648, false);
            kx += Math.round((float)font.width(use) * 0.75F) + 6;
            if (list.size() > 1) {
               String shift = "⇧";
               g.drawString(font, shift, kx, ky + 1, -5197648, false);
               kx += font.width(shift) + 1;
               kx += HudKit.keyCap(g, font, key, kx, ky, false) + 3;
               HudKit.text(g, font, Component.translatable("hud.vaz2109.role.switch").getString(), (float)kx, (float)(ky + 2), 0.75F, -5197648, false);
            }

            if (charging >= 0) {
               float c = Math.min(1.0F, (float)charging / 40.0F);
               int col = chosen == Ability.PIERCING_BLOOD ? (c >= 1.0F ? 16724016 : 11141120) : (c >= 1.0F ? 16764992 : 16738832);
               HudKit.progressRing(g, (float)width / 2.0F, (float)height / 2.0F, 14.0F, 3.0F, c, col);
               String kind = chosen == Ability.PIERCING_BLOOD ? "blood" : "fuga";
               Component label = Component.translatable(c >= 1.0F ? "hud.vaz2109." + kind + ".ready" : "hud.vaz2109." + kind + ".charging");
               HudKit.outlined(g, font, label.getString(), width / 2, height / 2 + 20, 0xFF000000 | col);
            }
         }
      }
   }
}
