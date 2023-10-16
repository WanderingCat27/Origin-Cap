package org.notdev.origincap.client;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.notdev.origincap.global.CapHandler;

public record S2C_IsOriginFullResult(String layerId, String originId, boolean isFull) implements FabricPacket {

    public static final Identifier id = new Identifier("origin-cap", "respond_origin_full");

    public static final PacketType<S2C_IsOriginFullResult> TYPE = PacketType.create(
            id, S2C_IsOriginFullResult::read);

    private static S2C_IsOriginFullResult read(PacketByteBuf buffer) {
        return new S2C_IsOriginFullResult(buffer.readString(), buffer.readString(), buffer.readBoolean());
    }

    @Override
    public void write(PacketByteBuf buffer) {
        buffer.writeString(layerId);
        buffer.writeString(originId);
        buffer.writeBoolean(isFull);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

}
