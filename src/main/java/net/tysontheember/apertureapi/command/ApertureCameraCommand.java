package net.tysontheember.apertureapi.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.apertureapi.ApertureAPI;
import net.tysontheember.apertureapi.camera.CameraPathRegistry;

import java.util.concurrent.CompletableFuture;

public class ApertureCameraCommand {

    private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (CameraPathRegistry.all().isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("No camera paths loaded."), false);
        } else {
            String ids = String.join(", ", CameraPathRegistry.all().keySet());
            ctx.getSource().sendSuccess(() -> Component.literal("Camera paths: " + ids), false);
        }
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestPathIds(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b) {
        for (String id : CameraPathRegistry.all().keySet()) {
            if (id.startsWith(b.getRemaining())) b.suggest(id);
        }
        return b.buildFuture();
    }

    private static int preview(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "pathId");
        if (!CameraPathRegistry.exists(id)) {
            ctx.getSource().sendFailure(Component.literal("Unknown camera path: " + id));
            return 0;
        }
        // Send a client-bound instruction to start local preview
        var player = ctx.getSource().getPlayerOrException();
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("id", id);
        net.tysontheember.apertureapi.common.network.ServerPayloadSender.send("playJsonPath", tag, player);
        ctx.getSource().sendSuccess(() -> Component.literal("Previewing path '" + id + "'"), false);
        return 1;
    }
}
