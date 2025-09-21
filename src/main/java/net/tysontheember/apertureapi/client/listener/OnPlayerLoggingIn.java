package net.tysontheember.apertureapi.client.listener;

import net.tysontheember.apertureapi.ApertureAPI;
import net.tysontheember.apertureapi.client.gui.screen.RemotePathSearchScreen;
import net.tysontheember.apertureapi.common.ModNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkRegistry;

@Mod.EventBusSubscriber(modid = ApertureAPI.MODID, value = Dist.CLIENT)
public class OnPlayerLoggingIn {
    @SubscribeEvent
    public static void loggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        RemotePathSearchScreen.REMOTE = ModNetwork.INSTANCE.isRemotePresent(event.getConnection());
    }
}

