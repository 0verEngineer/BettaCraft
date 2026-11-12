package org.overengineer.bettacraft.registry;

import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.TankBlockItem;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BettaCraft.MOD_ID);

    public static final Supplier<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.bettacraft"))
            .icon(() -> new ItemStack(ModItems.BETTA_BUCKET.get()))
            .displayItems((params, out) -> {
                // the handbook first (only with GuideME, M6)
                ItemStack book = org.overengineer.bettacraft.progression.GuideBook.createBook();
                if (!book.isEmpty()) out.accept(book);
                // catching: fish bag and a wild betta
                out.accept(ModItems.FISH_BAG.get());
                net.minecraft.util.RandomSource wildRandom = net.minecraft.util.RandomSource.create(7);
                out.accept(org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()),
                        org.overengineer.bettacraft.entity.betta.BettaData.create(org.overengineer.bettacraft.genetics.BettaGenotype.randomWild(wildRandom), true, 0)));
                // One tank per glass type of #c:glass_blocks (tags are bound when the tab is built).
                boolean any = false;
                for (Holder<Block> glass : BuiltInRegistries.BLOCK.getTagOrEmpty(ModTags.TANK_GLASS_BLOCKS)) {
                    if (glass.value().asItem() == net.minecraft.world.item.Items.AIR) continue;
                    ItemStack s = new ItemStack(ModItems.TANK.get());
                    TankBlockItem.setGlass(s, BuiltInRegistries.BLOCK.getKey(glass.value()));
                    TankBlockItem.setStyle(s, org.overengineer.bettacraft.tank.TankStyle.TEXTURED);
                    out.accept(s);
                    any = true;
                }
                if (!any) out.accept(ModItems.TANK.get());
                // M11: the clean tank (clear vanilla glass without texture)
                out.accept(TankBlockItem.of(ModItems.TANK.get(), org.overengineer.bettacraft.tank.TankBlockEntity.DEFAULT_GLASS,
                        org.overengineer.bettacraft.tank.TankStyle.CLEAN, 1));
                // equipment
                out.accept(ModItems.HEATER_SMALL.get());
                out.accept(ModItems.HEATER_MEDIUM.get());
                out.accept(ModItems.HEATER_LARGE.get());
                out.accept(ModItems.COOLER_SMALL.get());
                out.accept(ModItems.COOLER_MEDIUM.get());
                out.accept(ModItems.COOLER_LARGE.get());
                out.accept(ModItems.INTERNAL_FILTER.get());
                out.accept(ModItems.EXTERNAL_FILTER.get());
                // cabinets: one per vanilla planks (modded planks work the same, see the cabinet recipe)
                for (Holder<net.minecraft.world.item.Item> planks : BuiltInRegistries.ITEM.getTagOrEmpty(net.minecraft.tags.ItemTags.PLANKS)) {
                    if (!(planks.value() instanceof net.minecraft.world.item.BlockItem bi)) continue;
                    var id = BuiltInRegistries.BLOCK.getKey(bi.getBlock());
                    if (!"minecraft".equals(id.getNamespace())) continue;
                    out.accept(org.overengineer.bettacraft.equipment.CabinetBlockItem.setWood(new ItemStack(ModItems.CABINET.get()), id));
                }
                out.accept(ModItems.FEEDER.get());
                out.accept(ModItems.TANK_LIGHT.get());
                out.accept(ModItems.TANK_LID.get());
                out.accept(ModItems.MIRROR_STICK.get());
                out.accept(ModItems.AQUARIUM_NET.get());
                // floating plants (M12)
                for (var p : ModItems.FLOATING_PLANTS.values()) out.accept(p.get());
                // food chain (in crafting order)
                out.accept(ModItems.SWAMP_WATER_BOTTLE.get());
                out.accept(ModItems.MOSQUITO_LARVAE.get());
                out.accept(ModItems.BETTA_PELLETS.get());
                out.accept(ModItems.SALT_WATER_BOTTLE.get());
                out.accept(ModItems.ARTEMIA_CYSTS.get());
                out.accept(ModItems.INFUSORIA_CULTURE.get());
                out.accept(ModItems.INFUSORIA.get());
                out.accept(ModItems.ARTEMIA_CULTURE.get());
                out.accept(ModItems.ARTEMIA_NAUPLII.get());
                out.accept(ModItems.LARVA_TUB.get());
                out.accept(ModItems.ARTEMIA_HATCHERY.get());
                // decoration
                for (var d : ModItems.DECOR.values()) out.accept(d.get());
                // one bucket per fin form (long-finned tails and the plakat forms), then one per named variety (its example fish)
                net.minecraft.util.RandomSource r = net.minecraft.util.RandomSource.create(42);
                for (String length : java.util.List.of(org.overengineer.bettacraft.genetics.BettaGenome.LONG, org.overengineer.bettacraft.genetics.BettaGenome.SHORT)) {
                    for (String shape : org.overengineer.bettacraft.genetics.BettaGenome.SHAPES) {
                        var g = org.overengineer.bettacraft.genetics.BettaGenotype.randomDomestic(r)
                                .with(org.overengineer.bettacraft.genetics.BettaLoci.FINFORM, new org.overengineer.bettacraft.genetics.BettaGenotype.AllelePair(shape, shape))
                                .with(org.overengineer.bettacraft.genetics.BettaLoci.FIN_LENGTH, new org.overengineer.bettacraft.genetics.BettaGenotype.AllelePair(length, length));
                        out.accept(org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()),
                                org.overengineer.bettacraft.entity.betta.BettaData.create(g, false, 0)));
                    }
                }
                for (var v : org.overengineer.bettacraft.genetics.GeneticsData.get().varieties()) {
                    if (v.example().isEmpty()) continue;
                    var ph = org.overengineer.bettacraft.genetics.BettaGenome.of(v.example(), org.overengineer.bettacraft.genetics.BettaGenome.DEFAULT);
                    out.accept(org.overengineer.bettacraft.entity.betta.BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()),
                            org.overengineer.bettacraft.entity.betta.BettaData.create(ph, false, 0)));
                }
                out.accept(ModItems.BETTA_SPAWN_EGG.get());
            })
            .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    private ModCreativeTabs() {}
}
