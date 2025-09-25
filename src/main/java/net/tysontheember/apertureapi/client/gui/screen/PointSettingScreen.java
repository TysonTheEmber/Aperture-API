package net.tysontheember.apertureapi.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.tysontheember.apertureapi.client.CameraAnimIdeCache;
import net.tysontheember.apertureapi.client.gui.widget.NumberEditBox;
import net.tysontheember.apertureapi.common.animation.CameraKeyframe;
import net.tysontheember.apertureapi.common.animation.GlobalCameraPath;
import net.tysontheember.apertureapi.common.animation.PathInterpolator;
import org.joml.Vector3f;

public class PointSettingScreen extends Screen {
  private final NumberEditBox[] numbers = new NumberEditBox[5];
  private CycleButton<PathInterpolator> type;
  private int panelX, panelY, panelW, panelH;
  private static final Component POS = Component.translatable("gui.camera_anim.point_setting.pos");
  private static final Component ROT = Component.translatable("gui.camera_anim.point_setting.rot");
  private static final Component ZOOM =
      Component.translatable("gui.camera_anim.point_setting.zoom");
  private static final Component TIME =
      Component.translatable("gui.camera_anim.point_setting.time");
  private static final Component SAVE =
      Component.translatable("gui.camera_anim.point_setting.save");
  private static final Component TYPE =
      Component.translatable("gui.camera_anim.point_setting.type");
  private static final Component POS_ERROR =
      Component.translatable("gui.camera_anim.point_setting.pos_error");
  private static final Component ROT_ERROR =
      Component.translatable("gui.camera_anim.point_setting.rot_error");
  private static final Component ZOOM_ERROR =
      Component.translatable("gui.camera_anim.point_setting.zoom_error");
  private static final Component TIME_ERROR =
      Component.translatable("gui.camera_anim.point_setting.time_error");
  private static final Component TIP = Component.translatable("gui.camera_anim.point_setting.tip");
  private static final Component INTERPOLATION =
      Component.translatable("gui.camera_anim.point_setting.interpolation");

  public PointSettingScreen() {
    super(Component.literal("Point Setting"));
  }

