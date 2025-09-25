package net.tysontheember.apertureapi.client.gui.screen;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;
import net.tysontheember.apertureapi.client.CameraAnimIdeCache;
import net.tysontheember.apertureapi.client.ClientUtil;
import net.tysontheember.apertureapi.client.gui.widget.NumberEditBox;
import net.tysontheember.apertureapi.common.animation.CameraKeyframe;
import net.tysontheember.apertureapi.common.animation.GlobalCameraPath;
import oshi.util.tuples.Triplet;

public class LocalPathSearchScreen extends Screen {
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
  private static final ZoneId ZONE_ID = ZoneId.systemDefault();
  private final List<Triplet<Component, Component, Component>> list = new ArrayList<>();
  private EditBox filterBox;
  private EditBox pathBox;
  private EditBox newIdBox;
  private int scrollOffset = 0;
  private int lastClickIndex = -1;
  private long lastClickTimeMs = 0L;
  private static final Gson GSON = new Gson();
  private static final String SERIALIZER_VERSION = "1.0.0";
  private static final Component PAGE =
      Component.translatable("gui.camera_anim.local_path_search.page");
  private static final Component SEARCH =
      Component.translatable("gui.camera_anim.local_path_search.search");
  private static final Component LOAD =
      Component.translatable("gui.camera_anim.local_path_search.load");
  private static final Component LOAD_ID =
      Component.translatable("gui.camera_anim.local_path_search.load_id");
  private static final Component SAVE =
      Component.translatable("gui.camera_anim.local_path_search.save");
  private static final Component SAVE_ID =
      Component.translatable("gui.camera_anim.local_path_search.save_id");
  private static final Component REMOTE_MODE =
      Component.translatable("gui.camera_anim.local_path_search.remote_mode");
  private static final Component PATH_ID =
      Component.translatable("gui.camera_anim.local_path_search.path_id");
  private static final Component MODIFIER =
      Component.translatable("gui.camera_anim.local_path_search.modifier");
  private static final Component TIME =
      Component.translatable("gui.camera_anim.local_path_search.time");
  private static final Component TIP =
      Component.translatable("gui.camera_anim.local_path_search.tip");
  private static final Component LOCAL_FILE =
      Component.translatable("gui.camera_anim.local_path_search.local_file");
  private static final Component LOAD_ERROR =
      Component.translatable("gui.camera_anim.local_path_search.load_error");
  private static final Component VERSION_ERROR =
      Component.translatable("gui.camera_anim.local_path_search.version_error");
  private static final Component FILE_LOAD_ERROR =
      Component.translatable("gui.camera_anim.local_path_search.file_load_error");
  private static final Component FILE_FORMAT_ERROR =
      Component.translatable("gui.camera_anim.local_path_search.file_format_error");
  private static final Component FILE_EXIST_ERROR =
      Component.translatable("gui.camera_anim.local_path_search.file_exist_error");
  private static final Component FILE_SAVE_ERROR =
      Component.translatable("gui.camera_anim.local_path_search.file_save_error");
  private static final Component FILE_LOAD_SUCCESS =
      Component.translatable("gui.camera_anim.local_path_search.file_load_success");

  public LocalPathSearchScreen() {
    super(Component.literal("local path search"));
  }

  @Override
  protected void init() {
    int m = 20; // margin
    int s = 10; // spacing
    int y = m;
    int rowH = 20;
    int avail = this.width - m * 2;

    // Compute widths to avoid overlap
    int pageW = 40;
    int searchW = 100;
    int loadBtnW = 60;
    int saveBtnW = 60;
    int modeW = 120;
    int gaps = 6; // page|search|loadId|load|saveId|save|mode => 6 gaps
    int remaining = avail - (pageW + searchW + loadBtnW + saveBtnW + modeW) - s * gaps;
    int flexW = Math.max(100, remaining / 2); // for loadId and saveId

    int x = m;
    NumberEditBox page =
        addRenderableWidget(
            new NumberEditBox(font, x, y, pageW, rowH, 1, Component.literal("page")));
    x += pageW + s;
    addRenderableWidget(
        Button.builder(
                SEARCH,
                b -> {
                  searchFromFile();
                })
            .pos(x, y)
            .size(searchW, rowH)
            .tooltip(Tooltip.create(Component.literal("Refreshes the local animations list")))
            .build());
    x += searchW + s;
    pathBox =
        addRenderableWidget(new EditBox(font, x, y, flexW, rowH, Component.literal("path id")));
    x += flexW + s;
    addRenderableWidget(
        Button.builder(LOAD, b -> getFromFile(pathBox.getValue()))
            .pos(x, y)
            .size(loadBtnW, rowH)
            .tooltip(Tooltip.create(Component.literal("Load the animation with the given ID")))
            .build());
    x += loadBtnW + s;
    newIdBox =
        addRenderableWidget(new EditBox(font, x, y, flexW, rowH, Component.literal("new id")));
    newIdBox.setValue(CameraAnimIdeCache.getPath().getId());
    x += flexW + s;
    addRenderableWidget(
        Button.builder(SAVE, b -> saveToFile(newIdBox.getValue()))
            .pos(x, y)
            .size(saveBtnW, rowH)
            .tooltip(
                Tooltip.create(
                    Component.literal("Save the current path to a local file with this ID")))
            .build());
    // Mode switch on far right
    addRenderableWidget(
        Button.builder(
                REMOTE_MODE, b -> Minecraft.getInstance().setScreen(new RemotePathSearchScreen()))
            .pos(this.width - m - modeW, y)
            .size(modeW, rowH)
            .tooltip(Tooltip.create(Component.literal("Switch to Remote mode")))
            .build());

    // Filter spanning full width
    y += rowH + 20;
    filterBox =
        addRenderableWidget(
            new EditBox(font, m, y, this.width - m * 2, 18, Component.literal("filter")));
    filterBox.setHint(Component.literal("Search..."));
    scrollOffset = 0;

    searchFromFile();
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
    guiGraphics.drawString(font, LOAD_ID, m + 205, 10, 0xffffffff);
    guiGraphics.drawString(font, SAVE_ID, m + 365, 10, 0xffffffff);

    // Headers
    int headerY = (int) (m + 20 + 20 + 24); // top margin + rowH + spacing + label offset
    guiGraphics.drawString(font, PATH_ID, idX, headerY, 0xffffffff);
    guiGraphics.drawString(font, MODIFIER, modX, headerY, 0xffffffff);
    guiGraphics.drawString(font, TIME, timeX, headerY, 0xffffffff);

    // Filter entries and draw scrollable list
    List<Triplet<Component, Component, Component>> filtered = new ArrayList<>();
    String q = filterBox.getValue() == null ? "" : filterBox.getValue().toLowerCase(Locale.ROOT);
    for (Triplet<Component, Component, Component> t : list) {
      String id = t.getA().getString().toLowerCase(Locale.ROOT);
      if (q.isEmpty() || id.contains(q)) filtered.add(t);
    }
    int top = headerY + 12;
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
  }

