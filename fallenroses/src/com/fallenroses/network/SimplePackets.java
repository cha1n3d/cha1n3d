package com.fallenroses.network;

import net.minecraft.network.FriendlyByteBuf;

public final class SimplePackets {
    private SimplePackets() {
    }

    public static record AbilityKey(int action) {
        public static final int ULTIMATE = 4;
        public static final int REINFORCE = 5;

        public void encode(FriendlyByteBuf buf) {
            buf.writeByte(this.action);
        }

        public static SimplePackets.AbilityKey decode(FriendlyByteBuf buf) {
            return new SimplePackets.AbilityKey(buf.readByte());
        }
    }

    public static record ChooseClass(int playerClass, int technique) {
        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(this.playerClass);
            buf.writeVarInt(this.technique + 1);
        }

        public static SimplePackets.ChooseClass decode(FriendlyByteBuf buf) {
            return new SimplePackets.ChooseClass(buf.readVarInt(), buf.readVarInt() - 1);
        }
    }

    public static record LevelUp(int level, int talentPoints) {
        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(this.level);
            buf.writeVarInt(this.talentPoints);
        }

        public static SimplePackets.LevelUp decode(FriendlyByteBuf buf) {
            return new SimplePackets.LevelUp(buf.readVarInt(), buf.readVarInt());
        }
    }

    public static record OpenIntro(boolean skipStory) {
        public void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(this.skipStory);
        }

        public static SimplePackets.OpenIntro decode(FriendlyByteBuf buf) {
            return new SimplePackets.OpenIntro(buf.readBoolean());
        }
    }

    public static record PlayerAnim(int entity, String animation) {
        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(this.entity);
            buf.writeUtf(this.animation);
        }

        public static SimplePackets.PlayerAnim decode(FriendlyByteBuf buf) {
            return new SimplePackets.PlayerAnim(buf.readVarInt(), buf.readUtf());
        }
    }

    public static record ResetTalents() {
        public void encode(FriendlyByteBuf buf) {
        }

        public static SimplePackets.ResetTalents decode(FriendlyByteBuf buf) {
            return new SimplePackets.ResetTalents();
        }
    }

    public static record ScreenFx(int kind, int color, int duration, float intensity, String key) {
        public static final int FLASH = 0;
        public static final int SHAKE = 1;
        public static final int DOMAIN_TITLE = 2;
        public static final int VIGNETTE = 3;

        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(this.kind);
            buf.writeInt(this.color);
            buf.writeVarInt(this.duration);
            buf.writeFloat(this.intensity);
            buf.writeUtf(this.key);
        }

        public static SimplePackets.ScreenFx decode(FriendlyByteBuf buf) {
            return new SimplePackets.ScreenFx(buf.readVarInt(), buf.readInt(), buf.readVarInt(), buf.readFloat(), buf.readUtf());
        }
    }

    public static record UnlockTalent(int talent) {
        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(this.talent);
        }

        public static SimplePackets.UnlockTalent decode(FriendlyByteBuf buf) {
            return new SimplePackets.UnlockTalent(buf.readVarInt());
        }
    }
}