  @Override
  protected void init() {
    CameraAnimIdeCache.SelectedPoint selectedPoint = CameraAnimIdeCache.getSelectedPoint();
    GlobalCameraPath track = CameraAnimIdeCache.getPath();
    int time = selectedPoint.getPointTime();
    Vector3f pos = selectedPoint.getPosition();
    if (pos == null) return;

    int m = 16; // margin inside the panel
    int s = 8; // spacing
    // panel origin (slightly inset)
    panelX = 12;
    panelY = 12;
    int y = panelY + 4; // content y
    int maxRight = panelX + 4;
    int labelW = 40;
    int boxW = 80;
    int boxH = 16;
    int btnW = 110;

    // Title labels and inputs laid out in rows
    addRenderableOnly(new StringWidget(panelX + 5, y, labelW, 12, POS, font));
    NumberEditBox[] xyz = new NumberEditBox[3];
    int x = panelX + 5 + labelW + s;
    xyz[0] = new NumberEditBox(font, x, y, boxW, boxH, pos.x, Component.literal("x"));
    x += boxW + s;
    xyz[1] = new NumberEditBox(font, x, y, boxW, boxH, pos.y, Component.literal("y"));
    x += boxW + s;
    xyz[2] = new NumberEditBox(font, x, y, boxW, boxH, pos.z, Component.literal("z"));
    x += boxW + s;
    addRenderableWidget(xyz[0]);
    addRenderableWidget(xyz[1]);
    addRenderableWidget(xyz[2]);
    maxRight = Math.max(maxRight, x);

    if (selectedPoint.getControl() == CameraAnimIdeCache.ControlType.NONE) {
      var posInterpBtn =
          net.minecraft.client.gui.components.Button.builder(
                  INTERPOLATION,
                  b -> Minecraft.getInstance().pushGuiLayer(new InterpolationSettingScreen(1)))
              .pos(x, y)
              .size(btnW, boxH)
              .tooltip(Tooltip.create(Component.literal("Open position interpolation settings")))
              .build();
      addRenderableWidget(posInterpBtn);
      maxRight = Math.max(maxRight, x + btnW);
      CameraKeyframe point = track.getPoint(time);
      assert point != null; // pos not null implies point not null
      float fov = point.getFov();
      Vector3f rot = point.getRot();

      // Rot row
      y += boxH + s;
      addRenderableOnly(new StringWidget(panelX + 5, y, labelW, 12, ROT, font));
      x = panelX + 5 + labelW + s;
      numbers[0] = new NumberEditBox(font, x, y, boxW, boxH, rot.x, Component.literal("xRot"));
      x += boxW + s;
      addRenderableWidget(numbers[0]);
      numbers[1] = new NumberEditBox(font, x, y, boxW, boxH, rot.y, Component.literal("yRot"));
      x += boxW + s;
      addRenderableWidget(numbers[1]);
      numbers[2] = new NumberEditBox(font, x, y, boxW, boxH, rot.z, Component.literal("zRot"));
      x += boxW + s;
      addRenderableWidget(numbers[2]);
      maxRight = Math.max(maxRight, x);
      var rotInterpBtn =
          net.minecraft.client.gui.components.Button.builder(
                  INTERPOLATION,
                  b -> Minecraft.getInstance().pushGuiLayer(new InterpolationSettingScreen(2)))
              .pos(x, y)
              .size(btnW, boxH)
              .tooltip(Tooltip.create(Component.literal("Open rotation interpolation settings")))
              .build();
      addRenderableWidget(rotInterpBtn);
      maxRight = Math.max(maxRight, x + btnW);

      // Zoom row
      y += boxH + s;
      addRenderableOnly(new StringWidget(panelX + 5, y, labelW, 12, ZOOM, font));
      x = panelX + 5 + labelW + s;
      numbers[3] = new NumberEditBox(font, x, y, boxW, boxH, fov, Component.literal("zoom"));
      x += boxW + s;
      addRenderableWidget(numbers[3]);
      maxRight = Math.max(maxRight, x);
      var zoomInterpBtn =
          net.minecraft.client.gui.components.Button.builder(
                  INTERPOLATION,
                  b -> Minecraft.getInstance().pushGuiLayer(new InterpolationSettingScreen(3)))
              .pos(x, y)
              .size(btnW, boxH)
              .tooltip(Tooltip.create(Component.literal("Open zoom interpolation settings")))
              .build();
      addRenderableWidget(zoomInterpBtn);
      maxRight = Math.max(maxRight, x + btnW);

      // Path interpolator row + Time
      y += boxH + s;
      type =
          CycleButton.builder(PathInterpolator::getDisplayName)
              .withValues(PathInterpolator.values())
              .withInitialValue(point.getPathInterpolator())
              .create(
                  panelX + 5 + labelW + s,
                  y,
                  100,
                  boxH + 2,
                  TYPE,
                  (btn, value) -> {
                    btn.setTooltip(
                        Tooltip.create(
                            Component.translatable(value.getDisplayNameKey() + ".tooltip")));
                  });
      addRenderableWidget(type);
      // initialize tooltip for current selection
      type.setTooltip(
          Tooltip.create(Component.translatable(type.getValue().getDisplayNameKey() + ".tooltip")));
      addRenderableOnly(new StringWidget(panelX + 5, y + boxH + s, labelW, 12, TIME, font));
      numbers[4] =
          new NumberEditBox(
              font,
              panelX + 5 + labelW + s,
              y + boxH + s,
              boxW,
              boxH,
              time,
              Component.literal("time"));
      addRenderableWidget(numbers[4]);
      maxRight = Math.max(maxRight, panelX + 5 + labelW + s + boxW);
      addRenderableWidget(numbers[4]);
    }

    // Info and Save button row
    y += (boxH + s) * 2;
    StringWidget info = new StringWidget(panelX + 5, y, 240, 12, Component.literal(""), font);
    addRenderableOnly(info);
    addRenderableOnly(new StringWidget(panelX + 4, y + boxH + s, 400, 12, TIP, font));

    int saveW = 100;
    int saveX = Math.max(panelX + 8, maxRight - saveW);
    panelW = (Math.max(maxRight, saveX + saveW) + s) - panelX;
    panelH = (y + boxH + s * 3) - panelY;

    Button button =
        Button.builder(
                SAVE,
                (b) -> {
                  float xn, yn, zn;

                  try {
                    xn = Float.parseFloat(xyz[0].getValue());
                    yn = Float.parseFloat(xyz[1].getValue());
                    zn = Float.parseFloat(xyz[2].getValue());
                  } catch (NumberFormatException e) {
                    info.setMessage(POS_ERROR);
                    return;
                  }

                  switch (selectedPoint.getControl()) {
                    case LEFT, RIGHT -> {
                      pos.set(xn, yn, zn);
                      onClose();
                    }
                    case NONE -> {
                      CameraKeyframe point = track.getPoint(time);
                      assert point != null; // pos不为null，则point不为null
                      pos.set(xn, yn, zn);

                      try {
                        float xRot = Float.parseFloat(numbers[0].getValue());
                        float yRot = Float.parseFloat(numbers[1].getValue());
                        float zRot = Float.parseFloat(numbers[2].getValue());
                        point.getRot().set(xRot, yRot, zRot);
                      } catch (NumberFormatException e) {
                        info.setMessage(ROT_ERROR);
                        return;
                      }

                      try {
                        point.setFov(Float.parseFloat(numbers[3].getValue()));
                      } catch (NumberFormatException e) {
                        info.setMessage(ZOOM_ERROR);
                        return;
                      }

                      if (point.getPathInterpolator() != type.getValue()) {
                        point.setPathInterpolator(type.getValue());
                        track.updateBezier(time);
                      }

                      try {
                        int newTime = Integer.parseInt(numbers[4].getValue());
                        track.setTime(time, newTime);
                        onClose();
                      } catch (NumberFormatException e) {
                        info.setMessage(TIME_ERROR);
                      }
                    }
                  }
                })
            .pos(saveX, y - 2)
            .size(saveW, 20)
            .tooltip(Tooltip.create(Component.literal("Save changes to this keyframe")))
            .build();
    addRenderableWidget(button);
  }

  @Override
  public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    renderBackground(guiGraphics);
    super.render(guiGraphics, mouseX, mouseY, partialTick);
    int x = panelX;
    int y = panelY;
    int w = Math.max(220, Math.min(this.width - 24, panelW));
    int h = Math.max(90, panelH);
    guiGraphics.hLine(x, x + w, y, 0xFF95e1d3);
    guiGraphics.hLine(x, x + w, y + h, 0xFF95e1d3);
    guiGraphics.vLine(x + w, y, y + h, 0xFF95e1d3);
    guiGraphics.vLine(x, y, y + h, 0xFF95e1d3);
  }
}
