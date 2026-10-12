package com.fallenroses.player;

import com.fallenroses.network.ModNetwork;
import com.fallenroses.network.SimplePackets;
import com.fallenroses.registry.ModItems;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Technique;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ClassSelection {
    private static final Set<UUID> IN_INTRO = new HashSet<>();

    public static void startIntroIfNeeded(ServerPlayer player, boolean skipStory) {
        PlayerData data = PlayerDataCap.get(player);
        if (data != null && data.getPlayerClass() == null) {
            IN_INTRO.add(player.getUUID());
            if (!player.isCreative() && !player.isSpectator()) {
                player.setInvulnerable(true);
            }

            ModNetwork.send(player, new SimplePackets.OpenIntro(skipStory));
        }
    }

    public static boolean inIntro(ServerPlayer player) {
        return IN_INTRO.contains(player.getUUID());
    }

    public static void forget(ServerPlayer player) {
        IN_INTRO.remove(player.getUUID());
    }

    public static void choose(ServerPlayer player, int classOrdinal, int ignoredTechnique) {
        PlayerData data = PlayerDataCap.get(player);
        PlayerClass c = PlayerClass.byOrdinal(classOrdinal);
        if (data != null && c != null) {
            if (data.getPlayerClass() != null) {
                ModNetwork.sync(player, data);
            } else {
                Technique technique = c.rollTechnique(player.getRandom());
                data.chooseClass(c, technique);
                IN_INTRO.remove(player.getUUID());
                if (!player.isCreative() && !player.isSpectator()) {
                    player.setInvulnerable(false);
                }

                Progression.applyAttributes(player);
                player.setHealth(player.getMaxHealth());
                giveKit(player, c);
                ModNetwork.sync(player, data);
                data.clean();
                FRAdvancements.award(player, "story/root");
                FRAdvancements.award(player, "story/chose_class");
                Vfx.at(player, VfxKind.LEVEL_UP);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CLASS_CONFIRM.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                Technique t = data.getTechnique();
                player.displayClientMessage(
                    Component.translatable(
                            "message.fallenroses.class_chosen",
                            c.displayName(),
                            t == null ? Component.translatable("technique.fallenroses.none") : t.displayName()
                        )
                        .withStyle(ChatFormatting.WHITE),
                    false
                );
                player.displayClientMessage(Component.translatable("message.fallenroses.controls_hint").withStyle(ChatFormatting.GRAY), false);
            }
        }
    }

    private static void giveKit(ServerPlayer player, PlayerClass c) {
        give(player, new ItemStack(Items.BREAD, 8));
        give(player, new ItemStack(Items.TORCH, 16));
        switch (c) {
            case SORCERER:
                give(player, new ItemStack(ModItems.CURSED_ESSENCE.get(), 3));
                break;
            case HEAVENLY_RESTRICTION:
                give(player, new ItemStack(ModItems.SLAUGHTER_DEMON.get()));
                break;
            case VESSEL:
                give(player, new ItemStack(Items.COOKED_BEEF, 8));
                break;
            case CURSED_SPIRIT:
                give(player, new ItemStack(ModItems.CURSED_ESSENCE.get(), 6));
                break;
            case JUDGE:
                give(player, new ItemStack(Items.WRITABLE_BOOK));
                break;
            case CURSE_MANIPULATOR:
                give(player, new ItemStack(ModItems.CURSED_ESSENCE.get(), 3));
                data(player).setAbsorbed(3);
        }
    }

    private static PlayerData data(ServerPlayer player) {
        return PlayerDataCap.get(player);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private ClassSelection() {
    }
}
