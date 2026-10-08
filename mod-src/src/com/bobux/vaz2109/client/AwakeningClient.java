package com.bobux.vaz2109.client;

import com.bobux.vaz2109.client.fx.FxClient;
import com.bobux.vaz2109.network.AwakeningS2C;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Sukuna's awakening on the client: red flashes and shakes on the hits of the track, and the cutscene of him
 * rising into the sky and expanding his domain. The camera follows Sukuna wherever he actually is.
 */
@EventBusSubscriber(modid = "vaz2109", value = Dist.CLIENT)
public final class AwakeningClient {
   /** Cutscene length past the domain opening, so the shrine is seen appearing. */
   private static final int TAIL = 30;
   private static int t = -1;
   private static int length;
   private static int sukunaId = -1;
   private static Vec3 ground = Vec3.ZERO;
   private static float yaw;
   private static ArmorStand camera;
   private static CameraType previousType;
   private static float flash;
   private static float flashO;
   /** Ticks of the awakening track still playing: vanilla music stays off meanwhile. */
   private static int trackLeft;

   private AwakeningClient() {
   }

   public static boolean active() {
      return t >= 0;
   }

   public static void handle(AwakeningS2C msg) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || mc.player == null) {
         return;
      }

      switch (msg.kind) {
         case AwakeningS2C.TRACK -> {
            trackLeft = 2560;
            mc.getMusicManager().stopPlaying();
         }
         case AwakeningS2C.PULSE -> {
            FxClient.addShake(msg.value);
            flash = Math.min(1.0F, flash + msg.value * 0.8F);
         }
         case AwakeningS2C.CUTSCENE -> start(msg.entity, (int)msg.value);
      }
   }

   private static void start(int entity, int untilDomain) {
      Minecraft mc = Minecraft.getInstance();
      Entity sukuna = mc.level.getEntity(entity);
      if (sukuna == null || active() || CutsceneClient.active()) {
         return;
      }

      sukunaId = entity;
      ground = sukuna.position();
      yaw = sukuna.getYRot();
      length = untilDomain + TAIL;
      camera = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
      camera.setInvisible(true);
      camera.setNoGravity(true);
      t = 0;
      place(true, sukuna.position());
      previousType = mc.options.getCameraType();
      mc.options.setCameraType(CameraType.FIRST_PERSON);
      mc.setCameraEntity(camera);
   }

   private static void stop() {
      Minecraft mc = Minecraft.getInstance();
      t = -1;
      camera = null;
      if (mc.player != null) {
         mc.setCameraEntity(mc.player);
      }

      if (previousType != null) {
         mc.options.setCameraType(previousType);
         previousType = null;
      }
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase != Phase.START) {
         return;
      }

      if (trackLeft > 0 && --trackLeft % 20 == 0) {
         Minecraft.getInstance().getMusicManager().stopPlaying();
      }

      flashO = flash;
      flash = Math.max(0.0F, flash * 0.82F - 0.01F);
      if (active()) {
         Minecraft mc = Minecraft.getInstance();
         Entity sukuna = mc.level != null ? mc.level.getEntity(sukunaId) : null;
         if (sukuna != null && mc.player != null && mc.player.isAlive() && ++t < length) {
            place(false, sukuna.position());
         } else {
            stop();
         }
      }
   }

   private static Vec3 rotate(Vec3 local) {
      double r = Math.toRadians((double)yaw);
      double fx = -Math.sin(r);
      double fz = Math.cos(r);
      return new Vec3(fz * local.x + fx * local.z, local.y, -fx * local.x + fz * local.z);
   }

   private static float ease(float a, float b) {
      float f = Mth.clamp(((float)t - a) / (b - a), 0.0F, 1.0F);
      return f * f * (3.0F - 2.0F * f);
   }

   /** Camera shots: from the ground looking up, a slow orbit, a close-up, then a fast pull back as the domain opens. */
   private static void place(boolean first, Vec3 at) {
      int domain = length - TAIL;
      Vec3 pos;
      Vec3 look = at.add(0.0, 1.4, 0.0);
      boolean cut;
      if (t < 50) {
         pos = ground.add(rotate(new Vec3(4.0, 0.6, 7.0)));
         cut = t == 0;
      } else if (t < 90) {
         double a = Math.toRadians(Mth.lerp(ease(50.0F, 90.0F), 20.0F, 200.0F));
         pos = at.add(Math.sin(a) * 9.0, 1.0 + 2.5 * ease(50.0F, 90.0F), Math.cos(a) * 9.0);
         cut = t == 50;
      } else if (t < domain) {
         pos = at.add(rotate(new Vec3(0.0, 1.6, 2.6 + 0.6 * ease(90.0F, (float)domain))));
         cut = t == 90;
      } else {
         float e = ease((float)domain, (float)length);
         pos = at.add(rotate(new Vec3(0.0, 1.6 + 14.0 * e, 2.6 + 34.0 * e)));
         look = at.add(0.0, 1.4 - 10.0 * e, 0.0);
         cut = false;
      }

      Vec3 d = look.subtract(pos);
      float newYaw = (float)(Mth.atan2(d.z, d.x) * 180.0F / (float)Math.PI) - 90.0F;
      float newPitch = (float)(-Mth.atan2(d.y, d.horizontalDistance()) * 180.0F / (float)Math.PI);
      double y = pos.y - (double)camera.getEyeHeight();
      if (first || cut) {
         camera.setPos(pos.x, y, pos.z);
         camera.xo = pos.x;
         camera.yo = y;
         camera.zo = pos.z;
         camera.setYRot(newYaw);
         camera.yRotO = newYaw;
         camera.setXRot(newPitch);
         camera.xRotO = newPitch;
      } else {
         camera.xo = camera.getX();
         camera.yo = camera.getY();
         camera.zo = camera.getZ();
         camera.setPos(pos.x, y, pos.z);
         camera.yRotO = camera.getYRot();
         camera.xRotO = camera.getXRot();
         camera.setYRot(camera.getYRot() + Mth.wrapDegrees(newYaw - camera.getYRot()));
         camera.setXRot(newPitch);
      }
   }

   /** While Sukuna hangs in the sky he does not walk. */
   @SubscribeEvent(priority = EventPriority.LOW)
   public static void onMovementInput(MovementInputUpdateEvent event) {
      Minecraft mc = Minecraft.getInstance();
      boolean hovering = CurseClient.active() && mc.player != null && mc.player.hasEffect(MobEffects.LEVITATION);
      if (active() || hovering) {
         event.getInput().forwardImpulse = 0.0F;
         event.getInput().leftImpulse = 0.0F;
         event.getInput().jumping = false;
         event.getInput().shiftKeyDown = false;
      }
   }

   @SubscribeEvent
   public static void onRenderHand(RenderHandEvent event) {
      if (active()) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onOverlay(Pre event) {
      if (active() && !event.getOverlay().id().getPath().equals("awakening")) {
         event.setCanceled(true);
      }
   }

   public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
      Minecraft mc = Minecraft.getInstance();
      float f = Mth.lerp(partialTick, flashO, flash);
      if (f > 0.01F) {
         int a = (int)(Mth.clamp(f, 0.0F, 1.0F) * 110.0F);
         int edge = Math.max(8, width / 6);
         g.fillGradient(0, 0, width, edge, a << 24 | 0x8A0006, 0x008A0006);
         g.fillGradient(0, height - edge, width, height, 0x008A0006, a << 24 | 0x8A0006);
         g.fill(0, 0, width, height, (int)(a * 0.35F) << 24 | 0x5A0004);
      }

      if (!active()) {
         return;
      }

      float time = (float)t + partialTick;
      int domain = length - TAIL;
      int bar = (int)((float)height * 0.12F);
      g.fill(0, 0, width, bar, -16777216);
      g.fill(0, height - bar, width, height, -16777216);
      if (time > 60.0F && time < (float)domain) {
         float a = Math.min(1.0F, Math.min((time - 60.0F) / 8.0F, ((float)domain - time) / 4.0F));
         int alpha = Mth.clamp((int)(a * 255.0F), 8, 255) << 24;
         g.drawCenteredString(mc.font, Component.translatable("entity.vaz2109.sukuna"), width / 2, height - bar + 6, alpha | 0xD02020);
         g.drawCenteredString(mc.font, Component.translatable("hud.vaz2109.sukuna.domain_line"), width / 2, height - bar + 18, alpha | 0xF0E8E8);
      }

      if (time >= (float)domain) {
         float a = Math.min(1.0F, (time - (float)domain) / 4.0F);
         int alpha = Mth.clamp((int)(a * 255.0F), 8, 255) << 24;
         g.pose().pushPose();
         g.pose().translate((float)width / 2.0F, (float)height * 0.42F, 0.0F);
         g.pose().scale(2.6F, 2.6F, 1.0F);
         g.drawCenteredString(mc.font, Component.translatable("hud.vaz2109.sukuna.domain_title"), 0, 0, alpha | 0xE01818);
         g.pose().popPose();
         g.drawCenteredString(mc.font, Component.translatable("hud.vaz2109.sukuna.domain_sub"), width / 2, (int)((float)height * 0.42F) + 30, alpha | 0xF0D0D0);
      }

      float black = Math.max(1.0F - time / 8.0F, 0.0F);
      black = Math.max(black, ((float)length - time - 6.0F) < 0.0F ? (6.0F - ((float)length - time)) / 6.0F : 0.0F);
      if (time >= (float)domain && time < (float)domain + 3.0F) {
         // the white flash of the domain opening
         g.fill(0, 0, width, height, (int)((1.0F - (time - (float)domain) / 3.0F) * 230.0F) << 24 | 0xFFFFFF);
      }

      if (black > 0.0F) {
         g.fill(0, 0, width, height, Mth.clamp((int)(black * 255.0F), 0, 255) << 24);
      }
   }
}
