package com.bobux.vaz2109.client;

import com.bobux.vaz2109.item.FrostmourneItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.minecraftforge.client.event.ScreenEvent.Opening;
import net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(
   modid = "vaz2109",
   value = {Dist.CLIENT}
)
public final class CurseClient {
   private static final ResourceLocation VIGNETTE = new ResourceLocation("vaz2109", "textures/misc/mask_vignette.png");
   private static final double SEARCH = 20.0;
   private static int ticks;
   private static int total = 1;
   private static int targetId = -1;
   private static float wander;
   private static int mode;

   private CurseClient() {
   }

   public static boolean active() {
      return ticks > 0;
   }

   /** Sukuna with 15+ fingers: never punches or swings his arms, only slashes (cast by the server), and kites. */
   private static boolean slashesOnly() {
      return mode == 1 && RoleClient.fingers() >= 15;
   }

   private static int strafe = 1;

   public static void possess(int duration, int how) {
      mode = how;
      ticks = duration;
      total = Math.max(1, duration);
      targetId = -1;
      Minecraft mc = Minecraft.getInstance();
      if (duration > 0 && mc.player != null) {
         mc.player.playSound(how == 1 ? SoundEvents.WITHER_AMBIENT : SoundEvents.ELDER_GUARDIAN_CURSE, 0.6F, 0.6F);
      }
   }

   @Nullable
   private static LivingEntity target(LocalPlayer player) {
      if ((targetId >= 0 && player.level() != null ? player.level().getEntity(targetId) : null) instanceof LivingEntity living
         && living.isAlive()
         && (double)living.distanceTo(player) < 24.0) {
         return living;
      }

      LivingEntity best = null;
      double bestDist = Double.MAX_VALUE;

      for (LivingEntity e : player.level()
         .getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(20.0),
            ex -> ex != player && ex.isAlive() && !ex.isSpectator() && !(ex instanceof ArmorStand) && !ex.isInvisible()
         )) {
         double d = e.distanceToSqr(player);
         if (d < bestDist && player.hasLineOfSight(e)) {
            bestDist = d;
            best = e;
         }
      }

