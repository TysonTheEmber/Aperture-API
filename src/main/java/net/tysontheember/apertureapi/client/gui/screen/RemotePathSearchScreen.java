package net.tysontheember.apertureapi.client.gui.screen;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.tysontheember.apertureapi.client.CameraAnimIdeCache;
import net.tysontheember.apertureapi.client.gui.widget.NumberEditBox;
import net.tysontheember.apertureapi.client.network.ClientPayloadSender;
import net.tysontheember.apertureapi.common.animation.GlobalCameraPath;
import net.tysontheember.apertureapi.common.data_entity.GlobalCameraPathInfo;
import oshi.util.tuples.Triplet;

public class RemotePathSearchScreen extends Screen {
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
  private static final ZoneId ZONE_ID = ZoneId.systemDefault();
  private static final Component PAGE =
      Component.translatable("gui.camera_anim.remote_path_search.page");
  private static final Component SEARCH =
      Component.translatable("gui.camera_anim.remote_path_search.search");
  private static final Component LOAD =
      Component.translatable("gui.camera_anim.remote_path_search.load");
  private static final Component LOAD_ID =
      Component.translatable("gui.camera_anim.remote_path_search.load_id");
  private static final Component SAVE =
      Component.translatable("gui.camera_anim.remote_path_search.save");
  private static final Component SAVE_ID =
      Component.translatable("gui.camera_anim.remote_path_search.save_id");
  private static final Component DELETE =
      Component.translatable("gui.camera_anim.remote_path_search.delete");
  private static final Component DELETE_ID =
      Component.translatable("gui.camera_anim.remote_path_search.delete_id");
  private static final Component LOCAL_MODE =
      Component.translatable("gui.camera_anim.remote_path_search.local_mode");
  private static final Component PATH_ID =
      Component.translatable("gui.camera_anim.remote_path_search.path_id");
  private static final Component MODIFIER =
      Component.translatable("gui.camera_anim.remote_path_search.modifier");
  private static final Component TIME =
      Component.translatable("gui.camera_anim.remote_path_search.time");
  private static final Component NO_SERVER =
      Component.translatable("gui.camera_anim.remote_path_search.no_server");
  private static final Component TIP =
      Component.translatable("gui.camera_anim.remote_path_search.tip");
  public static boolean REMOTE;

  private final List<Triplet<Component, Component, Component>> list = new ArrayList<>();
  private EditBox filterBox;
  private EditBox pathBox;
  private EditBox newIdBox;
  private EditBox removeIdBox;
  private int scrollOffset = 0;
  private int lastClickIndex = -1;
  private long lastClickTimeMs = 0L;

  public RemotePathSearchScreen() {
    super(Component.literal("remote path search"));
  }

