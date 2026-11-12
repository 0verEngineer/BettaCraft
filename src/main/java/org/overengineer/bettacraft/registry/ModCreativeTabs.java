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
                // One tank per glass type of #c:glass_blocks (tags are bound when the tab is built).
                boolean any = false;
                for (Holder<Block> glass : BuiltInRegistries.BLOCK.getTagOrEmpty(ModTags.TANK_GLASS_BLOCKS)) {
                    if (glass.value().asItem() == net.minecraft.world.item.Items.AIR) continue;
                    ItemStack s = new ItemStack(ModItems.TANK.get());
                    TankBlockItem.setGlass(s, BuiltInRegistries.BLOCK.getKey(glass.value()));
                    out.accept(s);
                    any = true;
                }
                if (!any) out.accept(ModItems.TANK.get());
                out.accept(ModItems.BETTA_BUCKET.get());
                out.accept(ModItems.FISH_BAG.get());
                out.accept(ModItems.BETTA_PELLETS.get());
                out.accept(ModItems.MOSQUITO_LARVAE.get());
                out.accept(ModItems.HEATER_SMALL.get());
                out.accept(ModItems.HEATER_MEDIUM.get());
                out.accept(ModItems.HEATER_LARGE.get());
            })
            .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    private ModCreativeTabs() {}
}
