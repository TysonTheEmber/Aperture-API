package net.tysontheember.apertureapi.common.security;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tysontheember.apertureapi.ApertureAPI;
import net.tysontheember.apertureapi.CommonConf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ApertureAPI.MODID)
public enum DamageGuard {
    INSTANCE;

    private final HashSet<UUID> playing = new HashSet<>();
    private final HashMap<UUID, Long> graceUntil = new HashMap<>();

    public net.minecraft.nbt.CompoundTag onClientCutsceneState(ServerPlayer player, boolean isPlaying) {
        net.minecraft.nbt.CompoundTag out = new net.minecraft.nbt.CompoundTag();
        if (player == null) return out;
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (isPlaying) {
            playing.add(player.getUUID());
            graceUntil.remove(player.getUUID());
        } else {
            playing.remove(player.getUUID());
            int grace = CommonConf.CUTSCENE_POST_GRACE_TICKS.get();
            if (grace > 0) graceUntil.put(player.getUUID(), now + grace);
        }
        return out;
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!CommonConf.CUTSCENE_INVULNERABLE.get()) return;
        if (INSTANCE.isProtected(sp)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!CommonConf.CUTSCENE_INVULNERABLE.get()) return;
        if (INSTANCE.isProtected(sp)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            UUID id = sp.getUUID();
            INSTANCE.playing.remove(id);
            INSTANCE.graceUntil.remove(id);
        }
    }

    private boolean isProtected(ServerPlayer sp) {
        UUID id = sp.getUUID();
        if (playing.contains(id)) return true;
        Long until = graceUntil.get(id);
        if (until == null) return false;
        long now = sp.serverLevel().getGameTime();
        if (now <= until) return true;
        graceUntil.remove(id);
        return false;
    }
}