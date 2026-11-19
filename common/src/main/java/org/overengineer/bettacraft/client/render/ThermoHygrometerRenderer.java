package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.ThermoHygrometerBlock;
import org.overengineer.bettacraft.terrarium.ThermoHygrometerBlockEntity;

/**
 * M31: the LCD of the thermo-hygrometer: the air temperature and the humidity of the terrarium behind it (from the tank's
 * client view, synced by the climate step), on an aquarium the water temperature, without a tank dashes.
 */
public class ThermoHygrometerRenderer extends org.overengineer.bettacraft.client.render.LegacyBlockEntityRenderer<ThermoHygrometerBlockEntity> {
    private static final int LCD_TEXT = 0xFF1E2A1E;
    private final Font font;

    public ThermoHygrometerRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.font();
    }

    @Override
    public void render(ThermoHygrometerBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) return;
        BlockState s = be.getBlockState();
        if (!(s.getBlock() instanceof ThermoHygrometerBlock)) return;
        TankBlockEntity c = ThermoHygrometerBlock.tank(be.getLevel(), be.getBlockPos(), s);
        TankData t = c == null ? null : c.getTank();
        String top, bottom;
        if (t == null) {
            top = "--.-°C";
            bottom = "--%";
        } else if (t.hasClimate()) {
            top = TankClimate.f1(t.airTemp()) + "°C";
            bottom = Math.round(t.humidity()) + "%";
        } else {
            top = TankClimate.f1(t.waterTemp()) + "°C";
            bottom = "H2O";
        }
        Direction f = s.getValue(ThermoHygrometerBlock.FACING);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-f.toYRot()));
        // the front of the case lies 1/16 in front of the wall it hangs on
        pose.translate(0, 0.07, -0.5 + 1.0 / 16 + 0.002);
        float scale = 0.0085F;
        pose.scale(scale, -scale, scale);
        draw(pose, buffers, top, -6, light);
        draw(pose, buffers, bottom, 4, light);
        pose.popPose();
    }

    private void draw(PoseStack pose, MultiBufferSource buffers, String text, float y, int light) {
        float x = -font.width(text) / 2F;
        LegacyBlockEntityRenderer.collector().submitText(pose, x, y, net.minecraft.util.FormattedCharSequence.forward(text, net.minecraft.network.chat.Style.EMPTY), false, Font.DisplayMode.POLYGON_OFFSET, light, LCD_TEXT, 0, 0);
    }
}
