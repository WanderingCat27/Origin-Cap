package org.notdev.origincap.global;

import java.util.function.Function;

import org.notdev.origincap.cap.OriginCap;
import org.notdev.origincap.client.S2C_IsOriginFullResult;
import org.notdev.origincap.server.C2S_IsOriginFullCheck;
import org.notdev.origincap.server.SaveLoadCap;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public class CapHandler {

  public static OriginCap originCap;

  public static Function<S2C_IsOriginFullResult, Void> onGetResult;

  @Environment(EnvType.SERVER)
  public static void serverInit() {
    originCap = SaveLoadCap.load();
  }

  /*
   * CLIENT
   */
  @Environment(EnvType.CLIENT)
  public static void registerClientPackets() {

    ClientPlayNetworking.registerReceiver(S2C_IsOriginFullResult.TYPE, CapHandler::IsOriginFullResult);
  }

  @Environment(EnvType.CLIENT)
  private static void IsOriginFullResult(S2C_IsOriginFullResult packet, ClientPlayerEntity player,
      PacketSender responseSender) {
    if (onGetResult != null)
      onGetResult.apply(packet);
  }

  /*
   * SERVER
   */
  @Environment(EnvType.SERVER)
  public static void registerServerPackets() {
    ServerPlayNetworking.registerGlobalReceiver(C2S_IsOriginFullCheck.TYPE, CapHandler::checkIsOriginFull);

  }

  @Environment(EnvType.SERVER)
  private static void checkIsOriginFull(C2S_IsOriginFullCheck packet, ServerPlayerEntity player,
      PacketSender responseSender) {
    // check origin cap and tell player if full
    responseSender.sendPacket(new S2C_IsOriginFullResult(packet.layerId(), packet.originId(),
        originCap.isFull(packet.layerId(), packet.originId())));
  }


}
