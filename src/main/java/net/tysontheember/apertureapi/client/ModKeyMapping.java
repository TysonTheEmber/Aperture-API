package net.tysontheember.apertureapi.client;

import net.tysontheember.apertureapi.ApertureAPI;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ApertureAPI.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModKeyMapping {
    public static final Lazy<KeyMapping> CAMERA_MODIFIER_SCREEN_KEY = Lazy.of(() ->
            new KeyMapping(
                    "key." + ApertureAPI.MODID + ".camera_modifier_screen",
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    "key.categories." + ApertureAPI.MODID
            ));

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(CAMERA_MODIFIER_SCREEN_KEY.get());
    }
}

