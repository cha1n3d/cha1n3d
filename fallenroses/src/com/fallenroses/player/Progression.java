package com.fallenroses.player;

import com.fallenroses.entity.curse.CursedSpirit;
import com.fallenroses.network.ModNetwork;
import com.fallenroses.network.SimplePackets;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;

public final class Progression {
    public static final int MAX_LEVEL = 50;
    private static final UUID HEALTH_ID = UUID.fromString("6b0f6f5e-6a7d-4f61-9d1b-1f0a2b3c4d01");
    private static final UUID DAMAGE_ID = UUID.fromString("6b0f6f5e-6a7d-4f61-9d1b-1f0a2b3c4d02");
    private static final UUID SPEED_ID = UUID.fromString("6b0f6f5e-6a7d-4f61-9d1b-1f0a2b3c4d03");
    private static final UUID ATTACK_SPEED_ID = UUID.fromString("6b0f6f5e-6a7d-4f61-9d1b-1f0a2b3c4d04");
    private static final UUID ARMOR_ID = UUID.fromString("6b0f6f5e-6a7d-4f61-9d1b-1f0a2b3c4d05");
    private static final UUID TOUGHNESS_ID = UUID.fromString("6b0f6f5e-6a7d-4f61-9d1b-1f0a2b3c4d06");
    private static final UUID KNOCKBACK_ID = UUID.fromString("6b0f6f5e-6a7d-4f61-9d1b-1f0a2b3c4d07");

    public static int talentPointsForLevel(int level) {
        return 1 + (level - 1) * 2 + level / 5;
    }

    public static int xpForNext(int level) {
        return (int)Math.round(40.0 * Math.pow((double)level, 1.5) + 60.0);
    }

    public static int xpForKill(LivingEntity victim) {
        if (victim instanceof Player) {
            return 0;
        } else if (victim instanceof CursedSpirit spirit) {
            return spirit.xpReward();
        } else {
            double hp = (double)victim.getMaxHealth();
            double xp = hp * 1.2 * (hp >= 150.0 ? 1.5 : 1.0);
            return (int)Math.min(3000L, Math.round(xp));
        }
    }

