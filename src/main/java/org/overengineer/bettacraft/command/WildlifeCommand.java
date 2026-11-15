package org.overengineer.bettacraft.command;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.phys.AABB;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.world.BiomeWaters;
import org.overengineer.bettacraft.world.WildSpawnPass;
import org.overengineer.bettacraft.world.Wildlife;

/**
 * M28.1: {@code /bettacraft wildlife} (OP): why there are (no) wild animals here. Biome, kind of water, the animals of the
 * mod that spawn here with their weights and their share of the water-ambient group, our wild animals nearby, the
 * water-ambient mobs around the player against the vanilla cap (and who fills it) and whether vanilla or the own spawn pass
 * can spawn now. {@code list [radius]} lists our animals nearby (tank inhabitants and the tank interior left out),
 * {@code glow [radius]} lets them glow for 30 seconds.
 */
public final class WildlifeCommand {
    /** The vanilla water-ambient cap counts the mobs in the chunks within 128 blocks of a player. */
    public static final int CAP_RADIUS = 128;
    private static final int LIST_LINES = 20;

    private WildlifeCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("wildlife").requires(s -> s.hasPermission(2))
                .executes(ctx -> info(ctx.getSource()))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource(), 64))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 512)).executes(ctx -> list(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.literal("glow").executes(ctx -> glow(ctx.getSource(), 64))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 512)).executes(ctx -> glow(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))));
    }

    private static void say(CommandSourceStack src, Component c) {
        src.sendSuccess(() -> c, false);
    }

    private static Component waterName(NetCatching.Water w) {
        return Component.translatable("command.bettacraft.wildlife.water." + w.name().toLowerCase(java.util.Locale.ROOT));
    }

    /** Water-ambient mobs that count for the vanilla cap around a position (not persistent), by mod. */
    public static Map<String, Integer> capMobs(ServerLevel level, BlockPos pos) {
        AABB box = new AABB(pos.getX() - CAP_RADIUS, level.getMinBuildHeight(), pos.getZ() - CAP_RADIUS,
                pos.getX() + CAP_RADIUS, level.getMaxBuildHeight(), pos.getZ() + CAP_RADIUS);
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Entity e : level.getEntities((Entity) null, box, e -> e.getType().getCategory() == MobCategory.WATER_AMBIENT)) {
            if (e instanceof Mob m && (m.isPersistenceRequired() || m.requiresCustomPersistence())) continue;
            out.merge(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace(), 1, Integer::sum);
        }
        return out;
    }

    static int info(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        BlockPos pos = BlockPos.containing(src.getPosition());
        if (InteriorManager.isInterior(level)) {
            say(src, Component.translatable("command.bettacraft.wildlife.interior"));
            return 0;
        }
        Holder<Biome> biome = level.getBiome(pos);
        String biomeId = biome.unwrapKey().map(k -> k.location().toString()).orElse("?");
        NetCatching.Water spawn = Wildlife.water(biome), net = BiomeWaters.water(biome, true);
        say(src, Component.translatable("command.bettacraft.wildlife.biome", biomeId, waterName(spawn), waterName(net)));
        // M28.1 part b: the water of the wild plants here (new chunks)
        var plants = BiomeWaters.plantWater(biome);
        say(src, plants == null ? Component.translatable("command.bettacraft.wildlife.plants.none")
                : Component.translatable("command.bettacraft.wildlife.plants", Component.translatable("command.bettacraft.wildlife.plants."
                + plants.name().toLowerCase(java.util.Locale.ROOT)), org.overengineer.bettacraft.sources.WildTables.wildPlants(plants).entries().size(),
                org.overengineer.bettacraft.sources.WildTables.wildFloating(plants).entries().size(),
                Features.enabled(Feature.AQUATIC_WILD) ? "" : " (" + Feature.AQUATIC_WILD.key() + ": off)"));
        // the animals of the mod in the spawn list and their share of the group
        int total = 0, oursTotal = 0;
        for (var d : biome.value().getMobSettings().getMobs(MobCategory.WATER_AMBIENT).unwrap()) total += d.getWeight().asInt();
        List<MobSpawnSettings.SpawnerData> ours = WildSpawnPass.ours(biome);
        if (ours.isEmpty()) {
            say(src, Component.translatable("command.bettacraft.wildlife.none"));
        } else {
            MutableComponent names = Component.empty();
            for (int i = 0; i < ours.size(); i++) {
                var d = ours.get(i);
                oursTotal += d.getWeight().asInt();
                if (i > 0) names.append(", ");
                names.append(d.type.getDescription()).append(" " + d.getWeight().asInt() + " (" + d.minCount + "-" + d.maxCount + ")");
            }
            int share = total <= 0 ? 0 : Math.round(100F * oursTotal / total);
            say(src, Component.translatable("command.bettacraft.wildlife.species", oursTotal, total, share, names));
        }
        // our wild animals nearby
        int near = WildSpawnPass.nearby(level, pos);
        say(src, Component.translatable("command.bettacraft.wildlife.nearby", WildSpawnPass.PLAYER_RADIUS, near));
        // the vanilla cap
        Map<String, Integer> cap = capMobs(level, pos);
        int capCount = cap.values().stream().mapToInt(Integer::intValue).sum(), capMax = MobCategory.WATER_AMBIENT.getMaxInstancesPerChunk();
        List<Map.Entry<String, Integer>> top = new ArrayList<>(cap.entrySet());
        top.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        StringBuilder who = new StringBuilder();
        for (int i = 0; i < Math.min(4, top.size()); i++) who.append(i > 0 ? ", " : "").append(top.get(i).getKey()).append(' ').append(top.get(i).getValue());
        say(src, Component.translatable("command.bettacraft.wildlife.cap", capCount, capMax, who.length() == 0 ? "-" : who.toString()));
        // can it spawn now?
        boolean vanillaRoom = capCount < capMax, any = !ours.isEmpty();
        say(src, Component.translatable(!any ? "command.bettacraft.wildlife.vanilla.no_species"
                : vanillaRoom ? "command.bettacraft.wildlife.vanilla.room" : "command.bettacraft.wildlife.vanilla.full"));
        int limit = WildSpawnPass.perPlayer();
        if (!Features.enabled(Feature.WILD_SPAWN_PASS)) {
            say(src, Component.translatable("command.bettacraft.wildlife.pass.off"));
        } else if (near >= limit) {
            say(src, Component.translatable("command.bettacraft.wildlife.pass.full", near, limit));
        } else {
            say(src, Component.translatable("command.bettacraft.wildlife.pass.on", WildSpawnPass.interval() / 20F, WildSpawnPass.MIN_DISTANCE,
                    WildSpawnPass.MAX_DISTANCE, limit - near));
        }
        return ours.size();
    }

    /** Our animals around the source, outside tanks, nearest first. */
    static List<Entity> around(CommandSourceStack src, int radius) {
        ServerLevel level = src.getLevel();
        var at = src.getPosition();
        List<Entity> out = new ArrayList<>(level.getEntities((Entity) null, new AABB(BlockPos.containing(at)).inflate(radius),
                e -> e.getType().is(ModTags.ALL_ANIMALS) && !TankInhabitants.hasTankRef(e) && e.distanceToSqr(at) <= (double) radius * radius));
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(at)));
        return out;
    }

    static int list(CommandSourceStack src, int radius) {
        if (InteriorManager.isInterior(src.getLevel())) {
            say(src, Component.translatable("command.bettacraft.wildlife.interior"));
            return 0;
        }
        List<Entity> all = around(src, radius);
        if (all.isEmpty()) {
            say(src, Component.translatable("command.bettacraft.wildlife.list.none", radius));
            return 0;
        }
        say(src, Component.translatable("command.bettacraft.wildlife.list.header", all.size(), radius));
        var at = src.getPosition();
        for (int i = 0; i < Math.min(LIST_LINES, all.size()); i++) {
            Entity e = all.get(i);
            BlockPos p = e.blockPosition();
            say(src, Component.translatable("command.bettacraft.wildlife.list.entry", e.getName(), p.getX(), p.getY(), p.getZ(),
                    (int) Math.round(Math.sqrt(e.distanceToSqr(at))),
                    Component.translatable(Wildlife.isWildAnimal(e) ? "command.bettacraft.wildlife.list.wild" : "command.bettacraft.wildlife.list.tame")));
        }
        if (all.size() > LIST_LINES) say(src, Component.translatable("command.bettacraft.wildlife.list.more", all.size() - LIST_LINES));
        return all.size();
    }

    static int glow(CommandSourceStack src, int radius) {
        if (InteriorManager.isInterior(src.getLevel())) {
            say(src, Component.translatable("command.bettacraft.wildlife.interior"));
            return 0;
        }
        int n = 0;
        for (Entity e : around(src, radius)) {
            if (e instanceof LivingEntity l && l.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0, false, false))) n++;
        }
        int count = n;
        say(src, Component.translatable("command.bettacraft.wildlife.glow", count, radius));
        return n;
    }
}
