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

   public static int selectedIndex() {
      return selected;
   }

   public static boolean isUnlocked(int index) {
      return (unlocked & 1 << index) != 0;
   }

   public static int cooldown(int index) {
      return index < cooldowns.length ? cooldowns[index] : 0;
   }

   public static int cooldownTotal(int index) {
      return index < totals.length ? totals[index] : 0;
   }

   public static int fingers() {
      return fingers;
   }

   public static int blueLeft() {
      return blue;
   }

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
               if (mc.screen == null && role >= 0) {
                  Network.CHANNEL.sendToServer(new RoleC2S(1, mc.player.isShiftKeyDown() ? 1 : 0));
               }
            }

            while (ExtraClientEvents.WHEEL.consumeClick()) {
               if (mc.screen == null && role >= 0) {
                  mc.setScreen(new AbilityWheelScreen());
               }
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
            int sel = Mth.clamp(selected, 0, Math.max(0, list.size() - 1));
            Ability chosen = list.isEmpty() ? null : list.get(sel);
            if (charging < 0 && blue > 0 && list.contains(Ability.BLUE_FISTS)) {
               String left = Component.translatable("ability.vaz2109.blue_fists").getString() + "  " + HudKit.duration(blue);
               HudKit.outlined(g, font, left, width / 2, height / 2 + 20, -11891457);
            }

            if (charging >= 0) {
               float c;
               int col;
               Component label;
               if (chosen == Ability.PIERCING_BLOOD) {
                  c = Math.min(1.0F, (float)charging / 16.0F);
                  col = c >= 1.0F ? 16724016 : 11141120;
                  label = Component.translatable(c >= 1.0F ? "hud.vaz2109.blood.ready" : "hud.vaz2109.blood.charging");
               } else {
                  c = Math.min(1.0F, (float)charging / 40.0F);
                  col = c >= 1.0F ? 16764992 : 16738832;
                  label = Component.translatable(c >= 1.0F ? "hud.vaz2109.fuga.ready" : "hud.vaz2109.fuga.charging");
               }

               HudKit.progressRing(g, (float)width / 2.0F, (float)height / 2.0F, 14.0F, 3.0F, c, col);
               HudKit.outlined(g, font, label.getString(), width / 2, height / 2 + 20, 0xFF000000 | col);
            }
         }
      }
   }
}
