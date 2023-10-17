package org.notdev.origincap.mixin;

import io.github.apace100.origins.component.PlayerOriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import net.minecraft.entity.player.PlayerEntity;
import org.notdev.origincap.global.CapHandler;
import org.notdev.origincap.server.SaveLoadCap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerOriginComponent.class, remap = false)
public class PlayerOriginComponentMixin {

    @Shadow
    private PlayerEntity player;

    @Inject(method = "setOrigin", at = @At("HEAD"), cancellable = true)
    void setOrigin(OriginLayer layer, Origin origin, CallbackInfo ci) {
        String layerId = layer.getIdentifier().toString();
        String originId = origin.getIdentifier().toString();


        // override setting origin of player if cap for origin is full and do not assign the origin
        if (CapHandler.originCap.isFull(layerId, originId)) {
            ci.cancel();
            return;
        }

        if (CapHandler.originCap.containsKey(layerId))
            CapHandler.originCap.get(layerId).removePlayer(player.getUuid());
        if (!Origin.EMPTY.equals(origin)) {
            CapHandler.originCap.tryAssign(layerId, originId, player.getUuid());
        }


        SaveLoadCap.save(CapHandler.originCap);
    }

}
