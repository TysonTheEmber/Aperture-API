package net.tysontheember.apertureapi.common.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.tysontheember.apertureapi.common.GlobalCameraSavedData;
import net.tysontheember.apertureapi.common.animation.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class PathExportCommand {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("aperture")
            .then(Commands.literal("path")
                .then(Commands.literal("export")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .executes(PathExportCommand::exportOne)))
                .then(Commands.literal("export-all")
                    .executes(PathExportCommand::exportAll))
            )
        );
        // Alias: /camera mirrors /aperture
        dispatcher.register(Commands.literal("camera")
            .then(Commands.literal("path")
                .then(Commands.literal("export")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .executes(PathExportCommand::exportOne)))
                .then(Commands.literal("export-all")
                    .executes(PathExportCommand::exportAll))
            )
        );
    }

    private static int exportOne(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "id");
        ServerLevel level = ctx.getSource().getLevel();
        GlobalCameraSavedData data = GlobalCameraSavedData.getData(level);
        GlobalCameraPath path = data.getPath(id);
        if (path == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown camera path: " + id));
            return 0;
        }
        try {
            Path out = writePathAsJson(level, path);
            ctx.getSource().sendSuccess(() -> Component.literal("Exported '" + id + "' to " + out), false);
            return 1;
        } catch (IOException e) {
            ctx.getSource().sendFailure(Component.literal("Failed to export '" + id + "': " + e.getMessage()));
            return 0;
        }
    }

    private static int exportAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        GlobalCameraSavedData data = GlobalCameraSavedData.getData(level);
        int count = 0;
        IOException last = null;
        for (GlobalCameraPath path : data.getPaths()) {
            try {
                writePathAsJson(level, path);
                count++;
            } catch (IOException e) {
                last = e;
            }
        }
        if (count > 0) {
            final int total = count;
            ctx.getSource().sendSuccess(() -> Component.literal("Exported " + total + " path(s) to world/data/apertureapi/paths"), false);
            return count;
        } else {
            ctx.getSource().sendFailure(Component.literal("No paths exported" + (last != null ? (": " + last.getMessage()) : "")));
            return 0;
        }
    }

    private static Path ensureDir(ServerLevel level) throws IOException {
        // saves/<world>/data/apertureapi/paths
        Path dir = level.getServer()
                .getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("data").resolve("apertureapi").resolve("paths");
        Files.createDirectories(dir);
        return dir;
    }

    private static String safeFileName(String id) {
        // Allow alphanumerics, underscore, dot, and hyphen
        return id.replaceAll("[^a-zA-Z0-9_.-]+", "_");
    }

    private static Path writePathAsJson(ServerLevel level, GlobalCameraPath path) throws IOException {
        JsonObject root = toJson(path);
        Path dir = ensureDir(level);
        Path file = dir.resolve(safeFileName(path.getId()) + ".json");
        String json = GSON.toJson(root);
        // atomic-ish write
        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        Files.writeString(tmp, json, StandardCharsets.UTF_8);
        Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return file;
    }

    private static JsonObject toJson(GlobalCameraPath path) {
        JsonObject root = new JsonObject();
        root.addProperty("id", path.getId());
        root.addProperty("version", path.getVersion());
        UUID last = path.getLastModifier();
        root.addProperty("lastModifier", last == null ? "00000000-0000-0000-0000-000000000000" : last.toString());
        root.addProperty("native", path.isNativeMode());

        JsonArray keyframes = new JsonArray();
        for (Int2ObjectMap.Entry<CameraKeyframe> e : path.getEntries()) {
            int time = e.getIntKey();
            CameraKeyframe kf = e.getValue();
            JsonObject k = new JsonObject();
            k.addProperty("time", time);

            // pos
            JsonArray pos = new JsonArray();
            Vector3f p = kf.getPos();
            pos.add(p.x); pos.add(p.y); pos.add(p.z);
            k.add("pos", pos);

            // rot
            JsonArray rot = new JsonArray();
            Vector3f r = kf.getRot();
            rot.add(r.x); rot.add(r.y); rot.add(r.z);
            k.add("rot", rot);

            k.addProperty("fov", kf.getFov());
            k.addProperty("pathType", kf.getPathInterpolator().index);
            k.addProperty("posType", kf.getPosTimeInterpolator().index);
            k.addProperty("rotType", kf.getRotTimeInterpolator().index);
            k.addProperty("fovType", kf.getFovTimeInterpolator().index);

            if (kf.getPathInterpolator() == PathInterpolator.BEZIER) {
                JsonObject bez = new JsonObject();
                JsonArray left = new JsonArray();
                JsonArray right = new JsonArray();
                Vector3f L = kf.getPathBezier().getLeft();
                Vector3f R = kf.getPathBezier().getRight();
                left.add(L.x); left.add(L.y); left.add(L.z);
                right.add(R.x); right.add(R.y); right.add(R.z);
                bez.add("left", left);
                bez.add("right", right);
                k.add("pathBezier", bez);
            }

            if (kf.getPosTimeInterpolator() == TimeInterpolator.BEZIER) {
                k.add("posBezier", bez2(kf.getPosBezier()));
            }
            if (kf.getRotTimeInterpolator() == TimeInterpolator.BEZIER) {
                k.add("rotBezier", bez2(kf.getRotBezier()));
            }
            if (kf.getFovTimeInterpolator() == TimeInterpolator.BEZIER) {
                k.add("fovBezier", bez2(kf.getFovBezier()));
            }

            JsonObject entry = new JsonObject();
            entry.addProperty("time", time);
            entry.add("keyframe", k);
            keyframes.add(entry);
        }
        root.add("keyframes", keyframes);
        return root;
    }

    private static JsonObject bez2(TimeBezierController c) {
        JsonObject o = new JsonObject();
        Vector2f L = c.getLeft();
        Vector2f R = c.getRight();
        JsonArray left = new JsonArray();
        JsonArray right = new JsonArray();
        left.add(L.x); left.add(L.y);
        right.add(R.x); right.add(R.y);
        o.add("left", left);
        o.add("right", right);
        return o;
    }
}