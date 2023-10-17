package org.notdev.origincap.cap;

import java.util.ArrayList;
import java.util.UUID;


/**
 * data structure is a map of layers each layer contains a map of origins to the plays who have chosen that origin
 */
public class OriginCap extends EaseHashMap<String, OriginCapLayer> {

    private static final long serialVersionUID = 9104933821843684646L; // Change this value if you want to "ignore" changes

    public ArrayList<String> ignoreLayers = new ArrayList<>(3);


    /**
     * must be > 1
     **/
    private int defaultCapMaxSize;

    public OriginCap(int defaultCapMaxSize) {
        this.defaultCapMaxSize = defaultCapMaxSize;

    }


    /**
     * attempts to add a player to the specified layer and origin
     *
     * @param layerId  the layer id
     * @param originId the origin id
     * @param player   the player's uuid
     * @return true if the origin was not full and successfully added player, false if unable to add player because origin was at capacity
     * true if layer is ignored
     */
    public boolean tryAssign(String layerId, String originId, UUID player) {
        return findOrCreateKey(layerId).tryAssignPlayer(originId, player);
    }

    public void removePlayerFromList(UUID player) {
        this.forEach((String layerId, OriginCapLayer layer) -> {
            layer.forEach((String originId, OriginCapEntry entry) -> {
                entry.remove(player);
            });
        });
    }


    /**
     * if origin in layer is at capacity
     *
     * @param layerId  the layer id
     * @param originId the origin id
     * @return is the origin in the specified layer full
     */
    public boolean isFull(String layerId, String originId) {
        OriginCapLayer layer = get(layerId);
        // if layer or origin is not found return false otherwise return whether the originEntry says its full
        return !ignoreLayers.contains(layerId) && layer != null && !layer.ignoreOrigins.contains(originId) && layer.containsKey(originId) && layer.get(originId).isFull();
    }

    @Override
    protected OriginCapLayer createBlankValue() {
        return new OriginCapLayer(defaultCapMaxSize);
    }


    public int getDefaultCapMaxSize() {
        return defaultCapMaxSize;
    }


    public void setDefaultCapMaxSize(int defaultCapMaxSize) {
        this.defaultCapMaxSize = defaultCapMaxSize;
        updateOriginMaxes();
    }

    public void updateOriginMaxes() {
        forEach((key, layer) -> {
            if (!layer.shouldOverrideMax())
                layer.setDefaultLayerCap(defaultCapMaxSize);
            layer.updateOriginMaxes();
        });
    }


    

}
