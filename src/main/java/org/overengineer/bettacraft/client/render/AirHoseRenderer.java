package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import org.overengineer.bettacraft.equipment.AirPumpBlockEntity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M23: the air hose of an air pump: from the pump up the glass, over the rim and down into the water of the tank it
 * serves (a pump on the tank: straight down). Thin clear silicone tube.
 */
public class AirHoseRenderer implements BlockEntityRenderer<AirPumpBlockEntity> {
    public static final ResourceLocation TEXTURE = BettaCraft.id("textures/entity/filter/air_hose.png");
    private static final float HALF = 0.02F;

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
        double surface = t.waterMb() > 0 ? t.waterSurfaceY() : t.min().getY() + 0.5;
        double bottom = Math.max(t.min().getY() + 0.15, surface - 0.6);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        int l = LevelRenderer.getLightColor(level, pos);
        Vec3 o = new Vec3(pos.getX(), pos.getY(), pos.getZ());
        Vec3 out = new Vec3(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5);
        if (tb.equals(pos.below())) {
            FlowRenderer.segment(ps, vc, out.subtract(o), new Vec3(out.x, bottom, out.z).subtract(o), HALF, l);
            return;
        }
        Direction d = Direction.getNearest(tb.getX() - pos.getX(), 0, tb.getZ() - pos.getZ());
        double rim = t.max().getY() + 1.06;
        Vec3 a = new Vec3(out.x + d.getStepX() * 0.3, out.y, out.z + d.getStepZ() * 0.3);
        Vec3 b = new Vec3(a.x, rim, a.z);
        Vec3 inside = new Vec3(out.x + d.getStepX() * 0.62, rim, out.z + d.getStepZ() * 0.62);
        Vec3 down = new Vec3(inside.x, bottom, inside.z);
        FlowRenderer.segment(ps, vc, out.subtract(o), a.subtract(o), HALF, l);
        FlowRenderer.segment(ps, vc, a.subtract(o), b.subtract(o), HALF, l);
        FlowRenderer.segment(ps, vc, b.subtract(o), inside.subtract(o), HALF, l);
        FlowRenderer.segment(ps, vc, inside.subtract(o), down.subtract(o), HALF, l);
    }

    @Override
    public boolean shouldRenderOffScreen(AirPumpBlockEntity be) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(AirPumpBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1.5, 16, 1.5);
    }
}
