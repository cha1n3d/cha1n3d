package com.fallenroses.technique;

import com.fallenroses.network.ModNetwork;
import com.fallenroses.network.SimplePackets;
import com.fallenroses.player.FRAdvancements;
import com.fallenroses.player.PlayerClass;
import com.fallenroses.player.PlayerData;
import com.fallenroses.player.Talent;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class BlackFlash {
    public static final float MULTIPLIER = 2.5F;

    public static float chance(PlayerData d) {
        if (d.strikeReadyTicks > 0) {
            return 1.0F;
        } else if (d.stationTicks > 0) {
            return 0.45F;
        } else if (d.isReinforced() && !d.usesStamina()) {
            float c = 0.02F;
            if (d.hasTalent(Talent.BLACK_FLASH)) {
                c += 0.05F;
            }

            c += (float)d.talentRank(Talent.SPARK) * 0.02F;
            if (d.getPlayerClass() == PlayerClass.VESSEL) {
                c += 0.04F;
            }

            if (d.zoneTicks > 0) {
                c += 0.03F;
            }

            return c;
        } else {
            return 0.0F;
        }
    }

    public static float tryTrigger(ServerPlayer p, PlayerData d, LivingEntity target, float amount) {
        float chance = chance(d);
        if (!(chance <= 0.0F) && !(p.getRandom().nextFloat() >= chance)) {
            d.strikeReadyTicks = 0;
            d.zoneTicks = 200;
            d.regen(20.0F);
            d.markUrgent();
            Vfx.play(
                p.serverLevel(), VfxKind.BLACK_FLASH, target.getBoundingBox().getCenter(), target.position().subtract(p.position()), 1.0F, 0, target.getId(), 0
            );
            Combat.sound(p, target.position(), ModSounds.BLACK_FLASH, 3.0F, 1.0F);
            Combat.shake(p.serverLevel(), target.position(), 1.6F, 14, 20.0);
            ModNetwork.send(p, new SimplePackets.ScreenFx(0, 1703940, 6, 0.55F, ""));
            p.displayClientMessage(Component.translatable("message.fallenroses.black_flash").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
            FRAdvancements.award(p, "story/black_flash");
            return amount * 2.5F;
        } else {
            return amount;
        }
    }

    private BlackFlash() {
    }
}
