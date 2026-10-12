package com.fallenroses.event;

import com.fallenroses.config.FRConfig;
import com.fallenroses.entity.curse.CurseEntity;
import com.fallenroses.entity.curse.CursedSpirit;
import com.fallenroses.item.CursedToolItem;
import com.fallenroses.player.FRAdvancements;
import com.fallenroses.player.PlayerClass;
import com.fallenroses.player.PlayerData;
import com.fallenroses.player.PlayerDataCap;
import com.fallenroses.player.Progression;
import com.fallenroses.player.Talent;
import com.fallenroses.registry.ModDamageTypes;
import com.fallenroses.registry.ModEffects;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.registry.ModTags;
import com.fallenroses.technique.BlackFlash;
import com.fallenroses.technique.Combat;
import com.fallenroses.technique.impl.BloodManipulation;
import com.fallenroses.technique.impl.Construction;
import com.fallenroses.technique.impl.CurseManipulation;
import com.fallenroses.technique.impl.DeadlySentencing;
import com.fallenroses.technique.impl.Heavenly;
import com.fallenroses.technique.impl.Lightning;
import com.fallenroses.technique.impl.Projection;
import com.fallenroses.technique.impl.Rika;
import com.fallenroses.technique.impl.StarRage;
import com.fallenroses.util.Scheduler;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(
    modid = "fallenroses"
)
public final class CombatEvents {
    @SubscribeEvent
    public static void attack(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity living && isParalyzed(living) && event.getSource().getDirectEntity() == attacker) {
            event.setCanceled(true);
            return;
        }

        if (event.getEntity() instanceof ServerPlayer target && attacker != target) {
            PlayerData data = PlayerDataCap.get(target);
            if (data == null) {
                return;
            }

            DamageSource source = event.getSource();
            if (attacker instanceof LivingEntity thornsVictim && thornsVictim != target && source.getDirectEntity() == attacker) {
                Construction.thorns(target, thornsVictim);
            }

            if (attacker instanceof LivingEntity la && la != target && DeadlySentencing.tryObjection(target, la)) {
                event.setCanceled(true);
                return;
            }

            if (data.hasTalent(Talent.SENSES)
                && attacker instanceof LivingEntity
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && target.getRandom().nextFloat() < 0.12F) {
                event.setCanceled(true);
                Vfx.play(target.serverLevel(), VfxKind.HIT_SPARK, target.getBoundingBox().getCenter(), target.getLookAngle(), 0.7F, 15259808, -1, 0);
                Combat.sound(target, target.position(), ModSounds.DASH, 0.5F, 1.6F);
                return;
            }

            if (!data.isInfinity()) {
                return;
            }

            boolean blockable = source.getDirectEntity() instanceof Projectile
                || attacker instanceof LivingEntity && source.getDirectEntity() == attacker
                || source.is(DamageTypeTags.IS_EXPLOSION);
            if (blockable && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                boolean free = target.getAbilities().instabuild;
                if (!free && !data.consume(4.0F)) {
                    data.setInfinity(false);
                    return;
                }

                event.setCanceled(true);
                Entity from = source.getDirectEntity() != null ? source.getDirectEntity() : attacker;
                if (from != null && target.tickCount % 3 == 0) {
                    Vfx.play(
                        target.serverLevel(),
                        VfxKind.INFINITY_RIPPLE,
                        target.getBoundingBox().getCenter().lerp(from.position(), 0.35),
                        target.position().subtract(from.position()),
                        0.9F,
                        12575999,
                        -1,
                        0
                    );
                    Combat.sound(target, target.position(), ModSounds.INFINITY_BLOCK, 0.7F, 1.3F);
                }

                return;
            }

            return;
        }
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        float amount = event.getAmount();
        boolean cursed = source.is(ModDamageTypes.CURSED_TECHNIQUE);
        ServerPlayer attacker = meleeAttacker(source);
        if (attacker != null) {
            PlayerData data = PlayerDataCap.get(attacker);
            if (data != null && data.getPlayerClass() != null) {
                boolean hr = data.getPlayerClass() == PlayerClass.HEAVENLY_RESTRICTION;
                if (hr) {
                    cursed = true;
                }

                if (data.isReinforced()) {
                    amount *= 1.0F + FRConfig.REINFORCE_BONUS.get().floatValue() + (data.hasTalent(Talent.REINFORCE_MASTERY) ? 0.15F : 0.0F);
                    cursed = true;
                }

                Item held = attacker.getMainHandItem().getItem();
                if (held instanceof CursedToolItem tool) {
                    cursed = true;
                    if (tool.kind() == CursedToolItem.Kind.PLAYFUL_CLOUD && spend(attacker, data, 3.0F)) {
                        amount += 4.0F * data.power();
                    }

                    if (tool.kind() == CursedToolItem.Kind.INVERTED_SPEAR && target.getType().is(ModTags.CURSED_SPIRITS)) {
                        amount *= 1.5F;
                    }

                    amount = toolEffects(attacker, data, tool, target, amount);
                    Combat.sound(attacker, target.position(), ModSounds.CURSED_HIT, 0.8F, 0.9F + attacker.getRandom().nextFloat() * 0.2F);
                }

                if (data.hasTalent(Talent.WEAPON_MASTERY) && held instanceof SwordItem) {
                    amount *= 1.2F;
                }

                if (data.hasTalent(Talent.BODY_PEAK)) {
                    amount *= 1.15F;
                }

                amount *= 1.0F + (float)data.talentRank(Talent.HEAVY_BLOWS) * 0.04F + (data.hasTalent(Talent.TITAN) ? 0.1F : 0.0F);
                if (data.hasTalent(Talent.LAST_STAND) && attacker.getHealth() < attacker.getMaxHealth() * 0.3F) {
                    amount *= 1.25F;
                }

                if (data.berserkTicks > 0) {
                    amount *= 1.5F;
                }

                if (!hr) {
                    amount = BlackFlash.tryTrigger(attacker, data, target, amount);
                }

                if (data.isReinforced() && data.hasTalent(Talent.DIVERGENT_FIST)) {
                    divergentFist(attacker, target, amount);
                }

                if (Lightning.inAmber(attacker)) {
                    cursed = true;
                    Lightning.onAmberHit(attacker, data, target);
                }

                if (Rika.manifested(attacker)) {
                    cursed = true;
                    Rika.onHit(attacker, data, target);
                }
            }
        }

