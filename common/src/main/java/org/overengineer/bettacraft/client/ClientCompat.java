package org.overengineer.bettacraft.client;

/** M52b: client helpers for the APIs that changed between Minecraft 1.21.1 and 1.21.11. */
public final class ClientCompat {
    private ClientCompat() {}

    /** The block atlas (1.21.1 {@code net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS}; since 1.21.9 managed by the atlas manager). */
    public static net.minecraft.client.renderer.texture.TextureAtlas blockAtlas() {
        return net.minecraft.client.Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS);
    }

    /** The sprites of the block atlas by texture id (1.21.1 {@code Minecraft#getTextureAtlas(BLOCK_ATLAS)}). */
    public static java.util.function.Function<net.minecraft.resources.Identifier, net.minecraft.client.renderer.texture.TextureAtlasSprite> blockSprites() {
        net.minecraft.client.renderer.texture.TextureAtlas atlas = blockAtlas();
        return atlas::getSprite;
    }

    /** The partial tick of the frame (1.21.1 {@code Minecraft#getTimer()}). */
    public static float partialTick() {
        return net.minecraft.client.Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    /**
     * A text colour as 1.21.1 drew it: an alpha of 0 (an RGB value like {@code 0xFFFFFF}) meant opaque. Since 1.21.6 text
     * colours are ARGB and such a colour would be invisible.
     */
    public static int textColor(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }
}
