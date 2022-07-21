package notstable.origincap;

import com.google.gson.Gson;
import io.github.apace100.origins.origin.Origin;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

public class OriginCapList {

    //files
    private static final String dir = "config/origin-cap/";
    private static final File originsCapsLog = new File(dir + "originCap.json");
    private static final String originsCapBackupFolderLoc = dir + "cap-backups/";
    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd---HH-mm-ss";
    private static final File playerBlackListFile = new File(dir + "playerBlacklist.json");
    private static final File originBlackListFile = new File(dir + "originBlacklist.json");
    private static final File layerBlackListFile = new File(dir + "originLayerBlacklist.json");
    // origin cap stuff
    private static final String capKey = "OriginCap";
    // String = later Object = Map<String, ArrayList<String>> unless its the origin cap
    public static Map<String, Object> originCapMap;
    //blacklists
    public static Blacklist playerUUIDBlacklist;
    public static Blacklist originBlacklist;
    public static Blacklist layerBlacklist;
    private static int originCap = 3;

    public static void initialize() {
        // if someone deletes the dir while playing, it will cause errors
        new File(dir).mkdirs();

        playerUUIDBlacklist = new Blacklist(playerBlackListFile);
        originBlacklist = new Blacklist(originBlackListFile);
        layerBlacklist = new Blacklist(layerBlackListFile);

        reloadCap(); // also updates choosable
    }


