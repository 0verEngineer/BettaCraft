package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.compat.info.InfoData;
import org.overengineer.bettacraft.compat.info.InfoLines;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.equipment.MirrorStickItem;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankBlockItem;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankRecipe;
import org.overengineer.bettacraft.tank.TankStyle;
import org.overengineer.bettacraft.tank.TankStyleRecipe;

/**
 * M11 GameTests: tank styles (recipes for textured and clean tanks and the conversion, styles never merge, drops keep
 * the style, an M10 tank block entity loads as textured), the glass in the info data, and the mirror stick making males
 * flare.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StyleGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final ResourceLocation GLASS = ResourceLocation.withDefaultNamespace("glass");
    private static final ResourceLocation LIGHT_BLUE = ResourceLocation.withDefaultNamespace("light_blue_stained_glass");

    private StyleGameTests() {}

    private static void add(GameTestHelper h, BlockPos rel, ResourceLocation glass, TankStyle style) {
        h.setBlock(rel, ModBlocks.TANK.get().defaultBlockState());
        if (h.getBlockEntity(rel) instanceof TankBlockEntity be) {
            be.setGlassId(glass);
            be.setStyle(style);
        }
        TankMultiblock.onTankBlockAdded(h.getLevel(), h.absolutePos(rel), null);
    }

    private static TankBlockEntity controller(GameTestHelper h, BlockPos rel) {
        TankBlockEntity c = TankMultiblock.getController(h.getLevel(), h.absolutePos(rel));
        if (c == null) throw new GameTestAssertException("no controller for " + rel);
        return c;
    }

    private static CraftingInput tankGrid(Item glass, Item middle) {
        List<ItemStack> l = new ArrayList<>();
        for (Item i : new Item[]{glass, glass, glass, glass, middle, glass, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT}) l.add(new ItemStack(i));
        return CraftingInput.of(3, 3, l);
    }

    private static ItemStack craft(ServerLevel level, CraftingInput in) {
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, in, level)
                .map(r -> r.value().assemble(in, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    private static CraftingInput single(ItemStack s) {
        return CraftingInput.of(1, 1, List.of(s));
    }

    // ------------------------------------------------------------------ recipes

    @GameTest(template = EMPTY)
    public static void stylesHaveTheirOwnRecipes(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        // textured: any glass, iron ingot in the middle
        var textured = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, tankGrid(Items.LIGHT_BLUE_STAINED_GLASS, Items.IRON_INGOT), level);
        h.assertTrue(textured.isPresent() && textured.get().value() instanceof TankRecipe, "textured tank recipe");
        ItemStack t = craft(level, tankGrid(Items.LIGHT_BLUE_STAINED_GLASS, Items.IRON_INGOT));
        h.assertTrue(t.getCount() == 4 && TankBlockItem.getGlass(t).equals(LIGHT_BLUE) && TankBlockItem.getStyle(t) == TankStyle.TEXTURED,
                "4 textured light blue tanks: " + t + " " + t.getComponents());
        // clean: vanilla glass, iron nugget in the middle
        ItemStack c = craft(level, tankGrid(Items.GLASS, Items.IRON_NUGGET));
        h.assertTrue(c.getCount() == 4 && TankBlockItem.getGlass(c).equals(GLASS) && TankBlockItem.getStyle(c) == TankStyle.CLEAN,
                "4 clean tanks: " + c + " " + c.getComponents());
        h.assertTrue(craft(level, tankGrid(Items.LIGHT_BLUE_STAINED_GLASS, Items.IRON_NUGGET)).isEmpty(), "clean only from vanilla glass");
        // mixed glass never works
        List<ItemStack> mixed = new ArrayList<>(tankGrid(Items.GLASS, Items.IRON_INGOT).items());
        mixed.set(0, new ItemStack(Items.TINTED_GLASS));
        h.assertTrue(craft(level, CraftingInput.of(3, 3, mixed)).isEmpty(), "mixed glass");
        // conversion of one vanilla-glass tank, also an item made before M11 (no style component)
        ItemStack old = TankBlockItem.setGlass(new ItemStack(ModItems.TANK.get()), GLASS);
        h.assertTrue(!old.has(ModDataComponents.TANK_STYLE.get()), "pre-M11 item");
        var toClean = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, single(old), level);
        h.assertTrue(toClean.isPresent() && toClean.get().value() instanceof TankStyleRecipe r && r.from() == TankStyle.TEXTURED, "conversion recipe");
        ItemStack converted = craft(level, single(old));
        h.assertTrue(converted.getCount() == 1 && TankBlockItem.getStyle(converted) == TankStyle.CLEAN, "old tank -> clean");
        ItemStack back = craft(level, single(converted));
        h.assertTrue(back.getCount() == 1 && TankBlockItem.getStyle(back) == TankStyle.TEXTURED && TankBlockItem.getGlass(back).equals(GLASS), "clean -> textured");
        h.assertTrue(craft(level, single(TankBlockItem.of(ModItems.TANK.get(), LIGHT_BLUE, TankStyle.TEXTURED, 1))).isEmpty(),
                "no clean variant of stained glass");
        h.succeed();
    }

    // ------------------------------------------------------------------ merging, drops, migration

    @GameTest(template = EMPTY)
    public static void stylesNeverMerge(GameTestHelper h) {
        add(h, new BlockPos(2, 1, 2), GLASS, TankStyle.TEXTURED);
        add(h, new BlockPos(3, 1, 2), GLASS, TankStyle.CLEAN);
        h.assertTrue(controller(h, new BlockPos(2, 1, 2)) != controller(h, new BlockPos(3, 1, 2)), "textured and clean stay separate");
        add(h, new BlockPos(4, 1, 2), GLASS, TankStyle.CLEAN);
        h.assertTrue(controller(h, new BlockPos(3, 1, 2)) == controller(h, new BlockPos(4, 1, 2)), "two clean blocks join");
        add(h, new BlockPos(2, 1, 3), LIGHT_BLUE, TankStyle.TEXTURED);
        h.assertTrue(controller(h, new BlockPos(2, 1, 2)) != controller(h, new BlockPos(2, 1, 3)), "other glass stays separate");
        add(h, new BlockPos(2, 1, 1), GLASS, TankStyle.TEXTURED);
        TankBlockEntity t = controller(h, new BlockPos(2, 1, 1));
        h.assertTrue(t == controller(h, new BlockPos(2, 1, 2)) && t.getTank().memberCount() == 2, "same glass and style join");
        // the drop keeps glass and style
        ServerLevel level = h.getLevel();
        BlockPos p = h.absolutePos(new BlockPos(4, 1, 2));
        List<ItemStack> drops = Block.getDrops(level.getBlockState(p), level, p, level.getBlockEntity(p));
        h.assertTrue(drops.size() == 1 && TankBlockItem.getStyle(drops.getFirst()) == TankStyle.CLEAN
                && TankBlockItem.getGlass(drops.getFirst()).equals(GLASS), "clean tank drops a clean tank: " + drops);
        h.succeed();
    }

    /** A tank block entity as M10 saved it (schema 5, no style) loads as a textured tank and keeps everything else. */
    @GameTest(template = EMPTY)
    public static void m10TankLoadsAsTextured(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        add(h, new BlockPos(2, 1, 2), LIGHT_BLUE, TankStyle.CLEAN);
        add(h, new BlockPos(3, 1, 2), LIGHT_BLUE, TankStyle.CLEAN);
        TankBlockEntity c = controller(h, new BlockPos(2, 1, 2));
        TankInteractions.changeWater(level, c, 1500);
        c.getTank().setName("M10 tank");
        CompoundTag be = c.saveWithFullMetadata(level.registryAccess());
        h.assertTrue(be.getInt("schema") >= 6 && "clean".equals(be.getString("style")), "M11 and later write schema 6+ with the style");
        be.putInt("schema", 5);
        be.remove("style");
        c.loadWithComponents(be, level.registryAccess());
        h.assertTrue(c.getStyle() == TankStyle.TEXTURED && c.getGlassId().equals(LIGHT_BLUE), "M10 tank is textured, glass kept");
        h.assertTrue(c.getTank().memberCount() == 2 && c.getTank().waterMb() == 1500 && "M10 tank".equals(c.getTank().name()), "tank data kept");
        TankBlockEntity member = (TankBlockEntity) h.getBlockEntity(new BlockPos(3, 1, 2));
        CompoundTag mt = member.saveWithFullMetadata(level.registryAccess());
        mt.putInt("schema", 5);
        mt.remove("style");
        member.loadWithComponents(mt, level.registryAccess());
        h.assertTrue(member.getStyle() == TankStyle.TEXTURED, "member block is textured");
        // a new textured block of that glass now joins the migrated tank
        add(h, new BlockPos(2, 1, 3), LIGHT_BLUE, TankStyle.TEXTURED);
        h.assertTrue(controller(h, new BlockPos(2, 1, 3)) == controller(h, new BlockPos(2, 1, 2)), "joins the migrated tank");
        h.succeed();
    }

    // ------------------------------------------------------------------ info overlays

    @GameTest(template = EMPTY)
    public static void infoDataNamesTheGlass(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        add(h, new BlockPos(2, 1, 2), LIGHT_BLUE, TankStyle.TEXTURED);
        CompoundTag d = InfoData.tank(level, h.absolutePos(new BlockPos(2, 1, 2)));
        h.assertTrue(d != null && LIGHT_BLUE.toString().equals(d.getString("glass")) && "textured".equals(d.getString("style")), "glass in the info data: " + d);
        List<Component> lines = InfoLines.tank(d, false);
        boolean found = false;
        for (Component l : lines) {
            if (l.getContents() instanceof TranslatableContents tc && tc.getKey().equals("jade.bettacraft.tank.glass")) {
                Object name = tc.getArgs()[0];
                found = name instanceof Component nc && nc.getContents() instanceof TranslatableContents n
                        && n.getKey().equals(Blocks.LIGHT_BLUE_STAINED_GLASS.getDescriptionId())
                        && tc.getArgs()[1] instanceof Component sc && sc.getContents() instanceof TranslatableContents s
                        && s.getKey().equals(TankStyle.TEXTURED.key());
            }
        }
        h.assertTrue(found, "overlay line with the localized glass name: " + lines);
        add(h, new BlockPos(6, 1, 2), GLASS, TankStyle.CLEAN);
        CompoundTag dc = InfoData.tank(level, h.absolutePos(new BlockPos(6, 1, 2)));
        h.assertTrue("clean".equals(dc.getString("style")), "clean style in the info data");
        // glass of a removed mod: the id is shown
        Component gone = InfoLines.glassLine("othermod:gone_glass", "textured");
        h.assertTrue(gone.getString().contains("othermod:gone_glass"), "unknown glass shows its id: " + gone.getString());
        h.succeed();
    }

    // ------------------------------------------------------------------ mirror stick

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void mirrorStickMakesMalesFlare(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++) add(h, new BlockPos(x, 1, z), GLASS, TankStyle.CLEAN);
        TankBlockEntity c = controller(h, new BlockPos(2, 1, 2));
        TankInteractions.changeWater(level, c, 9000);
        BettaEntity male = insert(h, c, false, 3);
        BettaEntity female = insert(h, c, true, 4);
        for (BettaEntity b : List.of(male, female)) {
            b.setNoAi(true);
            b.setNoGravity(true);
        }
        Vec3 centre = Vec3.atCenterOf(h.absolutePos(new BlockPos(3, 1, 3)));
        male.moveTo(centre.x - 0.4, centre.y, centre.z, 0F, 0F);
        female.moveTo(centre.x + 0.4, centre.y, centre.z, 0F, 0F);
        ServerPlayer p = MockPlayers.serverPlayer(h);
        p.moveTo(centre.x, centre.y - 1.62 + 0.1, centre.z - 3.5, 0F, 0F);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, centre);
        // no mirror: nothing happens
        h.assertTrue(!MirrorStickItem.holds(p), "empty hands");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MIRROR_STICK.get()));
        h.assertTrue(MirrorStickItem.holds(p), "holds the mirror");
        // looking away: no flare
        p.lookAt(EntityAnchorArgument.Anchor.EYES, centre.add(0, 0, -8));
        h.assertTrue(MirrorStickItem.flareNear(level, p) == 0, "males behind the player do not flare");
        p.lookAt(EntityAnchorArgument.Anchor.EYES, centre);
        int n = MirrorStickItem.flareNear(level, p);
        h.assertTrue(n == 1, "only the adult male flares: " + n);
        h.assertTrue(male.breeding().isTrained() && !female.breeding().isTrained(), "mirror training of the male");
        h.runAfterDelay(3, () -> {
            h.assertTrue(male.isFlaring(), "male flares (synced flag)");
            h.assertTrue(!female.isFlaring(), "female does not");
            CompoundTag info = InfoData.betta(male);
            h.assertTrue(info.getBoolean("trained") && info.getBoolean("flaring"), "info overlay shows the training: " + info);
            // an opaque wall between the player and the tank blocks the view
            for (int x = 1; x <= 5; x++) for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, 1), Blocks.STONE);
            p.moveTo(centre.x, centre.y - 1.62 + 0.1, centre.z - 3.5, 0F, 0F);
            p.lookAt(EntityAnchorArgument.Anchor.EYES, centre);
            h.assertTrue(MirrorStickItem.flareNear(level, p) == 0, "no flare through stone");
            p.discard();
            h.succeed();
        });
    }

    /**
     * A male displays broadside to the mirror (bettas see with the eyes on the sides of the head): starting nose to
     * the mirror it turns about 90 degrees (plus a slow sway) and keeps its spot.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void maleShowsItsSideToTheMirror(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++) add(h, new BlockPos(x, 1, z), GLASS, TankStyle.CLEAN);
        TankBlockEntity c = controller(h, new BlockPos(2, 1, 2));
        TankInteractions.changeWater(level, c, 9000);
        BettaEntity male = insert(h, c, false, 5);
        male.setNoAi(true);
        male.setNoGravity(true);
        Vec3 centre = Vec3.atCenterOf(h.absolutePos(new BlockPos(3, 1, 3)));
        ServerPlayer p = MockPlayers.serverPlayer(h);
        p.moveTo(centre.x, centre.y - 1.62 + 0.1, centre.z - 3.5, 0F, 0F);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, centre);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MIRROR_STICK.get()));
        // nose to the mirror (the player is to the north: yaw 180)
        male.moveTo(centre.x, centre.y, centre.z, 180F, 0F);
        male.setYBodyRot(180F);
        Vec3 start = male.position();
        h.onEachTick(() -> {
            if (h.getTick() % 10 == 0) MirrorStickItem.flareNear(level, p);
        });
        h.runAfterDelay(80, () -> {
            Vec3 eye = p.getEyePosition();
            float towards = (float) (net.minecraft.util.Mth.atan2(eye.z - male.getZ(), eye.x - male.getX()) * (180F / Math.PI)) - 90F;
            float off = Math.abs(net.minecraft.util.Mth.wrapDegrees(male.getYRot() - towards));
            h.assertTrue(male.isFlaring(), "flares");
            h.assertTrue(off > 65 && off < 115, "broadside to the mirror: " + off + " degrees off the line to it");
            h.assertTrue(male.position().distanceTo(start) < 0.3, "holds its spot");
            p.discard();
            h.succeed();
        });
    }

    private static BettaEntity insert(GameTestHelper h, TankBlockEntity c, boolean female, long seed) {
        BettaData d = BettaData.create(BettaGenotype.randomDomestic(RandomSource.create(seed)), female, 0);
        ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d);
        Component err = TankInhabitants.insert(h.getLevel(), c, bucket, ModEntities.BETTA.get(), null, false);
        if (err != null) throw new GameTestAssertException("insert failed: " + err.getString());
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) {
            if (e instanceof BettaEntity b && b.getBettaData().fishId().equals(d.fishId())) return b;
        }
        throw new GameTestAssertException("inserted betta not found");
    }
}
