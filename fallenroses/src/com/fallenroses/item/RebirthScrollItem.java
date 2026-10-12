package com.fallenroses.item;

import com.fallenroses.network.ModNetwork;
import com.fallenroses.player.ClassSelection;
import com.fallenroses.player.PlayerData;
import com.fallenroses.player.PlayerDataCap;
import com.fallenroses.player.Progression;
import com.fallenroses.registry.ModSounds;
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

public class RebirthScrollItem extends Item {
    public RebirthScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            PlayerData data = PlayerDataCap.get(sp);
            if (data == null || data.getPlayerClass() == null) {
                return InteractionResultHolder.fail(stack);
            } else if (!sp.isShiftKeyDown()) {
                sp.displayClientMessage(Component.translatable("message.fallenroses.rebirth_hint").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            } else {
                data.prepareRebirth();
                Progression.applyAttributes(sp);
                ModNetwork.sync(sp, data);
                Vfx.at(sp, VfxKind.EXORCISM, 1.5F, 0);
                level.playSound(null, sp.blockPosition(), ModSounds.INTRO_FLASH.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
                sp.displayClientMessage(Component.translatable("message.fallenroses.rebirth").withStyle(ChatFormatting.LIGHT_PURPLE), false);
                if (!sp.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                ClassSelection.startIntroIfNeeded(sp, true);
                return InteractionResultHolder.consume(stack);
            }
        } else {
            return InteractionResultHolder.success(stack);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.fallenroses.scroll_rebirth.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.fallenroses.rebirth_shift").withStyle(ChatFormatting.DARK_GRAY));
    }
}
