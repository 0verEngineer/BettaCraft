package org.overengineer.bettacraft.aquarist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.progression.Progress;
import org.overengineer.bettacraft.registry.ModEntities;

/**
 * M69: several aquarists per player ({@code features.aquaristNpc.team}, at most {@code hiredAquarist.maxAquarists}). Only the
 * first comes by himself ({@link AquaristArrival}); every further one is a friend whom the hired aquarist who has been with
 * the player longest (the "mediator") recommends, in his menu (tab Friend) and in the chat, once all conditions hold:
 * <ul>
 *     <li>the team is not full;</li>
 *     <li>the mediator worked {@code friendAfterDays} in-game days without a break (his wage paid every morning), and as many
 *     days went by since the last hiring;</li>
 *     <li>enough work: {@code tanksPerAquarist} own tanks per aquarist, counting the friend (the second from 6, the third
 *     from 9);</li>
 *     <li>another breeding since the last hiring (or since the last friend was refused);</li>
 *     <li>no trouble for {@code friendCalmDays} days: no aquarist of the team went without his wage and no tank in their care
 *     had bad water;</li>
 *     <li>after a refused friend: the wait of {@code returnDaysMin..returnDaysMax} days.</li>
 * </ul>
 * Hired, the friend comes to the mediator and takes a free bed and breeding basin; without them he waits next to the mediator
 * ({@code friendHomeGraceDays}) before days without a home count. Every tank is looked after by exactly one aquarist of the
 * team: the one it was given to in the menu, else the one with the nearest workplace ({@link #responsible}). The days are
 * in-game days of the overworld's day time (sleeping counts, like the wage).
 */
public final class AquaristTeam {
    /** One condition of the next friend for the menu: key (gui.bettacraft.aquarist.friend.cond.<key>), met, have and need. */
    public record Condition(String key, boolean ok, int have, int need) {}

    private AquaristTeam() {}

    /** The time the days of this class count in: the overworld's day time (ticks). */
    public static long now(MinecraftServer server) {
        return server.overworld().getDayTime();
    }

    /** The most aquarists per player (1 when the switch is off or for a config value below 1). */
    public static int max() {
        if (!Features.enabled(Feature.AQUARIST_TEAM)) return 1;
        return Math.max(1, ServerConfig.getOr(ServerConfig.AQUARIST_MAX_COUNT, 3));
    }

    /** Times of worlds before M69 (-1) or after a /time set backwards (later than now): from now on. */
    static void normalize(MinecraftServer server, AquaristRecords.PlayerRecord r) {
        long now = now(server);
        boolean dirty = false;
        for (AquaristRecords.Member m : r.team) {
            if (m.since == -1L || m.since > now) {
                m.since = now;
                dirty = true;
            }
            if (m.paidSince == -1L || m.paidSince > now) {
                m.paidSince = now;
                dirty = true;
            }
            if (m.trouble != Long.MIN_VALUE && m.trouble > now) {
                m.trouble = now;
                dirty = true;
            }
        }
        if (r.hiredCount() > 0 && (r.lastHire == -1L || r.lastHire > now)) {
            r.lastHire = now;
            dirty = true;
        }
        if (dirty) AquaristRecords.get(server).setDirty();
    }

    // ------------------------------------------------------------------ the conditions

