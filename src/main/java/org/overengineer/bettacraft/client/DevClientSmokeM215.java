package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.decor.DecorItem;
import org.overengineer.bettacraft.decor.DecorType;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.ClipFeederBlock;
import org.overengineer.bettacraft.equipment.ClipFeederBlockEntity;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.progression.GuideBook;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M21.5 dev client smoke scene ({@code -Psmoketestm21_5}), screenshots {@code bettacraft_m21_5_*}:
 * <ul>
 *     <li>smooth swimming: a tank with neons, green neons, kuhli loaches and otocinclus; for 240 ticks the pose of every
 *     fish is sampled each client tick (largest change of pitch, body yaw and tail phase per tick, smooth vs. the old
 *     raw values) and a frame sequence is taken; a pleco on its way to the glass must stay flat;</li>
 *     <li>the green and blue neon, the racer nerites with their shades, gold apple snails with bands, red ramshorns;</li>
 *     <li>cabinets of several woods (also of other mods' planks if there are any) near and far: no pink or blue edges;</li>
 *     <li>the detailed dino skull and shipwreck in an interior; the clip-on feeder on a one-block tank with a light;</li>
 *     <li>the tank menu with the break protection (en, de) and the message when breaking a protected tank;</li>
 *     <li>the handbook start page with the byline (en, de).</li>
 * </ul>
 */
final class DevClientSmokeM215 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private BlockPos base, swim, colors, snails, cabinets, decor, clip;
    private volatile InteriorRegion decorRegion;
    private int wait;
    private final String lang = mc.options.languageCode;
    // sampling of the swimming poses (client entities)
    private boolean sampling;
    private int samples;
    private final Map<Integer, float[]> last = new HashMap<>();
    private float maxPitchStep, maxYawStep, maxPhaseStep, maxRawPitchStep;
    private int plecoUprightEarly;

    DevClientSmokeM215() {
        then(1, () -> server(this::build));
        then(1, () -> mc.options.hideGui = true);
        // ---- smooth swimming: samples and a frame sequence
        then(160, () -> look(swim.getX() + 2.0, swim.getY() + 1.1, swim.getZ() - 2.4, 0F, 8F));
        then(40, () -> sampling = true);
        for (int i = 0; i < 8; i++) {
            int n = i;
            then(3, () -> shot("swim_seq_" + n));
        }
        then(216, () -> {
            sampling = false;
            LOGGER.info(String.format(java.util.Locale.ROOT,
                    "[smoketest] M21.5 swimming: %d samples, largest step per tick: pitch %.2f deg (raw last-tick pitch %.2f), body yaw %.2f deg, tail phase %.3f; "
                            + "pleco upright before the glass: %d", samples, maxPitchStep, maxRawPitchStep, maxYawStep, maxPhaseStep, plecoUprightEarly));
            if (samples < 200) LOGGER.error("[smoketest] M21.5 too few swimming samples");
            if (maxPitchStep > 4.5F) LOGGER.error("[smoketest] M21.5 the pitch jumps ({} deg in a tick)", maxPitchStep);
            if (maxYawStep > 20F) LOGGER.error("[smoketest] M21.5 the body yaw jumps ({} deg in a tick)", maxYawStep);
            if (maxPhaseStep > 1.6F) LOGGER.error("[smoketest] M21.5 the tail phase jumps ({})", maxPhaseStep);
            if (plecoUprightEarly > 0) LOGGER.error("[smoketest] M21.5 the pleco stood upright away from the glass");
            shot("swim_tank");
        });
        then(1, () -> look(swim.getX() + 1.0, swim.getY() + 0.5, swim.getZ() - 0.9, 0F, 15F));
        then(40, () -> shot("swim_kuhli"));
        // ---- colour forms: green and blue neon, snails
        then(1, () -> look(colors.getX() + 1.0, colors.getY() + 0.55, colors.getZ() - 0.55, 0F, 6F));
        then(80, () -> shot("green_neon"));
        then(1, () -> look(snails.getX() + 1.0, snails.getY() + 0.85, snails.getZ() - 1.1, 0F, 20F));
        then(80, () -> shot("snails"));
        then(1, () -> look(snails.getX() + 1.0, snails.getY() + 2.2, snails.getZ() + 1.0, 0F, 90F));
        then(40, () -> shot("snails_top"));
        // ---- cabinets of several woods, near and far
        then(1, () -> look(cabinets.getX() + 6.5, cabinets.getY() + 1.6, cabinets.getZ() - 3.2, 0F, 18F));
        then(80, () -> shot("cabinets_near"));
        then(1, () -> look(cabinets.getX() + 3.0, cabinets.getY() + 1.0, cabinets.getZ() - 1.4, 0F, 20F));
        then(40, () -> shot("cabinets_close"));
        then(1, () -> look(cabinets.getX() + 6.5, cabinets.getY() + 6.0, cabinets.getZ() - 24.0, 0F, 10F));
        then(60, () -> shot("cabinets_far"));
        // ---- detailed decoration inside and outside
        then(1, () -> server(p -> {
            InteriorRegion r = decorRegion;
            BlockPos at = r.toWorld(16, 17, 1);
            p.teleportTo(InteriorManager.interiorLevel(p.server), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 42F);
        }));
        then(120, () -> shot("decor_inside"));
        then(1, () -> look(decor.getX() + 1.0, decor.getY() + 0.8, decor.getZ() - 1.6, 0F, 12F));
        then(80, () -> shot("decor_outside"));
        // ---- clip-on feeder
        then(1, () -> look(clip.getX() - 0.9, clip.getY() + 1.4, clip.getZ() - 1.6, -35F, 25F));
        then(60, () -> shot("clip_feeder"));
        // ---- menu with the break protection, the message when breaking
        then(1, () -> mc.options.hideGui = false);
        then(1, () -> server(p -> {
            reach(p, true);
            p.teleportTo(p.serverLevel(), swim.getX() + 2.0, swim.getY(), swim.getZ() - 3.0, 0F, 0F);
        }));
        then(10, () -> openMenu(swim));
        then(30, () -> shot("menu_protection_off"));
        then(1, () -> {
            if (mc.screen instanceof org.overengineer.bettacraft.client.screen.TankScreen) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new org.overengineer.bettacraft.network.TankActionPayload(
                        mc.player.containerMenu.containerId, org.overengineer.bettacraft.tank.menu.TankActions.TOGGLE_PROTECT, 0, "", java.util.Optional.empty()));
            }
        });
        then(30, () -> shot("menu_protection_on"));
        then(1, () -> language("de_de"));
        then(20, () -> shot("menu_protection_on_de"));
        then(1, () -> language(lang));
        then(1, () -> server(ServerPlayer::closeContainer));
        then(5, () -> mc.setScreen(null));
        then(1, () -> server(p -> {
            p.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            boolean broke = p.gameMode.destroyBlock(swim);
            p.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
            boolean still = p.serverLevel().getBlockState(swim).is(ModBlocks.TANK.get());
            LOGGER.info("[smoketest] M21.5 breaking a protected tank: broken {}, still there {}", broke, still);
            if (broke || !still) LOGGER.error("[smoketest] M21.5 the protected tank was broken");
        }));
        then(10, () -> shot("protected_message"));
        then(1, () -> server(p -> reach(p, false)));
        // ---- the handbook start page (en, de)
        then(1, () -> mc.options.hideGui = true);
        guide("en");
        then(1, () -> language("de_de"));
        guide("de");
        then(1, () -> language(lang));
        then(1, () -> mc.setScreen(null));
    }

    private void guide(String l) {
        if (!GuideBook.available()) return;
        then(10, () -> org.overengineer.bettacraft.compat.guideme.GuideMECompat.open(mc.player, GuideBook.GUIDE_ID, BettaCraft.id("index.md")));
        then(40, () -> shot("guide_start_" + l));
        then(5, () -> mc.setScreen(null));
    }

    // ------------------------------------------------------------------ sampling

    /** Every client tick: the change of the smoothed pose of each tankmate since the last tick. */
    private void sample() {
        if (!sampling || mc.level == null) return;
        samples++;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof TankmateEntity f) || f.distanceToSqr(Vec3.atCenterOf(swim)) > 64) continue;
            var sp = f.swimPose;
            float pitch = sp.pitch(1F), phase = sp.phase(1F), yaw = f.yBodyRot;
            double vx = f.getX() - f.xo, vz = f.getZ() - f.zo, vy = f.getY() - f.yo, h = Math.sqrt(vx * vx + vz * vz);
            float raw = Math.abs(vy) > 0.004 && h + Math.abs(vy) > 0.006 ? (float) Mth.clamp(Math.toDegrees(Math.atan2(vy, Math.max(h, 0.01))), -35, 35) : 0F;
            float[] l = last.get(f.getId());
            if (l != null) {
                maxPitchStep = Math.max(maxPitchStep, Math.abs(pitch - l[0]));
                maxYawStep = Math.max(maxYawStep, Math.abs(Mth.wrapDegrees(yaw - l[1])));
                maxPhaseStep = Math.max(maxPhaseStep, Math.abs(phase - l[2]));
                maxRawPitchStep = Math.max(maxRawPitchStep, Math.abs(raw - l[3]));
            }
            last.put(f.getId(), new float[] {pitch, yaw, phase, raw});
            if (f.species() == TankmateSpecies.BRISTLENOSE_PLECO && sp.glassRaw() > 0.5F && f.getDeltaMovement().horizontalDistance() > 0.004) plecoUprightEarly++;
        }
    }

    // ------------------------------------------------------------------ building

    private void build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        base = p.blockPosition().offset(-30, 3, 380);
        RandomSource r = RandomSource.create(215);
        // smooth swimming: neons, green neons, kuhli loaches, otocinclus, a bristlenose
        swim = base;
        TankBlockEntity s = tank(level, swim, 4, 2, 2, p);
        floor(level, s, Blocks.SAND.defaultBlockState());
        for (int i = 0; i < 8; i++) fish(level, s, TankmateSpecies.NEON_TETRA, TankmateGenes.wild(TankmateSpecies.NEON_TETRA), i % 2 == 0);
        for (int i = 0; i < 6; i++) fish(level, s, TankmateSpecies.GREEN_NEON_TETRA, TankmateGenes.pure(TankmateSpecies.GREEN_NEON_TETRA, i < 3 ? "wild" : "blue"), i % 2 == 0);
        for (int i = 0; i < 3; i++) fish(level, s, TankmateSpecies.KUHLI_LOACH, TankmateGenes.wild(TankmateSpecies.KUHLI_LOACH), i % 2 == 0);
        for (int i = 0; i < 3; i++) fish(level, s, TankmateSpecies.OTOCINCLUS, TankmateGenes.wild(TankmateSpecies.OTOCINCLUS), i % 2 == 0);
        fish(level, s, TankmateSpecies.BRISTLENOSE_PLECO, TankmateGenes.wild(TankmateSpecies.BRISTLENOSE_PLECO), false);
        light(level, swim, 4, 2, 1);
        // green and blue neon, still for the picture
        colors = base.offset(7, 0, 0);
        TankBlockEntity c = tank(level, colors, 2, 1, 2, p);
        floor(level, c, Blocks.GRAVEL.defaultBlockState());
        for (int i = 0; i < 6; i++) {
            TankmateEntity e = fish(level, c, TankmateSpecies.GREEN_NEON_TETRA, TankmateGenes.pure(TankmateSpecies.GREEN_NEON_TETRA, i % 2 == 0 ? "wild" : "blue"), i % 2 == 0);
            if (e != null) {
                e.moveTo(colors.getX() + 0.45 + (i % 3) * 0.55, colors.getY() + 0.3 + (i / 3) * 0.3, colors.getZ() + 0.6 + (i % 2) * 0.2, 90F, 0F);
                e.setYBodyRot(90F);
                e.setYHeadRot(90F);
                SmokeFreeze.freeze(e, true);
            }
        }
        light(level, colors, 2, 1, 1);
        // snails: red and yellow racer nerites, gold apple snails, red ramshorns
        snails = base.offset(11, 0, 0);
        TankBlockEntity sn = tank(level, snails, 2, 1, 2, p);
        floor(level, sn, Blocks.SAND.defaultBlockState());
        for (int i = 0; i < 6; i++) TankCritters.add(level, sn, CritterSpecies.RED_NERITE, CritterGenes.WILD, i % 2 == 0, 10);
        for (int i = 0; i < 6; i++) TankCritters.add(level, sn, CritterSpecies.YELLOW_NERITE, CritterGenes.WILD, i % 2 == 0, 10);
        for (int i = 0; i < 3; i++) TankCritters.add(level, sn, CritterSpecies.APPLE_SNAIL, CritterGenes.pure(0, 0.5F), i % 2 == 0, 10);
        TankCritters.add(level, sn, CritterSpecies.APPLE_SNAIL, CritterGenes.WILD, true, 10);
        for (int i = 0; i < 3; i++) TankCritters.add(level, sn, CritterSpecies.RAMSHORN, CritterGenes.pure(2, 0.6F), true, 10);
        light(level, snails, 2, 1, 1);
        // cabinets of several woods (and other mods' planks), each under a small tank
        cabinets = base.offset(0, 0, 8);
        List<ResourceLocation> woods = new ArrayList<>();
        for (String w : new String[] {"oak_planks", "spruce_planks", "mangrove_planks", "bamboo_planks", "cherry_planks", "crimson_planks"}) {
            woods.add(ResourceLocation.withDefaultNamespace(w));
        }
        for (var e : BuiltInRegistries.BLOCK.entrySet()) {
            ResourceLocation id = e.getKey().location();
            if (!id.getNamespace().equals("minecraft") && !id.getNamespace().equals(BettaCraft.MOD_ID) && id.getPath().endsWith("_planks") && woods.size() < 8) woods.add(id);
        }
        LOGGER.info("[smoketest] M21.5 cabinet woods: {}", woods);
        for (int i = 0; i < woods.size(); i++) {
            BlockPos at = cabinets.offset(i * 2, 0, 0);
            level.setBlockAndUpdate(at, ModBlocks.CABINET.get().defaultBlockState().setValue(CabinetBlock.FACING, Direction.NORTH));
            if (level.getBlockEntity(at) instanceof CabinetBlockEntity cab) cab.setWood(woods.get(i));
            tank(level, at.above(), 1, 1, 1, p);
        }
        // detailed decoration in a 2x1x2 tank
        decor = base.offset(0, 0, 14);
        TankBlockEntity d = tank(level, decor, 2, 1, 2, p);
        InteriorRegion dr = floor(level, d, Blocks.SAND.defaultBlockState());
        decorRegion = dr;
        ServerLevel in = InteriorManager.interiorLevel(p.server);
        Component e1 = DecorItem.place(in, DecorType.DINO_SKULL, dr.toWorld(9, 0, 10).above(0), Rotation.NONE, null);
        Component e2 = DecorItem.place(in, DecorType.SHIPWRECK, dr.toWorld(22, 0, 22), Rotation.NONE, null);
        if (e1 != null || e2 != null) LOGGER.error("[smoketest] M21.5 decor not placed: {} {}", e1 == null ? "" : e1.getString(), e2 == null ? "" : e2.getString());
        light(level, decor, 2, 1, 1);
        // the clip-on feeder on a one-block tank with a light on top
        clip = base.offset(6, 0, 14);
        TankBlockEntity cl = tank(level, clip, 1, 1, 1, p);
        level.setBlockAndUpdate(clip.above(), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.LIT, true));
        level.setBlockAndUpdate(clip.north(), ModBlocks.CLIP_FEEDER.get().defaultBlockState().setValue(ClipFeederBlock.FACING, Direction.NORTH));
        if (level.getBlockEntity(clip.north()) instanceof ClipFeederBlockEntity f) f.items().insertItem(0, new ItemStack(ModItems.BETTA_PELLETS.get(), 20), false);
        BettaData bd = BettaData.create(BettaGenotype.randomDomestic(RandomSource.create(5)), false, 0);
        TankInhabitants.insert(level, cl, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), bd), ModEntities.BETTA.get(), p, 0L);
        TankEquipment.invalidate(cl);
        LOGGER.info("[smoketest] M21.5 scene at {}: feeder of the clip tank {}", base,
                org.overengineer.bettacraft.tank.TankCare.findFeeder(level, cl.getTank()) instanceof ClipFeederBlockEntity ? "clip-on" : "none");
        reach(p, false);
    }

    private static TankBlockEntity tank(ServerLevel level, BlockPos min, int sx, int sy, int sz, ServerPlayer owner) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) for (int x = 0; x < sx; x++) blocks.add(min.offset(x, y, z));
        for (BlockPos pos : blocks) level.setBlockAndUpdate(pos, ModBlocks.TANK.get().defaultBlockState());
        for (BlockPos pos : blocks) TankMultiblock.onTankBlockAdded(level, pos, null);
        TankBlockEntity c = TankMultiblock.getController(level, min);
        TankInteractions.changeWater(level, c, blocks.size() * 1000L);
        c.getTank().setFront(Direction.NORTH);
        c.getTank().setSignVisible(false);
        c.getTank().setOwner(owner.getUUID());
        c.getTank().setWaterTemp(26F);
        c.getTank().setTargetTemp(26F);
        c.getTank().setWaterQuality(1F);
        c.markTankChanged();
        return c;
    }

    private static InteriorRegion floor(ServerLevel level, TankBlockEntity c, BlockState s) {
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        InteriorRegion r = InteriorManager.ensureRegion(level, c);
        for (int x = 0; x < r.sizeX(); x++)
            for (int z = 0; z < r.sizeZ(); z++) Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), s, false);
        return r;
    }

    private static void light(ServerLevel level, BlockPos min, int sx, int y, int z) {
        for (int x = 0; x < sx; x++) {
            level.setBlockAndUpdate(min.offset(x, y, z), ModBlocks.TANK_LIGHT.get().defaultBlockState().setValue(TankLightBlock.AXIS, Direction.Axis.X)
                    .setValue(TankLightBlock.NEG, x > 0).setValue(TankLightBlock.POS, x < sx - 1).setValue(TankLightBlock.LIT, true));
        }
    }

    private static TankmateEntity fish(ServerLevel level, TankBlockEntity c, TankmateSpecies s, TankmateGenes g, boolean female) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        var r = level.getRandom();
        Vec3 p = w.clamp(new Vec3(w.box().minX + r.nextDouble() * (w.box().maxX - w.box().minX), w.box().minY + 0.3 + r.nextDouble() * 0.5,
                w.box().minZ + r.nextDouble() * (w.box().maxZ - w.box().minZ)), 0.35, 0.1, 0.2);
        TankmateEntity e = TankmateBreeding.spawn(level, c, s, g, female, LifeStage.ADULT, 0, p);
        if (e == null) LOGGER.error("[smoketest] M21.5 {} not spawned", s.id());
        else e.health().feed();
        return e;
    }

    /** No info overlay (Jade) in the pictures: no reach while the scene runs. */
    private static void reach(ServerPlayer p, boolean normal) {
        var b = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
        var e = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE);
        if (b != null) b.setBaseValue(normal ? 4.5 : 0.0);
        if (e != null) e.setBaseValue(normal ? 3.0 : 0.0);
    }

    private void openMenu(BlockPos at) {
        server(p -> {
            TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), at);
            if (c == null) return;
            p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new org.overengineer.bettacraft.tank.menu.TankMenu(id, inv, c),
                    Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(c.getBlockPos()));
        });
    }

    private void language(String code) {
        mc.options.languageCode = code;
        mc.getLanguageManager().setSelected(code);
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
        if (mc.screen != null) mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    private void look(double x, double eyeY, double z, float yaw, float pitch) {
        server(p -> {
            if (InteriorManager.isInterior(p.level())) p.teleportTo(p.server.overworld(), x, eyeY - p.getEyeHeight(), z, yaw, pitch);
            else p.teleportTo(p.serverLevel(), x, eyeY - p.getEyeHeight(), z, yaw, pitch);
        });
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        sample();
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (steps.isEmpty()) return true;
        Step s = steps.peek();
        if (wait < s.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        s.action().run();
        return steps.isEmpty();
    }

    private void server(java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m21_5_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
