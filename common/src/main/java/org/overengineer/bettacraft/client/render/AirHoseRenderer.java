package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.interior.MiniatureJets;
import org.overengineer.bettacraft.equipment.AirPumpBlockEntity;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.FilterFlow;
import org.overengineer.bettacraft.equipment.FilterHost;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M23: the air hose of an air pump: from the pump up the glass, over the rim and down into the water of the tank it
 * serves (a pump on the tank: straight down). Thin clear silicone tube. M29.1: the hose goes down along the glass to
 * the floor and over the floor to the air stones and sponge filters (to the nearest one first, then from one to the
 * next); an air pump in the cabinet sends its hose up next to the filter hose at the cabinet's outlet side.
 */
public class AirHoseRenderer implements BlockEntityRenderer<AirPumpBlockEntity> {
    public static final ResourceLocation TEXTURE = BettaCraft.id("textures/entity/filter/air_hose.png");
    private static final float HALF = 0.02F;
    /** At most this many stones are joined by one hose (more would only clutter the floor). */
    private static final int MAX_TARGETS = 6;

    public AirHoseRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(AirPumpBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        BlockPos pos = be.getBlockPos();
        BlockPos tb = AirPumpBlockEntity.tankBlock(level, pos);
        if (tb == null) return;
        TankBlockEntity c = TankMultiblock.getController(level, tb);
        if (c == null || c.getTank() == null) return;
        TankData t = c.getTank();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        int l = LevelRenderer.getLightColor(level, pos);
        Vec3 o = new Vec3(pos.getX(), pos.getY(), pos.getZ());
        Vec3 out = new Vec3(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5);
        List<Vec3> pts = new ArrayList<>();
        pts.add(out);
        if (!tb.equals(pos.below())) {
            Direction d = Direction.getNearest(tb.getX() - pos.getX(), 0, tb.getZ() - pos.getZ());
            double rim = t.max().getY() + 1.06;
            Vec3 a = new Vec3(out.x + d.getStepX() * 0.3, out.y, out.z + d.getStepZ() * 0.3);
            pts.add(a);
            pts.add(new Vec3(a.x, rim, a.z));
            // over the rim and down on the inside of the glass
            pts.add(new Vec3(out.x + d.getStepX() * 0.58, rim, out.z + d.getStepZ() * 0.58));
        }
        inWater(pts, t);
        draw(ps, vc, o, pts, l);
    }

    /**
     * M29.1: the part in the water, from the last point of {@code pts} (inside the tank, above the water or at its rim):
     * straight down to the floor near the glass, then over the floor to the nearest air stone or sponge filter and from
     * there on to the next nearest (at most {@link #MAX_TARGETS}); without any, the hose ends on the floor.
     */
    static void inWater(List<Vec3> pts, TankData t) {
        Vec3 entry = pts.getLast();
        List<Vec3> targets = new ArrayList<>(MiniatureJets.airTargets(t));
        double floor = MiniatureJets.floorY(t);
        if (targets.isEmpty()) {
            pts.add(new Vec3(entry.x, floor, entry.z));
            return;
        }
        Vec3 at = new Vec3(entry.x, floor, entry.z);
        Vec3 first = nearest(targets, at);
        pts.add(new Vec3(entry.x, Math.min(first.y, entry.y), entry.z));
        for (int n = 0; n < MAX_TARGETS && !targets.isEmpty(); n++) {
            Vec3 next = n == 0 ? first : nearest(targets, at);
            targets.remove(next);
            // along the floor (or at the height of a sponge filter's lift tube) in straight runs, like a laid hose
            straight(pts, next);
            at = next;
        }
    }

    /** Adds the way from the last point to {@code to} as straight runs along the axes (down first, up last). */
    private static void straight(List<Vec3> pts, Vec3 to) {
        Vec3 p = pts.getLast();
        if (to.y < p.y) pts.add(p = new Vec3(p.x, to.y, p.z));
        if (Math.abs(to.x - p.x) > 1e-4) pts.add(p = new Vec3(to.x, p.y, p.z));
        if (Math.abs(to.z - p.z) > 1e-4) pts.add(p = new Vec3(p.x, p.y, to.z));
        if (Math.abs(to.y - p.y) > 1e-4) pts.add(to);
    }

