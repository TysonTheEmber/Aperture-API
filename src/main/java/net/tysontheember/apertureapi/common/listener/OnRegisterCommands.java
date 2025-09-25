package net.tysontheember.apertureapi.common.listener;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.apertureapi.ApertureAPI;

@Mod.EventBusSubscriber(modid = ApertureAPI.MODID)
public class OnRegisterCommands {
  @SubscribeEvent
  public static void register(RegisterCommandsEvent event) {
    CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
    // Central command registry: only '/aperture' and '/camera' roots
    net.tysontheember.apertureapi.commands.CommandRegistry.register(dispatcher);

    // Note: legacy roots like '/cameraanim' and '/apertureapi' are no longer registered here.
  }
}
