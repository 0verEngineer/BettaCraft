package org.overengineer.bettacraft.client.interior;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.joml.Vector3f;
import org.overengineer.bettacraft.plants.PlantPart;
import org.overengineer.bettacraft.plants.PlantSpecies;

/**
 * M56: the meshes of the terrarium plants ({@link PlantSpecies#land()}), built from their blocks like the aquarium plants
 * ({@link PlantMeshBuilder}):
 * <ul>
 *     <li>bromeliads: a cup of strap leaves, the red heart of the Neoregelia, the bract star of the Guzmania, the flat sword of
 *     the Vriesea; the air plant a ball of silvery spikes on its piece of wood;</li>
 *     <li>ferns, begonias, the succulent rosettes and the grasses as rosettes of their own leaves; Pothos, Fittonia, Peperomia as
 *     shoots with their leaves; papyrus and the umbrella sedge as culms with their umbrellas;</li>
 *     <li>the carnivorous plants: sundew pads with a flower stalk, the upright tubes of the Sarracenia, the Nepenthes with its
 *     pitchers hanging on tendrils, the hinged traps of the flytrap;</li>
 *     <li>the cacti: ribbed balls (golden barrel, the heads of the Mammillaria), the column with its arms, the pads of the prickly
 *     pear; their spines, crowns and flowers on top;</li>
 *     <li>the mosses and the lichen as cushions of sprigs, spikemoss and stonecrop as low sprigs.</li>
 * </ul>
 * They barely move (no current in the air of a terrarium).
 */
final class LandPlantMeshes {
    private LandPlantMeshes() {}

    static void build(PlantMeshBuilder b) {
        switch (b.sp) {
            case NEOREGELIA, GUZMANIA, VRIESEA -> bromeliad(b);
            case TILLANDSIA -> epiphyte(b, PlantTex.SPIKY, 0.3F, 0.5F, false);
            case MARCGRAVIA -> epiphyte(b, PlantTex.SHINGLE, 0.7F, 1.0F, false);
            case MINI_ORCHID -> epiphyte(b, PlantTex.OVATE, 0.9F, 1.4F, true);
            case FITTONIA -> b.stems(PlantTex.VEINED, 0.9F, 0.85F, 0.6F);
            case POTHOS -> b.stems(PlantTex.HEART, 1.3F, 1.15F, 0.7F);
            case PEPEROMIA -> b.stems(PlantTex.OVATE, 0.95F, 0.9F, 0.6F);
            case PAPYRUS -> b.stems(PlantTex.NEEDLE, 1.5F, 0.3F, 0.5F, 2);
            case UMBRELLA_SEDGE -> b.stems(PlantTex.NARROW, 2.0F, 0.55F, 0.4F);
            case BOSTON_FERN -> b.rosette(PlantTex.FERN, 0.12F, 0.9F, 1.5F, 0, 0);
            case BIRDS_NEST_FERN -> b.crypt(PlantTex.SWORD, 1.2F, 2.0F, 0);
            case BEGONIA -> b.rosette(PlantTex.VELVET, 0.45F, 1.6F, 2.4F, 0, 0);
            case CUSHION_MOSS -> b.moss();
            case REINDEER_LICHEN -> sprigs(b, PlantTex.LICHEN, 1.3F, false);
            case SELAGINELLA -> sprigs(b, PlantTex.FERN, 1.1F, true);
            case SEDUM -> sprigs(b, PlantTex.SUCCULENT, 0.8F, true);
            case SUNDEW -> flat(b, PlantTex.SUNDEW, 0.55F, 0.75F, 0.55F);
            case BUTTERWORT -> flat(b, PlantTex.OVATE, 0.8F, 1.2F, 0F);
            case VENUS_FLYTRAP -> flytrap(b);
            case SARRACENIA -> pitchers(b);
            case NEPENTHES -> nepenthes(b);
            case SWAMP_SEDGE, BLUE_GRAMA -> b.crypt(PlantTex.NARROW, 0.25F, 0.4F, 0);
            case SOFT_RUSH, NEEDLEGRASS -> b.crypt(PlantTex.NEEDLE, 0.22F, 0.32F, 0);
            case ECHEVERIA -> b.crypt(PlantTex.SUCCULENT, 0.8F, 1.2F, 0);
            case HAWORTHIA -> b.crypt(PlantTex.ZEBRA, 0.45F, 0.7F, 0);
            case ALOE, AGAVE -> b.crypt(PlantTex.SPIKY, 0.7F, 1.1F, 0);
            case LITHOPS -> lithops(b);
            case BARREL_CACTUS, MAMMILLARIA -> balls(b);
            case COLUMN_CACTUS -> column(b);
            case PRICKLY_PEAR -> opuntia(b);
            default -> b.rosette(PlantTex.SWORD, 0.2F, 0.8F, 1.4F, 0, 0);
        }
        // the air of a terrarium hardly moves them
        b.m.swayPerBlock = Math.min(b.m.swayPerBlock, 0.004);
        b.m.swayMax = Math.min(b.m.swayMax, 0.05);
    }

