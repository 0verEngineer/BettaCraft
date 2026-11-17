package org.overengineer.bettacraft.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.foreign.ForeignProfiles;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M33 part d: {@code /bettacraft foreign list [radius]} (OP): the animals of vanilla and other mods living in our tanks, in
 * the tank the player looks at (within 8 blocks) or else in every tank within {@code radius} blocks (default 32), each with
 * its entity id and the profile file that maps it ({@link ForeignProfiles}) or "no profile"; then the entity types of other
 * mods (not vanilla) that live in these tanks without a profile, for modpack makers who want to write one.
 * <p>
 * M38: {@code /bettacraft foreign plants [radius]} does the same for the plants, corals and live rock of Minecraft and other
 * mods in the interiors: every plant that counts with its block, role, plant mass and profile file, then the plant-like
 * blocks of other mods in there without a profile.
 */
public final class ForeignCommand {
    public static final int DEFAULT_RADIUS = 32;
    /** Lines per tank (the rest is counted). */
    private static final int PER_TANK = 20;

    private ForeignCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("foreign").requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource(), DEFAULT_RADIUS))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 256))
                                .executes(ctx -> list(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.literal("plants").executes(ctx -> plants(ctx.getSource(), DEFAULT_RADIUS))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 256))
                                .executes(ctx -> plants(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))));
    }

    // ------------------------------------------------------------------ M38: plants

    /** One kind of foreign plant in a tank: block, profile file, role, how many plants, their blocks and their plant mass. */
    public record PlantRow(ResourceLocation block, ResourceLocation profile, String role, int plants, int blocks, double mass) {}

    /** The plants of other mods in a tank by block and profile (GameTests). */
    public static List<PlantRow> plantRows(ServerLevel level, TankBlockEntity c) {
        var r = org.overengineer.bettacraft.plants.InteriorPlants.region(level.getServer(), c.getTank());
        if (r == null) return List.of();
        var effect = org.overengineer.bettacraft.plants.foreign.ForeignPlants.effect(level.getServer(), r);
        Map<String, PlantRow> rows = new LinkedHashMap<>();
        for (var g : effect.groups()) {
            // shown by the first block of its profile (kelp, not kelp_plant), the blocks of a tag by their own id
            var prof = org.overengineer.bettacraft.plants.foreign.ForeignPlants.files().get(g.file());
            ResourceLocation id = prof != null && !prof.blocks().isEmpty() ? prof.blocks().get(0) : BuiltInRegistries.BLOCK.getKey(g.block());
            String k = id + " " + g.file();
            PlantRow o = rows.get(k);
            rows.put(k, o == null ? new PlantRow(id, g.file(), g.role().id(), 1, g.blocks(), g.mass())
                    : new PlantRow(id, g.file(), o.role(), o.plants() + 1, o.blocks() + g.blocks(), o.mass() + g.mass()));
        }
        List<PlantRow> out = new ArrayList<>(rows.values());
        out.sort(Comparator.comparing((PlantRow p) -> p.block().toString()));
        return out;
    }

    /** Plant-like blocks of other mods (not vanilla, not ours) in the interior of a tank without a profile, with their count. */
    public static Map<ResourceLocation, Integer> unprofiledPlants(ServerLevel level, TankBlockEntity c) {
        Map<ResourceLocation, Integer> out = new java.util.TreeMap<>();
        var r = org.overengineer.bettacraft.plants.InteriorPlants.region(level.getServer(), c.getTank());
        ServerLevel in = org.overengineer.bettacraft.tank.interior.InteriorManager.interiorLevel(level.getServer());
        if (r == null || in == null) return out;
        int x0 = r.originX(), z0 = r.originZ(), y0 = org.overengineer.bettacraft.tank.interior.InteriorRegion.BASE_Y;
        org.overengineer.bettacraft.tank.interior.InteriorWorldOps.forEachBlock(in, x0, y0, z0, x0 + r.sizeX() - 1, y0 + r.sizeY() - 1, z0 + r.sizeZ() - 1,
                (x, y, z, s) -> {
                    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(s.getBlock());
                    if ("minecraft".equals(id.getNamespace()) || BettaCraft.MOD_ID.equals(id.getNamespace())) return;
                    if (!plantLike(s) || org.overengineer.bettacraft.plants.foreign.ForeignPlants.entry(s.getBlock()).file() != null) return;
                    out.merge(id, 1, Integer::sum);
                });
        return out;
    }

    /** A plant (bush, kelp-like, coral) or a block in our water plant tags. */
    static boolean plantLike(net.minecraft.world.level.block.state.BlockState s) {
        var b = s.getBlock();
        return b instanceof net.minecraft.world.level.block.BushBlock || b instanceof net.minecraft.world.level.block.GrowingPlantBlock
                || b instanceof net.minecraft.world.level.block.BaseCoralPlantTypeBlock || s.is(net.minecraft.tags.BlockTags.CORALS)
                || s.is(org.overengineer.bettacraft.registry.ModTags.SWAYING_PLANTS) || s.is(org.overengineer.bettacraft.registry.ModTags.INTERIOR_WATER_PLANTS)
                || s.is(org.overengineer.bettacraft.registry.ModTags.FLOATING_PLANTS);
    }

    static int plants(CommandSourceStack src, int radius) {
        ServerLevel level = src.getLevel();
        TankBlockEntity looked = lookedAt(src);
        List<TankBlockEntity> tanks = looked != null ? List.of(looked) : tanksNear(level, src.getPosition(), radius);
        if (!org.overengineer.bettacraft.plants.foreign.ForeignPlants.enabled()) say(src, Component.translatable("command.bettacraft.foreign.plants_off").withStyle(ChatFormatting.GOLD));
        int shown = 0, total = 0;
        Map<ResourceLocation, Integer> missing = new java.util.TreeMap<>();
        for (TankBlockEntity c : tanks) {
            List<PlantRow> rows = plantRows(level, c);
            Map<ResourceLocation, Integer> un = unprofiledPlants(level, c);
            un.forEach((k, v) -> missing.merge(k, v, Integer::sum));
            var r = org.overengineer.bettacraft.plants.InteriorPlants.region(level.getServer(), c.getTank());
            var effect = org.overengineer.bettacraft.plants.foreign.ForeignPlants.effect(level.getServer(), r);
            if (rows.isEmpty() && effect.liveRock() == 0 && un.isEmpty()) continue;
            shown++;
            BlockPos p = c.getBlockPos();
            Component name = c.getTank().name().isEmpty() ? Component.translatable("block.bettacraft.tank") : Component.literal(c.getTank().name());
            say(src, Component.translatable("command.bettacraft.foreign.plants_tank", name, p.getX() + " " + p.getY() + " " + p.getZ(), effect.plants(),
                    String.format(java.util.Locale.ROOT, "%.1f", org.overengineer.bettacraft.plants.InteriorPlants.foreignShare(level.getServer(), c.getTank()) * 100))
                    .withStyle(ChatFormatting.AQUA));
            for (int i = 0; i < Math.min(PER_TANK, rows.size()); i++) {
                PlantRow row = rows.get(i);
                total += row.plants();
                say(src, Component.translatable("command.bettacraft.foreign.plant", row.block().toString(), row.plants(), row.role(),
                        String.valueOf(Math.round(row.mass())), row.profile().toString()));
            }
            if (rows.size() > PER_TANK) say(src, Component.translatable("command.bettacraft.foreign.more", rows.size() - PER_TANK));
            if (effect.liveRock() > 0) say(src, Component.translatable("command.bettacraft.foreign.live_rock", effect.liveRock()));
            if (effect.overLimit() > 0) say(src, Component.translatable("command.bettacraft.foreign.over_limit", effect.overLimit()).withStyle(ChatFormatting.GOLD));
        }
        if (shown == 0) {
            say(src, looked != null ? Component.translatable("command.bettacraft.foreign.plants_none_here")
                    : Component.translatable("command.bettacraft.foreign.plants_none", radius));
            return 0;
        }
        if (missing.isEmpty()) {
            say(src, Component.translatable("command.bettacraft.foreign.plants_unprofiled.none"));
        } else {
            StringBuilder b = new StringBuilder();
            for (Map.Entry<ResourceLocation, Integer> e : missing.entrySet()) b.append(b.length() > 0 ? ", " : "").append(e.getKey()).append(" (").append(e.getValue()).append(')');
            say(src, Component.translatable("command.bettacraft.foreign.plants_unprofiled", b.toString()).withStyle(ChatFormatting.GOLD));
        }
        return total;
    }

    /** Whether an animal in a tank counts as foreign: not ours and without Bettacraft's own care. */
    public static boolean foreign(Entity e) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return !BettaCraft.MOD_ID.equals(id.getNamespace()) && !ForeignProfiles.ownCare(e.getType());
    }

    /** One foreign animal of a tank: its type and the file of its profile (null: none). */
    public record Row(EntityType<?> type, Component name, @Nullable ResourceLocation profile) {}

    /** The foreign animals of a tank (GameTests). */
    public static List<Row> rows(ServerLevel level, TankBlockEntity c) {
        List<Row> out = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(level, c.getTank())) {
            if (!foreign(e)) continue;
            out.add(new Row(e.getType(), e.getName(), ForeignProfiles.fileOf(e.getType())));
        }
        out.sort(Comparator.comparing((Row r) -> BuiltInRegistries.ENTITY_TYPE.getKey(r.type()).toString()));
        return out;
    }

    /** The types of other mods (not vanilla) without a profile among these rows, with their count. */
    public static Map<ResourceLocation, Integer> unprofiled(List<Row> rows) {
        Map<ResourceLocation, Integer> out = new LinkedHashMap<>();
        for (Row r : rows) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(r.type());
            if (r.profile() == null && !"minecraft".equals(id.getNamespace())) out.merge(id, 1, Integer::sum);
        }
        return out;
    }

    /** The tank the player looks at (8 blocks), or null. */
    @Nullable
    static TankBlockEntity lookedAt(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer p)) return null;
        HitResult hit = p.pick(8, 0, false);
        if (!(hit instanceof BlockHitResult b) || hit.getType() != HitResult.Type.BLOCK) return null;
        return TankMultiblock.getController(p.level(), b.getBlockPos());
    }

    /** The loaded tank controllers whose tank comes within {@code radius} blocks of {@code at}, the nearest first. */
    public static List<TankBlockEntity> tanksNear(ServerLevel level, Vec3 at, int radius) {
        int scan = radius + 32; // a controller sits in a corner of its tank
        int cx0 = ((int) Math.floor(at.x) - scan) >> 4, cx1 = ((int) Math.floor(at.x) + scan) >> 4;
        int cz0 = ((int) Math.floor(at.z) - scan) >> 4, cz1 = ((int) Math.floor(at.z) + scan) >> 4;
        Map<UUID, TankBlockEntity> found = new LinkedHashMap<>();
        for (int cx = cx0; cx <= cx1; cx++) for (int cz = cz0; cz <= cz1; cz++) {
            LevelChunk ch = level.getChunkSource().getChunkNow(cx, cz);
            if (ch == null) continue;
            for (BlockEntity be : ch.getBlockEntities().values()) {
                if (be instanceof TankBlockEntity t && t.isController() && !t.isRemoved() && distance(at, t) <= radius) found.put(t.getTank().id(), t);
            }
        }
        List<TankBlockEntity> out = new ArrayList<>(found.values());
        out.sort(Comparator.comparingDouble((TankBlockEntity t) -> distance(at, t)));
        return out;
    }

    private static double distance(Vec3 p, TankBlockEntity t) {
        BlockPos mn = t.getTank().min(), mx = t.getTank().max();
        double dx = Math.max(0, Math.max(mn.getX() - p.x, p.x - (mx.getX() + 1)));
        double dy = Math.max(0, Math.max(mn.getY() - p.y, p.y - (mx.getY() + 1)));
        double dz = Math.max(0, Math.max(mn.getZ() - p.z, p.z - (mx.getZ() + 1)));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static void say(CommandSourceStack src, Component c) {
        src.sendSuccess(() -> c, false);
    }

    static int list(CommandSourceStack src, int radius) {
        ServerLevel level = src.getLevel();
        TankBlockEntity looked = lookedAt(src);
        List<TankBlockEntity> tanks = looked != null ? List.of(looked) : tanksNear(level, src.getPosition(), radius);
        if (!Features.enabled(Feature.FOREIGN_CARE)) say(src, Component.translatable("command.bettacraft.foreign.care_off").withStyle(ChatFormatting.GOLD));
        List<Row> all = new ArrayList<>();
        int shown = 0;
        for (TankBlockEntity c : tanks) {
            List<Row> rows = rows(level, c);
            if (rows.isEmpty()) continue;
            all.addAll(rows);
            shown++;
            BlockPos p = c.getBlockPos();
            Component name = c.getTank().name().isEmpty() ? Component.translatable("block.bettacraft.tank") : Component.literal(c.getTank().name());
            say(src, Component.translatable("command.bettacraft.foreign.tank", name, p.getX() + " " + p.getY() + " " + p.getZ(), rows.size())
                    .withStyle(ChatFormatting.AQUA));
            for (int i = 0; i < Math.min(PER_TANK, rows.size()); i++) {
                Row r = rows.get(i);
                String id = BuiltInRegistries.ENTITY_TYPE.getKey(r.type()).toString();
                say(src, r.profile() != null
                        ? Component.translatable("command.bettacraft.foreign.animal", id, r.name(), r.profile().toString())
                        : Component.translatable("command.bettacraft.foreign.no_profile", id, r.name()).withStyle(ChatFormatting.YELLOW));
            }
            if (rows.size() > PER_TANK) say(src, Component.translatable("command.bettacraft.foreign.more", rows.size() - PER_TANK));
        }
        if (shown == 0) {
            say(src, looked != null ? Component.translatable("command.bettacraft.foreign.none_here")
                    : Component.translatable("command.bettacraft.foreign.none", radius));
            return 0;
        }
        Map<ResourceLocation, Integer> missing = unprofiled(all);
        if (missing.isEmpty()) {
            say(src, Component.translatable("command.bettacraft.foreign.unprofiled.none"));
        } else {
            StringBuilder b = new StringBuilder();
            for (Map.Entry<ResourceLocation, Integer> e : missing.entrySet()) b.append(b.length() > 0 ? ", " : "").append(e.getKey()).append(" (").append(e.getValue()).append(')');
            say(src, Component.translatable("command.bettacraft.foreign.unprofiled", b.toString()).withStyle(ChatFormatting.GOLD));
        }
        return all.size();
    }
}
