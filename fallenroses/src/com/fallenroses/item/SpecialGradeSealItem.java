package com.fallenroses.item;

import com.fallenroses.entity.curse.SakuraBossEntity;
import com.fallenroses.registry.ModEntities;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.util.Scheduler;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SpecialGradeSealItem extends Item {
    public SpecialGradeSealItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            ServerLevel server = (ServerLevel)level;
            BlockPos pos = context.getClickedPos().above();
            Vec3 at = Vec3.atBottomCenterOf(pos);
            Vfx.play(server, VfxKind.ROOT_ERUPT, at, Vec3.ZERO, 0.8F, 3807762, -1, 0);
            Vfx.play(server, VfxKind.EXORCISM, at.add(0.0, 1.0, 0.0), Vec3.ZERO, 3.0F, 16033992, -1, 0);
            server.playSound(null, pos, ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 4.0F, 0.6F);
            Combat.shake(server, at, 1.2F, 40, 32.0);
            if (player != null) {
                player.displayClientMessage(
                    Component.translatable("message.fallenroses.seal_broken").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true
                );
                if (!player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
            }

            Scheduler.later(40, () -> {
                SakuraBossEntity boss = ModEntities.SAKURA_BOSS.get().create(server);
                if (boss != null) {
                    boss.moveTo(at.x, at.y, at.z, context.getRotation() + 180.0F, 0.0F);
                    boss.finalizeSpawn(server, server.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
                    server.addFreshEntity(boss);
                    Vfx.play(server, VfxKind.ROOT_ERUPT, at, Vec3.ZERO, 3.0F, 3807762, -1, 1);
                    Combat.flash(server, at, 4194320, 14, 0.7F, 40.0);
                }
            });
            return InteractionResult.CONSUME;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.fallenroses.special_grade_seal.desc").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
