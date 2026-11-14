package org.overengineer.bettacraft.tank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nullable;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.overengineer.bettacraft.chem.WaterChemistry;
import org.overengineer.bettacraft.registry.ModDataComponents;

/**
 * M24: the values a portion of water carries through pipes (data component {@code bettacraft:water} on the fluid stack):
 * temperature (°C), salt (ppt), calcium (mg/L), quality (0..1) and nitrate (mg/L); M25: TDS (ppm) and pH. Water without
 * the component is tap water (fresh, clean, no nitrate, TDS 250, pH 7.5, the temperature of the place it comes in).
 */
public record WaterValues(float temp, float salinity, float calcium, float quality, float nitrate, float tds, float ph) {
    public static final Codec<WaterValues> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("temp").forGetter(WaterValues::temp),
            Codec.FLOAT.optionalFieldOf("salinity", 0F).forGetter(WaterValues::salinity),
            Codec.FLOAT.optionalFieldOf("calcium", 0F).forGetter(WaterValues::calcium),
            Codec.FLOAT.optionalFieldOf("quality", 1F).forGetter(WaterValues::quality),
            Codec.FLOAT.optionalFieldOf("nitrate", 0F).forGetter(WaterValues::nitrate),
            Codec.FLOAT.optionalFieldOf("tds", WaterChemistry.TAP_TDS).forGetter(WaterValues::tds),
            Codec.FLOAT.optionalFieldOf("ph", WaterChemistry.TAP_PH).forGetter(WaterValues::ph)).apply(i, WaterValues::new));

    public static final StreamCodec<ByteBuf, WaterValues> STREAM_CODEC = StreamCodec.of((buf, v) -> {
        buf.writeFloat(v.temp);
        buf.writeFloat(v.salinity);
        buf.writeFloat(v.calcium);
        buf.writeFloat(v.quality);
        buf.writeFloat(v.nitrate);
        buf.writeFloat(v.tds);
        buf.writeFloat(v.ph);
    }, buf -> new WaterValues(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()));

    /** M24 values (tap water's TDS and pH). */
    public WaterValues(float temp, float salinity, float calcium, float quality, float nitrate) {
        this(temp, salinity, calcium, quality, nitrate, WaterChemistry.TAP_TDS, WaterChemistry.TAP_PH);
    }

    /** The values of the water in a tank now. */
    public static WaterValues of(TankData t) {
        return new WaterValues(round(t.waterTemp()), round(t.salinity()), Math.round(t.calcium()), round(t.waterQuality()), round(t.nitrate()),
                Math.round(t.tds()), round(t.ph()));
    }

    /** Tap water at a temperature (fresh, clean). */
    public static WaterValues tap(float temp) {
        return new WaterValues(round(temp), 0F, 0F, 1F, 0F);
    }

    /** M25: reverse osmosis water at a temperature (fresh, clean, nothing dissolved; part b makes it). */
    public static WaterValues ro(float temp) {
        return new WaterValues(round(temp), 0F, 0F, 1F, 0F, WaterChemistry.RO_TDS, WaterChemistry.RO_PH);
    }

    /** M25: sea water at a temperature (35 ppt with the calcium of the sea, clean, the pH of the sea). */
    public static WaterValues sea(float temp) {
        return new WaterValues(round(temp), Salinity.SEA, Salinity.calciumOf(Salinity.SEA), 1F, 0F, WaterChemistry.TAP_TDS, WaterChemistry.SEA_PH);
    }

    /** M25: temperature of reverse osmosis water and sea water out of other mods' tanks (room temperature). */
    public static final float FLUID_TEMP = 20F;

    /** Rounded so that portions of the same water stack together in other mods' tanks (two decimals). */
    private static float round(float v) {
        return Math.round(v * 100F) / 100F;
    }

    /**
     * The values a fluid stack carries, or null (tap water or a fluid without them). M25: reverse osmosis water and sea
     * water ({@link org.overengineer.bettacraft.registry.ModFluids}) without values are pure: {@link #ro} and {@link #sea}
     * at room temperature.
     */
    @Nullable
    public static WaterValues from(FluidStack stack) {
        return from(stack, FLUID_TEMP);
    }

    /** As {@link #from(FluidStack)}, with the temperature reverse osmosis and sea water have where they arrive. */
    @Nullable
    public static WaterValues from(FluidStack stack, float temp) {
        if (stack.isEmpty()) return null;
        WaterValues v = stack.get(ModDataComponents.WATER_VALUES.get());
        if (v != null) return v;
        if (stack.getFluid().isSame(org.overengineer.bettacraft.registry.ModFluids.RO_WATER.get())) return ro(temp);
        if (stack.getFluid().isSame(org.overengineer.bettacraft.registry.ModFluids.SEA_WATER.get())) return sea(temp);
        return null;
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
