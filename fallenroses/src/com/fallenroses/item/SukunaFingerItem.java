package com.fallenroses.item;

import com.fallenroses.config.FRConfig;
import com.fallenroses.player.FRAdvancements;
import com.fallenroses.player.PlayerClass;
import com.fallenroses.player.PlayerData;
import com.fallenroses.player.PlayerDataCap;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.technique.Technique;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class SukunaFingerItem extends Item {
    public SukunaFingerItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            PlayerData data = PlayerDataCap.get(player);
            if (data != null && data.getPlayerClass() != null) {
                if (data.getPlayerClass() != PlayerClass.VESSEL) {
                    player.displayClientMessage(Component.translatable("message.fallenroses.finger_not_vessel").withStyle(ChatFormatting.DARK_RED), true);
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
                    return stack;
                } else {
                    int limit = FRConfig.MAX_FINGERS.get();
                    if (data.getFingers() >= limit) {
                        player.displayClientMessage(Component.translatable("message.fallenroses.finger_limit").withStyle(ChatFormatting.DARK_RED), true);
                        return stack;
                    } else {
                        data.setFingers(data.getFingers() + 1);
                        data.setEnergy(data.getMax());
                        player.displayClientMessage(
                            Component.translatable("message.fallenroses.finger", data.getFingers(), limit, Math.round(data.getMax()))
                                .withStyle(ChatFormatting.DARK_RED),
                            false
                        );
                        Technique shrine = Technique.VESSEL;

                        for (int slot = 0; slot < 5; slot++) {
                            if (shrine.ability(slot).fingers() == data.getFingers()) {
                                player.displayClientMessage(
                                    Component.translatable("message.fallenroses.finger_awakens", shrine.ability(slot).displayName())
                                        .withStyle(s -> s.withColor(shrine.color())),
                                    false
                                );
                            }
                        }

                        level.playSound(null, player.blockPosition(), ModSounds.BOSS_ROAR.get(), SoundSource.PLAYERS, 0.8F, 1.6F);
                        Vfx.at(player, VfxKind.BERSERK, 1.2F, 0);
                        Combat.shake(player.serverLevel(), player.position(), 1.0F, 20, 8.0);
                        FRAdvancements.award(player, "story/sukuna_finger");
                        if (data.getFingers() >= 10) {
                            FRAdvancements.award(player, "story/ten_fingers");
                        }

                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }

                        return stack;
                    }
                }
            } else {
                return stack;
            }
        } else {
            return stack;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.fallenroses.sukuna_finger.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.fallenroses.sukuna_finger.desc2").withStyle(ChatFormatting.DARK_RED));
    }
}
