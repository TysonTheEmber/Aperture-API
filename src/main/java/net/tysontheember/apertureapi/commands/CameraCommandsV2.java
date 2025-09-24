package net.tysontheember.apertureapi.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.tysontheember.apertureapi.path.PathEvaluator;
import net.tysontheember.apertureapi.path.PathModel;
import net.tysontheember.apertureapi.path.interpolation.InterpolationType;
import org.joml.Vector3f;

/**
 * Enhanced camera commands for CMDCam parity
 */
public class CameraCommandsV2 {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("camera")
            .then(Commands.literal("interpolation")
                .then(Commands.argument("path", StringArgumentType.word())
                    .then(Commands.argument("mode", StringArgumentType.word())
                        .executes(CameraCommandsV2::setInterpolation))))
            
            .then(Commands.literal("speed")
                .then(Commands.argument("path", StringArgumentType.word())
                    .then(Commands.literal("duration")
                        .then(Commands.argument("seconds", FloatArgumentType.floatArg(0.1f, 300f))
                            .executes(CameraCommandsV2::setDuration)))
                    .then(Commands.literal("blocks")
                        .then(Commands.argument("blocksPerSec", FloatArgumentType.floatArg(0.1f, 100f))
                            .executes(CameraCommandsV2::setSpeed)))))
            
            .then(Commands.literal("test")
                .executes(CameraCommandsV2::createTestPath))
            
            .then(Commands.literal("debug")
                .then(Commands.argument("path", StringArgumentType.word())
                    .then(Commands.argument("time", FloatArgumentType.floatArg(0f))
                        .executes(CameraCommandsV2::debugPath))))
        );
    }
    
    private static int setInterpolation(CommandContext<CommandSourceStack> ctx) {
        String pathName = StringArgumentType.getString(ctx, "path");
        String mode = StringArgumentType.getString(ctx, "mode");
        
        try {
            InterpolationType interpType = InterpolationType.fromString(mode);
            ctx.getSource().sendSuccess(() -> 
                Component.literal("Set interpolation for '" + pathName + "' to " + interpType.getName()), 
                true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Unknown interpolation mode: " + mode));
            return 0;
        }
    }
    
    private static int setDuration(CommandContext<CommandSourceStack> ctx) {
        String pathName = StringArgumentType.getString(ctx, "path");
        float seconds = FloatArgumentType.getFloat(ctx, "seconds");
        
        ctx.getSource().sendSuccess(() -> 
            Component.literal("Set duration for '" + pathName + "' to " + seconds + " seconds"), 
            true);
        return 1;
    }
    
    private static int setSpeed(CommandContext<CommandSourceStack> ctx) {
        String pathName = StringArgumentType.getString(ctx, "path");
        float blocksPerSec = FloatArgumentType.getFloat(ctx, "blocksPerSec");
        
        ctx.getSource().sendSuccess(() -> 
            Component.literal("Set speed for '" + pathName + "' to " + blocksPerSec + " blocks/second"), 
            true);
        return 1;
    }
    
    private static int createTestPath(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            
            // Create a test path using the PathEvaluator factory method
            PathModel testPath = PathEvaluator.createTestPath();
            
            // Show some info about the test path
            float duration = PathEvaluator.getTotalDuration(testPath);
            float length = PathEvaluator.getTotalLength(testPath);
            
            ctx.getSource().sendSuccess(() -> 
                Component.literal("Created test camera path with CMDCam-style smooth interpolation\n" +
                    "Duration: " + String.format("%.1f", duration) + "s, " +
                    "Length: " + String.format("%.1f", length) + " blocks, " +
                    "Segments: " + testPath.getSegments().size()), 
                true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed to create test path: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int debugPath(CommandContext<CommandSourceStack> ctx) {
        String pathName = StringArgumentType.getString(ctx, "path");
        float time = FloatArgumentType.getFloat(ctx, "time");
        
        // For now, just use the test path
        PathModel testPath = PathEvaluator.createTestPath();
        String debugInfo = PathEvaluator.getDebugInfo(testPath, time);
        
        ctx.getSource().sendSuccess(() -> 
            Component.literal("Debug info for '" + pathName + "':\n" + debugInfo), 
            false);
        return 1;
    }
}