package com.bobux.vaz2109.entity;

import com.bobux.vaz2109.ModRegistry;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * A gopnik from the block. Comes up to a player at night: "got any sunflower seeds?". Hand him seeds (right-click)
 * and he is happy and sometimes gives something back; ignore him for ten seconds or hit him and he goes for you
 * with his bat, and his mates nearby join in.
 */
public class GopnikEntity extends Monster {
   private static final int PATIENCE = 200;
   private final Set<UUID> friends = new HashSet<>();
   @Nullable
   private UUID asked;
   private long askedAt;
   private long nextAsk;

   public GopnikEntity(EntityType<? extends GopnikEntity> type, Level level) {
      super(type, level);
      this.xpReward = 6;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 24.0)
         .add(Attributes.ATTACK_DAMAGE, 3.0)
         .add(Attributes.MOVEMENT_SPEED, 0.3)
         .add(Attributes.FOLLOW_RANGE, 24.0);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false));
      this.goalSelector.addGoal(2, new GopnikEntity.Shakedown(this));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(GopnikEntity.class));
   }

   @Nullable
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
      Item bat = ForgeRegistries.ITEMS.getValue(new ResourceLocation("vaz2109", "bat"));
      if (bat != null && this.random.nextFloat() < 0.7F) {
         this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(bat));
         this.setDropChance(EquipmentSlot.MAINHAND, 0.06F);
      }

      return super.finalizeSpawn(level, difficulty, reason, data, tag);
   }

   private void say(Player player, String key, ChatFormatting color) {
      player.displayClientMessage(Component.translatable("entity.vaz2109.gopnik").append(": ").append(Component.translatable(key)).withStyle(color), false);
   }

   private boolean friend(Player player) {
      return this.friends.contains(player.getUUID());
   }

   public InteractionResult mobInteract(Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!stack.is(ModRegistry.SNACKS.get(0).get())) {
         return super.mobInteract(player, hand);
      } else if (this.level().isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         if (!player.getAbilities().instabuild) {
            stack.shrink(1);
         }

         this.friends.add(player.getUUID());
         this.asked = null;
         if (this.getTarget() == player) {
            this.setTarget(null);
         }

         this.playSound(SoundEvents.VILLAGER_YES, 1.0F, 0.7F);
         this.say(player, "chat.vaz2109.gopnik.thanks." + this.random.nextInt(3), ChatFormatting.GREEN);
         if (this.random.nextFloat() < 0.35F) {
            String[] gifts = new String[]{"vaz2109:cigarettes", "vaz2109:beer", "vaz2109:instant_noodles", "vaz2109:banknote_100"};
            Item gift = ForgeRegistries.ITEMS.getValue(new ResourceLocation(gifts[this.random.nextInt(gifts.length)]));
            if (gift != null) {
               this.spawnAtLocation(new ItemStack(gift));
            }
         }

         return InteractionResult.CONSUME;
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      if (source.getEntity() instanceof Player player) {
         this.friends.remove(player.getUUID());
      }

      return super.hurt(source, amount);
   }

   /** Nobody gave him anything: he and his mates go for that player. */
   private void provoke(Player player) {
      this.say(player, "chat.vaz2109.gopnik.angry." + this.random.nextInt(3), ChatFormatting.RED);
      this.setTarget(player);
      this.playSound(SoundEvents.VINDICATOR_CELEBRATE, 1.0F, 0.8F);

      for (GopnikEntity mate : this.level().getEntitiesOfClass(GopnikEntity.class, this.getBoundingBox().inflate(16.0), g -> g != this && g.getTarget() == null)) {
         if (!mate.friend(player)) {
            mate.setTarget(player);
         }
      }
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.VINDICATOR_AMBIENT;
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return SoundEvents.VINDICATOR_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.VINDICATOR_DEATH;
   }

   public float getVoicePitch() {
      return 1.25F + (this.random.nextFloat() - 0.5F) * 0.2F;
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
      for (UUID id : this.friends) {
         list.add(net.minecraft.nbt.NbtUtils.createUUID(id));
      }

      tag.put("Friends", list);
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      for (net.minecraft.nbt.Tag t : tag.getList("Friends", 11)) {
         this.friends.add(net.minecraft.nbt.NbtUtils.loadUUID(t));
      }
   }

   /** Walk up to the nearest player who is not a friend yet and ask; wait; then either back off or start a fight. */
   static final class Shakedown extends Goal {
      private final GopnikEntity gopnik;
      @Nullable
      private Player player;

      Shakedown(GopnikEntity gopnik) {
         this.gopnik = gopnik;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      }

      public boolean canUse() {
         if (this.gopnik.getTarget() != null || this.gopnik.level().getGameTime() < this.gopnik.nextAsk) {
            return false;
         }

         this.player = this.gopnik.level().getNearestPlayer(this.gopnik, 14.0);
         return this.player != null && !this.player.isCreative() && !this.player.isSpectator() && !this.gopnik.friend(this.player);
      }

      public boolean canContinueToUse() {
         return this.player != null && this.player.isAlive() && this.gopnik.getTarget() == null && !this.gopnik.friend(this.player)
            && this.gopnik.distanceToSqr(this.player) < 400.0;
      }

      public void stop() {
         this.gopnik.asked = null;
         this.gopnik.nextAsk = this.gopnik.level().getGameTime() + 400L;
         this.player = null;
         this.gopnik.getNavigation().stop();
      }

      public void tick() {
         Player p = this.player;
         if (p == null) {
            return;
         }

         this.gopnik.getLookControl().setLookAt(p, 30.0F, 30.0F);
         long now = this.gopnik.level().getGameTime();
         if (this.gopnik.distanceToSqr(p) > 9.0) {
            this.gopnik.getNavigation().moveTo(p, 1.0);
         } else {
            this.gopnik.getNavigation().stop();
            if (this.gopnik.asked == null) {
               this.gopnik.asked = p.getUUID();
               this.gopnik.askedAt = now;
               this.gopnik.say(p, "chat.vaz2109.gopnik.ask." + this.gopnik.random.nextInt(4), ChatFormatting.GOLD);
               this.gopnik.playSound(SoundEvents.VINDICATOR_AMBIENT, 1.0F, 1.3F);
            } else if (now - this.gopnik.askedAt == (long)(PATIENCE / 2)) {
               this.gopnik.say(p, "chat.vaz2109.gopnik.wait", ChatFormatting.GOLD);
            } else if (now - this.gopnik.askedAt >= (long)PATIENCE) {
               this.gopnik.provoke(p);
            }
         }
      }
   }

   public static boolean canSpawn(EntityType<GopnikEntity> type, ServerLevelAccessor level, MobSpawnType reason, net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
      return Monster.checkMonsterSpawnRules(type, (ServerLevel)level.getLevel(), reason, pos, random) && random.nextInt(3) == 0;
   }
}
