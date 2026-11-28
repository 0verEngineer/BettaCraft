package org.overengineer.bettacraft.aquarist;

import org.overengineer.bettacraft.util.Perms;
import org.overengineer.bettacraft.util.NbtCompat;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.network.AquaristActionPayload;
import org.overengineer.bettacraft.network.AquaristViewPayload;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M51: the menu of a hired aquarist (no container: a view sent to the client, actions sent back). The offering aquarist
 * shows his offer with the buttons to hire or refuse him; the hired one his status, home, wage, the tasks with their
 * levels, the tanks around his workplace with what he may do there, and the rearing tank. Only his employer (or an
 * operator) uses it, within 8 blocks. M69: every aquarist shows the team of his employer (tab Team); the one who
 * recommends friends shows the friend's conditions and the offer (tab Friend); a tank can be given to one of the team.
 */
public final class AquaristMenu {
    public static final int HIRE = 0;
    public static final int DECLINE = 1;
    /** value = task ordinal * 10 + level. */
    public static final int SET_LEVEL = 2;
    /** text = the tank id, or "" for none. */
    public static final int SET_REARING = 3;
    public static final int FIND_HOME = 4;
    public static final int DISMISS = 5;
    /** M66: text = a tank id: adds it at the end of his rearing tanks / takes it out. */
    public static final int ADD_REARING = 6, REMOVE_REARING = 7;
    /** M66: most rearing tanks one aquarist has. */
    public static final int MAX_REARING = 5;
    /** M66: text = "tank id|species key": switches the species on or off for that rearing tank; CLEAR_SPECIES (text = tank id): every species. */
    public static final int TOGGLE_SPECIES = 8, CLEAR_SPECIES = 9;
    /** M66: value = task ordinal * 2 + (1 = on, 0 = off), text = the tank id: what he may do at that tank. */
    public static final int SET_TANK_TASK = 10;
    /** M66: wait for the next right-click on a chest / a bed; AUTO_*: back to looking by himself. */
    public static final int ASSIGN_CHEST = 11, ASSIGN_BED = 12, AUTO_CHEST = 13, AUTO_BED = 14, ASSIGN_BASIN = 15, AUTO_BASIN = 16;
    /** M69: hire / refuse the friend the mediator recommends. */
    public static final int FRIEND_HIRE = 17, FRIEND_DECLINE = 18;
    /** M69: text = "tank id|aquarist id" gives the tank to that aquarist of the team, "tank id|" back to the nearest workplace. */
    public static final int SET_RESPONSIBLE = 19;
    /** Rows of the tank list. */
    public static final int MAX_TANK_ROWS = 24;

    private AquaristMenu() {}

    public static void open(ServerPlayer p, AquaristEntity npc) {
        PacketDistributor.sendToPlayer(p, new AquaristViewPayload(view(p, npc)));
    }