    // ------------------------------------------------------------------ helpers

    static int x(long k) {
        return PlantSkeleton.x(k);
    }

    static int y(long k) {
        return PlantSkeleton.y(k);
    }

    static int z(long k) {
        return PlantSkeleton.z(k);
    }

    /** A little flower (or spine tuft) of crossed petals in the colour of the tops. */
    static void flower(PlantMeshBuilder b, int x, int y, int z, float size) {
        b.use(x, y, z, PlantPart.TOP);
        b.cross(PlantTex.FLOWER, new Vector3f(x + 0.5F, y + 0.4F, z + 0.5F), size, PlantMeshBuilder.hash(x, y, z, 5) * 6.28F, true);
    }

    /** M56: the stalk of a flower or a tendril (ribbon blocks) and the tops at it: the flowers or the pitchers, not leaves. */
    static LongOpenHashSet stalkBlocks(Long2ObjectOpenHashMap<PlantPart> parts) {
        LongOpenHashSet out = new LongOpenHashSet();
        for (long k : parts.keySet()) if (parts.get(k) == PlantPart.RIBBON) out.add(k);
        LongOpenHashSet tops = new LongOpenHashSet();
        for (long k : parts.keySet()) {
            if (parts.get(k) != PlantPart.TOP) continue;
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) if (out.contains(PlantSkeleton.key(x(k) + dx, y(k) + dy, z(k) + dz))) tops.add(k);
        }
        out.addAll(tops);
        return out;
    }

    /** A thin stalk through the given blocks (lowest first) from the crown, a flower on its last block. */
    static void stalk(PlantMeshBuilder b, List<long[]> pts, float flower) {
        if (pts.isEmpty()) return;
        List<Vector3f> line = new ArrayList<>();
        line.add(new Vector3f(0.5F, 0.3F, 0.5F));
        for (long[] p : pts) line.add(new Vector3f(p[0] + 0.5F, p[1] + 0.5F, p[2] + 0.5F));
        long[] last = pts.getLast();
        b.use((int) last[0], (int) last[1], (int) last[2], PlantPart.STEM);
        b.tubes(PlantTex.STEM, PlantMeshBuilder.smooth(line, 2), 0.05F * b.style.stem(), 0.035F * b.style.stem(), 4);
        if (flower > 0) flower(b, (int) last[0], (int) last[1], (int) last[2], flower);
    }

    // ------------------------------------------------------------------ bromeliads, epiphytes

    static void bromeliad(PlantMeshBuilder b) {
        Long2ObjectOpenHashMap<PlantPart> parts = b.parts();
        long root = PlantSkeleton.key(0, 0, 0);
        boolean spike = b.sp != PlantSpecies.NEOREGELIA;
        LongOpenHashSet skip = new LongOpenHashSet();
        int spikeTop = 0;
        if (spike) {
            for (long k : parts.keySet()) {
                boolean column = x(k) == 0 && z(k) == 0 && y(k) >= 1;
                boolean bract = b.sp == PlantSpecies.GUZMANIA && parts.get(k) == PlantPart.TOP && Math.abs(x(k)) + Math.abs(z(k)) == 1 && y(k) >= 2;
                if (column || bract) skip.add(k);
                if (column) spikeTop = Math.max(spikeTop, y(k));
            }
        }
        PlantSkeleton.Tree tree = PlantSkeleton.tree(parts, k -> k == root, k -> !skip.contains(k));
        b.use(0, 0, 0, PlantPart.STEM);
        b.tube(PlantTex.STEM, new Vector3f(0.5F, 0F, 0.5F), new Vector3f(0.5F, 0.45F, 0.5F), 0.36F, 0.3F, 6);
        b.leaves(parts, tree, skip, b.sp == PlantSpecies.VRIESEA ? PlantTex.BANDED : PlantTex.STRAP, 0F, 0.9F, 1.5F, 0.06F,
                new Vector3f(0.5F, 0.3F, 0.5F));
        if (spikeTop <= 0) return;
        b.use(0, spikeTop, 0, PlantPart.TOP);
        Vector3f base = new Vector3f(0.5F, 0.6F, 0.5F);
        if (b.sp == PlantSpecies.VRIESEA) {
            // the flat red sword of the inflorescence
            b.leaf(PlantTex.STRAP, base, new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), spikeTop + 0.4F, 0.9F, 0F);
            b.leaf(PlantTex.STRAP, base, new Vector3f(0, 1, 0), new Vector3f(0, 0, 1), spikeTop + 0.2F, 0.3F, 0F);
        } else {
            // the Guzmania's star of coloured bracts in two tiers on a short stalk
            b.tube(PlantTex.STEM, base, new Vector3f(0.5F, spikeTop - 0.3F, 0.5F), 0.12F, 0.1F, 4);
            for (int tier = 0; tier < 2; tier++) {
                float y = spikeTop - 1.2F + tier * 0.8F;
                for (int i = 0; i < 7; i++) {
                    double a = i * Math.PI * 2 / 7 + tier * 0.45;
                    Vector3f d = new Vector3f((float) Math.cos(a), tier == 0 ? 0.55F : 1.1F, (float) Math.sin(a));
                    b.leaf(PlantTex.STRAP, new Vector3f(0.5F, y, 0.5F), d, new Vector3f((float) -Math.sin(a), 0, (float) Math.cos(a)),
                            (tier == 0 ? 1.7F : 1.2F) * b.style.leafLong(), 0.55F * b.style.leafWide(), 0.08F);
                }
            }
        }
    }

    /** The rhizome on its wood or stone, the leaves from it; the orchid's flower spike drawn on its own. */
    static void epiphyte(PlantMeshBuilder b, PlantTex tex, float minW, float maxW, boolean spike) {
        b.rhizome(0.12F);
        Long2ObjectOpenHashMap<PlantPart> parts = b.parts();
        LongOpenHashSet skip = spike ? stalkBlocks(parts) : new LongOpenHashSet();
        List<long[]> stalk = new ArrayList<>();
        for (long k : skip) if (parts.get(k) == PlantPart.RIBBON) stalk.add(new long[]{x(k), y(k), z(k)});
        PlantSkeleton.Tree tree = PlantSkeleton.tree(parts, k -> {
            PlantPart p = parts.get(k);
            return p == PlantPart.ROOT || p == PlantPart.RHIZOME;
        }, k -> !skip.contains(k));
        b.leaves(parts, tree, skip, tex, 0.05F, minW, maxW, 0.05F, null);
        if (spike && !stalk.isEmpty()) {
            stalk.sort((p, q) -> Long.compare(p[1], q[1]));
            stalk(b, stalk, 0F);
            for (long k : skip) if (parts.get(k) == PlantPart.TOP) flower(b, x(k), y(k), z(k), 0.9F * b.style.leafWide());
        }
    }

    // ------------------------------------------------------------------ low rosettes and carnivorous plants

    /** A flat rosette on the ground (sundew, butterwort) with leaves on petioles ({@code stalk}), a flower stalk above it. */
    static void flat(PlantMeshBuilder b, PlantTex tex, float minW, float maxW, float stalkShare) {
        Long2ObjectOpenHashMap<PlantPart> parts = b.parts();
        long root = PlantSkeleton.key(0, 0, 0);
        LongOpenHashSet skip = stalkBlocks(parts);
        List<long[]> stalk = new ArrayList<>();
        long top = Long.MIN_VALUE;
        for (long k : skip) {
            if (parts.get(k) == PlantPart.RIBBON) stalk.add(new long[]{x(k), y(k), z(k)});
            else if (top == Long.MIN_VALUE || y(k) > y(top)) top = k;
        }
        PlantSkeleton.Tree tree = PlantSkeleton.tree(parts, k -> k == root, k -> !skip.contains(k));
        b.use(0, 0, 0, PlantPart.STEM);
        b.tube(PlantTex.STEM, new Vector3f(0.5F, 0F, 0.5F), new Vector3f(0.5F, 0.15F, 0.5F), 0.2F, 0.16F, 6);
        b.leaves(parts, tree, skip, tex, stalkShare, minW, maxW, 0.035F, new Vector3f(0.5F, 0.08F, 0.5F));
        if (top != Long.MIN_VALUE) stalk.add(new long[]{x(top), y(top), z(top)});
        stalk.sort((p, q) -> Long.compare(p[1], q[1]));
        stalk(b, stalk, top != Long.MIN_VALUE ? 0.8F * b.style.leafWide() : 0F);
    }

    /** The Venus flytrap: short winged petioles, at the end of each the hinged trap, open, red inside. */
    static void flytrap(PlantMeshBuilder b) {
        Long2ObjectOpenHashMap<PlantPart> parts = b.parts();
        long root = PlantSkeleton.key(0, 0, 0);
        PlantSkeleton.Tree tree = PlantSkeleton.tree(parts, k -> k == root, k -> true);
        b.use(0, 0, 0, PlantPart.STEM);
        b.tube(PlantTex.STEM, new Vector3f(0.5F, 0F, 0.5F), new Vector3f(0.5F, 0.12F, 0.5F), 0.16F, 0.12F, 6);
        for (long tip : PlantSkeleton.leafTips(tree, 1, 1.2)) {
            LongArrayList path = tree.path(tip);
            Vector3f end = PlantMeshBuilder.c(tip);
            Vector3f d = new Vector3f(end).sub(0.5F, 0.1F, 0.5F);
            if (d.lengthSquared() < 1e-4F) d.set(1, 0.3F, 0);
            float len = d.length();
            d.normalize();
            Vector3f across = new Vector3f(-d.z, 0, d.x);
            if (across.lengthSquared() < 1e-4F) across.set(1, 0, 0);
            across.normalize();
            // the winged petiole (green)
            b.use(path.size() > 1 ? path.getLong(1) : tip, PlantPart.LEAF);
            Vector3f start = new Vector3f(0.5F, 0.1F, 0.5F);
            b.leaf(PlantTex.SWORD, start, d, across, len * 0.7F * b.style.leafLong(), 0.45F * b.style.leafWide(), 0.05F);
            // the trap: two lobes hinged on the midrib, opened in a V
            b.use(tip, PlantPart.TOP);
            Vector3f hinge = new Vector3f(start).add(new Vector3f(d).mul(len * 0.7F * b.style.leafLong()));
            float lobe = 0.75F * b.style.leafWide(), trap = 0.8F * b.style.leafLong();
            for (int side = -1; side <= 1; side += 2) {
                Vector3f a = new Vector3f(across).mul(side).add(0, 0.9F, 0).normalize();
                Vector3f base = new Vector3f(hinge).add(new Vector3f(a).mul(lobe / 2));
                b.leaf(PlantTex.TRAP, base, d, a, trap, lobe, 0.12F);
            }
        }
    }

    /** The trumpet pitchers of the Sarracenia: upright tubes widening to the top, the hood bent over the mouth. */
    static void pitchers(PlantMeshBuilder b) {
        Long2ObjectOpenHashMap<PlantPart> parts = b.parts();
        long root = PlantSkeleton.key(0, 0, 0);
        b.use(0, 0, 0, PlantPart.STEM);
        b.tube(PlantTex.STEM, new Vector3f(0.5F, 0F, 0.5F), new Vector3f(0.5F, 0.2F, 0.5F), 0.2F, 0.16F, 6);
        PlantSkeleton.Tree tree = PlantSkeleton.tree(parts, k -> k == root, k -> true);
        for (long tip : PlantSkeleton.leafTips(tree, 2, 1.2)) {
            LongArrayList path = tree.path(tip);
            List<Vector3f> pts = new ArrayList<>();
            pts.add(new Vector3f(0.5F, 0.05F, 0.5F));
            for (int j = 1; j < path.size(); j++) pts.add(PlantMeshBuilder.c(path.getLong(j)));
            pts = PlantMeshBuilder.smooth(pts, 2);
            int cut = Math.max(1, pts.size() * 2 / 3);
            b.use(path.size() > 1 ? path.getLong(1) : tip, PlantPart.LEAF);
            b.tubes(PlantTex.PITCHER, pts.subList(0, cut + 1), 0.07F, 0.22F, 6);
            b.use(tip, PlantPart.TOP);
            b.tubes(PlantTex.PITCHER, pts.subList(cut, pts.size()), 0.22F, 0.32F, 6);
            // the hood over the mouth
            Vector3f top = pts.getLast();
            Vector3f out = new Vector3f(top.x - 0.5F, 0, top.z - 0.5F);
            if (out.lengthSquared() < 1e-4F) out.set(1, 0, 0);
            out.normalize().negate();
            b.leaf(PlantTex.ROUND, new Vector3f(top).add(new Vector3f(out).mul(-0.2F)), new Vector3f(out).add(0, 1.4F, 0), new Vector3f(-out.z, 0, out.x),
                    0.7F, 0.65F, 0.25F);
        }
    }

    /** The Nepenthes: a rosette of leaves, from it tendrils to the pitchers that hang beside it. */
    static void nepenthes(PlantMeshBuilder b) {
        Long2ObjectOpenHashMap<PlantPart> parts = b.parts();
        long root = PlantSkeleton.key(0, 0, 0);
        LongOpenHashSet skip = new LongOpenHashSet();
        List<long[]> hangers = new ArrayList<>(); // x, y of the tendril end, z, lowest y of the pitcher
        for (long k : parts.keySet()) if (parts.get(k) == PlantPart.RIBBON) skip.add(k); // the tendrils
        for (long k : parts.keySet()) {
            if (parts.get(k) != PlantPart.RIBBON) continue;
            long below = PlantSkeleton.key(x(k), y(k) - 1, z(k));
            if (parts.get(below) != PlantPart.TOP) continue;
            int low = y(k) - 1;
            while (parts.get(PlantSkeleton.key(x(k), low - 1, z(k))) == PlantPart.TOP) low--;
            for (int y = low; y <= y(k); y++) skip.add(PlantSkeleton.key(x(k), y, z(k)));
            hangers.add(new long[]{x(k), y(k), z(k), low});
        }
        PlantSkeleton.Tree tree = PlantSkeleton.tree(parts, k -> k == root, k -> !skip.contains(k));
        b.use(0, 0, 0, PlantPart.STEM);
        b.tube(PlantTex.STEM, new Vector3f(0.5F, 0F, 0.5F), new Vector3f(0.5F, 0.5F, 0.5F), 0.22F, 0.16F, 6);
        b.leaves(parts, tree, skip, PlantTex.ELLIPTIC, 0.1F, 0.8F, 1.3F, 0.05F, new Vector3f(0.5F, 0.35F, 0.5F));
        for (long[] h : hangers) {
            int x = (int) h[0], y = (int) h[1], z = (int) h[2], low = (int) h[3];
            Vector3f end = new Vector3f(x + 0.5F, y + 0.6F, z + 0.5F);
            List<Vector3f> tendril = List.of(new Vector3f(0.5F, 0.45F, 0.5F), new Vector3f((x + 0.5F + 0.5F) / 2, y + 1.0F, (z + 0.5F + 0.5F) / 2), end);
            b.use(x, y, z, PlantPart.STEM);
            b.tubes(PlantTex.STEM, PlantMeshBuilder.smooth(new ArrayList<>(tendril), 2), 0.035F, 0.03F, 4);
            // the pitcher: a fat bottom, narrower towards the mouth, the lid over it
            b.use(x, low, z, PlantPart.TOP);
            Vector3f bottom = new Vector3f(x + 0.5F, low + 0.05F, z + 0.5F), mouth = new Vector3f(x + 0.5F, y + 0.3F, z + 0.5F);
            Vector3f belly = new Vector3f(bottom).lerp(mouth, 0.35F);
            b.tube(PlantTex.PITCHER, bottom, belly, 0.18F, 0.36F, 6);
            b.tube(PlantTex.PITCHER, belly, mouth, 0.36F, 0.26F, 6);
            b.leaf(PlantTex.ROUND, new Vector3f(mouth).add(0, 0.05F, 0), new Vector3f(0.3F, 1, 0), new Vector3f(0, 0, 1), 0.5F, 0.55F, 0.1F);
        }
    }

    /** Lithops: two fat half-domes split by a cleft, a flower out of the cleft when grown. */
    static void lithops(PlantMeshBuilder b) {
        int n = b.blocks.size();
        float r = Math.min(0.75F, 0.3F + 0.06F * n);
        int col = b.colorOf(0, 0, 0, PlantPart.LEAF);
        b.use(0, 0, 0, PlantPart.LEAF);
        for (int side = -1; side <= 1; side += 2) {
            Vector3f c = new Vector3f(0.5F + side * r * 0.55F, 0.05F, 0.5F);
            b.disc(PlantTex.SUCCULENT, c, new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), r * 0.55F, r * 0.7F, 0F, 8, col, col);
        }
        for (var e : b.blocks.long2ObjectEntrySet()) {
            if (e.getValue() != PlantPart.TOP) continue;
            long k = e.getLongKey();
            flower(b, BlockPos.getX(k), BlockPos.getY(k), BlockPos.getZ(k), 0.9F * b.style.leafWide());
        }
    }

    // ------------------------------------------------------------------ mosses and carpets

    /** Crossed sprigs on every block (lichen, spikemoss, stonecrop), flowers on the tops. */
    static void sprigs(PlantMeshBuilder b, PlantTex tex, float size, boolean flat) {
        for (var e : b.blocks.long2ObjectEntrySet()) {
            long k = e.getLongKey();
            int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
            if (e.getValue() == PlantPart.TOP && b.sp != PlantSpecies.SELAGINELLA) {
                flower(b, x, y, z, 0.6F * b.style.leafWide());
                continue;
            }
            b.use(x, y, z, e.getValue() == PlantPart.TOP ? PlantPart.TOP : e.getValue() == PlantPart.MOSS ? PlantPart.MOSS : PlantPart.LEAF);
            float yaw = PlantMeshBuilder.hash(x, y, z, 1) * 6.28F;
            Vector3f at = new Vector3f(x + 0.3F + PlantMeshBuilder.hash(x, y, z, 2) * 0.4F, y + 0.5F, z + 0.3F + PlantMeshBuilder.hash(x, y, z, 3) * 0.4F);
            b.cross(tex, at, (size + PlantMeshBuilder.hash(x, y, z, 4) * 0.4F) * b.style.leafWide(), yaw, flat);
        }
    }

    // ------------------------------------------------------------------ cacti

    /** The top block of every column of the plant (body and crown). */
    static Long2IntOpenHashMap tops(PlantMeshBuilder b) {
        Long2IntOpenHashMap top = new Long2IntOpenHashMap();
        for (long k : b.blocks.keySet()) {
            long c = PlantMeshBuilder.col(BlockPos.getX(k), BlockPos.getZ(k));
            int y = BlockPos.getY(k);
            if (!top.containsKey(c) || y > top.get(c)) top.put(c, y);
        }
        return top;
    }

    /**
     * Golden barrel and Mammillaria: a ribbed ball per head (the columns that stand higher than all their neighbours), sized
     * by the blocks around it; the crown (golden spines) or the ring of flowers on top.
     */
    static void balls(PlantMeshBuilder b) {
        Long2IntOpenHashMap top = new Long2IntOpenHashMap();
        for (var e : b.blocks.long2ObjectEntrySet()) {
            if (e.getValue() == PlantPart.TOP) continue;
            long k = e.getLongKey();
            long c = PlantMeshBuilder.col(BlockPos.getX(k), BlockPos.getZ(k));
            int y = BlockPos.getY(k);
            if (!top.containsKey(c) || y > top.get(c)) top.put(c, y);
        }
        if (top.isEmpty()) top.put(PlantMeshBuilder.col(0, 0), 0);
        // the heads: local maxima, at least two blocks apart
        List<long[]> heads = new ArrayList<>();
        LongArrayList cols = new LongArrayList(top.keySet());
        cols.sort((p, q) -> Integer.compare(top.get(q), top.get(p)));
        for (long c : cols) {
            int x = PlantMeshBuilder.colX(c), z = PlantMeshBuilder.colZ(c), h = top.get(c);
            boolean near = false;
            for (long[] hd : heads) if (Math.abs(hd[0] - x) <= 2 && Math.abs(hd[1] - z) <= 2) near = true;
            if (near) continue;
            boolean max = true;
            for (int dx = -1; dx <= 1 && max; dx++)
                for (int dz = -1; dz <= 1; dz++) if (top.getOrDefault(PlantMeshBuilder.col(x + dx, z + dz), -1) > h) max = false;
            if (max) heads.add(new long[]{x, z, h});
        }
        // the horizontal size of each head: the farthest column that is nearer to it than to any other head
        float[] rad = new float[heads.size()];
        for (long c : cols) {
            int x = PlantMeshBuilder.colX(c), z = PlantMeshBuilder.colZ(c);
            int best = 0;
            double bd = Double.MAX_VALUE;
            for (int i = 0; i < heads.size(); i++) {
                double d = Math.hypot(heads.get(i)[0] - x, heads.get(i)[1] - z);
                if (d < bd) {
                    bd = d;
                    best = i;
                }
            }
            rad[best] = (float) Math.max(rad[best], bd + 0.5);
        }
        for (int i = 0; i < heads.size(); i++) {
            long[] hd = heads.get(i);
            float ry = (hd[2] + 1) / 2F, rh = Math.max(0.5F, Math.min(rad[i], ry * 1.4F));
            ball(b, new Vector3f(hd[0] + 0.5F, ry, hd[1] + 0.5F), rh, ry, (int) hd[0], (int) hd[2], (int) hd[1]);
        }
        for (var e : b.blocks.long2ObjectEntrySet()) {
            if (e.getValue() != PlantPart.TOP) continue;
            long k = e.getLongKey();
            flower(b, BlockPos.getX(k), BlockPos.getY(k), BlockPos.getZ(k), (b.sp == PlantSpecies.MAMMILLARIA ? 0.8F : 1.1F) * b.style.leafWide());
        }
    }

    /** A ribbed ellipsoid of cactus body (darker underneath), {@code rh} wide, {@code ry} high, standing on the ground. */
    static void ball(PlantMeshBuilder b, Vector3f c, float rh, float ry, int lx, int ly, int lz) {
        int lat = 6, lon = 12;
        int color = b.colorOf(lx, 0, lz, PlantPart.LEAF), dark = PlantMeshBuilder.lerpColor(color, 0xFF000000, 0.2F);
        for (int i = 0; i < lat; i++) {
            double t0 = Math.PI * i / lat, t1 = Math.PI * (i + 1) / lat;
            for (int j = 0; j < lon; j++) {
                double p0 = Math.PI * 2 * j / lon, p1 = Math.PI * 2 * (j + 1) / lon;
                Vector3f na = sphere(t0, p0), nb = sphere(t0, p1), nc = sphere(t1, p1), nd = sphere(t1, p0);
                Vector3f pa = scaled(na, c, rh, ry), pb = scaled(nb, c, rh, ry), pc = scaled(nc, c, rh, ry), pd = scaled(nd, c, rh, ry);
                b.use(Math.round(pa.x - 0.5F), Math.max(0, Math.round(pa.y - 0.5F)), Math.round(pa.z - 0.5F), PlantPart.LEAF);
                int ca = PlantMeshBuilder.lerpColor(dark, color, (na.y + 1) / 2), cc = PlantMeshBuilder.lerpColor(dark, color, (nc.y + 1) / 2);
                // one rib per quad: the texture's rib pattern repeats around the ball
                b.quad4(PlantTex.CACTUS, pa, pb, pc, pd, 0, i / (float) lat, 1, i / (float) lat, 1, (i + 1) / (float) lat, 0, (i + 1) / (float) lat,
                        na, nb, nc, nd, ca, ca, cc, cc);
            }
        }
    }

    private static Vector3f scaled(Vector3f n, Vector3f c, float rh, float ry) {
        return new Vector3f(c.x + n.x * rh, Math.max(0, c.y + n.y * ry), c.z + n.z * rh);
    }

    private static Vector3f sphere(double theta, double phi) {
        return new Vector3f((float) (Math.sin(theta) * Math.cos(phi)), (float) Math.cos(theta), (float) (Math.sin(theta) * Math.sin(phi)));
    }

    /** The column cactus: a ribbed column with a rounded top, its arms bending out and up, the woolly white tips. */
    static void column(PlantMeshBuilder b) {
        int trunkTop = 0;
        boolean thick = b.blocks.containsKey(BlockPos.asLong(1, 1, 0)) || b.blocks.containsKey(BlockPos.asLong(-1, 1, 0));
        for (long k : b.blocks.keySet()) {
            int x = BlockPos.getX(k), z = BlockPos.getZ(k);
            if (Math.abs(x) <= 1 && Math.abs(z) <= 1 && Math.abs(x) + Math.abs(z) <= 1) trunkTop = Math.max(trunkTop, BlockPos.getY(k));
        }
        float r = thick ? 1.35F : 0.6F;
        b.use(0, Math.max(0, trunkTop / 2), 0, PlantPart.LEAF);
        Vector3f foot = new Vector3f(0.5F, 0F, 0.5F), head = new Vector3f(0.5F, trunkTop + 0.5F, 0.5F);
        segTube(b, PlantTex.CACTUS, foot, head, r, r * 0.95F, 10);
        cap(b, head, r * 0.95F, 0, trunkTop, 0);
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int reach = thick ? 2 : 1;
        for (int[] d : dirs) {
            int far = 0, y0 = Integer.MAX_VALUE, armTop = -1;
            for (long k : b.blocks.keySet()) {
                int x = BlockPos.getX(k), z = BlockPos.getZ(k), y = BlockPos.getY(k);
                int along = x * d[0] + z * d[1], side = Math.abs(x * d[1] + z * d[0]);
                if (along < reach || side > 0) continue;
                far = Math.max(far, along);
                y0 = Math.min(y0, y);
            }
            if (far == 0) continue;
            for (long k : b.blocks.keySet()) {
                int x = BlockPos.getX(k), z = BlockPos.getZ(k);
                if (x == d[0] * far && z == d[1] * far) armTop = Math.max(armTop, BlockPos.getY(k));
            }
            float ar = r * 0.6F;
            Vector3f from = new Vector3f(0.5F, y0 + 0.5F, 0.5F), elbow = new Vector3f(d[0] * far + 0.5F, y0 + 0.5F, d[1] * far + 0.5F);
            b.use(d[0] * far, y0, d[1] * far, PlantPart.LEAF);
            segTube(b, PlantTex.CACTUS, from, elbow, ar, ar, 8);
            if (armTop > y0) {
                Vector3f up = new Vector3f(elbow.x, armTop + 0.5F, elbow.z);
                segTube(b, PlantTex.CACTUS, new Vector3f(elbow).sub(0, ar * 0.9F, 0), up, ar, ar * 0.95F, 8);
                cap(b, up, ar * 0.95F, d[0] * far, armTop, d[1] * far);
            }
        }
    }

    /** A tube cut into pieces of about one block (long quads would leave gaps between the parts that join them). */
    static void segTube(PlantMeshBuilder b, PlantTex tex, Vector3f a, Vector3f c, float r0, float r1, int sides) {
        float len = a.distance(c);
        int n = Math.max(1, (int) Math.ceil(len));
        for (int i = 0; i < n; i++) {
            Vector3f p = new Vector3f(a).lerp(c, i / (float) n), q = new Vector3f(a).lerp(c, (i + 1) / (float) n);
            b.tube(tex, p, q, r0 + (r1 - r0) * i / n, r0 + (r1 - r0) * (i + 1) / n, sides);
        }
    }

    /** A rounded top with a tuft of spines (the tops' colour) on it. */
    static void cap(PlantMeshBuilder b, Vector3f at, float r, int x, int y, int z) {
        int col = b.colorOf(x, 0, z, PlantPart.LEAF);
        b.use(x, y, z, PlantPart.LEAF);
        b.disc(PlantTex.CACTUS, at, new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), r, r * 0.6F, 0F, 10, col, col);
        flower(b, x, y, z, Math.max(0.5F, r * 0.9F));
    }

    /** The prickly pear: every block of a pad a round flat disc in the pad's plane, so the pads come out as ovals; flowers on the rim. */
    static void opuntia(PlantMeshBuilder b) {
        for (var e : b.blocks.long2ObjectEntrySet()) {
            long k = e.getLongKey();
            int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
            if (e.getValue() == PlantPart.TOP) {
                flower(b, x, y, z, 0.9F * b.style.leafWide());
                continue;
            }
            int alongX = 0, alongZ = 0;
            for (int dy = -1; dy <= 1; dy++) {
                int w = dy == 0 ? 2 : 1; // the own layer counts most
                for (int s = -1; s <= 1; s += 2) {
                    if (b.blocks.containsKey(BlockPos.asLong(x + s, y + dy, z))) alongX += w;
                    if (b.blocks.containsKey(BlockPos.asLong(x, y + dy, z + s))) alongZ += w;
                }
            }
            boolean inX = alongX >= alongZ;
            Vector3f normal = inX ? new Vector3f(0, 0, 1) : new Vector3f(1, 0, 0);
            Vector3f ux = inX ? new Vector3f(1, 0, 0) : new Vector3f(0, 0, 1);
            int col = b.colorOf(x, y, z, PlantPart.LEAF);
            b.use(x, y, z, PlantPart.LEAF);
            b.disc(PlantTex.CACTUS_PAD, PlantMeshBuilder.c(x, y, z), normal, ux, 1.0F, 0.08F, 0F, 6, col, col);
        }
    }
}
