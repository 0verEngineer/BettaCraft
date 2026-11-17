package org.overengineer.bettacraft.aquarist;

import java.util.UUID;
import javax.annotation.Nullable;
import org.overengineer.bettacraft.compat.FtbTeamsCompat;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.tank.TankData;

/**
 * M51 (question B1): at which tanks a hired aquarist works. Every check of a task asks again right before it touches the
 * tank, so a tank locked or taken away while he walks to it stays untouched.
 * <ul>
 *     <li>locked tanks never (the lock of the tank menu keeps out everybody but the owner, and the aquarist is not him);</li>
 *     <li>tanks set to "no aquarist" never;</li>
 *     <li>tanks without an owner never (the next player who opens one becomes its owner);</li>
 *     <li>the employer's own tanks;</li>
 *     <li>with FTB Teams ({@code hiredAquarist.ftbTeams}) the tanks of the other members of the employer's party;</li>
 *     <li>tanks of other players only when their owner released them for every aquarist.</li>
 * </ul>
 */
public final class AquaristRules {
    public enum Verdict {
        OWN, TEAM, RELEASED, FOREIGN, LOCKED, EXCLUDED, NO_OWNER;

        public boolean works() {
            return this == OWN || this == TEAM || this == RELEASED;
        }

        public String translationKey() {
            return "gui.bettacraft.aquarist.tank." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private AquaristRules() {}

    public static Verdict verdict(TankData t, @Nullable UUID employer) {
        if (t.locked()) return Verdict.LOCKED;
        if (t.aquaristAccess() == AquaristAccess.NONE) return Verdict.EXCLUDED;
        UUID owner = t.owner();
        if (owner == null || employer == null) return Verdict.NO_OWNER;
        if (owner.equals(employer)) return Verdict.OWN;
        if (ServerConfig.getOr(ServerConfig.AQUARIST_FTB_TEAMS, true) && FtbTeamsCompat.sameTeam(owner, employer)) return Verdict.TEAM;
        if (t.aquaristAccess() == AquaristAccess.ALL) return Verdict.RELEASED;
        return Verdict.FOREIGN;
    }

    public static boolean mayWork(TankData t, @Nullable UUID employer) {
        return verdict(t, employer).works();
    }
}
