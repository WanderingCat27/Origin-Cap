package org.notdev.origincap.server;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.notdev.origincap.global.CapHandler;

public record C2S_IsOriginFullCheck(String layerId, String originId) implements FabricPacket {
    private static final Identifier id = new Identifier("origin-cap", "ask_origin_full");
    public static final PacketType<C2S_IsOriginFullCheck> TYPE = PacketType.create(
            id, C2S_IsOriginFullCheck::read);

    private static C2S_IsOriginFullCheck read(PacketByteBuf buffer) {
        return new C2S_IsOriginFullCheck(buffer.readString(), buffer.readString());
    }

    @Override
    public void write(PacketByteBuf buffer) {
        buffer.writeString(layerId);
        buffer.writeString(originId);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

}