        if (Projection.framed(target)) {
            amount *= 1.5F;
        }

        if (DeadlySentencing.hasEvidence(target)) {
            amount *= 1.2F;
        }

        if (attacker != null) {
            PlayerData ad = PlayerDataCap.get(attacker);
            if (ad != null && ad.stationTicks > 0) {
                amount *= 1.3F;
                cursed = true;
            }
        }

        if (!cursed && target.getType().is(ModTags.CURSED_SPIRITS) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount *= FRConfig.NON_CURSED_MULTIPLIER.get().floatValue();
        }

        if (target instanceof ServerPlayer victim) {
            PlayerData vd = PlayerDataCap.get(victim);
            if (vd != null) {
                if (vd.isReinforced() && vd.hasTalent(Talent.VEIL)) {
                    amount *= 0.85F;
                }

                if (vd.hasTalent(Talent.LAST_STAND) && victim.getHealth() < victim.getMaxHealth() * 0.3F) {
                    amount *= 0.8F;
                }

                amount *= 1.0F - (float)vd.talentRank(Talent.THICK_SKIN) * 0.03F;
                if (vd.getEnergy() > vd.getMax() * 0.5F) {
                    amount *= 1.0F - (float)vd.talentRank(Talent.CE_BARRIER) * 0.03F;
                }

                if (vd.hasTalent(Talent.SIMPLE_DOMAIN) && source.is(ModDamageTypes.CURSED_TECHNIQUE)) {
                    amount *= 0.75F;
                }

                if (vd.hasTalent(Talent.SECOND_WIND) && victim.getHealth() - amount < victim.getMaxHealth() * 0.25F) {
                    long now = victim.level().getGameTime();
                    CompoundTag pd = victim.getPersistentData();
                    if (now - pd.getLong("fr_second_wind") > 2400L) {
                        pd.putLong("fr_second_wind", now);
                        Scheduler.later(1, () -> {
                            if (victim.isAlive()) {
                                victim.heal(victim.getMaxHealth() * 0.3F);
                                victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1, false, false, true));
                                Vfx.at(victim, VfxKind.RCT_HEAL);
                                Combat.sound(victim, victim.position(), ModSounds.RCT, 1.0F, 0.8F);
                            }
                        });
                    }
                }
            }
        }

        event.setAmount(amount);
    }

    private static float toolEffects(ServerPlayer attacker, PlayerData data, CursedToolItem tool, LivingEntity target, float amount) {
        ItemStack stack = attacker.getMainHandItem();
        Vec3 dir = target.position().subtract(attacker.position());
        switch (tool.kind()) {
            case DRAGON_BONE:
                CompoundTag tagx = stack.getOrCreateTag();
                int charge = tagx.getInt("Charge") + 1;
                if (charge >= 5) {
                    tagx.putInt("Charge", 0);
                    amount += 10.0F * data.power();
                    Combat.knock(target, dir, 1.6, 0.4);
                    Vfx.play(attacker.serverLevel(), VfxKind.DRAGON_JET, target.getBoundingBox().getCenter(), dir, 1.4F, 9099519, -1, 0);
                    Combat.sound(attacker, target.position(), ModSounds.DRAGON_JET, 1.6F, 1.0F);
                    Combat.shake(attacker.serverLevel(), target.position(), 0.8F, 8, 10.0);
                } else {
                    tagx.putInt("Charge", charge);
                }
                break;
            case KAMUTOKE:
                CompoundTag tag = stack.getOrCreateTag();
                int hits = tag.getInt("Hits") + 1;
                if (hits >= 3) {
                    tag.putInt("Hits", 0);
                    Scheduler.later(2, () -> lightning(attacker, target.position(), target, 7.0F));
                } else {
                    tag.putInt("Hits", hits);
                }
                break;
            case NANAMI_CLEAVER:
                if (attacker.getRandom().nextFloat() < 0.3F) {
                    amount *= 1.6F;
                    Vfx.play(
                        attacker.serverLevel(), VfxKind.RATIO_MARK, target.getBoundingBox().getCenter(), dir, target.getBbHeight(), 15253600, target.getId(), 0
                    );
                    Vfx.play(attacker.serverLevel(), VfxKind.RATIO_CRIT, target.getBoundingBox().getCenter(), dir, 1.0F, 15253600, -1, 0);
                    Combat.sound(attacker, target.position(), ModSounds.RATIO_CRIT, 1.2F, 1.1F);
                }
                break;
            case BLACK_ROPE:
                for (MobEffectInstance effect : new ArrayList<>(target.getActiveEffects())) {
                    if (effect.getEffect().getCategory() == MobEffectCategory.BENEFICIAL) {
                        target.removeEffect(effect.getEffect());
                    }
                }

                if (target instanceof ServerPlayer tp) {
                    PlayerData td = PlayerDataCap.get(tp);
                    if (td != null) {
                        td.setInfinity(false);
                        td.setReinforced(false);
                    }
                }

                Vfx.play(attacker.serverLevel(), VfxKind.ROPE_LASH, target.getBoundingBox().getCenter(), dir, 1.0F, 2763312, target.getId(), 0);
                break;
            case CHAIN_OF_THOUSAND_MILES:
                target.knockback(0.4, -dir.x, -dir.z);
                break;
            case EXECUTIONER_SWORD:
                CompoundTag tagxx = stack.getTag();
                boolean live = tagxx != null && tagxx.contains("Expire") && tagxx.getLong("Expire") > attacker.level().getGameTime();
                if (live && tagxx.hasUUID("Condemned") && tagxx.getUUID("Condemned").equals(target.getUUID())) {
                    amount = Math.max(amount, target.getHealth() + target.getAbsorptionAmount() + 1000.0F);
                    target.invulnerableTime = 0;
                    Vfx.play(attacker.serverLevel(), VfxKind.VERDICT, target.position(), Vec3.ZERO, 1.6F, 16777215, target.getId(), 1);
                    Combat.sound(attacker, target.position(), ModSounds.CLEAVE, 2.2F, 0.5F);
                    Combat.shake(attacker.serverLevel(), target.position(), 1.4F, 12, 20.0);
                    stack.shrink(1);
                }
        }

        return amount;
    }

    public static void lightning(ServerPlayer owner, Vec3 at, @Nullable LivingEntity target, float damage) {
        ServerLevel level = owner.serverLevel();
        Vec3 ground = BloodManipulation.groundBelow(owner, at.add(0.0, 1.0, 0.0));
        Vfx.play(level, VfxKind.LIGHTNING, ground, Vec3.ZERO, 1.0F, 16773242, -1, 0);
        Combat.sound(owner, ground, ModSounds.THUNDER, 3.0F, 1.0F);
        Combat.flash(level, ground, 16777184, 5, 0.35F, 24.0);
        Combat.shake(level, ground, 0.8F, 8, 16.0);

        for (LivingEntity e : Combat.around(owner, ground.add(0.0, 1.0, 0.0), 2.2)) {
            Combat.hurt(e, owner, owner, damage);
            e.addEffect(new MobEffectInstance(ModEffects.OVERLOAD.get(), 20, 0), owner);
        }
    }

    private static void divergentFist(ServerPlayer attacker, LivingEntity target, float amount) {
        Scheduler.later(
            6,
            () -> {
                if (target.isAlive()) {
                    Combat.hurt(target, attacker, attacker, amount * 0.4F);
                    Vfx.play(
                        attacker.serverLevel(),
                        VfxKind.HIT_SPARK,
                        target.getBoundingBox().getCenter(),
                        target.position().subtract(attacker.position()),
                        1.4F,
                        4877567,
                        -1,
                        0
                    );
                    Combat.sound(attacker, target.position(), ModSounds.CURSED_HIT, 1.0F, 0.6F);
                }
            }
        );
    }

    @SubscribeEvent
    public static void damage(LivingDamageEvent event) {
        ServerPlayer attacker = meleeAttacker(event.getSource());
        if (attacker != null) {
            PlayerData data = PlayerDataCap.get(attacker);
            if (data != null) {
                if (attacker.getMainHandItem().getItem() instanceof CursedToolItem tool
                    && tool.kind() == CursedToolItem.Kind.SPLIT_SOUL
                    && spend(attacker, data, 2.0F)) {
                    event.setAmount(event.getAmount() + 3.0F * data.power());
                }

                if (data.berserkTicks > 0) {
                    attacker.heal(event.getAmount() * 0.15F);
                }
            }
        }
    }

    @SubscribeEvent(
        priority = EventPriority.LOW
    )
    public static void death(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.getSource().getEntity() instanceof ServerPlayer player && victim != player) {
            PlayerData data = PlayerDataCap.get(player);
            if (data != null && data.getPlayerClass() != null) {
                Progression.addXp(player, Progression.xpForKill(victim));
                if (data.talentRank(Talent.BLOODLUST) > 0) {
                    player.heal((float)data.talentRank(Talent.BLOODLUST));
                }

                if (data.hasTalent(Talent.MOMENTUM)) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, false, true));
                    data.regen(data.getMax() * 0.05F);
                }

                if (victim instanceof CursedSpirit spirit) {
                    if (data.getPlayerClass() == PlayerClass.CURSE_MANIPULATOR && (!(victim instanceof CurseEntity c) || !c.isSummon())) {
                        CurseManipulation.onExorcised(player, data, victim);
                    }

                    int before = (int)(data.getMax() / 10.0F);
                    if (!data.usesStamina()) {
                        data.addKillBonus(spirit.energyReward());
                    }

                    data.regen(5.0F);
                    if (!data.usesStamina() && (int)(data.getMax() / 10.0F) > before) {
                        player.displayClientMessage(
                            Component.translatable("message.fallenroses.energy_grows", Math.round(data.getMax())).withStyle(ChatFormatting.LIGHT_PURPLE), true
                        );
                    }

                    FRAdvancements.award(player, "story/first_exorcism");
                    if (spirit.grade() <= 2) {
                        FRAdvancements.award(player, "story/grade_2");
                    }

                    if (spirit.grade() == 0) {
                        FRAdvancements.award(player, "story/special_grade");
                    }
                }

                return;
            }

            return;
        }
    }

    @SubscribeEvent
    public static void fall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && (Heavenly.ignoresFall(player) || StarRage.ignoresFall(player))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void heal(LivingHealEvent event) {
        if (event.getEntity().hasEffect(ModEffects.ROT.get())) {
            event.setAmount(event.getAmount() * 0.5F);
        }
    }

    @SubscribeEvent
    public static void jump(LivingJumpEvent event) {
        LivingEntity e = event.getEntity();
        if (isParalyzed(e)) {
            e.setDeltaMovement(e.getDeltaMovement().multiply(0.0, 0.0, 0.0));
            e.hurtMarked = true;
        }
    }

    private static boolean isParalyzed(LivingEntity e) {
        return e.hasEffect(ModEffects.OVERLOAD.get()) || e.hasEffect(ModEffects.BOUND.get());
    }

    @Nullable
    private static ServerPlayer meleeAttacker(DamageSource source) {
        return source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() instanceof ServerPlayer player ? player : null;
    }

    private static boolean spend(Player player, PlayerData data, float amount) {
        return player.getAbilities().instabuild || data.consume(amount);
    }

    private CombatEvents() {
    }
}
