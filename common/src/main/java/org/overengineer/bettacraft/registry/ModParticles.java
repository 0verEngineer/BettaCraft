package org.overengineer.bettacraft.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.overengineer.bettacraft.platform.registry.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;

/** M23: particle types: the air bubble that rises to a given height (bubble columns of air stones, also without water blocks). */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, BettaCraft.MOD_ID);

    /** An air bubble rising up to {@code top} (world y), {@code size} = quad size in blocks. */
    public record AirBubble(float top, float size) implements ParticleOptions {
        public static final MapCodec<AirBubble> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.FLOAT.fieldOf("top").forGetter(AirBubble::top), Codec.FLOAT.fieldOf("size").forGetter(AirBubble::size)).apply(i, AirBubble::new));
        public static final StreamCodec<ByteBuf, AirBubble> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, AirBubble::top, ByteBufCodecs.FLOAT, AirBubble::size, AirBubble::new);

        @Override
        public ParticleType<?> getType() {
            return AIR_BUBBLE.get();
        }
    }

    public static final Supplier<ParticleType<AirBubble>> AIR_BUBBLE = PARTICLES.register("air_bubble", () -> new ParticleType<AirBubble>(false) {
        @Override
        public MapCodec<AirBubble> codec() {
            return AirBubble.CODEC;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, AirBubble> streamCodec() {
            return AirBubble.STREAM_CODEC;
        }
    });

    public static void register() {
        PARTICLES.register();
    }

    private ModParticles() {}
}