    /** The conditions of the next friend of {@code player} (all of them, met or not), in the order of the menu. */
    public static List<Condition> conditions(MinecraftServer server, UUID player, AquaristRecords.PlayerRecord r) {
        normalize(server, r);
        List<Condition> out = new ArrayList<>();
        long now = now(server);
        int hired = r.hiredCount(), max = max();
        out.add(new Condition("limit", hired < max, hired, max));
        AquaristRecords.Member med = r.mediator();
        int need = ServerConfig.getOr(ServerConfig.AQUARIST_FRIEND_DAYS, 7);
        long streak = med == null ? 0 : now - Math.max(med.paidSince, r.lastHire);
        int days = (int) Math.max(0, streak / 24000L);
        out.add(new Condition("days", med != null && days >= need, days, need));
        int tanks = AquaristRecords.get(server).ownTanks(server, player);
        int tankNeed = ServerConfig.getOr(ServerConfig.AQUARIST_TANKS_PER, 3) * (hired + 1);
        out.add(new Condition("tanks", tanks >= tankNeed, tanks, tankNeed));
        int bred = Math.max(0, r.breedings - r.breedingsAtHire);
        out.add(new Condition("breeding", bred >= 1, bred, 1));
        int calmNeed = ServerConfig.getOr(ServerConfig.AQUARIST_CALM_DAYS, 3);
        long lastTrouble = Long.MIN_VALUE;
        for (AquaristRecords.Member m : r.team) if (m.hired) lastTrouble = Math.max(lastTrouble, m.trouble);
        int calm = lastTrouble == Long.MIN_VALUE ? Integer.MAX_VALUE : (int) Math.max(0, (now - lastTrouble) / 24000L);
        out.add(new Condition("calm", calmNeed <= 0 || calm >= calmNeed, Math.min(calm, calmNeed), calmNeed));
        if (r.friendNotBefore != Long.MIN_VALUE && now < r.friendNotBefore) {
            out.add(new Condition("wait", false, (int) Math.ceil((r.friendNotBefore - now) / 24000.0), 0));
        }
        return out;
    }

    /** Whether a friend may be recommended now (the switch, a hired mediator and every condition). */
    public static boolean ready(MinecraftServer server, UUID player, AquaristRecords.PlayerRecord r) {
        if (!Features.enabled(Feature.AQUARIST_NPC) || !Features.enabled(Feature.AQUARIST_TEAM) || r.mediator() == null) return false;
        for (Condition c : conditions(server, player, r)) if (!c.ok()) return false;
        return true;
    }

    // ------------------------------------------------------------------ the server tick

