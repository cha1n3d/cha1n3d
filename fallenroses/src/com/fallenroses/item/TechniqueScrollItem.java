package com.fallenroses.item;

import com.fallenroses.player.FRAdvancements;
import com.fallenroses.player.PlayerClass;
import com.fallenroses.player.PlayerData;
import com.fallenroses.player.PlayerDataCap;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Technique;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class TechniqueScrollItem extends Item {
    private final Technique technique;

    public TechniqueScrollItem(Technique technique, Properties properties) {
        super(properties);
        this.technique = technique;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            PlayerData data = PlayerDataCap.get(player);
            if (data == null || data.getPlayerClass() == null) {
                return InteractionResultHolder.fail(stack);
            } else if (data.usesStamina()) {
                msg(player, "message.fallenroses.scroll_hr");
                return InteractionResultHolder.fail(stack);
            } else {
                boolean spirit = this.technique.source() == Technique.Source.SPIRIT;
                PlayerClass need = spirit ? PlayerClass.CURSED_SPIRIT : PlayerClass.SORCERER;
                if (data.getPlayerClass() != need) {
                    msg(player, spirit ? "message.fallenroses.scroll_not_spirit" : "message.fallenroses.scroll_not_sorcerer");
                    return InteractionResultHolder.fail(stack);
                } else {
                    Technique current = data.getTechnique();
                    if (current == this.technique) {
                        msg(player, "message.fallenroses.already_known");
                        return InteractionResultHolder.fail(stack);
                    } else if (current != null && !player.isShiftKeyDown()) {
                        player.displayClientMessage(
                            Component.translatable("message.fallenroses.replace_hint", current.displayName()).withStyle(ChatFormatting.GRAY), true
                        );
                        return InteractionResultHolder.fail(stack);
                    } else {
                        data.setTechnique(this.technique);
                        player.displayClientMessage(Component.translatable("message.fallenroses.learned", this.technique.displayName()), false);
                        level.playSound(null, player.blockPosition(), ModSounds.SKILL_UNLOCK.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
                        Vfx.at(player, VfxKind.LEVEL_UP, 0.7F, this.technique.color());
                        FRAdvancements.award(sp, "story/new_technique");
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }

                        return InteractionResultHolder.consume(stack);
                    }
                }
            }
        } else {
            return InteractionResultHolder.success(stack);
        }
    }

    private static void msg(Player player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(this.technique.displayName());
        tooltip.add(this.technique.description().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.fallenroses.scroll").withStyle(ChatFormatting.DARK_GRAY));
    }
}