    /** What the menu shows (also read by GameTests and the client smoke). */
    public static CompoundTag view(ServerPlayer viewer, AquaristEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        CompoundTag v = new CompoundTag();
        v.putInt("id", npc.getId());
        NbtCompat.putUUID(v, "uuid", npc.getUUID()); // M69: to tell his tanks from those of the team
        v.putString("name", npc.getName().getString());
        v.putString("phase", npc.phase().name().toLowerCase(java.util.Locale.ROOT));
        v.putString("status", npc.computeStatus(level));
        v.putString("employer", npc.employerName());
        v.putBoolean("boss", viewer.getUUID().equals(npc.employer()) || Perms.has(viewer, 2));
        v.putBoolean("feature", Features.enabled(Feature.AQUARIST_NPC));
        int[] lv = new int[AquaristTask.ALL.length];
        for (AquaristTask t : AquaristTask.ALL) lv[t.ordinal()] = npc.taskLevel(t);
        v.putIntArray("tasks", lv);
        if (npc.workplace() != null) v.putLong("workplace", npc.workplace().asLong());
        if (npc.bed() != null) v.putLong("bed", npc.bed().asLong());
        v.putBoolean("wage", ServerConfig.getOr(ServerConfig.AQUARIST_WAGE, true));
        v.putInt("wage_emeralds", ServerConfig.getOr(ServerConfig.AQUARIST_WAGE_EMERALDS, 1));
        v.putInt("wage_food", ServerConfig.getOr(ServerConfig.AQUARIST_WAGE_FOOD, 3));
        v.putInt("unpaid", npc.unpaidDays());
        v.putInt("homeless", npc.homelessDays());
        v.putInt("leave_days", ServerConfig.getOr(ServerConfig.AQUARIST_LEAVE_DAYS, 3));
        v.putInt("max_tanks", ServerConfig.getOr(ServerConfig.AQUARIST_MAX_TANKS, 16));
        v.putInt("radius", ServerConfig.getOr(ServerConfig.AQUARIST_WORK_RADIUS, 32));
        v.putInt("cared", AquaristWork.tanks(level, npc).size());
        v.putString("job", npc.currentJob() == null ? "" : npc.currentJob().kind().name().toLowerCase(java.util.Locale.ROOT));
        v.putBoolean("food", AquaristWork.chestsHaveFood(level, npc));
        v.putInt("chests", AquaristWork.chests(level, npc).size());
        if (npc.chest() != null) v.putLong("chest", npc.chest().asLong()); // M66: assigned (else automatic)
        v.putInt("home_radius", ServerConfig.getOr(ServerConfig.AQUARIST_HOME_RADIUS, 32));
        if (npc.offerUntil() != Long.MAX_VALUE) v.putLong("offer_left", Math.max(0, npc.offerUntil() - level.getServer().overworld().getGameTime()));
        if (npc.rearingTank() != null) org.overengineer.bettacraft.util.NbtCompat.putUUID(v, "rearing", npc.rearingTank());
        AquaristRecords rec = AquaristRecords.get(level.getServer());
        // M66: the rearing tanks in order, each with its species
        ListTag rear = new ListTag();
        for (AquaristEntity.RearingTank r : npc.rearingTanks()) {
            CompoundTag t = new CompoundTag();
            org.overengineer.bettacraft.util.NbtCompat.putUUID(t, "id", r.tank());
            AquaristRecords.TankEntry e = rec.tank(r.tank());
            TankBlockEntity c = e == null ? null : rec.resolve(level.getServer(), e);
            if (e != null) t.putLong("pos", e.pos().asLong());
            t.putString("name", c == null ? "" : c.getTank().name());
            t.putBoolean("ok", AquaristWork.tank(level, npc, r.tank()) != null);
            ListTag sp = new ListTag();
            for (String k : r.species()) sp.add(net.minecraft.nbt.StringTag.valueOf(k));
            t.put("species", sp);
            rear.add(t);
        }
        v.put("rearing_tanks", rear);
        ListTag opts = new ListTag();
        for (String[] o : AquaristWork.speciesOptions()) {
            CompoundTag t = new CompoundTag();
            t.putString("k", o[0]);
            t.putString("t", o[1]);
            opts.add(t);
        }
        v.put("species_options", opts);
        ListTag tanks = new ListTag();
        for (AquaristRecords.TankEntry e : AquaristWork.tanksAround(level, npc)) {
            if (tanks.size() >= MAX_TANK_ROWS) break;
            TankBlockEntity c = rec.resolve(level.getServer(), e);
            CompoundTag t = new CompoundTag();
            NbtCompat.putUUID(t, "id", e.tank());
            t.putLong("pos", e.pos().asLong());
            t.putInt("off", npc.tasksOff(e.tank())); // M66: the tasks switched off here
            // M69: who of the team looks after it, and whether it was given to him in the menu
            UUID who = npc.employer() == null ? null : AquaristTeam.responsible(level.getServer(), e, npc.employer());
            if (who != null) NbtCompat.putUUID(t, "who", who);
            t.putBoolean("given", rec.assignedTo(e.tank()) != null && rec.assignedTo(e.tank()).equals(who));
            if (c == null) {
                t.putString("name", "");
                t.putString("verdict", "unloaded");
            } else {
                t.putString("name", c.getTank().name());
                t.putString("verdict", AquaristRules.verdict(c.getTank(), npc.employer()).name().toLowerCase(java.util.Locale.ROOT));
            }
            tanks.add(t);
        }
        v.put("tanks", tanks);
        team(level, npc, rec, v);
        return v;
    }