    public static void addXp(ServerPlayer player, int amount) {
        PlayerData data = PlayerDataCap.get(player);
        if (data != null && data.getPlayerClass() != null && amount > 0) {
            int level = data.getLevel();
            int xp = data.getXp() + amount;
            int gained = 0;

            while (level < 50 && xp >= xpForNext(level)) {
                xp -= xpForNext(level);
                level++;
                gained += talentPointsForLevel(level) - talentPointsForLevel(level - 1);
                FRAdvancements.onLevel(player, level);
            }

            if (level >= 50) {
                xp = 0;
            }

            int before = data.getLevel();
            data.setXp(xp);
            if (level != before) {
                data.setLevel(level);
                data.setTalentPoints(data.getTalentPoints() + gained);
                data.setEnergy(data.getMax());
                player.setHealth(player.getMaxHealth());
                ModNetwork.send(player, new SimplePackets.LevelUp(level, gained));
                Vfx.at(player, VfxKind.LEVEL_UP);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LEVEL_UP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    public static void unlockTalent(ServerPlayer player, int ordinal) {
        PlayerData data = PlayerDataCap.get(player);
        if (data != null && data.getPlayerClass() != null && ordinal >= 0 && ordinal < Talent.values().length) {
            Talent talent = Talent.values()[ordinal];
            if (talent.blockReason(data.getPlayerClass(), data.getLevel(), data.getTalentPoints(), data.talentRanks(), data.getTechnique(), data.getFingers())
                != null) {
                player.playNotifySound(ModSounds.UI_ERROR.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
            } else {
                data.setTalentPoints(data.getTalentPoints() - talent.cost);
                int rank = data.talentRank(talent) + 1;
                data.setTalentRank(talent, rank);
                applyAttributes(player);
                boolean major = talent.stat == null;
                player.playNotifySound(
                    major ? ModSounds.SKILL_UNLOCK.get() : ModSounds.STAT_UP.get(),
                    SoundSource.PLAYERS,
                    major ? 1.0F : 0.7F,
                    major ? 1.0F : 0.9F + (float)rank * 0.06F
                );
                if (major) {
                    player.displayClientMessage(
                        Component.translatable("message.fallenroses.talent_unlocked", talent.displayName()).withStyle(ChatFormatting.LIGHT_PURPLE), true
                    );
                }

                if (talent.slot() > 0) {
                    FRAdvancements.award(player, "story/new_ability");
                    FRAdvancements.award(player, "story/slot_" + (talent.slot() + 1));
                }
            }
        }
    }

    public static void resetTalents(ServerPlayer player) {
        PlayerData data = PlayerDataCap.get(player);
        if (data != null && data.getPlayerClass() != null) {
            data.refundTalents();
            data.setReinforced(false);
            data.setInfinity(false);
            applyAttributes(player);
            player.playNotifySound(ModSounds.UI_ERROR.get(), SoundSource.PLAYERS, 0.8F, 0.6F);
            player.displayClientMessage(Component.translatable("message.fallenroses.talents_reset").withStyle(ChatFormatting.GRAY), true);
        }
    }

    public static void applyAttributes(ServerPlayer player) {
        PlayerData data = PlayerDataCap.get(player);
        if (data != null) {
            PlayerClass c = data.getPlayerClass();
            boolean hr = c == PlayerClass.HEAVENLY_RESTRICTION;
            double health = 0.0;
            double damage = 0.0;
            double speed = 0.0;
            double attackSpeed = 0.0;
            double armor = 0.0;
            double toughness = 0.0;
            double knockback = 0.0;
            if (c != null) {
                health = (double)(c.bonusHealth() + data.getStat(Stat.VITALITY));
                damage = (double)c.bonusDamage() + (double)data.getStat(Stat.STRENGTH) * (hr ? 0.5 : 0.3);
                speed = (double)c.bonusSpeed() + (double)data.getStat(Stat.AGILITY) * 0.004;
                attackSpeed = (double)data.getStat(Stat.AGILITY) * 0.01 + (double)data.talentRank(Talent.BATTLE_TRANCE) * 0.05;
                if (data.hasTalent(Talent.IRON_BODY)) {
                    armor = 4.0;
                    toughness = 2.0;
                    knockback = 0.2;
                }

                if (data.hasTalent(Talent.BODY_PEAK)) {
                    health += 4.0;
                }

                if (data.hasTalent(Talent.SPIRIT_BODY)) {
                    health += 4.0;
                }

                if (data.hasTalent(Talent.TITAN)) {
                    health += 6.0;
                }

                armor += (double)(data.talentRank(Talent.UNBREAKABLE) * 2);
                armor += (double)data.talentRank(Talent.TOUGHNESS);
                knockback += (double)data.talentRank(Talent.TOUGHNESS) * 0.05;
            }

            set(player, Attributes.MAX_HEALTH, HEALTH_ID, "fallenroses.health", health, Operation.ADDITION);
            set(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "fallenroses.damage", damage, Operation.ADDITION);
            set(player, Attributes.MOVEMENT_SPEED, SPEED_ID, "fallenroses.speed", speed, Operation.MULTIPLY_BASE);
            set(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, "fallenroses.attack_speed", attackSpeed, Operation.MULTIPLY_BASE);
            set(player, Attributes.ARMOR, ARMOR_ID, "fallenroses.armor", armor, Operation.ADDITION);
            set(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, "fallenroses.toughness", toughness, Operation.ADDITION);
            set(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, "fallenroses.knockback", knockback, Operation.ADDITION);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    private static void set(ServerPlayer player, Attribute attribute, UUID id, String name, double value, Operation op) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst != null) {
            inst.removeModifier(id);
            if (value != 0.0) {
                inst.addPermanentModifier(new AttributeModifier(id, name, value, op));
            }
        }
    }

    private Progression() {
    }
}
