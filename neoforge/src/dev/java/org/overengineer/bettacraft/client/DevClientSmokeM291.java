package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.plants.AquaticPlantItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M29.1 client smoke ({@code -Psmoketestm291}): every block item of the mod (and the items that place a device block:
 * heaters, coolers, internal filters, the flow pump) in a grid with its id, as the inventory draws them, on a few pages.
 * Screenshots {@code bettacraft_m291_items_<page>} (big icons) and {@code bettacraft_m291_items_small_<page>} (16 px,
 * like the hotbar).
 */
final class DevClientSmokeM291 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    /** Items that are no {@link BlockItem} but place a device block in the tank. */
    private static final Set<String> DEVICES = Set.of("heater_small", "heater_medium", "heater_large", "cooler_small", "cooler_medium",
            "cooler_large", "internal_filter", "sponge_filter", "corner_filter", "flow_pump");

    /** Close-ups (4x). */
    private static final List<String> ZOOM = List.of("crucible", "daphnia_barrel", "food_maker", "monitor", "tank_light", "led_panel",
            "high_power_led", "heater_large", "internal_filter", "sponge_filter", "tubifex_dish", "larva_tub", "artemia_hatchery", "bio_co2", "clip_feeder");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;

    DevClientSmokeM291() {
        List<ItemStack> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(BettaCraft.MOD_ID)) continue;
            if (item instanceof BlockItem || DEVICES.contains(id.getPath())) items.add(new ItemStack(item));
        }
        // the cabinet in another wood and in stone (the item model follows the material)
        for (String m : new String[]{"minecraft:birch_planks", "minecraft:crimson_planks", "minecraft:stone_bricks", "minecraft:polished_deepslate"}) {
            items.add(org.overengineer.bettacraft.equipment.CabinetBlockItem.setWood(
                    new ItemStack(org.overengineer.bettacraft.registry.ModItems.CABINET.get()), Identifier.parse(m)));
        }
        // the plants last (flat icons on purpose, like vanilla saplings)
        items.sort((a, b) -> Boolean.compare(a.getItem() instanceof AquaticPlantItem || plant(a), b.getItem() instanceof AquaticPlantItem || plant(b)));
        LOGGER.info("[smoketest] M29.1 item grid: {} block items", items.size());
        int prevScale = mc.options.guiScale().get();
        then(1, () -> mc.options.guiScale().set(2));
        then(1, mc::resizeGui);
        int perPage = Grid.COLS * Grid.ROWS;
        int pages = (items.size() + perPage - 1) / perPage;
        for (int p = 0; p < pages; p++) {
            List<ItemStack> page = items.subList(p * perPage, Math.min(items.size(), (p + 1) * perPage));
            int n = p + 1;
            then(1, () -> mc.setScreen(new Grid(page, 2F)));
            then(15, () -> shot("items_" + n));
        }
        int smallPer = Grid.COLS * 2 * Grid.ROWS;
        for (int p = 0; p * smallPer < items.size(); p++) {
            List<ItemStack> page = items.subList(p * smallPer, Math.min(items.size(), (p + 1) * smallPer));
            int n = p + 1;
            then(1, () -> mc.setScreen(new Grid(page, 1F)));
            then(15, () -> shot("items_small_" + n));
        }
        // close-ups of the icons that were hard to read before M29.1
        List<ItemStack> zoom = new ArrayList<>();
        for (String id : ZOOM) zoom.add(new ItemStack(BuiltInRegistries.ITEM.getValue(BettaCraft.id(id))));
        then(1, () -> mc.setScreen(new Grid(zoom, 4F)));
        then(15, () -> shot("items_zoom"));
        then(1, () -> mc.setScreen(null));
        then(1, () -> mc.options.guiScale().set(prevScale));
        then(1, mc::resizeGui);
    }

    private static boolean plant(ItemStack s) {
        String path = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
        return path.startsWith("wild_");
    }

    /** A plain screen with the items in a grid, each with its id below. */
    private static final class Grid extends Screen {
        static final int COLS = 10, ROWS = 8;
        private final List<ItemStack> items;
        private final float scale;

        Grid(List<ItemStack> items, float scale) {
            super(Component.literal("items"));
            this.items = items;
            this.scale = scale;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            g.fill(0, 0, width, height, 0xFF8B8B8B);
            int cols = scale > 3F ? 5 : scale > 1.5F ? COLS : COLS * 2;
            int cw = width / cols, ch = height / (scale > 3F ? 3 : ROWS);
            for (int i = 0; i < items.size(); i++) {
                int cx = (i % cols) * cw, cy = (i / cols) * ch;
                g.fill(cx + 1, cy + 1, cx + cw - 1, cy + ch - 1, 0xFFC6C6C6);
                var pose = g.pose();
                pose.pushMatrix();
                pose.translate(cx + (cw - 16 * scale) / 2F, cy + 3);
                pose.scale(scale, scale);
                g.item(items.get(i), 0, 0);
                pose.popMatrix();
                String id = BuiltInRegistries.ITEM.getKey(items.get(i).getItem()).getPath();
                pose.pushMatrix();
                pose.translate(cx + 2, cy + 5 + 16 * scale);
                pose.scale(0.5F, 0.5F);
                int max = (cw - 4) * 2;
                String s = font.plainSubstrByWidth(id, max);
                g.text(font, s, 0, 0, 0xFF202020, false);
                if (s.length() < id.length()) g.text(font, font.plainSubstrByWidth(id.substring(s.length()), max), 0, 9, 0xFF202020, false);
                pose.popMatrix();
            }
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {}

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private void then(int delay, Runnable r) {
        steps.add(new Step(delay, r));
    }

    /** Runs the next step when its delay is over; true when all steps are done. */
    boolean tick() {
        if (wait > 0) {
            wait--;
            return false;
        }
        Step s = steps.poll();
        if (s == null) return true;
        s.action().run();
        if (!steps.isEmpty()) wait = steps.peek().delay();
        return false;
    }

    private void shot(String name) {
        Screenshot.grab(mc.gameDirectory, "bettacraft_m291_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
