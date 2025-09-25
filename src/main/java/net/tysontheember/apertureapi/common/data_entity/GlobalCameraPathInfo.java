package net.tysontheember.apertureapi.common.data_entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.tysontheember.apertureapi.common.animation.GlobalCameraPath;

public record GlobalCameraPathInfo(String id, long version, UUID lastModifier) {
  public static GlobalCameraPathInfo fromNBT(CompoundTag root) {
    String id = root.getString("id");
    long version = root.getLong("version");
    UUID lastModifier = root.getUUID("lastModifier");
    return new GlobalCameraPathInfo(id, version, lastModifier);
  }

  public static CompoundTag toNBT(GlobalCameraPath path) {
    CompoundTag root = new CompoundTag();
    root.putString("id", path.getId());
    root.putLong("version", path.getVersion());
    root.putUUID("lastModifier", path.getLastModifier());
    return root;
  }
}
