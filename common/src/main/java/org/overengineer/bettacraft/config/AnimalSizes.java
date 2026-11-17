package org.overengineer.bettacraft.config;

import javax.annotation.Nullable;

/**
 * M36: the size setting of the animals ({@code sizes.betta} in the server config). The server's value counts for everyone:
 * the hitbox follows it on the server, and a client of a remote server gets it with the server settings (login and every
 * reload of the server config; {@link org.overengineer.bettacraft.network.ServerSettingsPayload}). The animals check the
 * factor every tick and refresh their hitbox when it changed, so a change works at once, without a restart.
 * <p>
 * Normal is about halfway between the bettas of DragN's Just Bettas and the size before M36 (measured in M36: male halfmoon
 * 0.72 blocks long with the tail before, 0.39 there): 0.77, 0.55 blocks; that is also the 12 cm per block of the tankmates.
 */
public final class AnimalSizes {
    /** Factor of a betta at {@link AnimalSize#NORMAL} on the size before M36 ({@link AnimalSize#LARGE} = 1). */
    public static final float BETTA_NORMAL = 0.77F;

    /** Client of a remote server: the server's setting (null = the local config: single player, an integrated server). */
    @Nullable
    private static volatile AnimalSize clientBetta;

    private AnimalSizes() {}

    /** The betta size setting on this side. */
    public static AnimalSize betta() {
        AnimalSize remote = clientBetta;
        if (remote != null && org.overengineer.bettacraft.platform.Platform.get().currentServer() == null) return remote;
        return ServerConfig.getOr(ServerConfig.SIZE_BETTA, AnimalSize.NORMAL);
    }

    /** The factor of the bettas' model and hitbox (1 = the size before M36). */
    public static float bettaFactor() {
        return factor(betta(), BETTA_NORMAL);
    }

    /** The factor of a setting for an animal with this Normal factor. */
    public static float factor(AnimalSize size, float normal) {
        return size == AnimalSize.LARGE ? 1F : normal;
    }

    /** The server setting (the server settings payload). */
    public static AnimalSize serverBetta() {
        return ServerConfig.getOr(ServerConfig.SIZE_BETTA, AnimalSize.NORMAL);
    }

    /** Client: the setting of the server it is connected to; null after logout. */
    public static void setClient(@Nullable AnimalSize betta) {
        clientBetta = betta;
    }
}
