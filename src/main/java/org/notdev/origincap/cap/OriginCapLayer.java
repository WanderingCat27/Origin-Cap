package org.notdev.origincap.cap;

import java.util.ArrayList;
import java.util.UUID;

public class OriginCapLayer extends EaseHashMap<String, OriginCapEntry> {

    private static final long serialVersionUID = 1231235245L;// Change this value if you want to update changes how serialization works

    public ArrayList<String> ignoreOrigins = new ArrayList<>(3);

    private int defaultLayerCap;

    private boolean shouldOverrideMax;

    public OriginCapLayer(int defaultLayerCap) {
        this.defaultLayerCap = defaultLayerCap;
    }

    public boolean tryAssignPlayer(String originId, UUID uuid) {
        return findOrCreateKey(originId).add(uuid);
    }

    @Override
    protected OriginCapEntry createBlankValue() {
        return new OriginCapEntry(defaultLayerCap);
    }

    public void  removePlayer(UUID uuid) {
        // loop thru origins and remove the player uuid from each
        forEach((key, value) -> {
            value.remove(uuid);
        });
    }

    public int getDefaultLayerCap() {
        return defaultLayerCap;
    }

    public void setDefaultLayerCap(int defaultLayerCap) {
        this.defaultLayerCap = defaultLayerCap;
        updateOriginMaxes();
    }

    public boolean shouldOverrideMax() {
        return shouldOverrideMax;
    }

    public void setShouldOverrideMax(boolean should, int max) {
      this.shouldOverrideMax = should;
      this.setDefaultLayerCap(max);
    }

    public void setShouldOverrideMax(boolean should) {
      this.shouldOverrideMax = should;
    }

    public void updateOriginMaxes() {
        forEach((key, origin) -> {
            if(!origin.shouldOverrideMax())
                origin.setMaxSize(defaultLayerCap);
        });
    }


}