  @Override
  protected void init() {
    int m = 20;
    int y = m;
    int rowH = 20;
    int s = 10;
    int avail = this.width - m * 2;
    // Fixed widths
    int pageW = 40, searchW = 100, loadBtnW = 60, saveBtnW = 60, deleteBtnW = 60, modeW = 120;
    int gaps = 8; // page|search|path|load|newId|save|removeId|delete|mode -> 8 gaps
    int remaining = avail - (pageW + searchW + loadBtnW + saveBtnW + deleteBtnW + modeW) - s * gaps;
    int flexW = Math.max(100, remaining / 3); // pathBox, newIdBox, removeIdBox

    int x = m;
    NumberEditBox page =
        addRenderableWidget(
            new NumberEditBox(font, x, y, pageW, rowH, 1, Component.literal("page")));
    x += pageW + s;
    Component RELOAD = Component.translatable("gui.camera_anim.remote_path_search.reload");
    addRenderableWidget(
        Button.builder(
                RELOAD,
                b -> {
                  if (REMOTE) {
                    ClientPayloadSender.checkGlobalPath(Integer.parseInt(page.getValue()), 128);
                  }
                })
            .pos(x, y)
            .size(searchW, rowH)
            .tooltip(Tooltip.create(Component.literal("Refresh list from server")))
            .build());
    x += searchW + s;
    pathBox =
        addRenderableWidget(new EditBox(font, x, y, flexW, rowH, Component.literal("path id")));
    x += flexW + s;
    addRenderableWidget(
        Button.builder(
                LOAD,
                b -> {
                  if (REMOTE) {
                    ClientPayloadSender.getGlobalPath(pathBox.getValue(), 0);
                  }
                })
            .pos(x, y)
            .size(loadBtnW, rowH)
            .tooltip(
                Tooltip.create(Component.literal("Load animation with the given ID from server")))
            .build());
    x += loadBtnW + s;
    newIdBox =
        addRenderableWidget(new EditBox(font, x, y, flexW, rowH, Component.literal("new id")));
    newIdBox.setValue(CameraAnimIdeCache.getPath().getId());
    x += flexW + s;
    addRenderableWidget(
        Button.builder(
                SAVE,
                b -> {
                  if (REMOTE) {
                    GlobalCameraPath track = CameraAnimIdeCache.getPath();
                    if (!track.getId().equals(newIdBox.getValue())) {
                      track = track.resetID(newIdBox.getValue());
                    }
                    if (track.isNativeMode()) {
                      track =
                          track.toNative(
                              CameraAnimIdeCache.getNativePos(),
                              CameraAnimIdeCache.getNativeRot().y);
                    }
                    ClientPayloadSender.putGlobalPath(track);
                  }
                })
            .pos(x, y)
            .size(saveBtnW, rowH)
            .tooltip(Tooltip.create(Component.literal("Save current path to server")))
            .build());
    x += saveBtnW + s;
    removeIdBox =
        addRenderableWidget(new EditBox(font, x, y, flexW, rowH, Component.literal("remove id")));
    x += flexW + s;
    addRenderableWidget(
        Button.builder(
                DELETE,
                b -> {
                  if (REMOTE) {
                    ClientPayloadSender.removeGlobalPath(removeIdBox.getValue());
                  }
                })
            .pos(x, y)
            .size(deleteBtnW, rowH)
            .tooltip(Tooltip.create(Component.literal("Delete animation by ID on server")))
            .build());
    // Mode switch on far right
    addRenderableWidget(
        Button.builder(
                LOCAL_MODE, b -> Minecraft.getInstance().setScreen(new LocalPathSearchScreen()))
            .pos(this.width - m - modeW, y)
            .size(modeW, rowH)
            .tooltip(Tooltip.create(Component.literal("Switch to Local mode")))
            .build());

    // Filter full width under actions
    y += rowH + 20;
    filterBox =
        addRenderableWidget(
            new EditBox(font, m, y, this.width - m * 2, 18, Component.literal("filter")));
    filterBox.setHint(Component.literal("Search..."));
    scrollOffset = 0;

    // Auto-load first page by default
    if (REMOTE) {
      ClientPayloadSender.checkGlobalPath(1, 128);
    }
  }

  @Override
  public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    renderBackground(guiGraphics);
    super.render(guiGraphics, mouseX, mouseY, partialTick);
    int m = 20;
    int avail = this.width - m * 2;
    int idW = (int) (avail * 0.5f);
    int modW = (int) (avail * 0.2f);
    int timeW = avail - idW - modW;
    int idX = m;
    int modX = m + idW + 10;
    int timeX = modX + modW + 10;

    guiGraphics.drawString(font, PAGE, m + 5, 10, 0xffffffff);
    // Center labels above their inputs
    int loadLabelX = pathBox.getX() + pathBox.getWidth() / 2 - font.width(LOAD_ID) / 2;
    int saveLabelX = newIdBox.getX() + newIdBox.getWidth() / 2 - font.width(SAVE_ID) / 2;
    int deleteLabelX = removeIdBox.getX() + removeIdBox.getWidth() / 2 - font.width(DELETE_ID) / 2;
    guiGraphics.drawString(font, LOAD_ID, loadLabelX, 10, 0xffffffff);
    guiGraphics.drawString(font, SAVE_ID, saveLabelX, 10, 0xffffffff);
    guiGraphics.drawString(font, DELETE_ID, deleteLabelX, 10, 0xffffffff);

