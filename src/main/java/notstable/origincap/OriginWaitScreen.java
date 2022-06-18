package notstable.origincap;

import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.screen.WaitForNextLayerScreen;

import java.util.ArrayList;

public class OriginWaitScreen extends WaitForNextLayerScreen {
    protected OriginWaitScreen(ArrayList<OriginLayer> layerList, int currentLayerIndex, boolean showDirtBackground) {
        super(layerList, currentLayerIndex, showDirtBackground);
    }
}
