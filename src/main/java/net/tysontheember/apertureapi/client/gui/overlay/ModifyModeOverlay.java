package net.tysontheember.apertureapi.client.gui.overlay;

import net.tysontheember.apertureapi.client.CameraAnimIdeCache;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import static net.tysontheember.apertureapi.client.ClientUtil.font;
import static net.tysontheember.apertureapi.client.register.ModKeyMapping.*;

public class ModifyModeOverlay implements IGuiOverlay {
    private static final MutableComponent OPEN = Component.translatable("hud.camera_anim.modify_mode.open");
    private static final MutableComponent CLOSE = Component.translatable("hud.camera_anim.modify_mode.close");
    private static final MutableComponent SELECT = Component.translatable("hud.camera_anim.modify_mode.select");
    private static final MutableComponent MOVE = Component.translatable("hud.camera_anim.modify_mode.move");
    private static final MutableComponent DRAG = Component.translatable("hud.camera_anim.modify_mode.drag");

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (CameraAnimIdeCache.EDIT) {
            int i = 0;
            guiGraphics.drawString(font(), Component.literal("Mode: ").append(CameraAnimIdeCache.getMode().name()), 0, 10 * i++, 0xffffff);
            var mt = CameraAnimIdeCache.getMoveMode().getMoveType();
            if (mt != CameraAnimIdeCache.MoveType.NONE) {
                String axis = switch (mt) {
                    case X, RX -> "X";
                    case Y, RY -> "Y";
                    case Z, RZ -> "Z";
                    default -> mt.name();
                };
                guiGraphics.drawString(font(), Component.literal("Axis: ").append(axis), 0, 10 * i++, 0xffe6b422);
            }
            // Show snapping info
            boolean sh = net.tysontheember.apertureapi.client.ClientUtil.shiftDown();
            boolean al = net.tysontheember.apertureapi.client.ClientUtil.altDown();
            String snap = (sh && al) ? "0.5° (Shift+Alt)" : (sh ? "5° (Shift)" : (al ? "1° (Alt)" : "Free"));
            guiGraphics.drawString(font(), Component.literal("Snap: ").append(snap), 0, 10 * i++, 0xff90ee90);
            KeyMapping edit = EDIT_MODE.get();
            guiGraphics.drawString(font(), Component.translatable(edit.getName()).append(": ").append(edit.getTranslatedKeyMessage()).append(OPEN), 0, 10 * i++, 0xffffff);
            KeyMapping preview = PREVIEW_MODE.get();
            guiGraphics.drawString(font(), Component.translatable(preview.getName()).append(": ").append(preview.getTranslatedKeyMessage()).append(CameraAnimIdeCache.PREVIEW ? OPEN : CLOSE), 0, 10 * i++, 0xffffff);
            KeyMapping forward = FORWARD.get();
            guiGraphics.drawString(font(), Component.translatable(forward.getName()).append(": ").append(forward.getTranslatedKeyMessage()), 0, 10 * i++, 0xffffff);
            KeyMapping back = BACK.get();
            guiGraphics.drawString(font(), Component.translatable(back.getName()).append(": ").append(back.getTranslatedKeyMessage()), 0, 10 * i++, 0xffffff);
            KeyMapping reset = RESET.get();
            guiGraphics.drawString(font(), Component.translatable(reset.getName()).append(": ").append(reset.getTranslatedKeyMessage()), 0, 10 * i++, 0xffffff);
            KeyMapping play = PLAY.get();
            guiGraphics.drawString(font(), Component.translatable(play.getName()).append(": ").append(play.getTranslatedKeyMessage()), 0, 10 * i++, 0xffffff);
            KeyMapping add = ADD_GLOBAL_CAMERA_POINT.get();
            guiGraphics.drawString(font(), Component.translatable(add.getName()).append(": ").append(add.getTranslatedKeyMessage()), 0, 10 * i++, 0xffffff);
            KeyMapping clean = CLEAN.get();
            guiGraphics.drawString(font(), Component.translatable(clean.getName()).append(": ").append(clean.getTranslatedKeyMessage()), 0, 10 * i++, 0xffffff);
            KeyMapping set = POINT_SETTING.get();
            guiGraphics.drawString(font(), Component.translatable(set.getName()).append(": ").append(set.getTranslatedKeyMessage()), 0, 10 * i++, 0xffffff);
            KeyMapping time = SET_CAMERA_TIME.get();
            guiGraphics.drawString(font(), Component.translatable(time.getName()).append(": ").append(time.getTranslatedKeyMessage()), 0, 10 * i++, 0xffffff);
            InputConstants.Key mouseLeft = InputConstants.Type.MOUSE.getOrCreate(InputConstants.MOUSE_BUTTON_LEFT);
            InputConstants.Key mouseRight = InputConstants.Type.MOUSE.getOrCreate(InputConstants.MOUSE_BUTTON_RIGHT);
            guiGraphics.drawString(font(), SELECT.copy().append(": ").append(mouseLeft.getDisplayName()), 0, 10 * i++, 0xffffff);
            guiGraphics.drawString(font(), MOVE.copy().append(": ").append(mouseRight.getDisplayName()), 0, 10 * i++, 0xffffff);
            guiGraphics.drawString(font(), DRAG.copy().append(": ").append(mouseRight.getDisplayName()), 0, 10 * i++, 0xffffff);
            KeyMapping rotateMode = ROTATE_MODE.get();
            guiGraphics.drawString(font(), Component.translatable(rotateMode.getName()).append(": ").append(rotateMode.getTranslatedKeyMessage()), 0, 10 * i++, 0xffffff);
            KeyMapping delete = DELETE_GLOBAL_CAMERA_POINT.get();
            guiGraphics.drawString(font(), Component.translatable(delete.getName()).append(": ").append(delete.getTranslatedKeyMessage()), 0, 10 * i++, 0xffffff);
            KeyMapping nativeCenter = NATIVE_CENTER.get();
            guiGraphics.drawString(font(), Component.translatable(nativeCenter.getName()).append(": ").append(nativeCenter.getTranslatedKeyMessage()), 0, 10 * i++, 0xffffff);
            KeyMapping removeNativeCenter = REMOVE_NATIVE_CENTER.get();
            guiGraphics.drawString(font(), Component.translatable(removeNativeCenter.getName()).append(": ").append(removeNativeCenter.getTranslatedKeyMessage()), 0, 10 * i++, 0xffffff);
        } else if (CameraAnimIdeCache.VIEW) {
            KeyMapping view = VIEW_MODE.get();
            guiGraphics.drawString(font(), Component.translatable(view.getName()).append(": ").append(view.getTranslatedKeyMessage()).append(OPEN), 0, 0, 0xffffff);
        }

        // If rotating and a point is selected, display current Euler angles
        if (CameraAnimIdeCache.EDIT && CameraAnimIdeCache.getMode() == CameraAnimIdeCache.Mode.ROTATE && CameraAnimIdeCache.getSelectedPoint().getPointTime() >= 0) {
            var path = CameraAnimIdeCache.getPath();
            var point = path.getPoint(CameraAnimIdeCache.getSelectedPoint().getPointTime());
            if (point != null) {
                var rot = point.getRot();
                guiGraphics.drawString(font(), Component.literal(String.format("Pitch: %.1f°, Yaw: %.1f°, Roll: %.1f°", rot.x, rot.y, rot.z)), 0, screenHeight - 20, 0xffff00);
            }
        }
    }
}

