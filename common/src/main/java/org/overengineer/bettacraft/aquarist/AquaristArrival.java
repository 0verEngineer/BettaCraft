package org.overengineer.bettacraft.aquarist;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.progression.Progress;
import org.overengineer.bettacraft.registry.ModEntities;

/**
 * M51 (questions A1, D1): when an aquarist comes, the offer, hiring, refusing and leaving. Per player: he comes when the
 * player owns at least {@code hiredAquarist.minOwnTanks} tanks and has bred something (a breeding advancement), by day,
 * near the player (not in a tank interior). Refused (or not answered within {@code offerDays}, dismissed, unpaid,
 * homeless, removed) the next one comes after a random {@code returnDaysMin..returnDaysMax} in-game days and another
 * breeding. The records ({@link AquaristRecords}) are the truth: an aquarist entity that is not the one of its player's
 * record (an old copy, a refused one that was unloaded) goes away when it is loaded.
 */
public final class AquaristArrival {
    /** The breeding advancements: one of them is the "bred something" of the arrival. */
    public static final List<String> BREEDING_ADVANCEMENTS = List.of("spawned", "fry_free", "livebearer_litter", "seahorse_fry", "critters_bred",
            "herp_clutch", "turtle_clutch", "frog_tadpole", "clown_larvae", "banggai_young", "amano_young");
    /** Ticks between two looks whether an aquarist should come. */
    static final int CHECK_EVERY = 200;
    /** Arrivals so far (GameTests). */
    public static int arrivals;

    private AquaristArrival() {}

    // ------------------------------------------------------------------ conditions

    /** Why no aquarist comes to the player now (a translation key below message.bettacraft.aquarist.why.), or null: he may come. */
    @Nullable
    public static String whyNot(ServerPlayer p, boolean ignoreTime) {
        if (!Features.enabled(Feature.AQUARIST_NPC)) return "off";
        MinecraftServer server = p.level().getServer();
        if (server == null || p.isSpectator()) return "spectator";
        AquaristRecords rec = AquaristRecords.get(server);
        AquaristRecords.PlayerRecord r = rec.player(p.getUUID());
        if (r.npc != null) return r.hired ? "hired" : "offering";
        if (rec.ownTanks(server, p.getUUID()) < ServerConfig.getOr(ServerConfig.AQUARIST_MIN_TANKS, 3)) return "tanks";
        if (!bred(p, r)) return "breeding";
        if (r.breedingsAtLeave >= 0) {
            if (server.overworld().getGameTime() < r.notBefore) return "wait";
            if (r.breedings <= r.breedingsAtLeave) return "breed_again";
        }
        if (org.overengineer.bettacraft.tank.interior.InteriorManager.isInterior(p.level())) return "place";
        if (!ignoreTime) {
            long t = Math.floorMod(p.level().getDayTime(), 24000L);
            if (t > 10000L) return "time";
        }
        return null;
    }

    /** Whether the player bred something: counted since M51, or a breeding advancement (worlds of earlier versions). */
    static boolean bred(ServerPlayer p, AquaristRecords.PlayerRecord r) {
        if (r.breedings > 0) return true;
        var adv = p.level().getServer().getAdvancements();
        for (String id : BREEDING_ADVANCEMENTS) {
            AdvancementHolder h = adv.get(BettaCraft.id(id));
            try {
                if (h != null && p.getAdvancements().getOrStartProgress(h).isDone()) return true;
            } catch (RuntimeException e) {
                return false; // mock players without advancement data
            }
        }
        return false;
    }

    /** A breeding advancement event reached the player (any breeding in a tank near him or his own). */
    public static void onBreeding(ServerPlayer p) {
        if (p.level().getServer() == null) return;
        AquaristRecords rec = AquaristRecords.get(p.level().getServer());
        rec.player(p.getUUID()).breedings++;
        rec.setDirty();
    }

    // ------------------------------------------------------------------ the server tick


    // ------------------------------------------------------------------ arriving and the offer

