package org.notdev.origincap.mixin;

import io.github.apace100.origins.networking.ModPackets;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.notdev.origincap.global.CapHandler;
import org.notdev.origincap.server.SaveLoadCap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.SERVER)
@Mixin(value = io.github.apace100.origins.networking.ModPacketsC2S.class)
public class ModPacketsC2SMixin {

    @Inject(method = "chooseOrigin", at = @At("HEAD"), cancellable = true)
    private static void onChooseOrigin(MinecraftServer minecraftServer, ServerPlayerEntity playerEntity, ServerPlayNetworkHandler serverPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender, CallbackInfo ci) {
        // duplicate and read from duplicate to prevent messing with origin's method after inject
        PacketByteBuf duplicate =new PacketByteBuf(packetByteBuf.duplicate());


        String originId = duplicate.readString(32767);
        String layerId = duplicate.readString(32767);

        minecraftServer.sendMessage(Text.of("Hello"));

        // if origin is full cancel choosing origin and if the client does not have origin cap installed just reset their choose origin screen
        if(CapHandler.originCap.isFull(layerId, originId)) {
            PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
            data.writeBoolean(false);
            ServerPlayNetworking.send(playerEntity, ModPackets.OPEN_ORIGIN_SCREEN, data);
            ci.cancel();
        } else {
            // add to origin cap
            if(CapHandler.originCap.tryAssign(layerId, originId, playerEntity.getUuid())) {
                SaveLoadCap.save(CapHandler.originCap);
            }
        }
    }

}
