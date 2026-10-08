package com.bobux.vaz2109.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Sandevistan on the client, as in Edgerunners: a trail of translucent, colour-shifting copies of the runner
 * left behind every other tick, seen by everyone nearby; and a kick of the field of view when it fires.
 */
@EventBusSubscriber(modid = "vaz2109", value = Dist.CLIENT)
public final class SandeClient {
   private static final int GHOSTS = 9;
   private static final int SPACING = 2;
   private static final Map<Integer, SandeClient.Run> RUNS = new HashMap<>();
   private static int ticks;
   /** Own buffer, so the ghosts never flush or reorder the level's shared batches. */
   private static final MultiBufferSource.BufferSource GHOST_BUFFER = MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.BufferBuilder(4096));

   private SandeClient() {
   }

   public static void start(int entity, int length) {
      RUNS.put(entity, new SandeClient.Run(ticks + length));
   }

   private static boolean localRunning() {
      Minecraft mc = Minecraft.getInstance();
      return mc.player != null && RUNS.containsKey(mc.player.getId());
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase != Phase.END) {
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         RUNS.clear();
         return;
      }

      ticks++;
      Iterator<Map.Entry<Integer, SandeClient.Run>> it = RUNS.entrySet().iterator();

      while (it.hasNext()) {
         Map.Entry<Integer, SandeClient.Run> e = it.next();
         SandeClient.Run run = e.getValue();
         Entity entity = mc.level.getEntity(e.getKey());
         boolean running = ticks < run.end;
         if (entity instanceof AbstractClientPlayer player && running && ticks % SPACING == 0) {
            run.trail.addFirst(new SandeClient.Ghost(player.position(), player.yBodyRot, player.yHeadRot, player.getXRot(),
               player.walkAnimation.position(), player.walkAnimation.speed(), (float)player.tickCount, ticks));
            while (run.trail.size() > GHOSTS) {
               run.trail.removeLast();
            }
         } else if (!running && ticks % SPACING == 0 && !run.trail.isEmpty()) {
            run.trail.removeLast();
         }

         if (!running && run.trail.isEmpty() || entity == null) {
            it.remove();
         }
      }
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || RUNS.isEmpty()) {
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      Vec3 cam = event.getCamera().getPosition();
      PoseStack pose = event.getPoseStack();
      MultiBufferSource.BufferSource buffers = GHOST_BUFFER;
      float now = (float)ticks + event.getPartialTick();

      for (Map.Entry<Integer, SandeClient.Run> e : RUNS.entrySet()) {
         if (!(mc.level.getEntity(e.getKey()) instanceof AbstractClientPlayer player)) {
            continue;
         }

         EntityRenderer<? super AbstractClientPlayer> renderer = mc.getEntityRenderDispatcher().getRenderer(player);
         if (!(renderer instanceof PlayerRenderer pr)) {
            continue;
         }

         PlayerModel<AbstractClientPlayer> model = pr.getModel();
         VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation()));
         boolean self = player == mc.player && mc.options.getCameraType().isFirstPerson();
         int i = 0;

         for (SandeClient.Ghost g : e.getValue().trail) {
            i++;
            if (self && i < 3 || g.pos.distanceToSqr(player.position()) < 0.04) {
               continue; // not inside the camera, not on top of the runner
            }

            float age = (now - (float)g.time) / (float)(GHOSTS * SPACING);
            float alpha = Mth.clamp(0.55F * (1.0F - age), 0.0F, 0.55F);
            if (alpha <= 0.02F) {
               continue;
            }

            int rgb = Mth.hsvToRgb((g.time * 0.045F + 0.35F) % 1.0F, 0.8F, 1.0F);
            pose.pushPose();
            pose.translate(g.pos.x - cam.x, g.pos.y - cam.y, g.pos.z - cam.z);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - g.body));
            pose.scale(-1.0F, -1.0F, 1.0F);
            pose.scale(0.9375F, 0.9375F, 0.9375F);
            pose.translate(0.0F, -1.501F, 0.0F);
            model.attackTime = 0.0F;
            model.crouching = false;
            model.riding = false;
            model.young = false;
            model.setupAnim(player, g.limb, g.limbSpeed, g.age, Mth.wrapDegrees(g.head - g.body), g.pitch);
            model.renderToBuffer(pose, vc, 15728880, OverlayTexture.NO_OVERLAY,
               (float)(rgb >> 16 & 255) / 255.0F, (float)(rgb >> 8 & 255) / 255.0F, (float)(rgb & 255) / 255.0F, alpha);
            pose.popPose();
         }

         buffers.endBatch();
      }
   }

   /** The view stretches as time breaks, then settles a little wider than normal while it runs. */
   @SubscribeEvent
   public static void onFov(ComputeFovModifierEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && event.getPlayer() == mc.player && localRunning()) {
         SandeClient.Run run = RUNS.get(mc.player.getId());
         int left = run.end - ticks;
         int since = com.bobux.vaz2109.cyber.Cyberware.SANDE_TICKS - left;
         float kick = since < 8 ? 1.0F + 0.25F * Mth.sin((float)since / 8.0F * (float)Math.PI) : 1.0F;
         event.setNewFovModifier(event.getNewFovModifier() * kick * (left > 0 ? 1.06F : 1.0F));
      }
   }

   private static final class Run {
      final int end;
      final ArrayDeque<SandeClient.Ghost> trail = new ArrayDeque<>();

      Run(int end) {
         this.end = end;
      }
   }

   private record Ghost(Vec3 pos, float body, float head, float pitch, float limb, float limbSpeed, float age, int time) {
   }
}