    private static void createCapBackup() {
        try {
            String date = new SimpleDateFormat(DATE_FORMAT_NOW).format(new Date());
            Files.createDirectories(Path.of(originsCapBackupFolderLoc));
            Files.copy(originsCapsLog.toPath(), Path.of(originsCapBackupFolderLoc + "cap-backup-" + date + ".json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void enforceWhitelist(MinecraftServer server) {
        createCapBackup();
        if (!server.getPlayerManager().isWhitelistEnabled()) System.err.println("whitelist not enabled");
        File w = new File("whitelist.json");
        if (!w.exists()) {
            System.err.println("whitelist does not exist");
            return;
        }
        try {
            ArrayList<Map<String, String>> whitelist = new Gson().fromJson(Files.readString(w.toPath()), ArrayList.class);
            ArrayList<String> uuidList = new ArrayList<String>();
            // get a list of all players in the cap
            for (String layerID : originCapMap.keySet()) {
                Object l_temp = originCapMap.get(layerID);
                if (l_temp == null || !(l_temp instanceof Map)) continue;
                // get layer map of origins
                Map<String, ArrayList<String>> currLayerMap = (Map<String, ArrayList<String>>) l_temp;
                Set<String> originKeySet = currLayerMap.keySet();
                for (String originKey : originKeySet) {
                    //check null and is list
                    Object o_temp = currLayerMap.get(originKey);
                    if (!(o_temp instanceof List)) continue;
                    if (o_temp == null) { // if list null, remove
                        currLayerMap.remove(originKey);
                        continue;
                    }
                    // get list
                    ArrayList<String> ul = (ArrayList<String>) o_temp;
                    // loop list
                    for (int i = 0; i < ul.size(); i++)
                        if (!uuidList.contains(ul.get(i))) uuidList.add(ul.get(i));
                }
            }
            boolean found = false;
            System.out.println(uuidList);
            for (String p : uuidList) {
                for (Map<String, String> m : whitelist)
                    if (uuidsEqual(m.get("uuid"), p)) {
                        found = true;
                        break;
                    }
                if (!found) {
                    removePlayerAllLayers(p);
                    System.out.println(UUIDTools.UUIDToPlayerName(p) + " removed");
                }
                found = false;
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addToLog(String playerUUID, String layerKey, String originKey) {
        // checks blacklist for layer, origin, and player
        if (playerUUIDBlacklist.containsIgnoreCase(playerUUID, true) || originBlacklist.containsIgnoreCase(originKey) || layerBlacklist.containsIgnoreCase(layerKey))
            return;

        if (originCapMap == null) reloadCap();

        // check add layer
        Map<String, ArrayList<String>> layerMap;
        if (!originCapMap.containsKey(layerKey)) {
            layerMap = new HashMap<>();
            originCapMap.put(layerKey, layerMap);
        } else {
            layerMap = (Map<String, ArrayList<String>>) originCapMap.get(layerKey);
        }

        // check add origin
        ArrayList<String> originUUIDList;
        if (!layerMap.containsKey(originKey)) {
            originUUIDList = new ArrayList<String>();
            layerMap.put(originKey, originUUIDList);
        } else {
            originUUIDList = layerMap.get(originKey);
        }

        // check add player
        if (!originUUIDList.contains(playerUUID)) originUUIDList.add(playerUUID);

        writeMapToFile();
    }

    public static void removePlayerFromLayer(String playerUUID, String layerID) {
        Object l_temp = originCapMap.get(layerID);
        if (l_temp == null || !(l_temp instanceof Map)) return;

        // get layer map of origins
        Map<String, ArrayList<String>> currLayerMap = (Map<String, ArrayList<String>>) l_temp;
        Set<String> originKeySet = currLayerMap.keySet();
        for (String originKey : originKeySet) {
            //check null and is list
            Object o_temp = currLayerMap.get(originKey);
            if (!(o_temp instanceof List)) continue;
            if (o_temp == null) { // if list null, remove
                currLayerMap.remove(originKey);
                continue;
            }

            // get list
            ArrayList<String> uuidList = (ArrayList<String>) o_temp;
            // loop list
            for (int i = 0; i < uuidList.size(); i++) {
                if (uuidsEqual(uuidList.get(i), playerUUID)) uuidList.remove(i); // remove matching uuids
            }

            // if list is now empty, remove it
            if (uuidList.isEmpty()) currLayerMap.remove(originKey);
        }
        writeMapToFile();

    }

    public static void removePlayerAllLayers(String playerUUID) {
        if (originCapMap == null) reloadCap();
        Set<String> layerKeySet = originCapMap.keySet();

        for (String layerKey : layerKeySet) {
            // checks
            if (layerKey.equalsIgnoreCase(capKey)) // skip cap identifier
                continue;
            Object l_temp = originCapMap.get(layerKey);
            if (l_temp == null || !(l_temp instanceof Map)) continue;

            // get layer map of origins
            Map<String, ArrayList<String>> currLayerMap = (Map<String, ArrayList<String>>) originCapMap.get(layerKey);
            Set<String> originKeySet = currLayerMap.keySet();
            for (String originKey : originKeySet) {
                //check null and is list
                Object o_temp = currLayerMap.get(originKey);
                if (!(o_temp instanceof List)) continue;
                if (o_temp == null) { // if list null, remove
                    currLayerMap.remove(originKey);
                    continue;
                }

                // get list
                ArrayList<String> uuidList = (ArrayList<String>) o_temp;
                // loop list
                for (int i = 0; i < uuidList.size(); i++) {
                    if (uuidsEqual(uuidList.get(i), playerUUID)) uuidList.remove(i); // remove matching uuids
                }

                // if list is now empty, remove it
                if (uuidList.isEmpty()) currLayerMap.remove(originKey);
            }

        }
        writeMapToFile();
    }

    private static boolean uuidsEqual(String uuid_1, String uuid_2) {
        uuid_1 = uuid_1.replaceAll("-", "");
        uuid_2 = uuid_2.replaceAll("-", "");
        return uuid_1.equals(uuid_2);
    }


    public static double getNumberOfOrigin(String layerKey, String originKey) {
        ArrayList<String> l = getPlayersOfOrigin(layerKey, originKey);
        if (l == null) return 0;
        return l.size();
    }

    //returns null if none found
    public static ArrayList<String> getPlayersOfOrigin(String layerKey, String originKey) {
        if (!originCapMap.containsKey(layerKey) || !((Map<String, Object>) originCapMap.get(layerKey)).containsKey(originKey))
            return null;
        return ((ArrayList<String>) ((Map<String, Object>) originCapMap.get(layerKey)).get(originKey));

    }

    public static boolean doesOriginExceedCap(String layerKey, String originKey) {
        return getNumberOfOrigin(layerKey, originKey) >= originCap;
    }

    private static void writeMapToFile() {
        try {
            if (!originsCapsLog.exists()) originsCapsLog.createNewFile();
            FileWriter flwr = new FileWriter(originsCapsLog);
            flwr.write(new Gson().toJson(originCapMap));
            flwr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // background stuff

    private static String ORIGIN_TO_KEY(Origin o) {
        return o.getIdentifier().toString();
    }

    private static Map<String, Object> getMapFromFile() {
        try {
            return new Gson().fromJson(Files.readString(originsCapsLog.toPath()), Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    private static double getOriginCapFromFile() {
        if (!originCapMap.containsKey(capKey)) {
            originCapMap.put(capKey, 3);
            writeMapToFile();
            return 3;
        }

        return (double) originCapMap.get(capKey);
    }

    public static void reloadCap() {
        // create file if none exists
        if (!originsCapsLog.exists()) try {
            originsCapsLog.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
            // load map
        else originCapMap = getMapFromFile();
        if (originCapMap == null) originCapMap = new HashMap<String, Object>();
        // load cap
        originCap = (int) getOriginCapFromFile();
    }

    public static void clearFullCap() {
        Set<String> layerKeySet = originCapMap.keySet();

        originCapMap = new HashMap<>();
        writeMapToFile();
        setCap(originCap);
    }

    public static void clearCapLayer(String originLayerKey) {
        if (!originCapMap.containsKey(originLayerKey)) return;

        originCapMap.remove(originLayerKey);
        writeMapToFile();
    }

    public static void clearCapOrigin(String originKey) {
        if (originCapMap == null) reloadCap();
        Set<String> layerKeySet = originCapMap.keySet();

        for (String layerKey : layerKeySet) {
            // checks
            if (layerKey.equalsIgnoreCase(capKey)) // skip cap identifier
                continue;
            Object l_temp = originCapMap.get(layerKey);
            if (l_temp == null || !(l_temp instanceof Map)) continue;

            ((Map<String, Object>) originCapMap.get(layerKey)).remove(originKey);
        }
        writeMapToFile();
    }

    public static int getCap() {
        return originCap;
    }

    public static void setCap(int cap) {
        if (originCapMap.containsValue(capKey)) originCapMap.replace(capKey, cap);
        else originCapMap.put(capKey, cap);
        originCap = cap;
        writeMapToFile();
    }
}
