package com.fallenroses.technique.impl;

import com.fallenroses.entity.ShotEntity;
import com.fallenroses.entity.curse.CurseEntity;
import com.fallenroses.entity.curse.CursedSpirit;
import com.fallenroses.player.PlayerData;
import com.fallenroses.registry.ModEntities;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.util.PlayerAnims;
import com.fallenroses.util.Scheduler;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class CurseManipulation {
    static final int JADE = 3844744;

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "absorb" -> absorb(p, d);
            case "release_spirits" -> release(p, d, power);
            case "curse_swarm" -> swarm(p, power);
            case "summon_grade_two" -> grade2(p, d, power);
            case "uzumaki" -> uzumaki(p, d, power);
            default -> false;
        };
    }

    public static void onExorcised(ServerPlayer p, PlayerData d, LivingEntity curse) {
        if (d.getAbsorbed() < d.maxAbsorbed()) {
            d.setAbsorbed(d.getAbsorbed() + 1);
            Vfx.play(p.serverLevel(), VfxKind.ABSORB, curse.getBoundingBox().getCenter(), Vec3.ZERO, 1.0F, 3844744, p.getId(), 0);
            p.displayClientMessage(
                Component.translatable("message.fallenroses.absorbed", d.getAbsorbed(), d.maxAbsorbed()).withStyle(s -> s.withColor(3844744)), true
            );
        }
    }

    private static boolean spend(ServerPlayer p, PlayerData d, int n) {
        if (p.getAbilities().instabuild) {
            return true;
        } else if (d.getAbsorbed() < n) {
            p.displayClientMessage(Component.translatable("message.fallenroses.not_enough_spirits", d.getAbsorbed(), n).withStyle(ChatFormatting.GRAY), true);
            return false;
        } else {
            d.setAbsorbed(d.getAbsorbed() - n);
            return true;
        }
    }

    private static boolean absorb(ServerPlayer p, PlayerData d) {
        LivingEntity target = Combat.lookTarget(p, 8.0);
        label24:
        if (target instanceof CursedSpirit spirit && spirit.grade() > 0) {
            if (target instanceof CurseEntity c && c.isSummon()) {
                break label24;
            }

            if (target.getHealth() > target.getMaxHealth() * 0.4F) {
                p.displayClientMessage(Component.translatable("message.fallenroses.absorb_too_strong").withStyle(ChatFormatting.GRAY), true);
                return false;
            }

            if (d.getAbsorbed() >= d.maxAbsorbed()) {
                p.displayClientMessage(Component.translatable("message.fallenroses.absorb_full").withStyle(ChatFormatting.GRAY), true);
                return false;
            }

            PlayerAnims.play(p, "touch");
            Vfx.play(p.serverLevel(), VfxKind.ABSORB, target.getBoundingBox().getCenter(), Vec3.ZERO, 1.6F, 3844744, p.getId(), 1);
            Combat.sound(p, target.position(), ModSounds.ABSORB, 1.6F, 1.0F);
            target.hurt(p.damageSources().indirectMagic(p, p), Float.MAX_VALUE);
            d.setAbsorbed(Math.min(d.maxAbsorbed(), d.getAbsorbed() + 2));
            p.displayClientMessage(
                Component.translatable("message.fallenroses.absorbed", d.getAbsorbed(), d.maxAbsorbed()).withStyle(s -> s.withColor(3844744)), true
            );
            return true;
        }

        p.displayClientMessage(Component.translatable("message.fallenroses.absorb_no_curse").withStyle(ChatFormatting.GRAY), true);
        return false;
    }

    private static boolean release(ServerPlayer p, PlayerData d, float power) {
        if (!spend(p, d, 3)) {
            return false;
        } else {
            PlayerAnims.play(p, "blue");
            Combat.sound(p, p.position(), ModSounds.SUMMON, 1.6F, 1.1F);
            Vec3 look = p.getViewVector(1.0F);

            for (int i = 0; i < 3; i++) {
                Vec3 at = p.position().add(look.scale(2.0)).add(p.getRandom().nextGaussian(), 1.5, p.getRandom().nextGaussian());
                buff(CurseEntity.summon(ModEntities.FLY_HEAD.get(), p, at, 900), power);
            }

            buff(CurseEntity.summon(ModEntities.ROTTEN_MOUTH.get(), p, p.position().add(look.scale(2.5)), 900), power);
            return true;
        }
    }

    private static boolean swarm(ServerPlayer p, float power) {
        PlayerAnims.play(p, "raise");
        Vec3 c = p.position().add(0.0, 1.6, 0.0);

        for (int i = 0; i < 6; i++) {
            double a = (double)i * Math.PI / 3.0;
            Vec3 v = new Vec3(Math.cos(a) * 0.4, 0.35, Math.sin(a) * 0.4);
            new ShotEntity(ModEntities.SHOT.get(), p.level()).kind(ShotEntity.Kind.SPIRIT_SWARM).launch(p, c, v, power);
        }

        Combat.sound(p, p.position(), ModSounds.HANGED_SHOOT, 2.0F, 0.8F);
        return true;
    }

    private static boolean grade2(ServerPlayer p, PlayerData d, float power) {
        if (!spend(p, d, 5)) {
            return false;
        } else {
            PlayerAnims.play(p, "domain");
            Combat.sound(p, p.position(), ModSounds.SUMMON, 2.5F, 0.7F);
            Vec3 look = p.getViewVector(1.0F);
            Vec3 side = look.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
            buff(CurseEntity.summon(ModEntities.HAND_SPIDER.get(), p, p.position().add(look.scale(2.5)).add(side.scale(2.0)), 1200), power);
            buff(CurseEntity.summon(ModEntities.HANGED.get(), p, p.position().add(look.scale(2.5)).add(side.scale(-2.0)).add(0.0, 2.0, 0.0), 1200), power);
            return true;
        }
    }

    static void buff(CurseEntity e, float power) {
        if (e != null) {
            AttributeInstance dmg = e.getAttribute(Attributes.ATTACK_DAMAGE);
            if (dmg != null) {
                dmg.setBaseValue(dmg.getBaseValue() * (double)power);
            }

            AttributeInstance hp = e.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) {
                hp.setBaseValue(hp.getBaseValue() * (0.8 + (double)power * 0.4));
                e.setHealth(e.getMaxHealth());
            }
        }
    }

    private static boolean uzumaki(ServerPlayer p, PlayerData d, float power) {
        int charges = p.getAbilities().instabuild ? Math.max(10, d.getAbsorbed()) : d.getAbsorbed();
        if (charges < 5) {
            p.displayClientMessage(Component.translatable("message.fallenroses.not_enough_spirits", d.getAbsorbed(), 5).withStyle(ChatFormatting.GRAY), true);
            return false;
        } else {
            if (!p.getAbilities().instabuild) {
                d.setAbsorbed(0);
            }

            ServerLevel level = p.serverLevel();
            PlayerAnims.play(p, "purple");
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 34, 4, false, false));
            p.displayClientMessage(Component.translatable("message.fallenroses.uzumaki", charges).withStyle(s -> s.withColor(3844744).withBold(true)), true);
            Vfx.play(
                level, VfxKind.UZUMAKI_CHARGE, Limitless.hand(p), p.getViewVector(1.0F), Math.min(3.0F, 1.0F + (float)charges * 0.08F), 3844744, p.getId(), 30
            );
            Combat.sound(p, p.position(), ModSounds.SUMMON, 3.0F, 0.5F);
            Scheduler.later(30, () -> {
                if (p.isAlive()) {
                    Vec3 from = p.getEyePosition().add(0.0, -0.3, 0.0);
                    Vec3 look = p.getViewVector(1.0F);
                    BlockHitResult wall = level.clip(new ClipContext(from, from.add(look.scale(40.0)), Block.COLLIDER, Fluid.NONE, p));
                    Vec3 end = wall.getType() == Type.MISS ? from.add(look.scale(40.0)) : wall.getLocation();
                    float dmg = Math.min(130.0F, 4.5F * (float)charges) * power;

                    for (LivingEntity e : BloodManipulation.along(p, from, end, 2.6)) {
                        Combat.hurt(e, p, p, dmg);
                        Combat.knock(e, look, 1.8, 0.4);
                    }

                    Vfx.play(level, VfxKind.UZUMAKI_BEAM, from, end.subtract(from), Math.min(3.5F, 1.4F + (float)charges * 0.06F), 3844744, -1, 0);
                    Combat.sound(p, p.position(), ModSounds.PURPLE_FIRE, 4.0F, 0.7F);
                    Combat.flash(level, p.position(), 398352, 14, 0.7F, 48.0);
                    Combat.shake(level, p.position(), 2.4F, 24, 40.0);
                }
            });
            return true;
        }
    }

    private CurseManipulation() {
    }
}
