package net.tysontheember.apertureapi;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = ApertureAPI.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonConf {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue CUTSCENE_INVULNERABLE = BUILDER
            .comment("If true, players are invulnerable during camera cutscenes.")
            .define("cutscene.invulnerableDuringCutscene", true);

    public static final ForgeConfigSpec.IntValue CUTSCENE_POST_GRACE_TICKS = BUILDER
            .comment("Number of ticks players remain invulnerable after a cutscene ends.")
            .defineInRange("cutscene.postInvulnerabilityTicks", 40, 0, 20 * 60 * 10);

    public static final ForgeConfigSpec.IntValue CUTSCENE_ENTER_FADE_OUT_TICKS = BUILDER
            .comment("Ticks to fade from gameplay to black when a cutscene starts.")
            .defineInRange("cutscene.fade.enterFadeOutTicks", 10, 0, 20 * 10);

    public static final ForgeConfigSpec.IntValue CUTSCENE_ENTER_FADE_IN_TICKS = BUILDER
            .comment("Ticks to fade from black into the cutscene after start.")
            .defineInRange("cutscene.fade.enterFadeInTicks", 10, 0, 20 * 10);

    public static final ForgeConfigSpec.IntValue CUTSCENE_EXIT_FADE_OUT_TICKS = BUILDER
            .comment("Ticks to fade from cutscene to black when a cutscene ends.")
            .defineInRange("cutscene.fade.exitFadeOutTicks", 10, 0, 20 * 10);

    public static final ForgeConfigSpec.IntValue CUTSCENE_EXIT_FADE_IN_TICKS = BUILDER
            .comment("Ticks to fade from black back to gameplay after cutscene ends.")
            .defineInRange("cutscene.fade.exitFadeInTicks", 10, 0, 20 * 10);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    public static void onLoad(ModConfigEvent event) {
        // nothing required; values are read directly where needed
    }
}
