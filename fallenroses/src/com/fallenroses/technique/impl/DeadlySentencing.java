package com.fallenroses.technique.impl;

import com.fallenroses.entity.DomainEntity;
import com.fallenroses.entity.ShotEntity;
import com.fallenroses.player.PlayerData;
import com.fallenroses.player.PlayerDataCap;
import com.fallenroses.registry.ModEffects;
import com.fallenroses.registry.ModEntities;
import com.fallenroses.registry.ModItems;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.util.PlayerAnims;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class DeadlySentencing {
    static final int GOLD = 15259808;
    public static final String OBJECTION = "fr_objection_until";
    public static final String EVIDENCE = "fr_evidence_until";

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "gavel_strike" -> strike(p, power);
            case "gavel_throw" -> gavelThrow(p, power);
            case "objection" -> objection(p);
            case "evidence" -> evidence(p);
            case "deadly_sentencing_domain" -> Domains.expand(p, d, 7, power);
            default -> false;
        };
    }

    private static boolean strike(ServerPlayer p, float power) {
        PlayerAnims.play(p, "hammer");
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1.0F);
        Vec3 at = p.getEyePosition().add(look.scale(2.2)).add(0.0, -0.5, 0.0);
        Vfx.play(level, VfxKind.MASS_IMPACT, at, look, 1.3F, 15259808, -1, 0);
        Combat.sound(p, at, ModSounds.HAMMER, 1.6F, 0.6F);
        Combat.sound(p, at, ModSounds.SLAM, 1.2F, 1.2F);

        for (LivingEntity e : Combat.cone(p, 4.5, 0.55)) {
            Combat.hurt(e, p, p, 8.0F * power);
            Combat.knock(e, look, 1.3, 0.35);
            e.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 8, 0), p);
        }

        return true;
    }

    private static boolean gavelThrow(ServerPlayer p, float power) {
        PlayerAnims.play(p, "throw");
        Vec3 look = p.getViewVector(1.0F);
        new ShotEntity(ModEntities.SHOT.get(), p.level()).kind(ShotEntity.Kind.GAVEL).launch(p, Limitless.hand(p), look.scale(1.4), power);
        Combat.sound(p, p.position(), ModSounds.DASH, 1.0F, 0.7F);
        return true;
    }

    private static boolean objection(ServerPlayer p) {
        PlayerAnims.play(p, "snap");
        p.getPersistentData().putLong("fr_objection_until", p.level().getGameTime() + 60L);
        Vfx.play(p.serverLevel(), VfxKind.JUDGEMAN, p.position(), Vec3.ZERO, 0.6F, 15259808, p.getId(), 4);
        Combat.sound(p, p.position(), ModSounds.COFFIN_BELL, 1.2F, 1.5F);
        p.displayClientMessage(Component.translatable("message.fallenroses.objection").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        return true;
    }

    public static boolean tryObjection(ServerPlayer victim, LivingEntity attacker) {
        CompoundTag tag = victim.getPersistentData();
        if (tag.getLong("fr_objection_until") <= victim.level().getGameTime()) {
            return false;
        } else {
            tag.remove("fr_objection_until");
            PlayerData d = PlayerDataCap.get(victim);
            float power = d == null ? 1.0F : d.power();
            Vfx.play(
                victim.serverLevel(),
                VfxKind.MASS_IMPACT,
                attacker.getBoundingBox().getCenter(),
                attacker.position().subtract(victim.position()),
                1.2F,
                15259808,
                -1,
                0
            );
            Combat.sound(victim, attacker.position(), ModSounds.HAMMER, 1.8F, 0.7F);
            Combat.hurt(attacker, victim, victim, 12.0F * power);
            attacker.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 30, 0), victim);
            victim.displayClientMessage(Component.translatable("message.fallenroses.objection_sustained").withStyle(ChatFormatting.GOLD), true);
            return true;
        }
    }

    private static boolean evidence(ServerPlayer p) {
        LivingEntity target = Combat.lookTarget(p, 20.0);
        if (target == null) {
            return Ratio.noTarget(p);
        } else {
            PlayerAnims.play(p, "touch");
            target.getPersistentData().putLong("fr_evidence_until", p.level().getGameTime() + 400L);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400, 0), p);
            Vfx.play(p.serverLevel(), VfxKind.VERDICT, target.position(), Vec3.ZERO, Math.max(1.0F, target.getBbHeight() * 0.6F), 15259808, target.getId(), 0);
            Combat.sound(p, target.position(), ModSounds.COFFIN_BELL, 1.0F, 1.8F);
            p.displayClientMessage(Component.translatable("message.fallenroses.evidence", target.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
            return true;
        }
    }

    public static boolean hasEvidence(LivingEntity e) {
        return e.getPersistentData().getLong("fr_evidence_until") > e.level().getGameTime();
    }

    @Nullable
    public static LivingEntity pickDefendant(ServerPlayer judge, List<LivingEntity> inside) {
        LivingEntity best = null;
        double score = -1.0;

        for (LivingEntity e : inside) {
            double s = (double)(e.getMaxHealth() + (float)(hasEvidence(e) ? 10000 : 0) + (float)(e instanceof Player ? 5000 : 0) - e.distanceTo(judge));
            if (s > score) {
                score = s;
                best = e;
            }
        }

        return best;
    }

    public static void openTrial(ServerPlayer judge, DomainEntity court, LivingEntity defendant) {
        ServerLevel level = judge.serverLevel();
        Vfx.play(level, VfxKind.JUDGEMAN, court.position(), Vec3.ZERO, 1.0F, 15259808, -1, 0);
        Vfx.play(level, VfxKind.VERDICT, defendant.position(), Vec3.ZERO, 1.2F, 15259808, defendant.getId(), 0);
        Combat.sound(judge, court.position(), ModSounds.COFFIN_BELL, 3.0F, 0.6F);
        Component name = defendant.getDisplayName();

        for (ServerPlayer sp : level.players()) {
            if (sp.distanceTo(court) < court.radius() + 4.0F) {
                sp.displayClientMessage(
                    Component.translatable("message.fallenroses.trial_open", name).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false
                );
            }
        }
    }

    public static void verdict(ServerPlayer judge, DomainEntity court, LivingEntity t) {
        ServerLevel level = judge.serverLevel();
        RandomSource r = judge.getRandom();
        boolean proof = hasEvidence(t);
        float guiltChance = proof ? 1.0F : (t instanceof Enemy ? 0.85F : (t instanceof Player ? 0.5F : 0.4F));
        boolean guilty = r.nextFloat() < guiltChance;
        String key;
        int outcome;
        if (!guilty) {
            outcome = 3;
            key = "message.fallenroses.verdict_innocent";
        } else if (r.nextFloat() < (proof ? 0.6F : 0.35F)) {
            outcome = 1;
            key = "message.fallenroses.verdict_death";
            grantSword(judge, t);
        } else {
            outcome = 2;
            key = "message.fallenroses.verdict_confiscation";
            confiscate(judge, t);
        }

        Vfx.play(level, VfxKind.JUDGEMAN, court.position(), Vec3.ZERO, 1.0F, 15259808, -1, outcome);
        Vfx.play(level, VfxKind.VERDICT, t.position(), Vec3.ZERO, 1.6F, 15259808, t.getId(), outcome == 1 ? 1 : 0);
        Combat.sound(judge, court.position(), ModSounds.COFFIN_BELL, 3.5F, outcome == 1 ? 0.5F : 0.9F);

        for (ServerPlayer sp : level.players()) {
            if (sp.distanceTo(court) < court.radius() + 4.0F) {
                sp.displayClientMessage(
                    Component.translatable(key)
                        .withStyle(outcome == 1 ? ChatFormatting.DARK_RED : (outcome == 2 ? ChatFormatting.GOLD : ChatFormatting.GRAY), ChatFormatting.BOLD),
                    true
                );
            }
        }
    }

    private static void confiscate(ServerPlayer judge, LivingEntity t) {
        if (t instanceof ServerPlayer tp) {
            PlayerData td = PlayerDataCap.get(tp);
            if (td != null) {
                td.confiscatedTicks = 600;
                td.setReinforced(false);
                td.setInfinity(false);
            }
        }

        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 2), judge);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 1), judge);
        Vfx.play(judge.serverLevel(), VfxKind.SPEECH_SEAL, t.getBoundingBox().getCenter(), Vec3.ZERO, t.getBbHeight(), 15259808, t.getId(), 60);
    }

    private static void grantSword(ServerPlayer judge, LivingEntity condemned) {
        ItemStack sword = new ItemStack(ModItems.EXECUTIONER_SWORD.get());
        CompoundTag tag = sword.getOrCreateTag();
        tag.putUUID("Condemned", condemned.getUUID());
        tag.putString("CondemnedName", Serializer.toJson(condemned.getDisplayName()));
        tag.putLong("Expire", judge.level().getGameTime() + 1200L);
        if (judge.getMainHandItem().isEmpty()) {
            judge.setItemInHand(InteractionHand.MAIN_HAND, sword);
        } else if (!judge.getInventory().add(sword)) {
            judge.drop(sword, false);
        }

        condemned.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1200, 0), judge);
        Vfx.play(judge.serverLevel(), VfxKind.LIGHT_BEAM, judge.position(), new Vec3(0.0, 6.0, 0.0), 1.0F, 16777215, -1, 0);
    }

    private DeadlySentencing() {
    }
}
