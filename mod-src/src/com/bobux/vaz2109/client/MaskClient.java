package com.bobux.vaz2109.client;

import com.bobux.vaz2109.item.HeadwearItem;
import com.bobux.vaz2109.item.MaskItem;
import com.bobux.vaz2109.network.ComboS2C;
import com.bobux.vaz2109.weapon.Mask;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "vaz2109",
   value = {Dist.CLIENT}
)
public final class MaskClient {
   private static final ResourceLocation VIGNETTE = new ResourceLocation("vaz2109", "textures/misc/mask_vignette.png");
   private static final ResourceLocation DESATURATE = new ResourceLocation("shaders/post/desaturate.json");
   private static final int PINK = 16726696;
   private static final int CYAN = 3860735;
   private static final int TITLE_TICKS = 70;
   private static Item lastHead;
   private static ItemStack title = ItemStack.EMPTY;
   private static int titleTicks;
   private static int combo;
   private static int comboPoints;
   private static int comboTotal;
   private static int comboTicks;
   private static int comboWindow = 1;
   private static boolean desaturated;

   private MaskClient() {
   }

   public static void onCombo(ComboS2C msg) {
      combo = msg.combo;
      comboPoints = msg.points;
      comboTotal = msg.total;
      comboWindow = Math.max(1, msg.window);
      comboTicks = comboWindow + 30;
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer player = mc.player;
         if (titleTicks > 0) {
            titleTicks--;
         }

         if (comboTicks > 0) {
            comboTicks--;
         }

         if (player == null) {
            lastHead = null;
            desaturated = false;
         } else {
            ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
            if (head.getItem() != lastHead) {
               if (head.getItem() instanceof HeadwearItem && lastHead != null) {
                  title = head.copy();
                  titleTicks = 70;
               }

               lastHead = head.getItem();
            }

            boolean bull = Mask.worn(player) == Mask.RUSSELL;
            PostChain effect = mc.gameRenderer.currentEffect();
            if (bull && effect == null) {
               mc.gameRenderer.loadEffect(DESATURATE);
               desaturated = true;
            } else if (!bull && desaturated) {
               if (effect != null && effect.getName().equals(DESATURATE.toString())) {
                  mc.gameRenderer.shutdownEffect();
               }

               desaturated = false;
            }
         }
      }
   }

   @SubscribeEvent
   public static void onFov(ComputeFovModifierEvent event) {
      if (Mask.worn(event.getPlayer()) == Mask.GEORGE) {
         event.setNewFovModifier(event.getNewFovModifier() * 1.15F);
      }
   }

   @SubscribeEvent
   public static void onMovementInput(MovementInputUpdateEvent event) {
      if (Mask.worn(event.getEntity()) == Mask.NIGEL) {
         Input input = event.getInput();
         input.forwardImpulse = -input.forwardImpulse;
         input.leftImpulse = -input.leftImpulse;
         boolean up = input.up;
         input.up = input.down;
         input.down = up;
         boolean left = input.left;
         input.left = input.right;
         input.right = left;
      }
   }

   public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null) {
         if (Mask.worn(player) == Mask.OSCAR) {
            RenderSystem.enableBlend();
            g.blit(VIGNETTE, 0, 0, 0, 0.0F, 0.0F, width, height, width, height);
            g.blit(VIGNETTE, 0, 0, 0, 0.0F, 0.0F, width, height, width, height);
            RenderSystem.disableBlend();
         }

         if (!mc.options.hideGui) {
            float time = (float)player.tickCount + partialTick;
            if (titleTicks > 0 && !title.isEmpty()) {
               renderTitle(g, mc.font, width, height, time, ((float)titleTicks - partialTick) / 70.0F);
            }

            if (comboTicks > 0 && combo > 0) {
               renderCombo(g, mc.font, width, height, time, ((float)comboTicks - partialTick) / ((float)comboWindow + 30.0F));
            }
         }
      }
   }

   private static int alpha(float fade) {
      return Mth.clamp((int)(fade * 255.0F), 4, 255) << 24;
   }

   private static void renderTitle(GuiGraphics g, Font font, int width, int height, float time, float left) {
      float fade = Math.min(1.0F, left * 4.0F) * Math.min(1.0F, (1.0F - left) * 10.0F + 0.2F);
      int a = alpha(fade);
      String name = title.getItem() instanceof MaskItem mask
         ? Component.translatable("mask.vaz2109." + mask.mask.id + ".name").getString()
         : title.getHoverName().getString();
      name = name.toUpperCase(Locale.ROOT);
      g.pose().pushPose();
      g.pose().translate((float)width / 2.0F, (float)height * 0.28F, 0.0F);
      g.pose().mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 0.12F) * 4.0F));
      float scale = 3.2F + Mth.sin(time * 0.2F) * 0.15F;
      g.pose().scale(scale, scale, 1.0F);
      int w = font.width(name);
      g.drawString(font, name, -w / 2 + 1, 1, a | 3860735, false);
      g.drawString(font, name, -w / 2, 0, a | 16726696, false);
      g.pose().popPose();
      Component sub = title.getItem() instanceof MaskItem maskx
         ? Component.translatable(maskx.mask == Mask.PHIL ? "mask.vaz2109.phil.french" : "mask.vaz2109." + maskx.mask.id + ".ability")
         : (title.getItem() instanceof com.bobux.vaz2109.item.PartyHatItem ? Component.translatable("tooltip.vaz2109.party_hat") : Component.empty());
      g.pose().pushPose();
      g.pose().translate((float)width / 2.0F, (float)height * 0.28F + 22.0F, 0.0F);
      g.pose().scale(1.2F, 1.2F, 1.0F);
      int line = 0;

      for (FormattedCharSequence part : font.split(sub, (int)((float)width * 0.8F / 1.2F))) {
         g.drawString(font, part, -font.width(part) / 2, line * 10, a | 16777215, true);
         line++;
      }

      g.pose().popPose();
   }

   private static void renderCombo(GuiGraphics g, Font font, int width, int height, float time, float left) {
      int a = alpha(Math.min(1.0F, left * 3.0F));
      String count = Component.translatable("hud.vaz2109.combo", new Object[]{combo}).getString();
      g.pose().pushPose();
      float top = (float)height * 0.3F;
      g.pose().translate((float)width - 14.0F, top, 0.0F);
      g.pose().mulPose(Axis.ZP.rotationDegrees(-6.0F + Mth.sin(time * 0.3F) * 2.0F));
      float scale = 2.4F + (combo > 1 ? Mth.sin(time * 0.5F) * 0.12F : 0.0F);
      g.pose().scale(scale, scale, 1.0F);
      int w = font.width(count);
      g.drawString(font, count, -w + 1, 1, a | 3860735, false);
      g.drawString(font, count, -w, 0, a | (combo > 1 ? 16769088 : 16726696), false);
      g.pose().popPose();
      String pts = "+" + comboPoints + Component.translatable("hud.vaz2109.points").getString();
      g.drawString(font, pts, width - 12 - font.width(pts), (int)top + 24, a | 16777215, true);
      String total = String.valueOf(comboTotal);
      g.drawString(font, total, width - 12 - font.width(total), (int)top + 36, a | 11579568, true);
   }
}
