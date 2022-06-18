package notstable.origincap.mixin;

import io.github.apace100.origins.screen.ChooseOriginScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import notstable.origincap.CappedChooseOriginScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class ChooseOriginScreenRedirectMixin {
    @Shadow
    public abstract void setScreen(@Nullable Screen screen);

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void checkForChooseOriginScreen(Screen screen, CallbackInfo ci) {
        System.out.println("injected");
//        minecraftClient.setScreen(new ChooseOriginScreen(layers, 0, showDirtBackground));
        if (screen instanceof ChooseOriginScreen)
            setScreen(new CappedChooseOriginScreen(((ChooseOriginScreenAccessor) screen).getLayerList(), ((ChooseOriginScreenAccessor) screen).getCurrentLayerIndex(), true));

    }
}
