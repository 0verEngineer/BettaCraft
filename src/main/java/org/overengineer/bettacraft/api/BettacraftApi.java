package org.overengineer.bettacraft.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.monitor.MonitorValue;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M32 part d: read-only helpers for modpack scripts (KubeJS: {@code Java.loadClass('org.overengineer.bettacraft.api.BettacraftApi')})
 * and other mods, server side. {@code pos} may be any block of a tank. The events are in {@code api.event}.
 */
public final class BettacraftApi {
    private BettacraftApi() {}

    @Nullable
    private static TankBlockEntity tank(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel) || !level.isLoaded(pos)) return null;
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        return c != null && c.isController() && !c.isRemoved() ? c : null;
    }

    /** Whether {@code pos} is a block of a Bettacraft tank. */
    public static boolean isTank(Level level, BlockPos pos) {
        return tank(level, pos) != null;
    }

    /** The stable id of the tank, null without one. */
    @Nullable
    public static UUID tankId(Level level, BlockPos pos) {
        TankBlockEntity c = tank(level, pos);
        return c == null ? null : c.getTank().id();
    }

    /** The name of the tank's sign ("" without one, null without a tank). */
    @Nullable
    public static String tankName(Level level, BlockPos pos) {
        TankBlockEntity c = tank(level, pos);
        return c == null ? null : c.getTank().name();
    }

    /** The controller block of the tank, null without one. */
    @Nullable
    public static BlockPos tankController(Level level, BlockPos pos) {
        TankBlockEntity c = tank(level, pos);
        return c == null ? null : c.getBlockPos().immutable();
    }

    /**
     * The values the monitors show, by id ({@code temperature}, {@code water_quality}, {@code ph}, {@code tds},
     * {@code salinity}, {@code fill}, ...; NaN where a value does not apply). Empty without a tank.
     */
    public static Map<String, Float> tankValues(Level level, BlockPos pos) {
        Map<String, Float> out = new LinkedHashMap<>();
        TankBlockEntity c = tank(level, pos);
        if (c == null) return out;
        float[] v = MonitorValue.sampleAll((ServerLevel) level, c);
        for (MonitorValue m : MonitorValue.ALL) out.put(m.id, v[m.ordinal()]);
        return out;
    }

    /** M45: the prefix of the FTB Quests tags for a feature switch ({@code bettacraft_feature:tankmates.livebearerGenetics}). */
    public static final String TAG_FEATURE = "bettacraft_feature:";
    /** M45: the prefix of the quest tags for another mod the quest needs ({@code bettacraft_mod:create}). */
    public static final String TAG_MOD = "bettacraft_mod:";
    /** M45: the quest tag of quests that only work while a device needs power ({@code power.*RequiresEnergy} in the server config). */
    public static final String TAG_NEEDS_POWER = "bettacraft_needs:power";
    /** M45: the quest tag of quests that are optional by design (they stay optional while shown). */
    public static final String TAG_OPTIONAL = "bettacraft_optional";

    /**
     * Whether the feature switch of this path is on ({@code tankmates.livebearerGenetics}, {@code corals.enabled}; the switch
     * itself, its parents and the features it requires, see {@code config/bettacraft-common.toml}). An unknown path counts as on.
     */
    public static boolean featureEnabled(String path) {
        Feature f = Feature.byPath(path);
        return f == null || Features.enabled(f);
    }

    /**
     * M45: sends an FTB Quests quest (or any quest object) that a script changed on the server to every player, the way FTB
     * Quests' editor does ({@code EditObjectResponseMessage}); scripts cannot pick that constructor themselves (it is
     * overloaded). By reflection, no dependency on FTB Quests; returns false without it.
     */
    public static boolean sendQuestEdit(Object questObject) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;
        try {
            Class<?> base = Class.forName("dev.ftb.mods.ftbquests.quest.QuestObjectBase");
            Object msg = Class.forName("dev.ftb.mods.ftbquests.net.EditObjectResponseMessage").getConstructor(base).newInstance(questObject);
            // FTB Quests' payloads go through Architectury's network (its own wrapper), not NeoForge's PacketDistributor
            Class.forName("dev.architectury.networking.NetworkManager")
                    .getMethod("sendToPlayers", Iterable.class, net.minecraft.network.protocol.common.custom.CustomPacketPayload.class)
                    .invoke(null, server.getPlayerList().getPlayers(), msg);
            return true;
        } catch (ReflectiveOperationException | ClassCastException | LinkageError e) {
            com.mojang.logging.LogUtils.getLogger().warn("Could not send the changed quest {}: {}", questObject, e.toString());
            return false;
        }
    }

    /**
     * M45: whether a quest with these FTB Quests tags fits this server: every {@code bettacraft_feature:<path>} is on, every
     * {@code bettacraft_mod:<modid>} is loaded and, with {@code bettacraft_needs:power}, a device needs power. Other tags are
     * ignored. The example script {@code bettacraft_quests.js} hides the quests of the Bettacraft chapter where this is false.
     */
    public static boolean questTagsOn(Iterable<String> tags) {
        for (String t : tags) {
            if (t.startsWith(TAG_FEATURE) && !featureEnabled(t.substring(TAG_FEATURE.length()))) return false;
            if (t.startsWith(TAG_MOD) && !net.neoforged.fml.ModList.get().isLoaded(t.substring(TAG_MOD.length()))) return false;
            if (t.equals(TAG_NEEDS_POWER) && !org.overengineer.bettacraft.tank.TankPower.anyRequired()) return false;
        }
        return true;
    }
}
