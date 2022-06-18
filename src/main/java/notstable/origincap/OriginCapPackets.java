package notstable.origincap;

import io.github.apace100.apoli.Apoli;
import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class OriginCapPackets {
    // Handshake
    public static final Identifier CLIENT_RECEIVE_HANDSHAKE = new Identifier(OriginCap.MODID, "server_handshake");
    public static final Identifier SERVER_RECEIVE_HANDSHAKE = new Identifier(OriginCap.MODID, "client_handshake");
    public static final Identifier HANDSHAKE = new Identifier(OriginCap.MODID, "handshake");


    public static final Identifier UPDATE_ORIGIN_CAP = new Identifier(OriginCap.MODID, "origin_cap_update");
    public static final Identifier REMOVE_PLAYER_FROM_CAP = new Identifier(OriginCap.MODID, "origin_cap_remove_player");
    // loading/fetch checks
    public static final Identifier SERVER_RESPOND_CHECK_CAP = new Identifier(OriginCap.MODID, "server_response_to_cap_check");
    public static final Identifier CLIENT_RECEIVE_CAP_CHECK_RESPONSE = new Identifier(OriginCap.MODID, "client_receive_server_responses");
    // origin check listener - client
    private static CapListener randomOriginCapListener;
    // origin check listener - client
    private static CapListener activeCapListener;
    // IF CLIENT DOESNT HAVE THE MOD, THE EVENT WILL STILL BE REGISTERED BY THE SERVER AND NEVER REMOVE IT
    private static ArrayList<HandshakeEvent> handshakeEvents;

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_ORIGIN_CAP, OriginCapPackets::updateOrginCap);
        ServerPlayNetworking.registerGlobalReceiver(SERVER_RESPOND_CHECK_CAP, OriginCapPackets::serverCapResponse);
        ServerPlayNetworking.registerGlobalReceiver(REMOVE_PLAYER_FROM_CAP, OriginCapPackets::serverRemovePlayerFromCap);
        ServerPlayNetworking.registerGlobalReceiver(SERVER_RECEIVE_HANDSHAKE, OriginCapPackets::serverHandshake);

            ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
                sender.sendPacket(OriginCapPackets.HANDSHAKE, PacketByteBufs.empty());
            });
            ServerLoginNetworking.registerGlobalReceiver(OriginCapPackets.HANDSHAKE, OriginCapPackets::handshakeReply);
    }

    private static void handshakeReply(MinecraftServer minecraftServer, ServerLoginNetworkHandler serverLoginNetworkHandler, boolean understood, PacketByteBuf packetByteBuf, ServerLoginNetworking.LoginSynchronizer loginSynchronizer, PacketSender packetSender) {
        if (understood) {
            String handshakeCheckString = packetByteBuf.readString();
            boolean mismatch = !(handshakeCheckString.equals(OriginCap.HANDSHAKE_CHECK));


            if(mismatch)
                serverLoginNetworkHandler.disconnect(new LiteralText("This server requires you have the mod origin cap  installed"));

        } else {
            serverLoginNetworkHandler.disconnect(new LiteralText("This server requires you have the mod origin cap  installed"));
        }
    }

    public static void alertHandshakeEvent(String clientResponse, String clientInfo) {
        System.out.println("checking handshake events");

        for (int i = 0; i < handshakeEvents.size(); i++) {
            if (handshakeEvents.get(i).getClientInfo().equals(clientInfo)) {
                handshakeEvents.get(i).handshake(clientResponse);
                handshakeEvents.remove(i);
                return;
            }
        }
    }

    private static void registerHandshake() {
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            System.out.println("query start");
            String connectionInfo = handler.getConnectionInfo();

            CompletableFuture future = CompletableFuture.runAsync(() -> {
                // make event
                handshakeEvents.add(new HandshakeEvent() {
                    final String info = connectionInfo;

                    @Override
                    public void handshake(String clientResponse) {
                        System.out.println("handshake");

                        // return in event
                        if (!checkClient(clientResponse)) {
                            handler.disconnect(Text.of("Server could not find origin cap mod on client"));
                            System.out.println("disconnected cause cap mod check failed");
                        }
                        // otherwise dont return or set idle timeout
                        System.out.println("handshake passed");
                        handler.acceptPlayer();
                    }

                    @Override
                    public String getClientInfo() {
                        return info;
                    }
                });
            });

            // Execute the task on a worker thread as not to block the server thread
//            Util.getMainWorkerExecutor().execute((Runnable) future);
            synchronizer.waitFor(future);


            // send packet to client for check
            PacketByteBuf packet = new PacketByteBuf(Unpooled.buffer());
            // client info
            packet.writeString(connectionInfo);
            // send packet to client
            sender.sendPacket(OriginCapPackets.CLIENT_RECEIVE_CAP_CHECK_RESPONSE, packet);
        });
    }

    private static boolean checkClient(String checkMessage) {
        return checkMessage == OriginCap.HANDSHAKE_CHECK;
    }

    public static void registerClient() {
        ClientLoginNetworking.registerGlobalReceiver(OriginCapPackets.HANDSHAKE, (ClientLoginNetworking.LoginQueryRequestHandler) (client, handler, packetBuf, listenerAdder) -> {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(OriginCap.HANDSHAKE_CHECK);
            return CompletableFuture.completedFuture(buf);
        });

        ClientPlayNetworking.registerGlobalReceiver(CLIENT_RECEIVE_CAP_CHECK_RESPONSE, OriginCapPackets::clientReceiveCheck);
        ClientPlayNetworking.registerGlobalReceiver(CLIENT_RECEIVE_HANDSHAKE, OriginCapPackets::clientHandshake);
    }

    private static void clientHandshake(MinecraftClient minecraftClient, ClientPlayNetworkHandler clientPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender) {
        // client info
        String clientInfo = packetByteBuf.readString();

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString(clientInfo);
        buf.writeString(OriginCap.HANDSHAKE_CHECK);

        packetSender.sendPacket(OriginCapPackets.SERVER_RECEIVE_HANDSHAKE, buf);
    }

    private static void serverHandshake(MinecraftServer server, ServerPlayerEntity serverPlayerEntity, ServerPlayNetworkHandler serverPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender) {
        // client info
        String clientInfo = packetByteBuf.readString();
        // read handshake test
        String handshake = packetByteBuf.readString();

        alertHandshakeEvent(handshake, clientInfo);

    }

    // remove player from cap
    private static void serverRemovePlayerFromCap(MinecraftServer server, ServerPlayerEntity serverPlayerEntity, ServerPlayNetworkHandler serverPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender) {
        OriginCapList.removePlayerFromLayer(serverPlayerEntity.getUuid().toString(), packetByteBuf.readString());
    }

    // cap check server
    private static void serverCapResponse(MinecraftServer server, ServerPlayerEntity serverPlayerEntity, ServerPlayNetworkHandler serverPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender) {

        // the received packets are
        // requestType
        CapRequestType requestType = packetByteBuf.readEnumConstant(CapRequestType.class);
        // origin id
        // layer id
        String originID = packetByteBuf.readString(32767);
        String layerID = packetByteBuf.readString(32767);

        boolean choosable = !OriginCapList.doesOriginExceedCap(layerID, originID);

        // send back to client
        // request type
        // origin id
        // layer id
        // origin choosable
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeEnumConstant(requestType);
        buf.writeString(originID);
        buf.writeString(layerID);
        buf.writeBoolean(choosable);

        ServerPlayNetworking.send(serverPlayerEntity, CLIENT_RECEIVE_CAP_CHECK_RESPONSE, buf);
    }

    // cap check client
    private static void clientReceiveCheck(MinecraftClient minecraftClient, ClientPlayNetworkHandler clientPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender) {
        // the received packets are
        // request type
        CapRequestType requestType = packetByteBuf.readEnumConstant(CapRequestType.class);
        // origin id
        // layer id
        // isChoosable
        String originID = packetByteBuf.readString(32767);
        String layerID = packetByteBuf.readString(32767);
        boolean isChoosable = packetByteBuf.readBoolean();

        if (requestType == CapRequestType.LOAD_CHECK)
            alertCapReceived(layerID, originID, isChoosable);
        else if (requestType == CapRequestType.RANDOM_CHECK)
            alertRandomCapReceived(layerID, originID, isChoosable);
    }

    private static void updateOrginCap(MinecraftServer minecraftServer, ServerPlayerEntity playerEntity, ServerPlayNetworkHandler serverPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender) {
        // received packets, originID, layerID

        String newOriginID = packetByteBuf.readString(32767);
        String layerID = packetByteBuf.readString(32767);
        // clear player from cap
        OriginCapList.removePlayerFromLayer(playerEntity.getUuid().toString(), layerID);
        // add origin and player to layer cap
        OriginCapList.addToLog(playerEntity.getUuid().toString(), layerID, newOriginID);
    }

    public static void setRandomCapReceivedListener(CapListener toAdd) {
        // only have one because there is only one button who only wants to receive info relevant to its current state
        randomOriginCapListener = toAdd;
    }

    public static void alertRandomCapReceived(String layerKey, String originKey, boolean isChoosable) {
        if (randomOriginCapListener == null)
            return;
        // Notify everybody that may be interested.
        randomOriginCapListener.receiveCapInfo(layerKey, originKey, isChoosable);
    }

    public static void setCapReceivedListener(CapListener toAdd) {
        // only have one because there is only one button who only wants to receive info relevant to its current state
        activeCapListener = toAdd;
    }

    public static void alertCapReceived(String layerKey, String originKey, boolean isChoosable) {
        if (activeCapListener == null)
            return;
        // Notify everybody that may be interested.
        activeCapListener.receiveCapInfo(layerKey, originKey, isChoosable);
        activeCapListener = null;
    }

    public enum CapRequestType {
        RANDOM_CHECK,
        LOAD_CHECK
    }

    interface HandshakeEvent {
        void handshake(String clientResponse);

        String getClientInfo();
    }


    public interface CapListener {
        void receiveCapInfo(String layerKey, String originKey, boolean isChoosable);
    }

}
