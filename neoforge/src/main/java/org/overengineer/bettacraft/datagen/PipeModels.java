package org.overengineer.bettacraft.datagen;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import org.overengineer.bettacraft.plumbing.PipeBlock;

/**
 * M65: the models of the water pipes (glass, stainless steel) and the filter pipes: 4 px thick ({@link #A} to {@link #B}),
 * every face textured with the texture's columns across the pipe and its rows along it. A pipe texture ({@code #t}) is shaded
 * by column only (lighter in the middle, darker along the edges, a coloured ring in the first and last row for the filter
 * lines); its transposed copy ({@code #r}, {@code <texture>_r}) is used on the faces whose texture rows run across the pipe
 * (the face's U axis along the pipe). The UVs are the default ones from the position, so the rings sit at the block borders
 * and the stripes run on without a seam from block to block.
 * <p>
 * The block state is a multipart without rotations: one arm per joined side (its four long faces; the end face only for
 * opaque pipes, against the block it joins) and the six faces of the core, each only when its side is not joined (no face
 * inside the pipe, which showed through glass) and with its stripes along the pipe: along the face's V axis when the pipe
 * goes on along it or along neither of the face's axes, else along its U axis.
 */
final class PipeModels {
    static final float A = 6, B = 10;

    private PipeModels() {}

    /** The axis that the face's U (texture columns) runs along with the default UVs. */
    static Direction.Axis uAxis(Direction f) {
        return f.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }

    /** The axis that the face's V (texture rows) runs along with the default UVs. */
    static Direction.Axis vAxis(Direction f) {
        return f.getAxis() == Direction.Axis.Y ? Direction.Axis.Z : Direction.Axis.Y;
    }

    /** The texture of face {@code f} of a piece running along {@code along}: stripes along V ({@code #t}) or along U ({@code #r}). */
    static String tex(Direction f, Direction.Axis along) {
        return vAxis(f) == along ? "#t" : "#r";
    }

    /**
     * The default UVs of face {@code f} of the box (vanilla's from the position), with the coordinate across the pipe set to
     * {@code across} (the shaded columns of the pipe texture) for pieces that are not at the middle of the block (the arms of a
     * pipe bundle).
     */
    static float[] uvs(Direction f, float[] from, float[] to, Direction.Axis along, float[] across) {
        float x1 = from[0], y1 = from[1], z1 = from[2], x2 = to[0], y2 = to[1], z2 = to[2];
        float[] uv = switch (f) {
            case DOWN -> new float[]{x1, 16 - z2, x2, 16 - z1};
            case UP -> new float[]{x1, z1, x2, z2};
            case NORTH -> new float[]{16 - x2, 16 - y2, 16 - x1, 16 - y1};
            case SOUTH -> new float[]{x1, 16 - y2, x2, 16 - y1};
            case WEST -> new float[]{z1, 16 - y2, z2, 16 - y1};
            case EAST -> new float[]{16 - z2, 16 - y2, 16 - z1, 16 - y1};
        };
        if (across != null) {
            if (vAxis(f) == along) {
                uv[0] = across[0];
                uv[2] = across[1];
            } else {
                uv[1] = across[0];
                uv[3] = across[1];
            }
        }
        return uv;
    }

    /** The box of the arm towards {@code d} ({@link #A}..{@link #B} across, from the core to the block face). */
    static float[][] armBox(Direction d) {
        float[] from = {A, A, A}, to = {B, B, B};
        int i = d.getAxis().ordinal();
        if (d.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            from[i] = B;
            to[i] = 16;
        } else {
            from[i] = 0;
            to[i] = A;
        }
        return new float[][]{from, to};
    }

    /**
     * A piece along {@code along} (box {@code from}..{@code to}) into the model: the long faces with the stripes along it, the
     * faces at the ends listed in {@code ends}. {@code across} != null: the UVs across the pipe forced to it (off-centre pieces).
     */
    static void piece(BlockModelBuilder m, float[] from, float[] to, Direction.Axis along, Direction[] ends, @Nullable float[] across, boolean cullEnds) {
        var e = m.element().from(from[0], from[1], from[2]).to(to[0], to[1], to[2]);
        for (Direction f : Direction.values()) {
            boolean end = f.getAxis() == along;
            if (end && !java.util.Arrays.asList(ends).contains(f)) continue;
            String t = end ? "#t" : tex(f, along);
            float[] uv = across == null ? null : uvs(f, from, to, along, end ? null : across);
            var face = e.face(f).texture(t);
            if (uv != null) face.uvs(uv[0], uv[1], uv[2], uv[3]);
            if (end && cullEnds) face.cullface(f);
            face.end();
        }
        e.end();
    }

