package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.client.interior.IrisCompat;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.FilterFlow;
import org.overengineer.bettacraft.equipment.FilterHost;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;

/**
 * Hose, outlet and water jet of an external filter (block next to a tank, or a filter in a cabinet under a tank).
 * The jet is skipped in the Iris shadow pass (translucent water must not cast a shadow); the hose is solid.
 */
public class FilterRenderer<T extends BlockEntity & FilterHost> extends org.overengineer.bettacraft.client.render.LegacyBlockEntityRenderer<T> {
    public FilterRenderer(BlockEntityRendererProvider.Context ctx) {}

    /** Path of this filter into its tank (client view of the tank), or null if it is not attached. */
    @Nullable
    public static FilterFlow.Path path(Level level, BlockEntity be) {
        if (!(be instanceof FilterHost host) || !host.hasFilter()) return null;
        TankBlockEntity c = FilterHost.tankOf(level, host.tankBlockPos());
        if (c == null || c.getTank() == null) return null;
        TankData t = c.getTank();
        BlockState s = be.getBlockState();
        if (s.getBlock() instanceof ExternalFilterBlock && be instanceof org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity f) return FilterFlow.forFilter(t, f); // M62: also from below
        if (s.getBlock() instanceof CabinetBlock && be instanceof CabinetBlockEntity cab) return FilterFlow.forCabinet(t, cab); // M22.4: its outlet side
        return null;
    }

    @Override
    public void render(T be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        if (BlockEntityCull.outside(getRenderBoundingBox(be))) return; // M66.5: Sodium draws the global list without a view test
        if (be instanceof CabinetBlockEntity drive) DriveRender.cabinetCoupling(drive, ps, buffers, light); // M41: the flange of the Create shaft
        // M29.1: the air pump in the cabinet: its hose beside the filter hose. M67.5: only with a filter in the cabinet (alone it
        // looked like the hose of a filter that is not there; the air stones bubble without it)
        if (be instanceof CabinetBlockEntity cab && cab.hasAirPump() && cab.hasFilter()) AirHoseRenderer.renderCabinet(cab, ps, buffers);
        FilterFlow.Path p = path(level, be);
        if (p == null) return;
        BlockPos pos = be.getBlockPos();
        Vec3 origin = new Vec3(pos.getX(), pos.getY(), pos.getZ());
        int hoseLight = net.minecraft.util.LightCoordsUtil.getLightCoords(level, BlockPos.containing(p.top()));
        Vec3 source = null;
        if (be.getBlockState().getBlock() instanceof CabinetBlock && be instanceof CabinetBlockEntity cab) {
            // M22.4: out of the cabinet's side towards the wall of its outlet
            net.minecraft.core.Direction out = cab.outletSide().wall(be.getBlockState().getValue(CabinetBlock.FACING));
            source = new Vec3(pos.getX() + 0.5 + out.getStepX() * 0.5, pos.getY() + 0.75, pos.getZ() + 0.5 + out.getStepZ() * 0.5);
        }
        FlowRenderer.hose(ps, buffers.getBuffer(FlowRenderer.hoseType()), p, origin, hoseLight, source);
        if (!p.wet() || IrisCompat.renderingShadowPass()) return;
        double share = ((FilterHost) be).powerShare(); // M23: the power regulator
        if (share <= 0) return;
        float time = (level.getGameTime() + partialTick) / 20F;
        int jetLight = net.minecraft.util.LightCoordsUtil.getLightCoords(level, BlockPos.containing(p.nozzle()));
        Vec3 n = p.nozzle();
        Vec3 start = n.add(p.inward().getStepX() * 0.1, 0, p.inward().getStepZ() * 0.1);
        FilterFlow.Path shifted = new FilterFlow.Path(p.bottom(), p.top(), p.inside(), start, p.inward(), p.surfaceY(), true);
        TankBlockEntity c = FilterHost.tankOf(level, ((FilterHost) be).tankBlockPos());
        double scale = c == null || c.getTank() == null ? 1.0 : FilterFlow.fitScale(c.getTank(), shifted, 1.0);
        scale *= 0.4 + 0.6 * share;
        FlowRenderer.jet(ps, buffers.getBuffer(FlowRenderer.jetType()), shifted.jet(10, scale), p.inward(), origin, jetLight, time,
                p.submerged() ? 0.06F : 0.05F, p.submerged() ? 0.7F : 1.0F);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        // M66.5: like the tank's details (was 96: every hose and jet of the rows far behind was built every frame)
        return org.overengineer.bettacraft.config.ClientConfig.TANK_DETAIL_DISTANCE.get();
    }

    public AABB getRenderBoundingBox(T be) {
        // M66.5: asked every frame for every cabinet and filter of the world (they are all in the global list): the tank's
        // controller is looked up once a second, not every frame
        Level level = be.getLevel();
        if (level != null && be instanceof FilterHost host && host.renderBoxCache() != null) return host.renderBoxCache().get(level.getGameTime(), () -> renderBox(be));
        return renderBox(be);
    }

    private AABB renderBox(T be) {
        BlockPos pos = be.getBlockPos();
        Level level = be.getLevel();
        AABB own = new AABB(pos);
        if (level == null || !(be instanceof FilterHost host)) return own;
        TankBlockEntity c = FilterHost.tankOf(level, host.tankBlockPos());
        if (c == null || c.getTank() == null) return own;
        TankData t = c.getTank();
        BlockPos mn = t.min(), mx = t.max();
        return own.minmax(new AABB(mn.getX(), mn.getY(), mn.getZ(), mx.getX() + 1, mx.getY() + 1.5, mx.getZ() + 1)).inflate(0.3);
    }

    /** Explicit renderer types for the two block entities. */
    public static FilterRenderer<org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity> forFilter(BlockEntityRendererProvider.Context ctx) {
        return new FilterRenderer<>(ctx);
    }

    public static FilterRenderer<CabinetBlockEntity> forCabinet(BlockEntityRendererProvider.Context ctx) {
        return new FilterRenderer<>(ctx);
    }

    static Minecraft mc() {
        return Minecraft.getInstance();
    }
}
