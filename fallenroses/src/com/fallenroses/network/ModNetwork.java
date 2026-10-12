package com.fallenroses.network;

import com.fallenroses.FallenRoses;
import com.fallenroses.client.ClientPackets;
import com.fallenroses.player.ClassSelection;
import com.fallenroses.player.PlayerData;
import com.fallenroses.player.Progression;
import com.fallenroses.technique.AbilitySystem;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "4";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(FallenRoses.id("main"), () -> "4", "4"::equals, "4"::equals);
    private static int nextId;

    public static void register() {
        toClient(
            SyncDataPacket.class,
            SyncDataPacket::encode,
            SyncDataPacket::decode,
            msg -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPackets.sync(msg))
        );
        toClient(VfxPacket.class, VfxPacket::encode, VfxPacket::decode, msg -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPackets.vfx(msg)));
        toClient(
            SimplePackets.PlayerAnim.class,
            SimplePackets.PlayerAnim::encode,
            SimplePackets.PlayerAnim::decode,
            msg -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPackets.playerAnim(msg))
        );
        toClient(
            SimplePackets.OpenIntro.class,
            SimplePackets.OpenIntro::encode,
            SimplePackets.OpenIntro::decode,
            msg -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPackets.openIntro(msg))
        );
        toClient(
            SimplePackets.LevelUp.class,
            SimplePackets.LevelUp::encode,
            SimplePackets.LevelUp::decode,
            msg -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPackets.levelUp(msg))
        );
        toClient(
            SimplePackets.ScreenFx.class,
            SimplePackets.ScreenFx::encode,
            SimplePackets.ScreenFx::decode,
            msg -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPackets.screenFx(msg))
        );
        toServer(
            SimplePackets.AbilityKey.class,
            SimplePackets.AbilityKey::encode,
            SimplePackets.AbilityKey::decode,
            (msg, p) -> AbilitySystem.onKey(p, msg.action())
        );
        toServer(
            SimplePackets.ChooseClass.class,
            SimplePackets.ChooseClass::encode,
            SimplePackets.ChooseClass::decode,
            (msg, p) -> ClassSelection.choose(p, msg.playerClass(), msg.technique())
        );
        toServer(
            SimplePackets.UnlockTalent.class,
            SimplePackets.UnlockTalent::encode,
            SimplePackets.UnlockTalent::decode,
            (msg, p) -> Progression.unlockTalent(p, msg.talent())
        );
        toServer(
            SimplePackets.ResetTalents.class, SimplePackets.ResetTalents::encode, SimplePackets.ResetTalents::decode, (msg, p) -> Progression.resetTalents(p)
        );
    }

    private static <T> void toClient(Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, Consumer<T> handler) {
        CHANNEL.messageBuilder(type, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(encoder)
            .decoder(decoder)
            .consumerMainThread((msg, ctx) -> handler.accept(msg))
            .add();
    }

    private static <T> void toServer(
        Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, ServerPlayer> handler
    ) {
        CHANNEL.messageBuilder(type, nextId++, NetworkDirection.PLAY_TO_SERVER).encoder(encoder).decoder(decoder).consumerMainThread((msg, ctx) -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                handler.accept(msg, player);
            }
        }).add();
    }

    public static void sync(ServerPlayer player, PlayerData data) {
        send(player, SyncDataPacket.of(data));
    }

    public static void send(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendTracking(Entity entity, Object message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    private ModNetwork() {
    }
}
