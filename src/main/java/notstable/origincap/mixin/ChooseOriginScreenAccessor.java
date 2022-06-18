package notstable.origincap.mixin;

import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.screen.ChooseOriginScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.ArrayList;

@Mixin(ChooseOriginScreen.class)
public interface ChooseOriginScreenAccessor {

    @Accessor
    ArrayList<OriginLayer> getLayerList();

    @Accessor
    int getCurrentLayerIndex();
}
