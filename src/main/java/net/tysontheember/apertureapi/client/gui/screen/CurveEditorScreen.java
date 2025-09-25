package net.tysontheember.apertureapi.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.tysontheember.apertureapi.common.animation.CurveConfig;

import java.util.function.Consumer;

/**
 * Minimal placeholder curve editor screen.
 * Provides Save and Cancel actions; UI can be expanded later.
 */
public class CurveEditorScreen extends Screen {
    private final CurveConfig config;
    private final Consumer<CurveConfig> onSave;
    private final Runnable onCancel;
    private final String curveName;

    public CurveEditorScreen(CurveConfig config, Consumer<CurveConfig> onSave, Runnable onCancel, String curveName) {
        super(Component.literal("Curve Editor: " + (curveName == null ? "Curve" : curveName)));
        this.config = config;
        this.onSave = onSave;
        this.onCancel = onCancel;
        this.curveName = curveName;
    }

    @Override
    protected void init() {
        int w = 100, h = 20, s = 8;
        int cx = this.width / 2;
        int cy = this.height / 2;

        // Save button
        Button save = Button.builder(Component.translatable("gui.curve.save"), b -> {
            if (onSave != null) onSave.accept(config);
            if (onCancel != null) onCancel.run();
        }).pos(cx - w - s/2, cy).size(w, h)
                .tooltip(Tooltip.create(Component.translatable("gui.curve.save.tooltip")))
                .build();

        // Cancel button
        Button cancel = Button.builder(Component.translatable("gui.curve.cancel"), b -> {
            if (onCancel != null) onCancel.run();
        }).pos(cx + s/2, cy).size(w, h)
                .tooltip(Tooltip.create(Component.translatable("gui.curve.cancel.tooltip")))
                .build();

        addRenderableWidget(save);
        addRenderableWidget(cancel);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.getTitle(), this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        // Placeholder info text
        g.drawCenteredString(this.font, Component.literal("Placeholder curve editor for " + (curveName == null ? "Curve" : curveName)).getString(), this.width / 2, this.height / 2 - 20, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        if (onCancel != null) onCancel.run();
        else Minecraft.getInstance().setScreen(null);
    }
}
