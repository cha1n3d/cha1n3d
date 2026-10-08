package com.bobux.vaz2109.client;

import com.bobux.vaz2109.cyber.Cyberware;
import com.bobux.vaz2109.cyber.Implant;
import com.bobux.vaz2109.network.CyberC2S;
import com.bobux.vaz2109.network.CyberS2C;
import com.bobux.vaz2109.network.Network;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Cyberware HUD: humanity next to the hotbar, the Sandevistan's charge, its colour grade, and the cyberpsychosis glitch. */
@EventBusSubscriber(modid = "vaz2109", value = Dist.CLIENT)
public final class CyberClient {
   private static int mask;
   private static int humanity = 100;
   private static int sande;
   private static int cooldown;
   private static int episode;
   private static int ticks;

   private CyberClient() {
   }

   public static void handle(CyberS2C msg) {
      mask = msg.mask;
      humanity = msg.humanity;
      sande = msg.sande;
      cooldown = msg.cooldown;
      episode = msg.episode;
   }

   public static void reset() {
      mask = 0;
      humanity = 100;
      sande = cooldown = episode = 0;
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase != Phase.START) {
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         reset();
         return;
      }

      ticks++;
      if (sande > 0) {
         sande--;
      }

      if (cooldown > 0) {
         cooldown--;
      }

      if (episode > 0) {
         episode--;
      }

      while (ExtraClientEvents.IMPLANT.consumeClick()) {
         if (mc.screen == null) {
            Network.CHANNEL.sendToServer(new CyberC2S());
         }
      }
   }

   public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mask == 0) {
         return;
      }

      float now = (float)ticks + partialTick;
      if (sande > 0) {
         // Sandevistan: the world goes teal and the edges split into colour
         float in = Math.min(1.0F, (float)(Cyberware.SANDE_TICKS - sande) / 6.0F) * Math.min(1.0F, (float)sande / 10.0F);
         int a = (int)(in * 70.0F);
         g.fill(0, 0, width, height, (int)(in * 28.0F) << 24 | 0x00E0B0);
         int edge = width / 7;
         g.fillGradient(0, 0, width, edge / 2, a << 24 | 0xFF30C0, 0x00FF30C0);
         g.fillGradient(0, height - edge / 2, width, height, 0x0030FFE0, a << 24 | 0x30FFE0);
      }

      if (episode > 0) {
         // cyberpsychosis: torn scanlines and error text
         RandomSource r = RandomSource.create((long)(now / 2.0F));
         int bars = 3 + r.nextInt(6);
         for (int i = 0; i < bars; i++) {
            int y = r.nextInt(height);
            int h = 1 + r.nextInt(6);
            int shift = r.nextInt(30) - 15;
            int col = r.nextBoolean() ? 0x80FF1030 : 0x6020FFF0;
            g.fill(Math.max(0, shift), y, Math.min(width, width + shift), y + h, col);
         }

         g.fill(0, 0, width, height, (int)(30.0F + 20.0F * Mth.sin(now * 0.7F)) << 24 | 0x400000);
         if (r.nextInt(3) == 0) {
            String text = Component.translatable("hud.vaz2109.cyber.psychosis").getString();
            g.drawString(mc.font, text, r.nextInt(Math.max(1, width - 120)), r.nextInt(Math.max(1, height - 20)), 0xFFFF2040, false);
         }
      }

      if (mc.options.hideGui) {
         return;
      }

      int x = width / 2 + 98;
      int y = height - 21;
      int col = humanity >= Cyberware.PSYCHOSIS ? 0xFF35E0FF : (humanity >= Cyberware.SEVERE ? 0xFFFFA020 : 0xFFFF3030);
      String label = Component.translatable("hud.vaz2109.cyber.humanity", new Object[]{humanity}).getString();
      g.drawString(mc.font, label, x, y, col, true);
      int w = 64;
      g.fill(x, y + 10, x + w, y + 13, 0x80000000);
      g.fill(x, y + 10, x + Mth.clamp(humanity, 0, 100) * w / 100, y + 13, col);
      if ((mask & Implant.SANDEVISTAN.bit()) != 0) {
         String s = sande > 0
            ? Component.translatable("hud.vaz2109.cyber.sande_on", new Object[]{(sande + 19) / 20}).getString()
            : (cooldown > 0
               ? Component.translatable("hud.vaz2109.cyber.sande_cd", new Object[]{(cooldown + 19) / 20}).getString()
               : Component.translatable("hud.vaz2109.cyber.sande_ready", new Object[]{ExtraClientEvents.IMPLANT.getTranslatedKeyMessage()}).getString());
         g.drawString(mc.font, s, x, y - 10, sande > 0 ? 0xFF30FFC0 : (cooldown > 0 ? 0xFF808890 : 0xFFB0FFE8), true);
      }
   }
}
