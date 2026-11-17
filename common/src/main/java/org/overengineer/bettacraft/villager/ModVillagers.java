package org.overengineer.bettacraft.villager;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.overengineer.bettacraft.platform.registry.DeferredHolder;
import org.overengineer.bettacraft.platform.registry.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.registry.ModBlocks;

/**
 * M47 part a: the point of interest "aquarist" (the {@link BreedingBasinBlock}, one villager per basin, like the vanilla
 * workstations) and the profession {@code bettacraft:aquarist}. The POI is in {@code #minecraft:acquirable_job_site}, so an
 * unemployed villager nearby claims it (B1: no house of its own, in every village). While {@code
 * features.sources.aquaristVillager} is off the profession does not hold or take the basin ({@link Aquarists} also takes the
 * profession away from aquarists that have one).
 */
public final class ModVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, BettaCraft.MOD_ID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, BettaCraft.MOD_ID);

    public static final ResourceKey<PoiType> AQUARIST_POI_KEY = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, BettaCraft.id("aquarist"));

    public static final DeferredHolder<PoiType, PoiType> AQUARIST_POI = POI_TYPES.register("aquarist",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.BREEDING_BASIN.get().getStateDefinition().getPossibleStates()), 1, 1));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> AQUARIST = PROFESSIONS.register("aquarist",
            () -> new VillagerProfession("aquarist", ModVillagers::isBasin, ModVillagers::isBasin, ImmutableSet.of(), ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_FISHERMAN));

    private ModVillagers() {}

    private static boolean isBasin(Holder<PoiType> poi) {
        return poi.is(AQUARIST_POI_KEY) && Features.enabled(Feature.AQUARIST_VILLAGER);
    }

    public static void register() {
        POI_TYPES.register();
        PROFESSIONS.register();
    }
}
