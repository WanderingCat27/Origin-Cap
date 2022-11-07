package notstable.origincap;

import io.github.apace100.origins.origin.Origin;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OriginCapStruct {


    private int cap;

    // {layer : {origin : [uuid] } }
    public Map<String, Map<String, ArrayList<UUID>>> originCapMap;

    //blacklists
    public Blacklist playerUUIDBlacklist;
    public Blacklist originBlacklist;
    public Blacklist layerBlacklist;

    private static final String capKey = "OriginCap";

    public OriginCapStruct() {
        cap = 3;
        originCapMap = new HashMap<>();
        playerUUIDBlacklist = new Blacklist();
        originBlacklist = new Blacklist();
        layerBlacklist = new Blacklist();
    }

    public String enforceWhitelist(MinecraftServer server, OriginCapSave save) {
        return null;
    }

    public void addToLog(String playerUUID, String layerKey, String originKey) {

    }

    public void removePlayerFromLayer(String playerUUID, String layerKey) {
        if(!originCapMap.containsKey(layerKey)) return;

        Map<String, ArrayList<UUID>> layer = originCapMap.get(layerKey);
        for( String key : layer.keySet()) {
            ArrayList<UUID> l = layer.get(key);
            l.remove(playerUUID);
            if(l.size() <= 0) layer.remove(key);
        }
    }

    public void removePlayerAllLayers(String playerUUID) {
        for(String key : originCapMap.keySet()) {
            removePlayerFromLayer(playerUUID, key);
        }
    }

//    private static boolean uuidsEqual(String uuid_1, String uuid_2) {
//        uuid_1 = uuid_1.replaceAll("-", "");
//        uuid_2 = uuid_2.replaceAll("-", "");
//        return uuid_1.equals(uuid_2);
//    }


    public double getNumberOfOrigin(String layerKey, String originKey) {
        ArrayList<UUID> l = getPlayersOfOrigin(layerKey, originKey);
        if (l == null)
            return 0;
        return l.size();
    }

    //returns null if none found
    public ArrayList<UUID> getPlayersOfOrigin(String layerKey, String originKey) {
        if (!originCapMap.containsKey(layerKey) || !originCapMap.get(layerKey).containsKey(originKey))
            return null;
        return originCapMap.get(layerKey).get(originKey);

    }

    public boolean doesOriginExceedCap(String layerKey, String originKey) {
        return getNumberOfOrigin(layerKey, originKey) >= cap;
    }


    private static String ORIGIN_TO_KEY(Origin o) {
        return o.getIdentifier().toString();
    }


    public void clearCapLayer(String originLayerKey) {
        if (!originCapMap.containsKey(originLayerKey))
            return;

        originCapMap.remove(originLayerKey);
    }

    public void clearOriginFromCap(String layerKey, String originKey) {
        if(!originCapMap.containsKey(layerKey) || !originCapMap.get(layerKey).containsKey(originKey)) return;

        originCapMap.get(layerKey).remove(originKey);
    }

    public int getCap() {
        return cap;
    }

    public void setCap(int cap) {
        this.cap = cap;
    }
}
