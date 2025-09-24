package net.tysontheember.apertureapi.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraft.client.gui.GuiGraphics;
import net.tysontheember.apertureapi.ApertureAPI;
import net.tysontheember.apertureapi.CommonConf;

@Mod.EventBusSubscriber(modid = ApertureAPI.MODID, value = Dist.CLIENT)
public class CutsceneFadeOverlay implements IGuiOverlay {
    private enum Phase { IDLE, ENTER_OUT, ENTER_IN, EXIT_OUT, EXIT_IN }

    private static Phase phase = Phase.IDLE;
    private static int phaseTimer = 0;
    private static int phaseDuration = 0;

    // Durations captured at sequence start (so config changes mid-sequence won't glitch)
    private static int enterOutTicks = 0;
    private static int enterInTicks = 0;
    private static int exitOutTicks = 0;
    private static int exitInTicks = 0;

    // Optional callback to run exactly once when we reach full black on ENTER_OUT
    private static Runnable atBlackCallback = null;
    private static Runnable atBlackExitCallback = null;

    // Backward-compatible starter (no gating)
    public static void onCutsceneStart() {
        startEnterSequence(null);
    }

    // New: begin enter sequence and run callback once we've reached full black
    public static void startEnterSequence(Runnable atBlack) {
        enterOutTicks = CommonConf.CUTSCENE_ENTER_FADE_OUT_TICKS.get();
        enterInTicks  = CommonConf.CUTSCENE_ENTER_FADE_IN_TICKS.get();
        atBlackCallback = atBlack;

        if (enterOutTicks > 0) {
            phase = Phase.ENTER_OUT;
            phaseDuration = enterOutTicks;
            phaseTimer = 0;
        } else if (enterInTicks > 0) {
            // No fade out -> implicitly at black instantly; run callback now if present
            if (atBlackCallback != null) { try { atBlackCallback.run(); } catch (Throwable ignored) {} finally { atBlackCallback = null; } }
            phase = Phase.ENTER_IN;
            phaseDuration = enterInTicks;
            phaseTimer = 0;
        } else {
            // No fades at all; still run callback immediately if provided
            if (atBlackCallback != null) { try { atBlackCallback.run(); } catch (Throwable ignored) {} finally { atBlackCallback = null; } }
            phase = Phase.IDLE;
            phaseTimer = 0;
            phaseDuration = 0;
        }
    }

    // Backward-compatible ender (no gating)
    public static void onCutsceneEnd() {
        startExitSequence(null);
    }

    // New: begin exit sequence and run callback once we've reached full black
    public static void startExitSequence(Runnable atBlack) {
        exitOutTicks = CommonConf.CUTSCENE_EXIT_FADE_OUT_TICKS.get();
        exitInTicks  = CommonConf.CUTSCENE_EXIT_FADE_IN_TICKS.get();
        atBlackExitCallback = atBlack;
        if (exitOutTicks > 0) {
            phase = Phase.EXIT_OUT;
            phaseDuration = exitOutTicks;
            phaseTimer = 0;
        } else if (exitInTicks > 0) {
            // No fade out -> implicitly at black instantly; run callback now if present
            if (atBlackExitCallback != null) { try { atBlackExitCallback.run(); } catch (Throwable ignored) {} finally { atBlackExitCallback = null; } }
            phase = Phase.EXIT_IN;
            phaseDuration = exitInTicks;
            phaseTimer = 0;
        } else {
            // No fades at all; still run callback immediately if provided
            if (atBlackExitCallback != null) { try { atBlackExitCallback.run(); } catch (Throwable ignored) {} finally { atBlackExitCallback = null; } }
            phase = Phase.IDLE;
            phaseTimer = 0;
            phaseDuration = 0;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (phase == Phase.IDLE) return;
        phaseTimer++;
        if (phaseDuration <= 0 || phaseTimer >= phaseDuration) {
            // Advance to next logical phase in the sequence
            switch (phase) {
                case ENTER_OUT -> {
                    // We just reached full black; run the callback once, then proceed
                    if (atBlackCallback != null) { try { atBlackCallback.run(); } catch (Throwable ignored) {} finally { atBlackCallback = null; } }
                    if (enterInTicks > 0) {
                        phase = Phase.ENTER_IN;
                        phaseDuration = enterInTicks;
                        phaseTimer = 0;
                    } else {
                        phase = Phase.IDLE;
                    }
                }
                case ENTER_IN -> phase = Phase.IDLE;
                case EXIT_OUT -> {
                    // We just reached full black on exit; run the exit callback once, then proceed
                    if (atBlackExitCallback != null) { try { atBlackExitCallback.run(); } catch (Throwable ignored) {} finally { atBlackExitCallback = null; } }
                    if (exitInTicks > 0) {
                        phase = Phase.EXIT_IN;
                        phaseDuration = exitInTicks;
                        phaseTimer = 0;
                    } else {
                        phase = Phase.IDLE;
                    }
                }
                case EXIT_IN -> phase = Phase.IDLE;
                case IDLE -> {}
            }
        }
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        float alpha = currentAlpha();
        if (alpha <= 0.001f) return;

        int a = Math.min(255, Math.max(0, (int)(alpha * 255)));
        int color = (a << 24) | 0x000000; // ARGB black with alpha

        // Ensure blending is enabled for alpha
        RenderSystem.enableBlend();
        g.fill(0, 0, screenWidth, screenHeight, color);
    }

    private static float currentAlpha() {
        if (phase == Phase.IDLE) return 0f;
        float t = phaseDuration <= 0 ? 1f : Math.min(1f, Math.max(0f, (float)phaseTimer / (float)phaseDuration));
        return switch (phase) {
            case ENTER_OUT -> t;           // 0 -> 1 (to black)
            case ENTER_IN -> 1f - t;       // 1 -> 0 (from black)
            case EXIT_OUT -> t;            // 0 -> 1 (to black)
            case EXIT_IN -> 1f - t;        // 1 -> 0 (from black)
            case IDLE -> 0f;
        };
    }
}
