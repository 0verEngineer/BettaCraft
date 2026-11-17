package org.overengineer.bettacraft.genetics.livebearer;

import org.overengineer.bettacraft.util.NbtCompat;
import java.util.Arrays;
import java.util.random.RandomGenerator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * M21.6: the genome of a guppy or an endler (and their hybrids). Modelled on real guppy genetics:
 * <ul>
 *     <li>an allele pair per autosomal {@link LbLocus locus} (tail shape, tail colour, tail pattern and the body colour
 *     mutations), one from each parent;</li>
 *     <li>the sex chromosomes: a female has two X ({@link #xa}, {@link #xb}), a male one X ({@link #xa}) and the Y
 *     ({@link #y}). A daughter gets one of her mother's X and her father's X, a son one of his mother's X and his
 *     father's Y: the Y patterns go only from father to son, the X patterns zig-zag (mother to son, father to daughter).
 *     The slot a fish does not use is kept (a male's {@code xb} copies his X, a female's {@code y} is "none"), so a
 *     genome made without knowing the sex (fish bags, the trader) works for both;</li>
 *     <li>the endler share of the ancestry (0 guppy, 1 endler, the mean of the parents: hybrids are in between).</li>
 * </ul>
 * NBT: {@code {tail:"delta/round", tail_color:"red/red", ..., x:"tuxedo/none", y:"red_head", endler:0.0f}}. Unknown
 * alleles are kept and count as the most recessive. Pure logic, unit-tested without a Minecraft bootstrap.
 */
public final class LivebearerGenome {
    private static final int N = LbLocus.values().length;
    /** Chance per allele and birth to mutate into a random allele of the pool. */
    public static final double MUTATION = 0.004;

    private final String[] a, b;
    public final String xa, xb, y;
    public final float endler;

    private LivebearerGenome(String[] a, String[] b, String xa, String xb, String y, float endler) {
        this.a = a;
        this.b = b;
        this.xa = xa;
        this.xb = xb;
        this.y = y;
        this.endler = Math.max(0F, Math.min(1F, endler));
    }

    public String a(LbLocus l) {
        return a[l.ordinal()];
    }

    public String b(LbLocus l) {
        return b[l.ordinal()];
    }

    public String expressed(LbLocus l) {
        return l.express(a(l), b(l));
    }

    /** The allele that does not show (the same one when homozygous). */
    public String hidden(LbLocus l) {
        String e = expressed(l);
        if (l == LbLocus.TAIL && e.equals("double_sword") && !a(l).equals(b(l))) return a(l).equals("double_sword") ? b(l) : a(l);
        return e.equals(a(l)) ? b(l) : a(l);
    }

    public boolean homozygous(LbLocus l) {
        return a(l).equals(b(l));
    }

    // ------------------------------------------------------------------ building

    public static final class Builder {
        private final String[] a = new String[N], b = new String[N];
        private String xa = "none", xb = "none", y = "none";
        private float endler;

        private Builder(boolean endlerBackground) {
            for (LbLocus l : LbLocus.values()) a[l.ordinal()] = b[l.ordinal()] = l.wild();
            if (endlerBackground) {
                a[LbLocus.TAIL_COLOR.ordinal()] = b[LbLocus.TAIL_COLOR.ordinal()] = "orange";
                y = "peacock";
                endler = 1F;
            } else {
                y = "wild";
            }
        }

        public Builder pair(LbLocus l, String x, String z) {
            a[l.ordinal()] = x;
            b[l.ordinal()] = z;
            return this;
        }

        public Builder pure(LbLocus l, String v) {
            return pair(l, v, v);
        }

        public Builder x(String p, String q) {
            xa = p;
            xb = q;
            return this;
        }

        public Builder x(String p) {
            return x(p, p);
        }

        public Builder y(String p) {
            y = p;
            return this;
        }

        public Builder endler(float e) {
            endler = e;
            return this;
        }

        public LivebearerGenome build() {
            return new LivebearerGenome(a.clone(), b.clone(), xa, xb, y, endler);
        }
    }

    /** Wild-type guppy or endler, homozygous (the builder's start). */
    public static Builder builder(boolean endler) {
        return new Builder(endler);
    }

    public Builder toBuilder() {
        Builder bd = new Builder(false);
        System.arraycopy(a, 0, bd.a, 0, N);
        System.arraycopy(b, 0, bd.b, 0, N);
        bd.xa = xa;
        bd.xb = xb;
        bd.y = y;
        bd.endler = endler;
        return bd;
    }

    // ------------------------------------------------------------------ expression

    /** The visible fish (sex decides which sex chromosomes count). */
    public LbPhenotype express(boolean female) {
        String body;
        boolean albino = expressed(LbLocus.ALBINO).equals("albino"), blond = expressed(LbLocus.BLOND).equals("blond");
        boolean blue = expressed(LbLocus.BLUE).equals("blue"), gold = expressed(LbLocus.GOLD).equals("gold");
        boolean dark = expressed(LbLocus.DARK).equals("dark");
        if (albino) body = "albino";
        else if (blond && blue) body = "white";
        else if (dark) body = "dark";
        else if (gold) body = "gold";
        else if (blond) body = "blond";
        else if (blue) body = "blue";
        else body = "wild";
        String x = female ? (LbLocus.xRank(xa) <= LbLocus.xRank(xb) ? xa : xb) : xa;
        return new LbPhenotype(expressed(LbLocus.TAIL), expressed(LbLocus.TAIL_COLOR), expressed(LbLocus.TAIL_PATTERN), body, x,
                female ? "none" : y, female, endler);
    }

    // ------------------------------------------------------------------ inheritance

    /** A young of this mother and father with the given sex (the sex has to be known for the X and Y). */
    public static LivebearerGenome cross(LivebearerGenome mother, LivebearerGenome father, boolean female, RandomGenerator r, double mutation) {
        String[] a = new String[N], b = new String[N];
        for (LbLocus l : LbLocus.values()) {
            int i = l.ordinal();
            a[i] = mutate(l, r.nextBoolean() ? mother.a[i] : mother.b[i], mother.endler, r, mutation);
            b[i] = mutate(l, r.nextBoolean() ? father.a[i] : father.b[i], father.endler, r, mutation);
        }
        String fromMother = r.nextBoolean() ? mother.xa : mother.xb;
        // the father's X is his only one; a son gets his Y
        String xa, xb, y;
        if (female) {
            xa = fromMother;
            xb = father.xa;
            y = "none";
        } else {
            xa = fromMother;
            xb = fromMother;
            y = father.y;
        }
        float e = Math.round((mother.endler + father.endler) * 0.5F * 64F) / 64F;
        return new LivebearerGenome(a, b, xa, xb, y, e);
    }

    private static String mutate(LbLocus l, String allele, float endler, RandomGenerator r, double chance) {
        if (chance <= 0 || r.nextDouble() >= chance) return allele;
        return LivebearerPools.pick(l, endler >= 0.5F, r, true);
    }

    // ------------------------------------------------------------------ random fish

    /** A wild fish of the net or a natural spawn: the wild type, now and then carrying a mutation hidden. */
    public static LivebearerGenome wild(boolean endler, RandomGenerator r) {
        return LivebearerPools.wild(endler, r);
    }

    /** The genome as a fish of this sex keeps it (a female has no Y). */
    public LivebearerGenome forSex(boolean female) {
        if (female) return y.equals("none") ? this : new LivebearerGenome(a, b, xa, xb, "none", endler);
        return this;
    }

    // ------------------------------------------------------------------ NBT

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        for (LbLocus l : LbLocus.values()) t.putString(l.id(), a(l) + "/" + b(l));
        t.putString("x", xa + "/" + xb);
        t.putString("y", y);
        t.putFloat("endler", endler);
        return t;
    }

    /** Loads a genome; missing loci get the wild type of the background ({@code endlerDefault} for a missing share). */
    public static LivebearerGenome load(CompoundTag t, boolean endlerDefault) {
        Builder bd = builder(endlerDefault);
        for (LbLocus l : LbLocus.values()) {
            if (!NbtCompat.has(t, l.id(), Tag.TAG_STRING)) continue;
            String[] p = split(t.getStringOr(l.id(), ""), l.wild());
            bd.pair(l, p[0], p[1]);
        }
        if (NbtCompat.has(t, "x", Tag.TAG_STRING)) {
            String[] p = split(t.getStringOr("x", ""), "none");
            bd.x(p[0], p[1]);
        }
        if (NbtCompat.has(t, "y", Tag.TAG_STRING)) bd.y(t.getStringOr("y", "").isEmpty() ? "none" : t.getStringOr("y", ""));
        if (NbtCompat.has(t, "endler", NbtCompat.ANY_NUMERIC)) {
            float e = t.getFloatOr("endler", 0f);
            bd.endler(Float.isFinite(e) ? e : endlerDefault ? 1F : 0F);
        }
        return bd.build();
    }

    private static String[] split(String s, String def) {
        int i = s.indexOf('/');
        String p = i < 0 ? s : s.substring(0, i), q = i < 0 ? s : s.substring(i + 1);
        if (p.isEmpty()) p = def;
        if (q.isEmpty()) q = p;
        return new String[] {p, q};
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LivebearerGenome g && Arrays.equals(a, g.a) && Arrays.equals(b, g.b) && xa.equals(g.xa) && xb.equals(g.xb)
                && y.equals(g.y) && Float.compare(endler, g.endler) == 0;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(a) * 31 + Arrays.hashCode(b) * 17 + (xa + xb + y).hashCode() + Float.hashCode(endler);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Lb[");
        for (LbLocus l : LbLocus.values()) sb.append(l.id()).append('=').append(a(l)).append('/').append(b(l)).append(' ');
        return sb.append("x=").append(xa).append('/').append(xb).append(" y=").append(y).append(" endler=").append(endler).append(']').toString();
    }
}