      targetId = best != null ? best.getId() : -1;
      return best;
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.START && ticks > 0) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer player = mc.player;
         if (player != null && player.isAlive()) {
            ticks--;

            for (int i = 0; i < 9; i++) {
               if (mode == 0 ? player.getInventory().getItem(i).getItem() instanceof FrostmourneItem : player.getInventory().getItem(i).isEmpty()) {
                  player.getInventory().selected = i;
                  break;
               }
            }

            while (mc.options.keyDrop.consumeClick()) {
            }

            if (mc.screen != null && !(mc.screen instanceof PauseScreen) && !(mc.screen instanceof DeathScreen)) {
               mc.setScreen(null);
            }

            LivingEntity target = target(player);
            if (target != null
               && !slashesOnly()
               && mc.gameMode != null
               && player.distanceTo(target) < 3.1F
               && player.getAttackStrengthScale(0.5F) >= 0.95F
               && player.hasLineOfSight(target)) {
               mc.gameMode.attack(player, target);
               player.swing(InteractionHand.MAIN_HAND);
            }

            if (ticks % 25 == 0) {
               player.level()
                  .playLocalSound(
                     player.getX(),
                     player.getY(),
                     player.getZ(),
                     SoundEvents.SOUL_ESCAPE,
                     SoundSource.PLAYERS,
                     0.7F,
                     0.4F + player.getRandom().nextFloat() * 0.3F,
                     false
                  );
            }
         } else {
            ticks = 0;
         }
      }
   }

   @SubscribeEvent
   public static void onRenderTick(RenderTickEvent event) {
      if (event.phase == Phase.START && ticks > 0) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer player = mc.player;
         if (player != null && !mc.isPaused()) {
            Entity e = targetId >= 0 ? player.level().getEntity(targetId) : null;
            float dYaw;
            float dPitch;
            if (e != null) {
               Vec3 d = e.getEyePosition().add(0.0, -0.3, 0.0).subtract(player.getEyePosition());
               float yaw = (float)(Mth.atan2(d.z, d.x) * 180.0F / (float)Math.PI) - 90.0F;
               float pitch = (float)(-Mth.atan2(d.y, d.horizontalDistance()) * 180.0F / (float)Math.PI);
               dYaw = Mth.wrapDegrees(yaw - player.getYRot()) * 0.2F;
               dPitch = (pitch - player.getXRot()) * 0.2F;
            } else {
               wander += 0.03F;
               dYaw = Mth.sin(wander) * 1.2F;
               dPitch = -player.getXRot() * 0.05F;
            }

            player.turn((double)(dYaw / 0.15F), (double)(dPitch / 0.15F));
         }
      }
   }

   @SubscribeEvent
   public static void onMovementInput(MovementInputUpdateEvent event) {
      if (ticks > 0 && event.getEntity() == Minecraft.getInstance().player) {
         LocalPlayer player = Minecraft.getInstance().player;
         Entity e = targetId >= 0 ? player.level().getEntity(targetId) : null;
         float dist = e != null ? player.distanceTo(e) : 99.0F;
         if (slashesOnly() && e != null) {
            // keep the enemy at slashing range: back off when it closes in, circle it, close the gap when it runs
            if (player.tickCount % 45 == 0 || player.horizontalCollision && player.tickCount % 8 == 0) {
               strafe = -strafe;
            }

            if (dist < 6.0F) {
               event.getInput().forwardImpulse = -1.0F;
               event.getInput().leftImpulse = 0.6F * (float)strafe;
            } else if (dist > 12.0F) {
               event.getInput().forwardImpulse = 1.0F;
               event.getInput().leftImpulse = 0.3F * (float)strafe;
            } else {
               event.getInput().forwardImpulse = 0.0F;
               event.getInput().leftImpulse = (float)strafe;
            }

            event.getInput().jumping = player.horizontalCollision && player.onGround();
            event.getInput().shiftKeyDown = false;
            player.setSprinting(dist > 14.0F);
            return;
         }

         event.getInput().forwardImpulse = e == null ? 0.5F : (dist > 1.8F ? 1.0F : 0.0F);
         event.getInput().leftImpulse = 0.0F;
         event.getInput().jumping = player.horizontalCollision && player.onGround();
         event.getInput().shiftKeyDown = false;
         player.setSprinting(e != null && dist > 5.0F);
      }
   }

   @SubscribeEvent
   public static void onInteraction(InteractionKeyMappingTriggered event) {
      if (ticks > 0) {
         event.setCanceled(true);
         event.setSwingHand(false);
      }
   }

   @SubscribeEvent
   public static void onScreen(Opening event) {
      if (ticks > 0 && !(event.getNewScreen() instanceof PauseScreen) && !(event.getNewScreen() instanceof DeathScreen)) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onCamera(ComputeCameraAngles event) {
      if (ticks > 0) {
         double time = (double)(total - ticks) + event.getPartialTick();
         event.setRoll(event.getRoll() + (float)Math.sin(time * 0.21) * 3.5F);
      }
   }

   @SubscribeEvent
   public static void onFov(ComputeFovModifierEvent event) {
      if (ticks > 0) {
         event.setNewFovModifier(event.getNewFovModifier() * (1.08F + Mth.sin((float)(total - ticks) * 0.4F) * 0.03F));
      }
   }

   public static void reset() {
      ticks = 0;
      targetId = -1;
   }

   public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
      if (ticks > 0) {
         Minecraft mc = Minecraft.getInstance();
         float time = (float)(total - ticks) + partialTick;
         float fade = Math.min(1.0F, Math.min(time / 10.0F, (float)ticks / 10.0F));
         float pulse = 0.65F + 0.35F * Mth.sin(time * 0.35F);
         RenderSystem.enableBlend();
         if (mode == 1) {
            RenderSystem.setShaderColor(1.0F, 0.1F, 0.12F, fade * pulse);
         } else {
            RenderSystem.setShaderColor(0.35F, 0.85F, 1.0F, fade * pulse);
         }

         g.blit(VIGNETTE, 0, 0, 0, 0.0F, 0.0F, width, height, width, height);
         g.blit(VIGNETTE, 0, 0, 0, 0.0F, 0.0F, width, height, width, height);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
         g.fill(0, 0, width, height, (int)(fade * 40.0F) << 24 | (mode == 1 ? 4194312 : 667728));
         Component text = Component.translatable(mode == 1 ? "hud.vaz2109.sukuna.possessed" : "hud.vaz2109.frostmourne.possessed");
         float shake = 1.5F;
         int x = width / 2 + (int)((mc.level != null ? mc.level.random.nextFloat() - 0.5F : 0.0F) * shake * 2.0F);
         g.pose().pushPose();
         g.pose().translate((float)x, (float)height * 0.22F, 0.0F);
         g.pose().scale(1.6F, 1.6F, 1.0F);
         g.drawCenteredString(mc.font, text, 0, 0, Mth.clamp((int)(fade * 255.0F), 8, 255) << 24 | (mode == 1 ? 16724032 : 9433087));
         g.pose().popPose();
      }
   }

   public static void renderHunger(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
      Minecraft mc = Minecraft.getInstance();
      if (!mc.options.hideGui && mc.player != null) {
         ItemStack stack = mc.player.getMainHandItem();
         if (!(stack.getItem() instanceof FrostmourneItem)) {
            stack = ItemStack.EMPTY;

            for (ItemStack s : mc.player.getInventory().items) {
               if (s.getItem() instanceof FrostmourneItem) {
                  stack = s;
                  break;
               }
            }

            if (stack.isEmpty()) {
               return;
            }
         }

         float f = (float)FrostmourneItem.hunger(stack) / 600.0F;
         int w = 104;
         int h = 26;
         int x = width / 2 + 96;
         int y = height - h - 3;
         boolean starving = f >= 0.8F || ticks > 0;
         boolean blink = mc.level == null || mc.level.getGameTime() % 10L < 5L;
         int accent = f < 0.5F ? 5220560 : (f < 0.8F ? 9071320 : 13647968);
         HudKit.panel(g, x, y, w, h, accent);
         g.pose().pushPose();
         g.pose().translate((float)(x + 5), (float)(y + 5), 0.0F);
         g.renderItem(stack, 0, 0);
         g.pose().popPose();
         int labelColor = starving && blink ? -40848 : -6299649;
         Component label = Component.translatable(ticks > 0 ? "hud.vaz2109.frostmourne.taken" : "hud.vaz2109.frostmourne.hunger");
         g.drawString(mc.font, mc.font.plainSubstrByWidth(label.getString(), w - 30), x + 25, y + 4, labelColor, true);
         HudKit.bar(g, x + 25, y + 16, w - 31, 4, f, accent, 0.8F);
         if (starving) {
            g.fill(x, y, x + w, y + h, HudKit.alpha(13647968, blink ? 0.12F : 0.04F));
         }
      }
   }
}
