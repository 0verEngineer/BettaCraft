package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.commands.RenderPass;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.platform.client.ClientEvents;
import org.overengineer.bettacraft.platform.client.RenderFrame;
import org.overengineer.bettacraft.platform.client.RenderLevelStageEvent;
import org.overengineer.bettacraft.platform.client.RenderLevelStageEvent.Stage;

/**
 * M52e: Minecraft 26.3 draws the solid and the translucent level in one render pass, and the loaders fire their level
 * render stages inside it. Inside a pass nothing may be uploaded and no other pass may be opened. So the stages of that pass
 * run earlier, right before the pass is opened ({@code LevelRendererPassMixin}, at {@code prepareTranslucents}): our
 * renderers work out what to draw, upload it ({@link ImmediateBuffers}, the miniature meshes and uniforms) and leave a
 * draw op for their stage; when the loader fires the stage inside the pass, the ops of that stage are drawn into it.
 * Stages outside the pass (and draws outside the level rendering) still draw at once in a pass of their own.
 */
public final class LevelDraws {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("bettacraft/render");

    /** The stages vanilla 26.3 draws inside its main level pass (in the order they come). */
    public static final List<Stage> MAIN_PASS = List.of(Stage.AFTER_SOLID_BLOCKS, Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS, Stage.AFTER_CUTOUT_BLOCKS,
            Stage.AFTER_ENTITIES, Stage.AFTER_BLOCK_ENTITIES, Stage.AFTER_TRANSLUCENT_BLOCKS);

    private static final EnumMap<Stage, List<Consumer<RenderPass>>> OPS = new EnumMap<>(Stage.class);
    private static boolean recording;
    private static @Nullable Stage recordingStage;
    private static @Nullable RenderPass mainPass;
    private static boolean warned;
    private static long frame;

    private LevelDraws() {}

    /** Whether our renderers are run ahead of the main pass now (their draws become ops). */
    public static boolean recording() {
        return recording;
    }

    /**
     * Right before the main pass is opened: the stages of the pass are run for our renderers (ahead of time), then everything
     * they made is uploaded.
     */
    public static void prepare() {
        frame++;
        for (List<Consumer<RenderPass>> l : OPS.values()) l.clear();
        mainPass = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ImmediateBuffers.beginFrame();
        recording = true;
        try {
            for (Stage s : MAIN_PASS) {
                recordingStage = s;
                ClientEvents.fireRenderLevel(new RenderLevelStageEvent(s, mc.levelRenderer, new PoseStack(), RenderFrame.modelView(),
                        RenderFrame.projection(), (int) mc.level.getGameTime(), mc.getDeltaTracker(), mc.gameRenderer.mainCamera(), RenderFrame.frustum()));
            }
        } finally {
            recording = false;
            recordingStage = null;
        }
        ImmediateBuffers.uploadFrame();
    }

    /** The number of the level frame (counted at {@link #prepare}). */
    public static long frame() {
        return frame;
    }

    /** The main pass of the level was opened (the solid part). */
    public static void mainPass(RenderPass pass) {
        mainPass = pass;
    }

    /** A draw: an op of the stage being recorded, otherwise at once in a pass of its own (if no pass is open). */
    public static void add(Consumer<RenderPass> op) {
        if (recording && recordingStage != null) {
            OPS.computeIfAbsent(recordingStage, k -> new ArrayList<>()).add(op);
            return;
        }
        if (inPass()) {
            if (!warned) {
                warned = true;
                LOGGER.warn("A draw of the mod came while another render pass is open; it is left out (only logged once)");
            }
            return;
        }
        var target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Bettacraft level draw",
                target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
            op.accept(pass);
        }
    }

    /**
     * A stage of the main pass came (fired by the loader inside the pass): its ops are drawn into the pass (the given one, or
     * the main pass the mixin saw). Returns whether the stage belongs to the main pass (then its listeners already ran).
     */
    public static boolean replay(Stage stage, @Nullable RenderPass pass) {
        if (!MAIN_PASS.contains(stage)) return false;
        List<Consumer<RenderPass>> ops = OPS.get(stage);
        if (ops == null || ops.isEmpty()) return true;
        RenderPass p = pass != null ? pass : mainPass;
        List<Consumer<RenderPass>> now = new ArrayList<>(ops);
        ops.clear();
        // M63b: with an Iris shader pack the main pass the mixin saw can be closed by the time the translucent stage comes (Iris
        // splits the level drawing): "Can't use a closed render pass" crashed the client (Fabric 26.3, also in 1.51). Then the
        // ops are drawn like a draw outside the pass (a pass of their own when none is open).
        if (p != null && closed(p)) p = null;
        if (p == null) {
            for (Consumer<RenderPass> op : now) add(op);
            return true;
        }
        for (Consumer<RenderPass> op : now) {
            RenderSystem.bindDefaultUniforms(p);
            op.accept(p);
        }
        return true;
    }

    private static @Nullable java.lang.reflect.Field closedField;
    private static boolean closedFieldTried;

    /** Whether a render pass of the frontend was closed already (its private flag; false when it cannot be read). */
    static boolean closed(RenderPass pass) {
        if (!(pass instanceof com.mojang.renderpearl.frontend.FrontendRenderPass)) return false;
        try {
            if (!closedFieldTried) {
                closedFieldTried = true;
                closedField = com.mojang.renderpearl.frontend.FrontendRenderPass.class.getDeclaredField("isClosed");
                closedField.setAccessible(true);
            }
            return closedField != null && closedField.getBoolean(pass);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /** Whether a render pass is open on the device's encoder. */
    public static boolean inPass() {
        return RenderSystem.getDevice().createCommandEncoder() instanceof com.mojang.renderpearl.frontend.FrontendCommandEncoder e && e.isInRenderPass();
    }
}
