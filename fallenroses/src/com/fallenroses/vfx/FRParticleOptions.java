package com.fallenroses.vfx;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleOptions.Deserializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.registries.ForgeRegistries;

public record FRParticleOptions(ParticleType<FRParticleOptions> type, int color, float scale, int life) implements ParticleOptions {
    public static final Deserializer<FRParticleOptions> DESERIALIZER = new Deserializer<FRParticleOptions>() {
        public FRParticleOptions fromCommand(ParticleType<FRParticleOptions> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int color = Integer.parseInt(reader.readUnquotedString(), 16);
            reader.expect(' ');
            float scale = reader.readFloat();
            reader.expect(' ');
            int life = reader.readInt();
            return new FRParticleOptions(type, color, scale, life);
        }

        public FRParticleOptions fromNetwork(ParticleType<FRParticleOptions> type, FriendlyByteBuf buf) {
            return new FRParticleOptions(type, buf.readInt(), buf.readFloat(), buf.readVarInt());
        }
    };

    @Override
    public ParticleType<?> getType() {
        return this.type;
    }

    public float r() {
        return (float)(this.color >> 16 & 0xFF) / 255.0F;
    }

    public float g() {
        return (float)(this.color >> 8 & 0xFF) / 255.0F;
    }

    public float b() {
        return (float)(this.color & 0xFF) / 255.0F;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeInt(this.color);
        buf.writeFloat(this.scale);
        buf.writeVarInt(this.life);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %06x %.2f %d", ForgeRegistries.PARTICLE_TYPES.getKey(this.type), this.color, this.scale, this.life);
    }

    public static Codec<FRParticleOptions> codec(ParticleType<FRParticleOptions> type) {
        return RecordCodecBuilder.create(
            inst -> inst.group(
                        Codec.INT.fieldOf("color").forGetter(FRParticleOptions::color),
                        Codec.FLOAT.fieldOf("scale").forGetter(FRParticleOptions::scale),
                        Codec.INT.fieldOf("life").forGetter(FRParticleOptions::life)
                    )
                    .apply(inst, (c, s, l) -> new FRParticleOptions(type, c, s, l))
        );
    }
}
