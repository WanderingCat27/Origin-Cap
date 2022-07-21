package notstable.origincap.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
