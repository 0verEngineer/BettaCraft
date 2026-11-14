package org.overengineer.bettacraft.tank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nullable;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.overengineer.bettacraft.registry.ModDataComponents;

/**
 * M24: the values a portion of water carries through pipes (data component {@code bettacraft:water} on the fluid stack):
 * temperature (°C), salt (ppt), calcium (mg/L), quality (0..1) and nitrate (mg/L). Water without the component is tap
 * water (fresh, clean, no nitrate, the temperature of the place it comes in).
 */
public record WaterValues(float temp, float salinity, float calcium, float quality, float nitrate) {
    public static final Codec<WaterValues> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("temp").forGetter(WaterValues::temp),
            Codec.FLOAT.optionalFieldOf("salinity", 0F).forGetter(WaterValues::salinity),
            Codec.FLOAT.optionalFieldOf("calcium", 0F).forGetter(WaterValues::calcium),
            Codec.FLOAT.optionalFieldOf("quality", 1F).forGetter(WaterValues::quality),
            Codec.FLOAT.optionalFieldOf("nitrate", 0F).forGetter(WaterValues::nitrate)).apply(i, WaterValues::new));

    public static final StreamCodec<ByteBuf, WaterValues> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, WaterValues::temp, ByteBufCodecs.FLOAT, WaterValues::salinity, ByteBufCodecs.FLOAT, WaterValues::calcium,
            ByteBufCodecs.FLOAT, WaterValues::quality, ByteBufCodecs.FLOAT, WaterValues::nitrate, WaterValues::new);

    /** The values of the water in a tank now. */
    public static WaterValues of(TankData t) {
        return new WaterValues(round(t.waterTemp()), round(t.salinity()), Math.round(t.calcium()), round(t.waterQuality()), round(t.nitrate()));
    }

    /** Tap water at a temperature (fresh, clean). */
    public static WaterValues tap(float temp) {
        return new WaterValues(round(temp), 0F, 0F, 1F, 0F);
    }

    /** Rounded so that portions of the same water stack together in other mods' tanks (two decimals). */
    private static float round(float v) {
        return Math.round(v * 100F) / 100F;
    }

    /** The values a fluid stack carries, or null (tap water or a fluid without them). */
    @Nullable
    public static WaterValues from(FluidStack stack) {
        return stack.isEmpty() ? null : stack.get(ModDataComponents.WATER_VALUES.get());
    }

    /** Water with these values. */
    public static FluidStack stack(int mb, @Nullable WaterValues v) {
        FluidStack s = new FluidStack(Fluids.WATER, mb);
        if (v != null) s.set(ModDataComponents.WATER_VALUES.get(), v);
        return s;
    }

    /** Whether this water counts as fresh (little salt), for the top-off. */
    public boolean fresh() {
        return salinity < Salinity.BRACKISH_FROM;
    }
}
