package com.fallenroses.technique;

import com.fallenroses.player.PlayerData;
import com.fallenroses.player.Talent;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public final class ReverseTechnique {
    private static final float HEAL_PER_SECOND = 1.5F;
    private static final float COST_PER_SECOND = 6.0F;

    public static void tick(ServerPlayer player, PlayerData data) {
        if (data.hasTalent(Talent.REVERSE_TECHNIQUE) && !data.usesStamina()) {
            if (!(player.getHealth() >= player.getMaxHealth()) && player.tickCount % 10 == 0) {
                float cost = 3.0F;
                if (player.getAbilities().instabuild || data.consume(cost)) {
                    player.heal(0.75F * (1.0F + (float)data.talentRank(Talent.RCT_MASTERY) * 0.5F));
                    if (player.tickCount % 40 == 0) {
                        Vfx.at(player, VfxKind.RCT_HEAL);
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RCT.get(), SoundSource.PLAYERS, 0.4F, 1.2F);
                    }
                }
            }
        }
    }

    private ReverseTechnique() {
    }
}
