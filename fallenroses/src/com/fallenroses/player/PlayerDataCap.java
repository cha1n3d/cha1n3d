package com.fallenroses.player;

import com.fallenroses.FallenRoses;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlayerDataCap {
    public static final Capability<PlayerData> CAP = CapabilityManager.get(new CapabilityToken<PlayerData>() {
    });
    public static final ResourceLocation ID = FallenRoses.id("player_data");

    public static Optional<PlayerData> of(Entity entity) {
        return entity.getCapability(CAP).resolve();
    }

    @Nullable
    public static PlayerData get(Entity entity) {
        return of(entity).orElse(null);
    }

    private PlayerDataCap() {
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PlayerData data = new PlayerData();
        private final LazyOptional<PlayerData> optional = LazyOptional.of(() -> this.data);

        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return PlayerDataCap.CAP.orEmpty(cap, this.optional);
        }

        public CompoundTag serializeNBT() {
            return this.data.save();
        }

        public void deserializeNBT(CompoundTag tag) {
            this.data.load(tag);
        }

        public void invalidate() {
            this.optional.invalidate();
        }
    }
}