    /** From {@link AquaristArrivalHandlers#onServerTick} (every 10 s): the recommendation of a friend to every online player. */
    static void tick(MinecraftServer server) {
        AquaristRecords rec = AquaristRecords.get(server);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            AquaristRecords.PlayerRecord r = rec.peek(p.getUUID());
            if (r == null || r.team.isEmpty()) continue;
            if (r.friendOffered) {
                // withdrawn: the switch went off, the team is full, or nobody is left to recommend him
                if (!Features.enabled(Feature.AQUARIST_TEAM) || r.mediator() == null || r.hiredCount() >= max()) {
                    r.friendOffered = false;
                    rec.setDirty();
                }
                continue;
            }
            if (ready(server, p.getUUID(), r)) offer(server, p, r);
        }
    }

    /** The mediator recommends his friend: the record, and the message with the button that opens his menu at the tab Friend. */
    public static void offer(MinecraftServer server, ServerPlayer p, AquaristRecords.PlayerRecord r) {
        r.friendOffered = true;
        r.friendName = friendName(server, r);
        AquaristRecords.get(server).setDirty();
        AquaristRecords.Member med = r.mediator();
        Component who = Component.literal(med == null || med.name.isEmpty() ? "?" : med.name);
        p.sendSystemMessage(Component.translatable("message.bettacraft.aquarist.say", who,
                Component.translatable("message.bettacraft.aquarist.friend.offer", r.friendName).withStyle(ChatFormatting.WHITE)));
        MutableComponent talk = Component.translatable("message.bettacraft.aquarist.friend.talk_button").withStyle(s -> s.withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bettacraft aquarist friend show"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("message.bettacraft.aquarist.friend.talk_hover"))));
        p.sendSystemMessage(talk);
    }

    /** A first name nobody of the team has (the friend's name in the offer; he keeps it when hired). */
    static String friendName(MinecraftServer server, AquaristRecords.PlayerRecord r) {
        Set<String> used = new HashSet<>();
        for (AquaristRecords.Member m : r.team) used.add(m.name);
        List<String> free = new ArrayList<>();
        for (String n : AquaristEntity.NAMES) if (!used.contains(n)) free.add(n);
        List<String> from = free.isEmpty() ? List.of(AquaristEntity.NAMES) : free;
        return from.get(server.overworld().getRandom().nextInt(from.size()));
    }

    // ------------------------------------------------------------------ the answer

    /** The player hires the recommended friend. Null: done (the friend is returned by {@link #lastHired}); else why not. */
    @Nullable
    public static Component hireFriend(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        AquaristRecords rec = AquaristRecords.get(server);
        AquaristRecords.PlayerRecord r = rec.player(p.getUUID());
        if (!r.friendOffered) return Component.translatable("message.bettacraft.aquarist.friend.none");
        if (r.hiredCount() >= max()) {
            r.friendOffered = false;
            rec.setDirty();
            return Component.translatable("message.bettacraft.aquarist.friend.full", r.hiredCount(), max());
        }
        AquaristEntity med = mediatorEntity(server, r);
        ServerLevel level = med != null ? (ServerLevel) med.level() : p.serverLevel();
        Entity near = med != null ? med : p;
        BlockPos at = spotNear(level, near);
        AquaristEntity npc = ModEntities.HIRED_AQUARIST.get().create(level);
        if (npc == null || at == null) return Component.translatable("message.bettacraft.aquarist.no_spot");
        npc.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360F, 0F);
        npc.setVariant(level.getRandom().nextInt(AquaristEntity.VARIANTS));
        npc.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        npc.setCustomName(Component.literal(r.friendName.isEmpty() ? friendName(server, r) : r.friendName));
        npc.setEmployer(p.getUUID(), p.getGameProfile().getName());
        npc.setPhase(AquaristEntity.Phase.HIRED);
        npc.setOfferUntil(Long.MAX_VALUE);
        npc.setGraceUntil(now(server) + ServerConfig.getOr(ServerConfig.AQUARIST_GRACE_DAYS, 5) * 24000L);
        if (!level.addFreshEntity(npc)) return Component.translatable("message.bettacraft.aquarist.no_spot");
        join(server, r, npc);
        r.friendOffered = false;
        r.friendName = "";
        rec.setDirty();
        npc.findHome(level);
        note(level, npc);
        lastHired = npc;
        Progress.award(p, Progress.AQUARIST_TEAM);
        npc.say(level, Component.translatable(npc.housed() ? "message.bettacraft.aquarist.friend.hired_home" : "message.bettacraft.aquarist.friend.hired_wait",
                med == null ? Component.literal("?") : med.getName()), ChatFormatting.GREEN);
        return null;
    }

    /** The friend hired last (GameTests, the client smoke). */
    @Nullable
    public static AquaristEntity lastHired;

    /** The player does not want the friend: the next one after the wait of a refusal and another breeding. */
    @Nullable
    public static Component declineFriend(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        AquaristRecords rec = AquaristRecords.get(server);
        AquaristRecords.PlayerRecord r = rec.player(p.getUUID());
        if (!r.friendOffered) return Component.translatable("message.bettacraft.aquarist.friend.none");
        int min = ServerConfig.getOr(ServerConfig.AQUARIST_RETURN_MIN, 5), maxDays = Math.max(min, ServerConfig.getOr(ServerConfig.AQUARIST_RETURN_MAX, 10));
        long days = min + (maxDays > min ? server.overworld().getRandom().nextInt(maxDays - min + 1) : 0);
        r.friendOffered = false;
        r.friendName = "";
        r.friendsRefused++;
        r.friendNotBefore = now(server) + days * 24000L;
        r.breedingsAtHire = r.breedings;
        rec.setDirty();
        AquaristEntity med = mediatorEntity(server, r);
        if (med != null) med.say((ServerLevel) med.level(), Component.translatable("message.bettacraft.aquarist.friend.declined"), ChatFormatting.GRAY);
        return null;
    }

    /** Puts {@code npc} into the team of his employer as a hired member (a new hiring: the friend conditions start again). */
    static void join(MinecraftServer server, AquaristRecords.PlayerRecord r, AquaristEntity npc) {
        long now = now(server);
        AquaristRecords.Member m = r.member(npc.getUUID());
        if (m == null) {
            m = new AquaristRecords.Member(npc.getUUID(), true, now);
            r.team.add(m);
        }
        m.hired = true;
        m.since = now;
        m.paidSince = now;
        m.name = npc.getName().getString();
        r.lastHire = now;
        r.breedingsAtHire = r.breedings;
        AquaristRecords.get(server).setDirty();
    }

    /** A free place to stand 3 to 8 blocks from {@code near} (else around it). */
    @Nullable
    static BlockPos spotNear(ServerLevel level, Entity near) {
        var r = level.getRandom();
        BlockPos c = near.blockPosition();
        for (int i = 0; i < 60; i++) {
            int x = c.getX() + r.nextInt(17) - 8, z = c.getZ() + r.nextInt(17) - 8;
            for (int dy = -2; dy <= 2; dy++) {
                BlockPos pos = new BlockPos(x, c.getY() + dy, z);
                if (pos.distSqr(c) >= 9 && level.hasChunkAt(pos) && AquaristArrival.standable(level, pos)) return pos;
            }
        }
        return AquaristArrival.standable(level, c) ? c : null;
    }

    /** The loaded entity of the team's mediator, or null. */
    @Nullable
    public static AquaristEntity mediatorEntity(MinecraftServer server, AquaristRecords.PlayerRecord r) {
        AquaristRecords.Member m = r.mediator();
        return m == null ? null : entity(server, m.id);
    }

    @Nullable
    public static AquaristEntity entity(MinecraftServer server, UUID id) {
        for (ServerLevel l : server.getAllLevels()) {
            Entity e = l.getEntity(id);
            if (e instanceof AquaristEntity a && a.isAlive()) return a;
        }
        return null;
    }

    /** Whether {@code npc} recommends the friends of his employer (the hired one with him longest). */
    public static boolean isMediator(MinecraftServer server, AquaristEntity npc) {
        if (npc.employer() == null) return false;
        AquaristRecords.PlayerRecord r = AquaristRecords.get(server).peek(npc.employer());
        AquaristRecords.Member m = r == null ? null : r.mediator();
        return m != null && m.id.equals(npc.getUUID());
    }

    // ------------------------------------------------------------------ what the entities report

    /** Every few seconds from the entity: his name, status, workplace and the tanks he looks after, for the team tab. */
    static void note(ServerLevel level, AquaristEntity npc) {
        if (npc.employer() == null) return;
        AquaristRecords rec = AquaristRecords.get(level.getServer());
        AquaristRecords.PlayerRecord r = rec.peek(npc.employer());
        AquaristRecords.Member m = r == null ? null : r.member(npc.getUUID());
        if (m == null) return;
        String name = npc.getName().getString(), status = npc.computeStatus(level);
        BlockPos wp = npc.workplace();
        boolean wpSame = wp == null ? m.workplace == null : wp.equals(m.workplace) && level.dimension().equals(m.dimension);
        if (!name.equals(m.name) || !status.equals(m.status) || !wpSame) {
            m.name = name;
            m.status = status;
            m.workplace = wp == null ? null : wp.immutable();
            m.dimension = wp == null ? null : level.dimension();
            rec.setDirty();
        }
    }

    /** The tanks he looks after now (for the team tab; from {@link AquaristWork#tanks}). */
    static void noteCared(ServerLevel level, AquaristEntity npc, int cared) {
        AquaristRecords.Member m = member(level.getServer(), npc);
        if (m != null && m.cared != cared) {
            m.cared = cared;
            AquaristRecords.get(level.getServer()).setDirty();
        }
    }

    /** The morning: wage paid (the streak goes on) or not (the streak starts again, and it is trouble). */
    static void notePay(ServerLevel level, AquaristEntity npc, boolean paid) {
        AquaristRecords.Member m = member(level.getServer(), npc);
        if (m == null || paid) return;
        long now = now(level.getServer());
        m.paidSince = now;
        m.trouble = now;
        AquaristRecords.get(level.getServer()).setDirty();
    }

    /** A tank in his care has bad water: trouble (no friend recommends himself into a badly run business). */
    static void noteTrouble(ServerLevel level, AquaristEntity npc) {
        AquaristRecords.Member m = member(level.getServer(), npc);
        if (m == null) return;
        m.trouble = now(level.getServer());
        AquaristRecords.get(level.getServer()).setDirty();
    }

    @Nullable
    static AquaristRecords.Member member(MinecraftServer server, AquaristEntity npc) {
        if (npc.employer() == null) return null;
        AquaristRecords.PlayerRecord r = AquaristRecords.get(server).peek(npc.employer());
        return r == null ? null : r.member(npc.getUUID());
    }

    // ------------------------------------------------------------------ one aquarist per tank

    /**
     * The aquarist of {@code employer}'s team who looks after the tank: the one it was given to in the menu (while he is hired
     * and the tank lies in his work radius), else the hired one whose workplace is nearest within the work radius (ties: the
     * one with the player longest). Null: none of the team has a workplace near it.
     */
    @Nullable
    public static UUID responsible(MinecraftServer server, AquaristRecords.TankEntry e, UUID employer) {
        AquaristRecords rec = AquaristRecords.get(server);
        AquaristRecords.PlayerRecord r = rec.peek(employer);
        if (r == null) return null;
        int radius = ServerConfig.getOr(ServerConfig.AQUARIST_WORK_RADIUS, 32);
        UUID given = rec.assignedTo(e.tank());
        if (given != null) {
            AquaristRecords.Member m = r.member(given);
            if (m != null && m.hired && inRadius(m, e, radius)) return given;
        }
        AquaristRecords.Member best = null;
        double bd = Double.MAX_VALUE;
        for (AquaristRecords.Member m : r.team) {
            if (!m.hired || !inRadius(m, e, radius)) continue;
            double d = m.workplace.distSqr(e.pos());
            if (d < bd || d == bd && best != null && m.since < best.since) {
                bd = d;
                best = m;
            }
        }
        return best == null ? null : best.id;
    }

    /** Whether the tank lies in the work radius of the member's workplace (the same test as {@link AquaristRecords#near}). */
    static boolean inRadius(AquaristRecords.Member m, AquaristRecords.TankEntry e, int radius) {
        if (m.workplace == null || m.dimension == null || !m.dimension.equals(e.dimension())) return false;
        long dx = e.pos().getX() - m.workplace.getX(), dz = e.pos().getZ() - m.workplace.getZ();
        return dx * dx + dz * dz <= (long) radius * radius && Math.abs(e.pos().getY() - m.workplace.getY()) <= radius;
    }

    /** Whether {@code npc} looks after the tank of the index (true when nobody of his team does: then he may). */
    public static boolean mine(MinecraftServer server, AquaristEntity npc, AquaristRecords.TankEntry e) {
        if (npc.employer() == null) return false;
        UUID who = responsible(server, e, npc.employer());
        return who == null || who.equals(npc.getUUID());
    }

    /**
     * The menu gives the tank to {@code to} (null: back to the nearest workplace). Null: done; else why not (not of the team,
     * or the tank is out of his work radius).
     */
    @Nullable
    public static Component assign(MinecraftServer server, UUID employer, UUID tank, @Nullable UUID to) {
        AquaristRecords rec = AquaristRecords.get(server);
        if (to == null) {
            rec.assign(tank, null);
            return null;
        }
        AquaristRecords.PlayerRecord r = rec.peek(employer);
        AquaristRecords.Member m = r == null ? null : r.member(to);
        AquaristRecords.TankEntry e = rec.tank(tank);
        if (m == null || !m.hired || e == null) return Component.translatable("message.bettacraft.aquarist.not_yours");
        if (!inRadius(m, e, ServerConfig.getOr(ServerConfig.AQUARIST_WORK_RADIUS, 32))) {
            return Component.translatable("message.bettacraft.aquarist.team.too_far", m.name);
        }
        rec.assign(tank, to);
        return null;
    }

    // ------------------------------------------------------------------ dev: a ready team for the showcase and the smoke

    /** Dev and GameTests: adds a hired aquarist to the team of {@code employer} at {@code at} (no checks, no offer). */
    public static AquaristEntity spawnMember(ServerLevel level, UUID employer, String employerName, Vec3 at) {
        AquaristEntity npc = ModEntities.HIRED_AQUARIST.get().create(level);
        if (npc == null) throw new IllegalStateException("no aquarist entity");
        npc.moveTo(at.x, at.y, at.z, 0F, 0F);
        AquaristRecords.PlayerRecord r = AquaristRecords.get(level.getServer()).player(employer);
        npc.setVariant(Math.floorMod(employer.hashCode() + r.team.size(), AquaristEntity.VARIANTS));
        npc.setCustomName(null);
        npc.setEmployer(employer, employerName);
        npc.setPhase(AquaristEntity.Phase.HIRED);
        npc.setOfferUntil(Long.MAX_VALUE);
        level.addFreshEntity(npc);
        r.name = employerName;
        join(level.getServer(), r, npc);
        return npc;
    }
}
