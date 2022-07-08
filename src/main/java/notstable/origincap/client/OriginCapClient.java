package notstable.origincap.client;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import notstable.origincap.OriginCap;
import notstable.origincap.OriginCapPackets;

@Environment(EnvType.CLIENT)
public class OriginCapClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
//      ClientPlayConnectionEvents.INIT.register((handler, client) -> {
//           ClientPlayNetworking.registerReceiver(OriginCapPackets.CLIENT_RECEIVE_HANDSHAKE, OriginCapClient::receive);
//     });

        OriginCapPackets.registerClient();
    }

}