  private void searchFromFile() {
    Path path = FMLPaths.GAMEDIR.get().resolve("camera-anim");
    list.clear();

    if (path.toFile().exists()) {
      try {
        Files.walkFileTree(
            path,
            new SimpleFileVisitor<>() {
              private int count = 0;

              @Override
              public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                  throws IOException {
                String name = file.getFileName().toString();

                if (name.endsWith(".json")) {
                  list.add(
                      new Triplet<>(
                          Component.literal(name.substring(0, name.length() - 5)),
                          LOCAL_FILE,
                          Component.literal(
                              FORMATTER.format(
                                  LocalDateTime.ofInstant(
                                      Instant.ofEpochMilli(attrs.lastModifiedTime().toMillis()),
                                      ZONE_ID)))));
                }

                return FileVisitResult.CONTINUE;
              }
            });
      } catch (IOException e) {
        ClientUtil.pushGuiLayer(new InfoScreen(LOAD_ERROR));
      }
    }
  }

  private void getFromFile(String id) {
    Path path = FMLPaths.GAMEDIR.get().resolve("camera-anim").resolve(id + ".json");
    File file = path.toFile();

    if (file.exists()) {
      try {
        String json = Files.readString(path);
        JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);

        if (jsonObject.has("version")
            && jsonObject.get("version").getAsString().equals(SERIALIZER_VERSION)) {
          TypeToken<TreeMap<Integer, CameraKeyframe>> type = new TypeToken<>() {};
          TreeMap<Integer, CameraKeyframe> map =
              GSON.fromJson(jsonObject.get("anim"), type.getType());
          CameraAnimIdeCache.setPath(new GlobalCameraPath(map, id));
          ClientUtil.pushGuiLayer(new InfoScreen(FILE_LOAD_SUCCESS));
        } else {
          ClientUtil.pushGuiLayer(new InfoScreen(VERSION_ERROR));
        }
      } catch (IOException e) {
        ClientUtil.pushGuiLayer(new InfoScreen(FILE_LOAD_ERROR));
      } catch (JsonSyntaxException | NullPointerException e) {
        ClientUtil.pushGuiLayer(new InfoScreen(FILE_FORMAT_ERROR));
      }
    } else {
      ClientUtil.pushGuiLayer(new InfoScreen(FILE_EXIST_ERROR));
    }
  }

  private void saveToFile(String id) {
    Path path = FMLPaths.GAMEDIR.get().resolve("camera-anim").resolve(id + ".json");
    try {
      JsonObject json =
          GSON.fromJson(CameraAnimIdeCache.getPath().toJsonString(GSON), JsonObject.class);
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("version", SERIALIZER_VERSION);
      jsonObject.add("anim", json);
      Files.writeString(path, jsonObject.toString());
    } catch (IOException e) {
      ClientUtil.pushGuiLayer(new InfoScreen(FILE_SAVE_ERROR));
    }
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    int m = 20;
    int headerY = (int) (m + 20 + 20 + 24);
    int top = headerY + 12;
    int rowH = 11;
    int visible = Math.max(0, (this.height - top - 40) / rowH);
    int max = Math.max(0, list.size() - visible);
    scrollOffset = (int) Math.max(0, Math.min(max, scrollOffset - Math.signum(delta)));
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    // Click on an entry to populate path id
    int m = 20;
    int headerY = (int) (m + 20 + 20 + 24);
    int top = headerY + 12;
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
        pathBox.setValue(id);
        long now = System.currentTimeMillis();
        if (button == 0 && index == lastClickIndex && now - lastClickTimeMs < 300) {
          // Double-click: load immediately
          getFromFile(id);
        }
        lastClickIndex = index;
        lastClickTimeMs = now;
      }
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }
}