    private static BlockModelBuilder model(BlockStateProvider p, String name, ResourceLocation tex, String render) {
        return p.models().getBuilder(name).texture("t", tex).texture("r", ResourceLocation.fromNamespaceAndPath(tex.getNamespace(), tex.getPath() + "_r"))
                .texture("particle", tex).renderType(render);
    }

    /**
     * The block state and models of a pipe: {@code <name>_arm_<side>}, {@code <name>_core_<face>_t|r}, {@code <name>_item};
     * {@code junction} != null: also the T-piece collar ({@code <name>_tee}, a 6 px steel sleeve) while it is true.
     */
    static void pipe(BlockStateProvider p, Block block, String name, String render, boolean endFaces, @Nullable BooleanProperty junction) {
        ResourceLocation tex = p.modLoc("block/" + name);
        MultiPartBlockStateBuilder mp = p.getMultipartBuilder(block);
        for (Direction d : Direction.values()) {
            BlockModelBuilder arm = model(p, name + "_arm_" + d.getSerializedName(), tex, render);
            float[][] box = armBox(d);
            piece(arm, box[0], box[1], d.getAxis(), endFaces ? new Direction[]{d} : new Direction[0], null, true);
            mp.part().modelFile(arm).addModel().condition(PipeBlock.SIDES.get(d), true).end();
        }
        for (Direction f : Direction.values()) {
            Direction.Axis u = uAxis(f), v = vAxis(f);
            Direction vPos = Direction.fromAxisAndDirection(v, Direction.AxisDirection.POSITIVE), vNeg = vPos.getOpposite();
            Direction uPos = Direction.fromAxisAndDirection(u, Direction.AxisDirection.POSITIVE), uNeg = uPos.getOpposite();
            BooleanProperty side = PipeBlock.SIDES.get(f);
            for (boolean alongV : new boolean[]{true, false}) {
                BlockModelBuilder m = model(p, name + "_core_" + f.getSerializedName() + (alongV ? "_t" : "_r"), tex, render);
                m.element().from(A, A, A).to(B, B, B).face(f).texture(alongV ? "#t" : "#r").end().end();
                var part = mp.part().modelFile(m).addModel().useOr();
                if (alongV) {
                    // the pipe goes on along V, or along neither axis of the face
                    part.nestedGroup().condition(side, false).condition(PipeBlock.SIDES.get(vPos), true).end();
                    part.nestedGroup().condition(side, false).condition(PipeBlock.SIDES.get(vNeg), true).end();
                    part.nestedGroup().condition(side, false).condition(PipeBlock.SIDES.get(uPos), false).condition(PipeBlock.SIDES.get(uNeg), false).end();
                } else {
                    part.nestedGroup().condition(side, false).condition(PipeBlock.SIDES.get(vPos), false).condition(PipeBlock.SIDES.get(vNeg), false)
                            .condition(PipeBlock.SIDES.get(uPos), true).end();
                    part.nestedGroup().condition(side, false).condition(PipeBlock.SIDES.get(vPos), false).condition(PipeBlock.SIDES.get(vNeg), false)
                            .condition(PipeBlock.SIDES.get(uNeg), true).end();
                }
                part.end();
            }
        }
        if (junction != null) {
            BlockModelBuilder tee = p.models().getBuilder(name + "_tee").texture("c", p.modLoc("block/pipe_tee")).texture("particle", tex).renderType(render);
            tee.element().from(5, 5, 5).to(11, 11, 11).allFaces((dd, f) -> f.texture("#c")).end();
            mp.part().modelFile(tee).addModel().condition(junction, true).end();
        }
        // item: a straight piece north-south
        BlockModelBuilder item = model(p, name + "_item", tex, render);
        piece(item, new float[]{A, A, 0}, new float[]{B, B, 16}, Direction.Axis.Z, new Direction[]{Direction.NORTH, Direction.SOUTH}, null, false);
    }

    /** The arm of a bundle's line (box {@code b} as from FilterPipeBundleBlock#arm) with the pipe texture's shaded columns across it. */
    static void bundleArm(BlockModelBuilder m, float[] b, Direction d) {
        piece(m, new float[]{b[0], b[1], b[2]}, new float[]{b[3], b[4], b[5]}, d.getAxis(), new Direction[0], new float[]{A, B}, false);
    }
}