    /** M69: the team of his employer (every member, as the records know them) and, for the mediator, the friend. */
    private static void team(ServerLevel level, AquaristEntity npc, AquaristRecords rec, CompoundTag v) {
        AquaristRecords.PlayerRecord r = npc.employer() == null ? null : rec.peek(npc.employer());
        v.putBoolean("team_feature", Features.enabled(Feature.AQUARIST_TEAM));
        v.putInt("team_max", AquaristTeam.max());
        if (r == null) return;
        AquaristRecords.Member med = r.mediator();
        ListTag team = new ListTag();
        for (AquaristRecords.Member m : r.team) {
            if (!m.hired) continue;
            AquaristEntity e = AquaristTeam.entity(level.getServer(), m.id);
            if (e != null) AquaristTeam.note((ServerLevel) e.level(), e);
            CompoundTag t = new CompoundTag();
            NbtCompat.putUUID(t, "id", m.id);
            t.putString("name", m.name);
            t.putString("status", e != null ? e.computeStatus((ServerLevel) e.level()) : m.status.isEmpty() ? "unloaded" : m.status);
            t.putBoolean("loaded", e != null);
            t.putInt("cared", m.cared);
            if (m.workplace != null) t.putLong("workplace", m.workplace.asLong());
            t.putBoolean("me", m.id.equals(npc.getUUID()));
            t.putBoolean("mediator", med != null && med.id.equals(m.id));
            t.putInt("days", (int) Math.max(0, (AquaristTeam.now(level.getServer()) - m.since) / 24000L));
            team.add(t);
        }
        v.put("team", team);
        v.putInt("team_hired", r.hiredCount());
        boolean mediator = med != null && med.id.equals(npc.getUUID());
        v.putBoolean("mediator", mediator);
        if (!mediator) {
            if (med != null) v.putString("mediator_name", med.name);
            return;
        }
        CompoundTag f = new CompoundTag();
        f.putBoolean("offered", r.friendOffered);
        f.putString("name", r.friendName);
        ListTag conds = new ListTag();
        for (AquaristTeam.Condition c : AquaristTeam.conditions(level.getServer(), npc.employer(), r)) {
            CompoundTag t = new CompoundTag();
            t.putString("k", c.key());
            t.putBoolean("ok", c.ok());
            t.putInt("have", c.have());
            t.putInt("need", c.need());
            conds.add(t);
        }
        f.put("conds", conds);
        v.put("friend", f);
    }

