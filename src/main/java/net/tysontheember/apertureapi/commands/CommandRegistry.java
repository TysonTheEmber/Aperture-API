package net.tysontheember.apertureapi.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public class CommandRegistry {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    // Register consolidated roots
    ApertureCommand.register(dispatcher, "aperture");
    CameraCommand.register(dispatcher, "camera");
  }
}
