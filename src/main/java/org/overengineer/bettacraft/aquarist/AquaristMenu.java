package org.overengineer.bettacraft.aquarist;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
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
 * operator) uses it, within 8 blocks.
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
        v.putString("name", npc.getName().getString());
        v.putString("phase", npc.phase().name().toLowerCase(java.util.Locale.ROOT));
        v.putString("status", npc.computeStatus(level));
        v.putString("employer", npc.employerName());
        v.putBoolean("boss", viewer.getUUID().equals(npc.employer()) || viewer.hasPermissions(2));
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
        v.putBoolean("food", AquaristWork.chestsHaveFood(level, npc.workplace()));
        v.putInt("chests", AquaristWork.chests(level, npc.workplace()).size());
        if (npc.offerUntil() != Long.MAX_VALUE) v.putLong("offer_left", Math.max(0, npc.offerUntil() - level.getServer().overworld().getGameTime()));
        if (npc.rearingTank() != null) v.putUUID("rearing", npc.rearingTank());
        ListTag tanks = new ListTag();
        AquaristRecords rec = AquaristRecords.get(level.getServer());
        for (AquaristRecords.TankEntry e : AquaristWork.tanksAround(level, npc)) {
            if (tanks.size() >= MAX_TANK_ROWS) break;
            TankBlockEntity c = rec.resolve(level.getServer(), e);
            CompoundTag t = new CompoundTag();
            t.putUUID("id", e.tank());
            t.putLong("pos", e.pos().asLong());
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
        return v;
    }

    public static void handle(ServerPlayer p, AquaristActionPayload a) {
        if (!(p.level().getEntity(a.entity()) instanceof AquaristEntity npc) || !npc.isAlive()) return;
        if (p.distanceToSqr(npc) > 8 * 8 || p.isSpectator()) return;
        boolean employer = p.getUUID().equals(npc.employer());
        boolean boss = employer || p.hasPermissions(2);
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
            case FIND_HOME -> {
                if (!boss) err = Component.translatable("message.bettacraft.aquarist.not_yours");
                else {
                    npc.releaseHome(level);
                    npc.findHome(level);
                    if (!npc.housed()) err = Component.translatable("message.bettacraft.aquarist.no_home_found", ServerConfig.getOr(ServerConfig.AQUARIST_HOME_RADIUS, 32));
                }
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
        if (err != null) p.displayClientMessage(err.copy().withStyle(ChatFormatting.RED), true);
        if (npc.isAlive() && npc.phase() != AquaristEntity.Phase.LEAVING) open(p, npc);
    }
}
