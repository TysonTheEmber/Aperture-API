package net.tysontheember.apertureapi.client.network;

import net.minecraft.nbt.CompoundTag;
import net.tysontheember.apertureapi.common.ModNetwork;
import net.tysontheember.apertureapi.common.animation.GlobalCameraPath;
import net.tysontheember.apertureapi.common.network.C2SPayloadManager;

public class ClientPayloadSender {
  public static void putGlobalPath(GlobalCameraPath path) {
    send("putGlobalPath", GlobalCameraPath.toNBT(path));
  }

  public static void removeGlobalPath(String id) {
    CompoundTag tag = new CompoundTag();
    tag.putString("id", id);
    send("removeGlobalPath", tag);
  }

  public static void getGlobalPath(String id, int receiver) {
    CompoundTag tag = new CompoundTag();
    tag.putString("id", id);
    tag.putInt("receiver", receiver);
    send("getGlobalPath", tag);
  }

  public static void checkGlobalPath(int page, int size) {
    CompoundTag tag = new CompoundTag();
    tag.putInt("page", page);
    tag.putInt("size", size);
    send("checkGlobalPath", tag);
  }

  public static void send(String key, CompoundTag value) {
    CompoundTag root = new CompoundTag();
    root.putString("key", key);
    root.put("value", value);
    ModNetwork.INSTANCE.sendToServer(new C2SPayloadManager(root));
  }

  public static void cutsceneInvul(boolean playing) {
    CompoundTag tag = new CompoundTag();
    tag.putBoolean("playing", playing);
    send("cutsceneInvul", tag);
  }
}
