package net.tysontheember.apertureapi.client.preview;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.apertureapi.ApertureAPI;
import net.tysontheember.apertureapi.client.ClientUtil;
import org.joml.Vector3f;

/**
 * Keeps the player's body frozen in place while IDE preview is active,
 * so it remains visible in third-person.
 */
@Mod.EventBusSubscriber(modid = ApertureAPI.MODID, value = Dist.CLIENT)
public class PreviewFreeze {
    private static boolean active;
    private static Vector3f savedPos;
    private static Vector3f savedRot; // pitch,yaw
    private static boolean savedNoGravity;

    public static void start() {
        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null) return;
        savedPos = new Vector3f((float)p.getX(), (float)p.getY(), (float)p.getZ());
        savedRot = new Vector3f(p.getXRot(), p.getYRot(), 0f);
        savedNoGravity = p.isNoGravity();
        p.setNoGravity(true);
        p.setDeltaMovement(0,0,0);
        p.setPos(savedPos.x, savedPos.y, savedPos.z);
        p.setXRot(savedRot.x);
        p.setYRot(savedRot.y);
        ClientUtil.toThirdView();
        active = true;
    }

    public static void stop() {
        var p = Minecraft.getInstance().player;
        if (p != null) {
            p.setNoGravity(savedNoGravity);
        }
        ClientUtil.resetCameraType();
        active = false;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!active) return;
        var p = Minecraft.getInstance().player;
        if (p == null || savedPos == null || savedRot == null) return;
        p.setDeltaMovement(0,0,0);
        p.setPos(savedPos.x, savedPos.y, savedPos.z);
        p.setXRot(savedRot.x);
        p.setYRot(savedRot.y);
    }
}