    // Headers
    int headerY = (int) (m + 20 + 20 + 24);
    guiGraphics.drawString(font, PATH_ID, idX, headerY, 0xffffffff);
    guiGraphics.drawString(font, MODIFIER, modX, headerY, 0xffffffff);
    guiGraphics.drawString(font, TIME, timeX, headerY, 0xffffffff);

    List<Triplet<Component, Component, Component>> filtered = new ArrayList<>();
    String q = filterBox.getValue() == null ? "" : filterBox.getValue().toLowerCase(Locale.ROOT);
    for (Triplet<Component, Component, Component> t : list) {
      String id = t.getA().getString().toLowerCase(Locale.ROOT);
      if (q.isEmpty() || id.contains(q)) filtered.add(t);
    }
    int headerY2 = (int) (m + 20 + 20 + 24);
    int top = headerY2 + 12;
    int rowH = 11;
    int visible = Math.max(0, (this.height - top - 40) / rowH);
    scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, filtered.size() - visible)));

    if (!filtered.isEmpty()) {
      for (int i = 0; i < visible && (i + scrollOffset) < filtered.size(); i++) {
        Triplet<Component, Component, Component> info = filtered.get(i + scrollOffset);
        int y = top + i * rowH;
        guiGraphics.drawString(font, info.getA(), idX, y, 0xffffffff);
        guiGraphics.drawString(font, info.getB(), modX, y, 0xffffffff);
        guiGraphics.drawString(font, info.getC(), timeX, y, 0xffffffff);
      }
    } else {
      guiGraphics.drawCenteredString(font, TIP, this.width / 2, this.height / 2, 0xffffffff);
    }

    if (!REMOTE) {
      guiGraphics.drawCenteredString(font, NO_SERVER, 100, 100, 0xffE11414);
    }
  }

  public void setInfo(List<GlobalCameraPathInfo> list) {
    this.list.clear();

    for (GlobalCameraPathInfo info : list) {
      Component id = Component.literal(info.id());
      Player player = Minecraft.getInstance().level.getPlayerByUUID(info.lastModifier());
      Component playerName;

      if (player == null) {
        playerName = Component.literal("未知");
      } else {
        playerName = player.getDisplayName();
      }

      LocalDateTime localDateTime =
          LocalDateTime.ofInstant(Instant.ofEpochMilli(info.version()), ZONE_ID);
      Component time = Component.literal(FORMATTER.format(localDateTime));
      this.list.add(new Triplet<>(id, playerName, time));
    }
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    int m = 20;
    int headerY2 = (int) (m + 20 + 20 + 24);
    int top = headerY2 + 12;
    int rowH = 11;
    int visible = Math.max(0, (this.height - top - 40) / rowH);
    int max = Math.max(0, list.size() - visible);
    scrollOffset = (int) Math.max(0, Math.min(max, scrollOffset - Math.signum(delta)));
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    int m = 20;
    int headerY2 = (int) (m + 20 + 20 + 24);
    int top = headerY2 + 12;
    int rowH = 11;
    int right = this.width - m;
    if (mouseY >= top && mouseX >= m && mouseX <= right) {
      int index = (int) ((mouseY - top) / rowH) + scrollOffset;
      String q = filterBox.getValue() == null ? "" : filterBox.getValue().toLowerCase(Locale.ROOT);
      List<Triplet<Component, Component, Component>> filtered = new ArrayList<>();
      for (Triplet<Component, Component, Component> t : list) {
        String id = t.getA().getString().toLowerCase(Locale.ROOT);
        if (q.isEmpty() || id.contains(q)) filtered.add(t);
      }
      if (index >= 0 && index < filtered.size()) {
        String id = filtered.get(index).getA().getString();
        if (pathBox != null) pathBox.setValue(id);
        if (removeIdBox != null) removeIdBox.setValue(id);
        long now = System.currentTimeMillis();
        if (button == 0 && index == lastClickIndex && now - lastClickTimeMs < 300) {
          if (REMOTE) {
            ClientPayloadSender.getGlobalPath(id, 0);
          }
        }
        lastClickIndex = index;
        lastClickTimeMs = now;
      }
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }
}
