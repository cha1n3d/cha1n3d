package com.bobux.vaz2109.item;

import com.bobux.vaz2109.entity.BossResistance;
import com.bobux.vaz2109.fx.ModSounds;
import com.bobux.vaz2109.network.CursedFxS2C;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

@EventBusSubscriber(
   modid = "vaz2109"
)
public class SlaughterDemonItem extends CursedToolItem {
   public static final float CHANCE = 0.08F;
   public static final float ZONE_CHANCE = 0.2F;
   /** Black Flash damage multiplier (knife crit and Vessel fists alike). */
   public static final float MULTIPLIER = 2.0F;
   /** "In the zone" after a Black Flash: 10 s, not extended by further flashes. */
   public static final int ZONE = 200;
   /** At least 2 s between two Black Flashes. */
   public static final int GAP = 40;
   private static final DustParticleOptions BLACK = new DustParticleOptions(new Vector3f(0.02F, 0.02F, 0.03F), 1.6F);
   private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(0.85F, 0.05F, 0.08F), 1.2F);
   private static final Map<Player, Float> STRENGTH = new WeakHashMap<>();

   public SlaughterDemonItem(Properties properties) {
      super(Tiers.IRON, 3, -2.0F, properties);
   }

   @Override
   public String style() {
      return "knife";
   }

   public static boolean inZone(Player player) {
      return player.level().getGameTime() - player.getPersistentData().getLong("vaz2109BlackFlash") < (long)ZONE;
   }

   /** False right after a Black Flash, so they cannot chain hit after hit. */
   public static boolean canFlash(Player player) {
      return player.level().getGameTime() - player.getPersistentData().getLong("vaz2109BlackFlashLast") >= (long)GAP;
   }

   @SubscribeEvent
   public static void onAttack(AttackEntityEvent event) {
      STRENGTH.put(event.getEntity(), event.getEntity().getAttackStrengthScale(0.5F));
   }

   @SubscribeEvent
   public static void onCritical(CriticalHitEvent event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide
         && player.getMainHandItem().getItem() instanceof SlaughterDemonItem
         && !(STRENGTH.getOrDefault(player, 0.0F) < 0.95F)
         && event.getTarget() instanceof LivingEntity target) {
         float chance = inZone(player) ? ZONE_CHANCE : CHANCE;
         if (canFlash(player) && player.getRandom().nextFloat() < chance) {
            event.setResult(Result.ALLOW);
            event.setDamageModifier(MULTIPLIER);
            blackFlash((ServerLevel)player.level(), player, target);
         }
      }
   }

   public static void blackFlash(ServerLevel server, Player player, LivingEntity target) {
      long now = server.getGameTime();
      if (!inZone(player)) {
         player.getPersistentData().putLong("vaz2109BlackFlash", now);
         player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ZONE, 0));
      }

      player.getPersistentData().putLong("vaz2109BlackFlashLast", now);
      double x = target.getX();
      double y = target.getY() + (double)target.getBbHeight() * 0.55;
      double z = target.getZ();

      for (int arm = 0; arm < 6; arm++) {
         double px = x;
         double py = y;
         double pz = z;
         double ax = server.random.nextGaussian();
         double ay = server.random.nextGaussian() * 0.6;
         double az = server.random.nextGaussian();
         double len = Math.sqrt(ax * ax + ay * ay + az * az);

         for (int k = 0; k < 8; k++) {
            px += ax / len * 0.3 + server.random.nextGaussian() * 0.12;
            py += ay / len * 0.3 + server.random.nextGaussian() * 0.12;
            pz += az / len * 0.3 + server.random.nextGaussian() * 0.12;
            server.sendParticles(k % 3 == 2 ? RED : BLACK, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }

      server.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
      server.sendParticles(ParticleTypes.SQUID_INK, x, y, z, 20, 0.3, 0.3, 0.3, 0.25);
      ModSounds.play(server, new Vec3(x, y, z), 1.2F, ModSounds.BLACK_FLASH, ModSounds.BLACK_FLASH_CRACK);
      CursedFxS2C.send(server, 7, player, target, player.position(), new Vec3(x, y, z), 15.0F);
   }

   @SubscribeEvent
   public static void onHurt(LivingHurtEvent event) {
      LivingEntity target = event.getEntity();
      if (event.getSource().getDirectEntity() instanceof Player player
         && event.getSource().is(DamageTypes.PLAYER_ATTACK)
         && player.getMainHandItem().getItem() instanceof SlaughterDemonItem
         && (target.getMobType() == MobType.UNDEAD || BossResistance.isBoss(target))) {
         event.setAmount(event.getAmount() + 4.0F);
      }
   }

   @Override
   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      this.tooltip(tooltip, "slaughter_demon", 2);
      super.appendHoverText(stack, level, tooltip, flag);
   }
}
