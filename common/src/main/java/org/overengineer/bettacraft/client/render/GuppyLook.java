package org.overengineer.bettacraft.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.genetics.livebearer.LbPhenotype;

/**
 * M21.6: the look of a guppy or endler with the livebearer genetics: which mask textures
 * ({@code textures/entity/tankmates/guppy/*.png}, made by {@code tools/assetgen/guppies.py}) are composited with which
 * tint ({@link BettaSkins}), bottom first: body and fins give the coverage, then the tail colour, the belly, the X and Y
 * patterns, the tail pattern, the eyes. Females (and the young) are grey with a faint tail colour and small fins; only
 * the tuxedo of their X shows a little. Endlers (and hybrids by their share) have clear tails with coloured edges and
 * swords. Albinos lack the black pigment (red eyes, the dark patterns turn pale), gold bodies have brown instead of black.
 */
public final class GuppyLook {
    private GuppyLook() {}

    private static final Map<String, Identifier> MASKS = new HashMap<>();

    public static Identifier mask(String name) {
        return MASKS.computeIfAbsent(name, n -> BettaCraft.id("textures/entity/tankmates/guppy/" + n + ".png"));
    }

    private static int body(String body, boolean female) {
        return switch (body) {
            case "gold" -> female ? 0xD0B468 : 0xDCBA5C;
            case "albino" -> female ? 0xEED8D0 : 0xF2DCD4;
            case "blond" -> female ? 0xDCD4B4 : 0xE6DCB4;
            case "white" -> female ? 0xE8E6E0 : 0xF2F0EA;
            case "blue" -> female ? 0x8A98B4 : 0x7890C0;
            case "dark" -> female ? 0x5A5854 : 0x4A4844;
            default -> female ? 0x9C9A84 : 0xA8A48A;
        };
    }

    public static int tail(String color) {
        return switch (color) {
            case "red" -> 0xE02A20;
            case "black" -> 0x262424;
            case "orange" -> 0xF07A1A;
            case "yellow" -> 0xF0D040;
            case "green" -> 0x8CC83C;
            case "blue" -> 0x3A70E0;
            case "white" -> 0xF4F4F0;
            default -> 0xD8CCA8; // clear
        };
    }

    private static int argb(int alpha, int rgb) {
        return (alpha & 0xFF) << 24 | (rgb & 0xFFFFFF);
    }