    /** An aquarist comes to the player (no checks): spawned 10 to 20 blocks away, he walks up and offers his work. */
    @Nullable
    public static AquaristEntity arrive(ServerPlayer p) {
        ServerLevel level = p.level();
        BlockPos at = spot(level, p);
        if (at == null) return null;
        AquaristEntity npc = ModEntities.HIRED_AQUARIST.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (npc == null) return null;
        npc.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360F, 0F);
        npc.setVariant(level.getRandom().nextInt(AquaristEntity.VARIANTS));
        npc.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.EVENT, null);
        npc.setCustomName(null); // his first name
        npc.setEmployer(p.getUUID(), p.getGameProfile().name());
        long until = level.getServer().overworld().getGameTime() + Math.round(ServerConfig.getOr(ServerConfig.AQUARIST_OFFER_DAYS, 1.0) * 24000L);
        npc.setOfferUntil(until);
        if (!level.addFreshEntity(npc)) return null;
        AquaristRecords rec = AquaristRecords.get(level.getServer());
        AquaristRecords.PlayerRecord r = rec.player(p.getUUID());
        r.npc = npc.getUUID();
        r.hired = false;
        r.offerUntil = until;
        r.name = p.getGameProfile().name();
        rec.setDirty();
        arrivals++;
        p.sendSystemMessage(Component.translatable("message.bettacraft.aquarist.coming", npc.getName()).withStyle(ChatFormatting.AQUA));
        return npc;
    }

    /**
     * An aquarist hired by {@code employer} at once, standing at {@code at} (GameTests, the client smoke and the multiplayer
     * test; an aquarist the player had is replaced). He takes the free bed and breeding basin around him.
     */
    public static AquaristEntity spawnHired(ServerLevel level, java.util.UUID employer, String employerName, net.minecraft.world.phys.Vec3 at) {
        AquaristEntity npc = spawnHiredHomeless(level, employer, employerName, at);
        npc.findHome(level);
        return npc;
    }

    /** As {@link #spawnHired} without looking for a home (the caller gives him one, {@link AquaristEntity#setHome}). */
    public static AquaristEntity spawnHiredHomeless(ServerLevel level, java.util.UUID employer, String employerName, net.minecraft.world.phys.Vec3 at) {
        AquaristEntity npc = ModEntities.HIRED_AQUARIST.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (npc == null) throw new IllegalStateException("no aquarist entity");
        npc.snapTo(at.x, at.y, at.z, 0F, 0F);
        npc.setVariant(Math.floorMod(employer.hashCode(), AquaristEntity.VARIANTS));
        npc.setCustomName(null);
        npc.setEmployer(employer, employerName);
        npc.setPhase(AquaristEntity.Phase.HIRED);
        npc.setOfferUntil(Long.MAX_VALUE);
        level.addFreshEntity(npc);
        AquaristRecords rec = AquaristRecords.get(level.getServer());
        AquaristRecords.PlayerRecord r = rec.player(employer);
        r.npc = npc.getUUID();
        r.hired = true;
        r.offerUntil = Long.MAX_VALUE;
        r.name = employerName;
        rec.setDirty();
        return npc;
    }

    /** A free place to stand 10 to 20 blocks from the player (on the surface near his height, else around his feet). */
    @Nullable
    static BlockPos spot(ServerLevel level, ServerPlayer p) {
        RandomSource r = level.getRandom();
        for (int i = 0; i < 40; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            int d = 10 + r.nextInt(11);
            int x = p.getBlockX() + (int) Math.round(Math.cos(a) * d), z = p.getBlockZ() + (int) Math.round(Math.sin(a) * d);
            if (!level.hasChunkAt(new BlockPos(x, p.getBlockY(), z))) continue;
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (Math.abs(pos.getY() - p.getBlockY()) <= 6 && standable(level, pos)) return pos;
        }
        for (int i = 0; i < 80; i++) { // under a roof or underground: around the player at his height
            int x = p.getBlockX() + r.nextInt(13) - 6, z = p.getBlockZ() + r.nextInt(13) - 6;
            for (int dy = -2; dy <= 2; dy++) {
                BlockPos pos = new BlockPos(x, p.getBlockY() + dy, z);
                if (pos.distSqr(p.blockPosition()) >= 9 && level.hasChunkAt(pos) && standable(level, pos)) return pos;
            }
        }
        return null;
    }

    static boolean standable(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                && level.getFluidState(pos).isEmpty() && level.getFluidState(pos.above()).isEmpty()
                && !(level.getBlockState(pos.below()).getBlock() instanceof org.overengineer.bettacraft.tank.TankBlock);
    }

    /** The offer in the chat, with the buttons to hire or refuse him. */
    public static void sayOffer(ServerPlayer p, AquaristEntity npc) {
        boolean wage = ServerConfig.getOr(ServerConfig.AQUARIST_WAGE, true);
        Component pay = !wage ? Component.translatable("message.bettacraft.aquarist.offer.free")
                : Component.translatable("message.bettacraft.aquarist.offer.wage", ServerConfig.getOr(ServerConfig.AQUARIST_WAGE_EMERALDS, 1),
                ServerConfig.getOr(ServerConfig.AQUARIST_WAGE_FOOD, 3));
        p.sendSystemMessage(Component.translatable("message.bettacraft.aquarist.say", npc.getName(),
                Component.translatable("message.bettacraft.aquarist.offer", pay).withStyle(ChatFormatting.WHITE)));
        MutableComponent hire = button("message.bettacraft.aquarist.hire_button", "/bettacraft aquarist hire", ChatFormatting.GREEN);
        MutableComponent no = button("message.bettacraft.aquarist.decline_button", "/bettacraft aquarist decline", ChatFormatting.RED);
        p.sendSystemMessage(Component.empty().append(hire).append("  ").append(no));
    }

    private static MutableComponent button(String key, String command, ChatFormatting color) {
        return Component.translatable(key).withStyle(s -> s.withColor(color).withBold(true)
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(command))));
    }

    /** The loaded aquarist of the player's record, or null. */
    @Nullable
    public static AquaristEntity npcOf(ServerPlayer p) {
        AquaristRecords.PlayerRecord r = AquaristRecords.get(p.level().getServer()).peek(p.getUUID());
        if (r == null || r.npc == null) return null;
        for (ServerLevel l : p.level().getServer().getAllLevels()) {
            Entity e = l.getEntity(r.npc);
            if (e instanceof AquaristEntity a && a.isAlive()) return a;
        }
        return null;
    }

    // ------------------------------------------------------------------ the answer

    /** The player hires the aquarist who offers his work. Null: done; else why not. */
    @Nullable
    public static Component hire(ServerPlayer p) {
        AquaristRecords.PlayerRecord r = AquaristRecords.get(p.level().getServer()).player(p.getUUID());
        if (r.npc == null || r.hired) return Component.translatable("message.bettacraft.aquarist.none_offering");
        AquaristEntity npc = npcOf(p);
        if (npc == null || npc.phase() != AquaristEntity.Phase.OFFERING) return Component.translatable("message.bettacraft.aquarist.too_far");
        ServerLevel level = (ServerLevel) npc.level();
        r.hired = true;
        r.offerUntil = Long.MAX_VALUE;
        AquaristRecords.get(p.level().getServer()).setDirty();
        npc.setPhase(AquaristEntity.Phase.HIRED);
        npc.setOfferUntil(Long.MAX_VALUE);
        npc.findHome(level);
        Progress.award(p, Progress.AQUARIST_HIRED);
        npc.say(level, Component.translatable(npc.housed() ? "message.bettacraft.aquarist.hired_home" : "message.bettacraft.aquarist.hired_need_home",
                ServerConfig.getOr(ServerConfig.AQUARIST_HOME_RADIUS, 32)), ChatFormatting.GREEN);
        return null;
    }

    /** The player refuses the aquarist who offers his work. */
    @Nullable
    public static Component decline(ServerPlayer p) {
        AquaristRecords.PlayerRecord r = AquaristRecords.get(p.level().getServer()).player(p.getUUID());
        if (r.npc == null || r.hired) return Component.translatable("message.bettacraft.aquarist.none_offering");
        AquaristEntity npc = npcOf(p);
        if (npc != null) npc.say((ServerLevel) npc.level(), Component.translatable("message.bettacraft.aquarist.declined"), ChatFormatting.GRAY);
        r.refused++;
        gone(p.level().getServer(), r, true);
        if (npc != null) npc.startLeaving((ServerLevel) npc.level());
        return null;
    }

    /** The employer dismisses his aquarist. */
    public static void dismiss(ServerPlayer p, AquaristEntity npc) {
        leave((ServerLevel) npc.level(), npc, "dismissed");
    }

    /** The aquarist leaves (unpaid, homeless, dismissed): records, message, he puts things back and walks off. */
    public static void leave(ServerLevel level, AquaristEntity npc, String why) {
        if (npc.phase() == AquaristEntity.Phase.LEAVING) return;
        AquaristRecords rec = AquaristRecords.get(level.getServer());
        UUID emp = npc.employer();
        if (emp != null) {
            AquaristRecords.PlayerRecord r = rec.player(emp);
            if (npc.getUUID().equals(r.npc)) {
                r.left++;
                gone(level.getServer(), r, true);
            }
        }
        npc.say(level, Component.translatable("message.bettacraft.aquarist.leaves." + why), ChatFormatting.RED);
        npc.startLeaving(level);
    }

    /** The record of a player whose aquarist went: the next one after the wait and another breeding. */
    static void gone(MinecraftServer server, AquaristRecords.PlayerRecord r, boolean breedingAgain) {
        int min = ServerConfig.getOr(ServerConfig.AQUARIST_RETURN_MIN, 5), max = Math.max(min, ServerConfig.getOr(ServerConfig.AQUARIST_RETURN_MAX, 10));
        long days = min + (max > min ? server.overworld().getRandom().nextInt(max - min + 1) : 0);
        r.npc = null;
        r.hired = false;
        r.offerUntil = Long.MAX_VALUE;
        r.notBefore = server.overworld().getGameTime() + days * 24000L;
        r.breedingsAtLeave = r.breedings;
        AquaristRecords.get(server).setDirty();
    }

    // ------------------------------------------------------------------ the entity against the records

    /** Every second from the aquarist: is he still the one of his player? Has the offer run out? */
    static void check(ServerLevel level, AquaristEntity npc) {
        if (npc.phase() == AquaristEntity.Phase.LEAVING) return;
        AquaristRecords rec = AquaristRecords.get(level.getServer());
        UUID emp = npc.employer();
        AquaristRecords.PlayerRecord r = emp == null ? null : rec.peek(emp);
        if (r == null || !npc.getUUID().equals(r.npc)) { // refused or replaced while he was not loaded: he goes
            npc.startLeaving(level);
            return;
        }
        if (npc.phase() == AquaristEntity.Phase.HIRED && !r.hired) r.hired = true;
        if (npc.phase() == AquaristEntity.Phase.OFFERING && level.getServer().overworld().getGameTime() > npc.offerUntil()) {
            r.refused++;
            gone(level.getServer(), r, true);
            npc.say(level, Component.translatable("message.bettacraft.aquarist.no_answer"), ChatFormatting.GRAY);
            npc.startLeaving(level);
        }
    }

    /** The aquarist entity is gone for good (/kill, discarded): his record lets the next one come later. */
    static void onGone(ServerLevel level, AquaristEntity npc) {
        npc.releaseHome(level);
        UUID emp = npc.employer();
        if (emp == null) return;
        AquaristRecords rec = AquaristRecords.get(level.getServer());
        AquaristRecords.PlayerRecord r = rec.peek(emp);
        if (r == null || !npc.getUUID().equals(r.npc)) return;
        r.left++;
        gone(level.getServer(), r, true);
    }
}
