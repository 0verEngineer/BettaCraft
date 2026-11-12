package org.overengineer.bettacraft.client.interior;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders tank interiors as miniatures (PLAN 7.4): all interior blocks are meshed once with the
 * {@link BlockRenderDispatcher} (with face culling) into one cached {@link VertexBuffer} per render layer and drawn
 * scaled by 1/scale inside the tank. Rebuilt only when the region data or the tank's light changes.
 * Block entity renderers are not supported in the miniature (PLAN). Drawn after block entities, i.e. before
 * translucent terrain, so the tank water/glass blends over it.
 */
public final class MiniatureRenderer {
    static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/miniature");
    private static final List<RenderType> LAYERS = List.of(RenderType.solid(), RenderType.cutoutMipped(), RenderType.cutout(), RenderType.translucent());
    /** Keeps the miniature 0.5 px inside the glass. */
    private static final float MARGIN = 0.5F / 16F;

    private MiniatureRenderer() {}

    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cam = event.getCamera().getPosition();
        double maxDist = ClientConfig.MINIATURE_RENDER_DISTANCE.get();
        boolean rebuiltThisFrame = false;
        for (TankBlockEntity be : TankBlockEntity.CLIENT_CONTROLLERS) {
            TankData t = be.getTank();
            if (t == null || t.regionId() < 0 || be.getLevel() != mc.level) continue;
            ClientRegion region = ClientInteriorCache.get(t.regionId());
            if (region == null) continue;
            BlockPos min = t.min();
            AABB box = new AABB(min.getX(), min.getY(), min.getZ(), min.getX() + t.sizeX(), min.getY() + t.sizeY(), min.getZ() + t.sizeZ());
            if (box.getCenter().distanceToSqr(cam) > maxDist * maxDist || !event.getFrustum().isVisible(box)) continue;

            int lightKey = lightKey(mc, t);
            if ((region.mesh == null || region.meshDirty || region.lightKey != lightKey) && !rebuiltThisFrame) {
                rebuild(mc, region, min);
                region.lightKey = lightKey;
                rebuiltThisFrame = true; // spread rebuilds over frames
            }
            if (region.mesh != null) draw(event, region, t, cam);
        }
    }

    private static int lightKey(Minecraft mc, TankData t) {
        BlockPos c = t.min().offset(t.sizeX() / 2, t.sizeY() / 2, t.sizeZ() / 2);
        return mc.level.getBrightness(LightLayer.BLOCK, c) * 31 + mc.level.getBrightness(LightLayer.SKY, c)
                + mc.level.getBrightness(LightLayer.BLOCK, t.min()) * 961;
    }

    private static void rebuild(Minecraft mc, ClientRegion region, BlockPos tankMin) {
        long t0 = System.nanoTime();
        region.close();
        BlockRenderDispatcher brd = mc.getBlockRenderer();
        RegionBlockGetter getter = new RegionBlockGetter(region, mc.level, tankMin);
        RandomSource rand = RandomSource.create();
        PoseStack ps = new PoseStack();
        Map<RenderType, ByteBufferBuilder> memory = new IdentityHashMap<>();
        Map<RenderType, BufferBuilder> builders = new IdentityHashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int blocks = 0;
        for (InteriorSnapshot.Section s : region.sections()) {
            for (int i = 0; i < InteriorSnapshot.SECTION_VOLUME; i++) {
                int pi = s.indices()[i];
                if (pi == 0) continue;
                BlockState state = s.palette()[pi];
                if (state.getRenderShape() != RenderShape.MODEL) continue;
                int lx = (s.sx() << 4) + (i & 15);
                int ly = (s.sy() << 4) + ((i >> 8) & 15) - InteriorRegion.BASE_Y;
                int lz = (s.sz() << 4) + ((i >> 4) & 15);
                pos.set(lx, ly, lz);
                BakedModel model = brd.getBlockModel(state);
                ModelData md = model.getModelData(getter, pos, state, ModelData.EMPTY);
                rand.setSeed(state.getSeed(pos));
                for (RenderType rt : model.getRenderTypes(state, rand, md)) {
                    RenderType layer = LAYERS.contains(rt) ? rt : RenderType.translucent();
                    BufferBuilder bb = builders.computeIfAbsent(layer, k -> {
                        ByteBufferBuilder mem = new ByteBufferBuilder(1 << 16);
                        memory.put(k, mem);
                        return new BufferBuilder(mem, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                    });
                    ps.pushPose();
                    ps.translate(lx, ly, lz);
                    brd.renderBatched(state, pos, getter, ps, bb, true, rand, md, rt);
                    ps.popPose();
                }
                blocks++;
            }
        }
        MiniatureMesh mesh = new MiniatureMesh();
        for (RenderType layer : LAYERS) {
            BufferBuilder bb = builders.get(layer);
            if (bb == null) continue;
            MeshData data = bb.build();
            if (data == null) continue;
            int vertices = data.drawState().vertexCount();
            mesh.quads += vertices / 4;
            mesh.vertexBytes += (long) vertices * DefaultVertexFormat.BLOCK.getVertexSize();
            VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
            vb.bind();
            vb.upload(data);
            VertexBuffer.unbind();
            mesh.buffers.put(layer, vb);
        }
        for (ByteBufferBuilder m : memory.values()) m.close();
        region.mesh = mesh;
        region.meshDirty = false;
        if (ClientConfig.LOG_MINIATURE_STATS.get()) {
            LOGGER.info("Miniature mesh region {} ({}x{}x{}, scale {}): {} blocks -> {} quads, {} KiB VRAM, built in {} ms",
                    region.id, region.sizeX, region.sizeY, region.sizeZ, region.scale, blocks, mesh.quads,
                    mesh.vertexBytes / 1024, (System.nanoTime() - t0) / 1_000_000.0);
        }
    }

    private static void draw(RenderLevelStageEvent event, ClientRegion region, TankData t, Vec3 cam) {
        BlockPos min = t.min();
        float fx = (t.sizeX() - 2 * MARGIN) / region.sizeX;
        float fy = (t.sizeY() - 2 * MARGIN) / region.sizeY;
        float fz = (t.sizeZ() - 2 * MARGIN) / region.sizeZ;
        Matrix4f mv = new Matrix4f(event.getModelViewMatrix())
                .translate((float) (min.getX() - cam.x) + MARGIN, (float) (min.getY() - cam.y) + MARGIN, (float) (min.getZ() - cam.z) + MARGIN)
                .scale(fx, fy, fz);
        Matrix4f proj = event.getProjectionMatrix();
        for (Map.Entry<RenderType, VertexBuffer> e : region.mesh.buffers.entrySet()) {
            RenderType rt = e.getKey();
            rt.setupRenderState();
            ShaderInstance shader = RenderSystem.getShader();
            if (shader != null) {
                shader.setDefaultUniforms(VertexFormat.Mode.QUADS, mv, proj, Minecraft.getInstance().getWindow());
                if (shader.CHUNK_OFFSET != null) shader.CHUNK_OFFSET.set(0F, 0F, 0F);
                shader.apply();
                e.getValue().bind();
                e.getValue().draw();
                shader.clear();
                VertexBuffer.unbind();
            }
            rt.clearRenderState();
        }
    }
}