    static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }

    /** The layers of a fish; {@code adultLook}: a male old enough to show his colours, {@code gravid}: the dark spot. */
    public static BettaSkins.Layer[] layers(LbPhenotype p, boolean adultLook, boolean gravid) {
        List<BettaSkins.Layer> out = new ArrayList<>();
        boolean female = p.female() || !adultLook;
        boolean albino = p.body().equals("albino");
        // black pigment: black, brown on gold bodies, pale on albinos
        int dark = albino ? 0xC8A8A0 : p.body().equals("gold") ? 0x4A3624 : 0x181614;
        int darkA = albino ? 0x90 : 0xFF;
        int tail = tail(p.tailColor());
        if (albino && p.tailColor().equals("black")) tail = 0xC8B0A8;
        int fins = albino ? 0xEEE0D8 : p.body().equals("dark") ? 0xA8A49C : 0xD8D2C0;
        out.add(new BettaSkins.Layer(mask("body"), argb(0xFF, body(p.body(), female))));
        out.add(new BettaSkins.Layer(mask("fins"), argb(0xFF, fins)));
        float endler = p.endler();
        if (female) {
            out.add(new BettaSkins.Layer(mask("tail_color"), argb(p.tailColor().equals("clear") ? 0x40 : 0x78, tail)));
            out.add(new BettaSkins.Layer(mask("belly"), argb(0xD0, 0xEDEBE0)));
            if (p.x().equals("tuxedo")) out.add(new BettaSkins.Layer(mask("tuxedo"), argb(albino ? 0x50 : 0x90, dark)));
            if (gravid) out.add(new BettaSkins.Layer(mask("gravid"), argb(albino ? 0x80 : 0xFF, albino ? 0xB89890 : 0x5A5040)));
        } else {
            // endlers: a clear tail with coloured edges; hybrids in between
            int tailA = p.tailColor().equals("clear") ? 0x80 : Math.round(0xFF - 0x9F * Math.min(1F, endler * 1.3F));
            out.add(new BettaSkins.Layer(mask("tail_color"), argb(tailA, tail)));
            if (endler >= 0.25F) out.add(new BettaSkins.Layer(mask("tail_edge"), argb(Math.round(0xFF * Math.min(1F, endler * 1.5F)), tail)));
            out.add(new BettaSkins.Layer(mask("belly"), argb(p.body().equals("dark") ? 0x70 : 0xC0, 0xEDEBE0)));
            // fancy guppy males: the tail colour runs into the rear of the body (the peduncle)
            if (!p.tailColor().equals("clear") && endler < 0.5F) out.add(new BettaSkins.Layer(mask("red_tail"), argb(0x78, tail)));
            patterns(out, p.x(), p, dark, darkA, tail);
            if (!p.y().equals(p.x())) patterns(out, p.y(), p, dark, darkA, tail);
            switch (p.tailPattern()) {
                case "spots" -> out.add(new BettaSkins.Layer(mask("tail_spots"), argb(darkA, dark)));
                case "grass" -> out.add(new BettaSkins.Layer(mask("grass"), argb(darkA, p.tailColor().equals("blue") ? 0x141830 : dark)));
                case "mosaic" -> out.add(new BettaSkins.Layer(mask("mosaic"), argb(darkA, mix(dark, tail, 0.25F))));
                case "leopard" -> out.add(new BettaSkins.Layer(mask("leopard"), argb(darkA, dark)));
                default -> {}
            }
        }
        out.add(new BettaSkins.Layer(mask("eyes"), argb(0xFF, albino ? 0xC81818 : 0x141414)));
        out.add(new BettaSkins.Layer(mask("glint"), argb(0xFF, albino ? 0xFFB0B0 : 0xE0E0E0)));
        return out.toArray(BettaSkins.Layer[]::new);
    }

    private static void patterns(List<BettaSkins.Layer> out, String pat, LbPhenotype p, int dark, int darkA, int tail) {
        switch (pat) {
            case "wild" -> {
                out.add(new BettaSkins.Layer(mask("spots_orange"), argb(0xFF, p.body().equals("blue") ? 0x6A7AB0 : 0xF07A28)));
                out.add(new BettaSkins.Layer(mask("spots_black"), argb(darkA, dark)));
            }
            case "cobra" -> {
                out.add(new BettaSkins.Layer(mask("cobra_base"), argb(0xE0, mix(tail, 0xF0E8C0, 0.15F))));
                out.add(new BettaSkins.Layer(mask("cobra"), argb(darkA, dark)));
            }
            case "tuxedo" -> out.add(new BettaSkins.Layer(mask("tuxedo"), argb(darkA, dark)));
            case "moscow" -> out.add(new BettaSkins.Layer(mask("moscow"), argb(0xFF, mix(tail, 0x0A0A14, p.tailColor().equals("black") ? 0.2F : 0.45F))));
            case "japan_blue" -> out.add(new BettaSkins.Layer(mask("japan_blue"), argb(0xFF, 0x50B0FF)));
            case "red_head" -> out.add(new BettaSkins.Layer(mask("red_head"), argb(0xFF, 0xE0281C)));
            case "red_tail" -> out.add(new BettaSkins.Layer(mask("red_tail"), argb(0xFF, 0xE0281C)));
            case "peacock" -> {
                out.add(new BettaSkins.Layer(mask("peacock_orange"), argb(0xFF, 0xF06A1A)));
                out.add(new BettaSkins.Layer(mask("peacock_green"), argb(0xFF, 0x3AE070)));
                out.add(new BettaSkins.Layer(mask("peacock_spot"), argb(darkA, dark)));
            }
            case "tiger" -> {
                out.add(new BettaSkins.Layer(mask("peacock_orange"), argb(0xFF, 0xF06A1A)));
                out.add(new BettaSkins.Layer(mask("tiger"), argb(darkA, dark)));
            }
            case "black_bar" -> {
                out.add(new BettaSkins.Layer(mask("peacock_green"), argb(0xFF, 0x3AE070)));
                out.add(new BettaSkins.Layer(mask("black_bar"), argb(darkA, dark)));
            }
            default -> {}
        }
    }

    /** Tail scale of a female or a young fish (the plane of the same shape, smaller). */
    public static float tailScale(LbPhenotype p, boolean adultLook) {
        if (p.female() || !adultLook) return 0.55F;
        return 1F;
    }
}
