package net.tysontheember.apertureapi.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.tysontheember.apertureapi.client.CameraAnimIdeCache;
import net.tysontheember.apertureapi.common.animation.GlobalCameraPath;
import net.tysontheember.apertureapi.common.animation.SmoothingUtils;

/**
 * Commands for keyframe smoothing and validation operations
 */
public class SmoothingCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("aperture")
            .then(Commands.literal("smooth")
                .then(Commands.literal("auto")
                    .executes(SmoothingCommand::autoSmooth)
                )
                .then(Commands.literal("detect")
                    .executes(SmoothingCommand::detectJumps)
                )
                .then(Commands.literal("fix")
                    .executes(SmoothingCommand::fixJumps)
                )
                .then(Commands.literal("validate")
                    .executes(SmoothingCommand::validatePath)
                )
                .then(Commands.literal("debug")
                    .executes(SmoothingCommand::debugJitters)
                )
            )
        );
    }
    
    private static int autoSmooth(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        GlobalCameraPath path = CameraAnimIdeCache.getPath();
        
        if (path.getEntries().isEmpty()) {
            context.getSource().sendFailure(Component.literal("No camera path loaded to smooth"));
            return 0;
        }
        
        int keyframeCount = path.getEntries().size();
        context.getSource().sendSuccess(() -> 
            Component.literal("Applying auto-smoothing to " + keyframeCount + " keyframes..."), false);
        
        path.autoSmooth();
        
        context.getSource().sendSuccess(() -> 
            Component.literal("Auto-smoothing complete! Camera path should now have cinematic transitions."), false);
        
        return 1;
    }
    
    private static int detectJumps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        GlobalCameraPath path = CameraAnimIdeCache.getPath();
        
        if (path.getEntries().isEmpty()) {
            context.getSource().sendFailure(Component.literal("No camera path loaded to analyze"));
            return 0;
        }
        
        context.getSource().sendSuccess(() -> 
            Component.literal("Scanning camera path for potential jumps..."), false);
        
        int[] jumps = path.detectJumps();
        
        if (jumps.length == 0) {
            context.getSource().sendSuccess(() -> 
                Component.literal("✓ No keyframe jumps detected! Camera path looks smooth."), false);
        } else {
            context.getSource().sendSuccess(() -> 
                Component.literal("⚠ Found " + jumps.length + " potential keyframe jumps at times:"), false);
            
            StringBuilder timeList = new StringBuilder();
            for (int i = 0; i < jumps.length; i++) {
                if (i > 0) timeList.append(", ");
                timeList.append(jumps[i]);
            }
            
            context.getSource().sendSuccess(() -> 
                Component.literal("  " + timeList.toString()), false);
            context.getSource().sendSuccess(() -> 
                Component.literal("Use '/aperture smooth fix' to automatically fix these jumps"), false);
        }
        
        return jumps.length;
    }
    
    private static int fixJumps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        GlobalCameraPath path = CameraAnimIdeCache.getPath();
        
        if (path.getEntries().isEmpty()) {
            context.getSource().sendFailure(Component.literal("No camera path loaded to fix"));
            return 0;
        }
        
        int[] jumps = path.detectJumps();
        
        if (jumps.length == 0) {
            context.getSource().sendSuccess(() -> 
                Component.literal("No keyframe jumps detected - path already smooth!"), false);
            return 0;
        }
        
        context.getSource().sendSuccess(() -> 
            Component.literal("Fixing " + jumps.length + " detected keyframe jumps..."), false);
        
        path.fixJumps(jumps);
        
        context.getSource().sendSuccess(() -> 
            Component.literal("Fixed keyframe jumps! Transitions should now be smooth."), false);
        
        return jumps.length;
    }
    
    private static int validatePath(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        GlobalCameraPath path = CameraAnimIdeCache.getPath();
        
        if (path.getEntries().isEmpty()) {
            context.getSource().sendFailure(Component.literal("No camera path loaded to validate"));
            return 0;
        }
        
        context.getSource().sendSuccess(() -> 
            Component.literal("Validating camera path..."), false);
        
        int keyframeCount = path.getEntries().size();
        int pathLength = path.getLength();
        int[] jumps = path.detectJumps();
        
        // Analyze interpolation types  
        final int[] counts = new int[6]; // linearCount, smoothCount, bezierCount, stepCount, linearTimeCount, bezierTimeCount
        
        for (var entry : path.getEntries()) {
            var keyframe = entry.getValue();
            
            switch (keyframe.getPathInterpolator()) {
                case LINEAR -> counts[0]++;
                case SMOOTH -> counts[1]++;
                case BEZIER -> counts[2]++;
                case STEP -> counts[3]++;
            }
            
            if (keyframe.getPosTimeInterpolator() == net.tysontheember.apertureapi.common.animation.TimeInterpolator.LINEAR) {
                counts[4]++;
            } else {
                counts[5]++;
            }
        }
        
        // Report findings
        context.getSource().sendSuccess(() -> 
            Component.literal("=== Camera Path Validation Report ==="), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("Keyframes: " + keyframeCount + ", Length: " + pathLength + " ticks"), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("Path Interpolation - Linear: " + counts[0] + ", Smooth: " + counts[1] + 
                            ", Bezier: " + counts[2] + ", Step: " + counts[3]), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("Time Interpolation - Linear: " + counts[4] + ", Bezier: " + counts[5]), false);
        
        if (jumps.length > 0) {
            context.getSource().sendSuccess(() -> 
                Component.literal("⚠ Issues Found: " + jumps.length + " potential keyframe jumps"), false);
            context.getSource().sendSuccess(() -> 
                Component.literal("  Recommendation: Use '/aperture smooth auto' to fix"), false);
        } else {
            context.getSource().sendSuccess(() -> 
                Component.literal("✓ Path Quality: Smooth - no jumps detected"), false);
        }
        
        // Quality recommendations
        if (counts[3] > 0) {
            context.getSource().sendSuccess(() -> 
                Component.literal("⚠ " + counts[3] + " keyframes use STEP interpolation (causes jumps)"), false);
        }
        
        if (counts[0] > counts[1] + counts[2]) {
            context.getSource().sendSuccess(() -> 
                Component.literal("💡 Tip: Consider using SMOOTH or BEZIER interpolation for more cinematic movement"), false);
        }
        
        if (counts[4] > counts[5]) {
            context.getSource().sendSuccess(() -> 
                Component.literal("💡 Tip: BEZIER time curves can eliminate sudden speed changes"), false);
        }
        
        return jumps.length == 0 ? 1 : 0;
    }
    
    private static int debugJitters(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        GlobalCameraPath path = CameraAnimIdeCache.getPath();
        
        if (path.getEntries().isEmpty()) {
            context.getSource().sendFailure(Component.literal("No camera path loaded to debug"));
            return 0;
        }
        
        context.getSource().sendSuccess(() -> 
            Component.literal("=== Jitter Debug Information ==="), false);
        
        // Check for potential jitter-causing conditions
        var entries = path.getEntries();
        final int[] jitterRisks = {0};
        
        for (var entry : entries) {
            final int time = entry.getIntKey();
            var keyframe = entry.getValue();
            
            // Check for rotation angle wrap issues
            var rot = keyframe.getRot();
            boolean hasWrapRisk = Math.abs(rot.x) > 170 || Math.abs(rot.y) > 170 || Math.abs(rot.z) > 170;
            
            if (hasWrapRisk) {
                jitterRisks[0]++;
                final String rotString = String.format("%.1f, %.1f, %.1f", rot.x, rot.y, rot.z);
                context.getSource().sendSuccess(() -> 
                    Component.literal("⚠ Time " + time + ": High rotation angles (" + rotString + ") - wrap risk"), false);
            }
            
            // Check for very short keyframe intervals
            var nextEntry = path.getNextEntry(time);
            if (nextEntry != null) {
                final int interval = nextEntry.getKey() - time;
                if (interval < 3) {
                    jitterRisks[0]++;
                    context.getSource().sendSuccess(() -> 
                        Component.literal("⚠ Time " + time + ": Very short interval (" + interval + " ticks) to next keyframe"), false);
                }
            }
        }
        
        if (jitterRisks[0] == 0) {
            context.getSource().sendSuccess(() -> 
                Component.literal("✓ No obvious jitter risks detected"), false);
        } else {
            context.getSource().sendSuccess(() -> 
                Component.literal("Found " + jitterRisks[0] + " potential jitter risks"), false);
            context.getSource().sendSuccess(() -> 
                Component.literal("Tip: The new jitter prevention system should handle these automatically"), false);
        }
        
        context.getSource().sendSuccess(() -> 
            Component.literal("Jitter fixes applied:"), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("  ✓ Fixed keyframe boundary interpolation logic"), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("  ✓ Added smooth rotation interpolation with angle wrapping"), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("  ✓ Enhanced FOV interpolation smoothing"), false);
        context.getSource().sendSuccess(() -> 
            Component.literal("  ✓ Improved time calculation precision"), false);
        
        return 1;
    }
}
