package net.tysontheember.apertureapi;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(ApertureAPI.MODID)
public class ApertureAPI {
  public static final String MODID = "apertureapi";

  public ApertureAPI() {
    // Register configs and initialize network
    ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ModConf.SPEC);
    ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConf.SPEC);
    net.tysontheember.apertureapi.common.ModNetwork.init();
  }
}
