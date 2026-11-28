package org.overengineer.bettacraft.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
import org.overengineer.bettacraft.network.EmptyConfirmPayload;

/**
 * M22.1: asks before the animals of a bucket or a bag go into the world ({@code ContainerRules}): the container and what
 * happens, two buttons "Cancel" (focused, also Escape) and "Empty". Only "Empty" tells the server to let them go.
 */
public final class ConfirmEmptyScreen extends Screen {
    /** M67.5: the box is as high as the wrapped text needs (a long bucket name pushed the text under the buttons). */
    private static final int W = 240, TEXT_TOP = 32, LINE = 10, BUTTONS = 34;
    private final InteractionHand hand;
    private final ItemStack stack;
    /** The last screen opened (smoke tests press its buttons). */
    public static ConfirmEmptyScreen last;
    private Button cancel, empty;

    public ConfirmEmptyScreen(InteractionHand hand, ItemStack stack) {
        super(Component.translatable("gui.bettacraft.confirm_empty.title"));
        this.hand = hand;
        this.stack = stack;
        last = this;
    }

    /** Opens the screen (client thread), unless another screen is open. */
    public static void open(InteractionHand hand, ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.gui.screen() == null) mc.gui.setScreen(new ConfirmEmptyScreen(hand, stack));
        });
    }

    private Component text() {
        return Component.translatable("gui.bettacraft.confirm_empty.text", stack.getHoverName());
    }

    /** Height of the box: title, every wrapped line of the text, the buttons. */
    int boxHeight() {
        return TEXT_TOP + font.split(text(), W - 20).size() * LINE + 4 + BUTTONS;
    }

    /** Smoke test: the last line of the text ends above the buttons. */
    public boolean textAboveButtons() {
        return TEXT_TOP + font.split(text(), W - 20).size() * LINE <= boxHeight() - 28;
    }

    @Override
    protected void init() {
        int H = boxHeight();
        int left = (width - W) / 2, top = (height - H) / 2;
        cancel = addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.confirm_empty.cancel"), b -> onClose())
                .bounds(left + 12, top + H - 28, 94, 20).build());
        empty = addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.confirm_empty.empty"), b -> confirm())
                .bounds(left + W - 106, top + H - 28, 94, 20).build());
        setInitialFocus(cancel);
    }

    /** "Empty": the server lets the animals go (and checks everything again). */
    public void confirm() {
        PacketDistributor.sendToServer(new EmptyConfirmPayload(hand));
        onClose();
    }

    public void cancel() {
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int H = boxHeight();
        int left = (width - W) / 2, top = (height - H) / 2;
        g.fill(left, top, left + W, top + H, 0xE0101820);
        g.outline(left, top, W, H, 0xFF6A8CA8);
        g.item(stack, left + 10, top + 9);
        g.text(font, title, left + 32, top + 13, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFFFFF));
        int y = top + TEXT_TOP;
        for (var line : font.split(text(), W - 20)) {
            g.text(font, line, left + 10, y, org.overengineer.bettacraft.client.ClientCompat.textColor(0xC8D2DC));
            y += LINE;
        }
        // the buttons over the box
        cancel.extractRenderState(g, mouseX, mouseY, partialTick);
        empty.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
