package com.fallenroses.technique.impl;

import com.fallenroses.entity.curse.CurseEntity;
import com.fallenroses.player.PlayerData;
import com.fallenroses.registry.ModEntities;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.util.PlayerAnims;
import com.fallenroses.util.Scheduler;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class Swarm {
    static final int ROACH = 9067050;

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "roach_swarm" -> swarm(p, power);
            case "devour" -> devour(p, power);
            case "molt" -> molt(p);
            case "brood" -> brood(p, power);
            case "infestation" -> infestation(p, power);
            default -> false;
        };
    }

    private static boolean swarm(ServerPlayer p, float power) {
        PlayerAnims.play(p, "throw");
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1.0F);
        Vfx.play(level, VfxKind.DUST_CLOUD, p.getEyePosition().add(look.scale(3.5)), Vec3.ZERO, 3.0F, 2759178, -1, 0);
        Combat.sound(p, p.position(), ModSounds.SPIDER_AMBIENT, 1.4F, 1.7F);

        for (int i = 0; i < 2; i++) {
            Scheduler.later(i * 8, () -> {
                for (LivingEntity e : Combat.cone(p, 7.0, 0.6)) {
                    Combat.hurt(e, p, p, 3.5F * power);
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), p);
                    e.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 1), p);
                }
            });
        }

        return true;
    }

    private static boolean devour(ServerPlayer p, float power) {
        LivingEntity target = Combat.lookTarget(p, 6.0);
        if (target == null) {
            return Ratio.noTarget(p);
        } else {
            PlayerAnims.play(p, "touch");
            ServerLevel level = p.serverLevel();
            float dmg = 9.0F * power;
            Combat.hurt(target, p, p, dmg);
            p.heal(dmg * 0.5F);
            Vfx.play(level, VfxKind.DUST_CLOUD, target.getBoundingBox().getCenter(), Vec3.ZERO, 1.6F, 1708038, -1, 0);
            Vfx.play(level, VfxKind.ABSORB, p.position(), Vec3.ZERO, 1.0F, 9067050, p.getId(), 0);
            Combat.sound(p, target.position(), ModSounds.SPIDER_HURT, 1.4F, 0.8F);
            return true;
        }
    }

    private static boolean molt(ServerPlayer p) {
        PlayerAnims.play(p, "raise");

        for (MobEffectInstance eff : new ArrayList<>(p.getActiveEffects())) {
            if (eff.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                p.removeEffect(eff.getEffect());
            }
        }

        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 3, false, true, true));
        Vfx.play(p.serverLevel(), VfxKind.ROT_BURST, p.position().add(0.0, 1.0, 0.0), Vec3.ZERO, 1.5F, 9067050, -1, 0);
        Combat.sound(p, p.position(), ModSounds.SPIDER_DEATH, 1.2F, 0.7F);
        return true;
    }

    private static boolean brood(ServerPlayer p, float power) {
        LivingEntity target = Combat.lookTarget(p, 10.0);
        if (target == null) {
            return Ratio.noTarget(p);
        } else {
            PlayerAnims.play(p, "touch");
            ServerLevel level = p.serverLevel();
            Vfx.play(level, VfxKind.PLAGUE_MARK, target.position(), Vec3.ZERO, Math.max(1.0F, target.getBbHeight()), 9067050, target.getId(), 60);
            Combat.sound(p, target.position(), ModSounds.SPIDER_AMBIENT, 1.2F, 0.6F);
            Scheduler.later(60, () -> {
                if (p.isAlive()) {
                    Vec3 at = target.isAlive() ? target.position() : target.position();
                    Vfx.play(level, VfxKind.ROT_BURST, at.add(0.0, 1.0, 0.0), Vec3.ZERO, 2.0F, 9067050, -1, 0);
                    Combat.sound(p, at, ModSounds.BLOOD_BURST, 1.6F, 0.7F);
                    if (target.isAlive()) {
                        Combat.hurt(target, p, p, 12.0F * power);
                    }

                    for (int i = 0; i < 3; i++) {
                        CurseEntity.summon(ModEntities.FLY_HEAD.get(), p, at.add(p.getRandom().nextGaussian(), 0.5, p.getRandom().nextGaussian()), 400);
                    }
                }
            });
            return true;
        }
    }

    private static boolean infestation(ServerPlayer p, float power) {
        PlayerAnims.play(p, "domain");
        ServerLevel level = p.serverLevel();
        p.displayClientMessage(Component.translatable("message.fallenroses.infestation").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        Vfx.play(level, VfxKind.SWARM_STORM, p.position(), Vec3.ZERO, 10.0F, 9067050, p.getId(), 120);
        Combat.sound(p, p.position(), ModSounds.SPIDER_AMBIENT, 3.0F, 0.5F);

        for (int i = 1; i <= 12; i++) {
            int k = i;
            Scheduler.later(
                i * 10,
                () -> {
                    if (p.isAlive()) {
                        for (LivingEntity e : Combat.around(p, p.position(), 10.0)) {
                            Combat.hurt(e, p, p, 3.0F * power);
                            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0), p);
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1), p);
                        }

                        if (k % 4 == 0) {
                            CurseEntity.summon(
                                ModEntities.FLY_HEAD.get(),
                                p,
                                p.position().add(p.getRandom().nextGaussian() * 3.0, 1.0, p.getRandom().nextGaussian() * 3.0),
                                300
                            );
                        }

                        Combat.sound(p, p.position(), ModSounds.SPIDER_AMBIENT, 1.2F, 1.5F);
                    }
                }
            );
        }

        return true;
    }

    private Swarm() {
    }
}
