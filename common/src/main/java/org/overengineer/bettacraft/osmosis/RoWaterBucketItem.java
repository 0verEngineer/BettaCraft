package org.overengineer.bettacraft.osmosis;

/**
 * M25: a bucket of reverse osmosis water. Poured into the world it is plain water (like the sea water bucket); into a
 * tank it is a water change with nothing dissolved (TDS 0, pH 6.8); other mods see the fluid {@code bettacraft:ro_water}.
 */
public class RoWaterBucketItem extends org.overengineer.bettacraft.reef.SeaWaterBucketItem {
    public RoWaterBucketItem(Properties properties) {
        super(properties, "tooltip.bettacraft.ro_water_bucket");
    }
}
