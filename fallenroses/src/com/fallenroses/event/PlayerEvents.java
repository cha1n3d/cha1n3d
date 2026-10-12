package com.fallenroses.event;

import com.fallenroses.FallenRoses;
import com.fallenroses.command.FallenRosesCommand;
import com.fallenroses.network.ModNetwork;
import com.fallenroses.player.ClassSelection;
import com.fallenroses.player.PlayerData;
import com.fallenroses.player.PlayerDataCap;
import com.fallenroses.player.Progression;
import com.fallenroses.technique.AbilitySystem;
import com.fallenroses.technique.impl.Heavenly;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
    modid = "fallenroses"
)
public final class PlayerEvents {
    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(PlayerDataCap.ID, new PlayerDataCap.Provider());
        }
    }

    @SubscribeEvent
    public static void clone(Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        PlayerData old = PlayerDataCap.get(original);
        PlayerData data = PlayerDataCap.get(event.getEntity());
        if (old != null && data != null) {
            data.copyFrom(old);
            if (event.isWasDeath()) {
                data.setReinforced(false);
                data.setInfinity(false);
                data.clearCooldowns();
                data.setEnergy(data.getMax() * 0.5F);
            }
        } else {
            FallenRoses.LOGGER
                .error(
                    "Не удалось перенести данные Fallen Roses игрока {} после {}",
                    original.getName().getString(),
                    event.isWasDeath() ? "смерти" : "смены измерения"
                );
        }

        original.invalidateCaps();
    }

    @SubscribeEvent
    public static void login(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDataCap.of(player).ifPresent(PlayerData::markUrgent);
            Progression.applyAttributes(player);
            ClassSelection.startIntroIfNeeded(player, false);
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDataCap.of(player).ifPresent(PlayerData::markUrgent);
            Progression.applyAttributes(player);
            player.setHealth(player.getMaxHealth());
            ClassSelection.startIntroIfNeeded(player, true);
        }
    }

    @SubscribeEvent
    public static void changeDimension(PlayerChangedDimensionEvent event) {
        PlayerDataCap.of(event.getEntity()).ifPresent(PlayerData::markUrgent);
    }

    @SubscribeEvent
    public static void logout(PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClassSelection.forget(player);
            Heavenly.forget(player);
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent event) {
        if (event.phase == Phase.END && event.side == LogicalSide.SERVER && event.player instanceof ServerPlayer player) {
            PlayerData data = PlayerDataCap.get(player);
            if (data != null) {
                if (data.getPlayerClass() != null) {
                    data.tick();
                    AbilitySystem.tick(player, data);
                }

                if (data.isDirty() && (data.isUrgent() || player.tickCount % 4 == 0)) {
                    ModNetwork.sync(player, data);
                    data.clean();
                }
            }
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        FallenRosesCommand.register(event.getDispatcher());
    }

    private PlayerEvents() {
    }
}
