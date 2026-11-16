package org.overengineer.bettacraft.compat.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.overengineer.bettacraft.client.render.DriveRender;

/**
 * M41 (client, only with Create): the turning shaft stub of the drive adapter, at the angle Create gives its shafts
 * ({@link KineticBlockEntityRenderer#getAngleForBe}), so it meshes with the shaft in front of it. A plain block entity
 * renderer with our own texture (it also draws while Flywheel draws Create's own blocks).
 */
public final class DriveAdapterRenderer implements BlockEntityRenderer<DriveAdapterBlockEntity> {
    public DriveAdapterRenderer(BlockEntityRendererProvider.Context ctx) {}

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CreateDrive.ADAPTER_BE.get(), DriveAdapterRenderer::new);
    }

    @Override
    public void render(DriveAdapterBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) return;
        Direction facing = be.getBlockState().getValue(DriveAdapterBlock.FACING);
        Direction.Axis axis = facing.getAxis();
        float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), axis);
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        Axis rot = switch (axis) {
            case X -> Axis.XP;
            case Y -> Axis.YP;
            case Z -> Axis.ZP;
        };
        ps.mulPose(rot.rotation(angle));
        ps.translate(-0.5, -0.5, -0.5);
        DriveRender.face(ps, facing);
        var vc = buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
        // the stub from the front face into the bearing ring (model north: z 0 .. 4)
        DriveRender.box(ps, vc, DriveRender.sprite("drive_adapter_shaft"), 6, 6, 0, 10, 10, 4, light);
        ps.popPose();
    }
}