    private static UUID parse(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void handle(ServerPlayer p, AquaristActionPayload a) {
        if (!(p.level().getEntity(a.entity()) instanceof AquaristEntity npc) || !npc.isAlive()) return;
        if (p.distanceToSqr(npc) > 8 * 8 || p.isSpectator()) return;
        boolean employer = p.getUUID().equals(npc.employer());
        boolean boss = employer || Perms.has(p, 2);
        ServerLevel level = (ServerLevel) npc.level();
        Component err = null;
        switch (a.action()) {
            case HIRE -> err = employer ? AquaristArrival.hire(p) : Component.translatable("message.bettacraft.aquarist.not_yours");
            case DECLINE -> {
                err = employer ? AquaristArrival.decline(p) : Component.translatable("message.bettacraft.aquarist.not_yours");
                if (err == null) {
                    p.closeContainer();
                    return;
                }
            }
            case SET_LEVEL -> {
                int task = a.value() / 10, lvl = a.value() % 10;
                if (!boss) err = Component.translatable("message.bettacraft.aquarist.not_yours");
                else if (task >= 0 && task < AquaristTask.ALL.length) npc.setTaskLevel(AquaristTask.ALL[task], lvl);
            }
            case SET_REARING -> {
                if (!boss) err = Component.translatable("message.bettacraft.aquarist.not_yours");
                else if (a.text().isEmpty()) npc.setRearingTank(null);
                else {
                    try {
                        UUID id = UUID.fromString(a.text());
                        if (AquaristWork.tank(level, npc, id) == null) err = Component.translatable("message.bettacraft.aquarist.rearing_not_his");
                        else npc.setRearingTank(id);
                    } catch (IllegalArgumentException e) {
                        err = Component.translatable("message.bettacraft.aquarist.rearing_not_his");
                    }
                }
            }
            case ADD_REARING -> {
                UUID id = parse(a.text());
                if (!boss) err = Component.translatable("message.bettacraft.aquarist.not_yours");
                else if (id == null || AquaristWork.tank(level, npc, id) == null) err = Component.translatable("message.bettacraft.aquarist.rearing_not_his");
                else if (npc.rearingTanks().size() >= MAX_REARING) err = Component.translatable("message.bettacraft.aquarist.rearing_max", MAX_REARING);
                else npc.addRearingTank(id);
            }
            case REMOVE_REARING -> {
                UUID id = parse(a.text());
                if (!boss) err = Component.translatable("message.bettacraft.aquarist.not_yours");
                else if (id != null) npc.removeRearingTank(id);
            }
            case TOGGLE_SPECIES -> {
                int bar = a.text().indexOf("|");
                UUID id = bar < 0 ? null : parse(a.text().substring(0, bar));
                if (!boss) err = Component.translatable("message.bettacraft.aquarist.not_yours");
                else if (id != null) npc.toggleRearingSpecies(id, a.text().substring(bar + 1));
            }
            case CLEAR_SPECIES -> {
                UUID id = parse(a.text());
                if (!boss) err = Component.translatable("message.bettacraft.aquarist.not_yours");
                else if (id != null) npc.clearRearingSpecies(id);
            }
            case SET_TANK_TASK -> {
                UUID id = parse(a.text());
                int task = a.value() / 2;
                if (!boss) err = Component.translatable("message.bettacraft.aquarist.not_yours");
                else if (id != null && task >= 0 && task < AquaristTask.ALL.length) npc.setMayDo(id, AquaristTask.ALL[task], a.value() % 2 == 1);
            }
            case ASSIGN_CHEST, ASSIGN_BED, ASSIGN_BASIN -> {
                if (!boss) err = Component.translatable("message.bettacraft.aquarist.not_yours");
                else {
                    AquaristAssign.start(p, npc, a.action() == ASSIGN_BED ? AquaristAssign.Kind.BED : a.action() == ASSIGN_BASIN ? AquaristAssign.Kind.BASIN : AquaristAssign.Kind.CHEST);
                    return; // the menu closed itself; it opens again when the block is clicked
                }
            }
            case AUTO_BASIN -> {
                if (!boss) err = Component.translatable("message.bettacraft.aquarist.not_yours");
                else npc.autoBasin(level);
            }
            case AUTO_CHEST -> {
                if (!boss) err = Component.translatable("message.bettacraft.aquarist.not_yours");
                else npc.setChest(null);
            }
            case AUTO_BED -> {
                if (!boss) err = Component.translatable("message.bettacraft.aquarist.not_yours");
                else {
                    var poi = level.getPoiManager();
                    if (npc.bed() != null && level.isLoaded(npc.bed()) && poi.existsAtPosition(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME, npc.bed())) poi.release(npc.bed());
                    npc.clearBed();
                    npc.findHome(level);
                }
            }
            case FIND_HOME -> {
                if (!boss) err = Component.translatable("message.bettacraft.aquarist.not_yours");
                else {
                    npc.releaseHome(level);
                    npc.findHome(level);
                    if (!npc.housed()) err = Component.translatable("message.bettacraft.aquarist.no_home_found", ServerConfig.getOr(ServerConfig.AQUARIST_HOME_RADIUS, 32));
                }
            }
            case FRIEND_HIRE, FRIEND_DECLINE -> {
                if (!employer) err = Component.translatable("message.bettacraft.aquarist.not_yours");
                else if (!AquaristTeam.isMediator(level.getServer(), npc)) err = Component.translatable("message.bettacraft.aquarist.friend.none");
                else err = a.action() == FRIEND_HIRE ? AquaristTeam.hireFriend(p) : AquaristTeam.declineFriend(p);
            }
            case SET_RESPONSIBLE -> {
                int bar = a.text().indexOf("|");
                UUID id = bar < 0 ? null : parse(a.text().substring(0, bar));
                UUID to = bar < 0 || bar + 1 >= a.text().length() ? null : parse(a.text().substring(bar + 1));
                if (!boss || npc.employer() == null) err = Component.translatable("message.bettacraft.aquarist.not_yours");
                else if (id != null) err = AquaristTeam.assign(level.getServer(), npc.employer(), id, to);
            }
            case DISMISS -> {
                if (!boss || npc.phase() != AquaristEntity.Phase.HIRED) err = Component.translatable("message.bettacraft.aquarist.not_yours");
                else {
                    AquaristArrival.dismiss(p, npc);
                    return;
                }
            }
            default -> {
                return;
            }
        }
        if (err != null) p.sendOverlayMessage(err.copy().withStyle(ChatFormatting.RED));
        if (npc.isAlive() && npc.phase() != AquaristEntity.Phase.LEAVING) open(p, npc);
    }
}