    private static Vec3 nearest(List<Vec3> targets, Vec3 from) {
        Vec3 best = targets.getFirst();
        double bd = Double.MAX_VALUE;
        for (Vec3 v : targets) {
            double dx = v.x - from.x, dz = v.z - from.z, d = dx * dx + dz * dz;
            if (d < bd) {
                bd = d;
                best = v;
            }
        }
        return best;
    }

    /** Draws the hose through the points (every segment is a box along one axis: {@link FlowRenderer#segment}). */
    static void draw(PoseStack ps, VertexConsumer vc, Vec3 origin, List<Vec3> pts, int light) {
        for (int i = 1; i < pts.size(); i++) {
            if (pts.get(i).distanceToSqr(pts.get(i - 1)) < 1e-6) continue;
            FlowRenderer.segment(ps, vc, pts.get(i - 1).subtract(origin), pts.get(i).subtract(origin), HALF, light);
        }
    }

    /**
     * M29.1: the air hose of the air pump in a cabinet: out of the cabinet's side next to the filter hose, up the wall of
     * the cabinet's outlet side beside it, over the rim and down the inside of the glass to the stones.
     */
    public static void renderCabinet(CabinetBlockEntity cab, PoseStack ps, MultiBufferSource buffers) {
        Level level = cab.getLevel();
        if (level == null || !cab.hasAirPump() || !(cab.getBlockState().getBlock() instanceof CabinetBlock)) return;
        TankBlockEntity c = FilterHost.tankOf(level, cab.tankBlockPos());
        if (c == null || c.getTank() == null) return;
        TankData t = c.getTank();
        FilterFlow.Path p = FilterFlow.forCabinet(t, cab);
        if (p == null) return;
        BlockPos pos = cab.getBlockPos();
        Direction out = cab.outletSide().wall(cab.getBlockState().getValue(CabinetBlock.FACING));
        // beside the filter hose: a little along the wall, towards the middle of the cabinet's column
        Direction along = out.getClockWise();
        double sign = (along.getAxis() == Direction.Axis.X ? p.top().x - (pos.getX() + 0.5) : p.top().z - (pos.getZ() + 0.5))
                * (along.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1) > 0.05 ? -1 : 1;
        Vec3 off = new Vec3(along.getStepX() * 0.075 * sign, 0, along.getStepZ() * 0.075 * sign);
        Vec3 source = new Vec3(pos.getX() + 0.5 + out.getStepX() * 0.5, pos.getY() + 0.62, pos.getZ() + 0.5 + out.getStepZ() * 0.5).add(off);
        Vec3 in = new Vec3(p.inward().getStepX(), 0, p.inward().getStepZ());
        Vec3 top = p.top().add(off);
        double reach = Math.abs((p.inside().x - p.top().x) + (p.inside().z - p.top().z));
        List<Vec3> pts = new ArrayList<>();
        pts.add(source);
        pts.add(new Vec3(p.bottom().x, source.y, p.bottom().z).add(off));
        pts.add(top);
        pts.add(top.add(in.scale(Math.max(0.12, reach - 0.18))));
        inWater(pts, t);
        int light = LevelRenderer.getLightColor(level, BlockPos.containing(p.top()));
        draw(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), new Vec3(pos.getX(), pos.getY(), pos.getZ()), pts, light);
    }

    @Override
    public boolean shouldRenderOffScreen(AirPumpBlockEntity be) {
        return true;
    }

    public AABB getRenderBoundingBox(AirPumpBlockEntity be) {
        AABB own = new AABB(be.getBlockPos()).inflate(1.5, 16, 1.5);
        Level level = be.getLevel();
        BlockPos tb = level == null ? null : AirPumpBlockEntity.tankBlock(level, be.getBlockPos());
        TankBlockEntity c = tb == null ? null : TankMultiblock.getController(level, tb);
        if (c == null || c.getTank() == null) return own;
        BlockPos mn = c.getTank().min(), mx = c.getTank().max();
        return own.minmax(new AABB(mn.getX(), mn.getY(), mn.getZ(), mx.getX() + 1, mx.getY() + 1.5, mx.getZ() + 1));
    }
}
