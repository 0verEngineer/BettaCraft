package org.overengineer.bettacraft.aquarist;

/**
 * M51: who may let his hired aquarist work at a tank (the button in the tank menu, owner or OP only). {@link #OWNER} is the
 * default of every tank: the aquarists of the owner and (with FTB Teams, {@code hiredAquarist.ftbTeams}) of his team.
 * {@link #ALL} releases the tank for the aquarists of every player (friends who share their aquarist), {@link #NONE} keeps
 * every aquarist away, also the owner's. A locked tank is never touched, whatever is set here.
 */
public enum AquaristAccess {
    OWNER("owner"),
    ALL("all"),
    NONE("none");

    private final String id;

    AquaristAccess(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public AquaristAccess next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public String translationKey() {
        return "gui.bettacraft.tank.aquarist." + id;
    }

    public static AquaristAccess byId(String id) {
        for (AquaristAccess a : values()) if (a.id.equals(id)) return a;
        return OWNER;
    }
}
