package notstable.origincap;

import java.util.ArrayList;

public class Blacklist {

    public ArrayList<String> blackList;

    public Blacklist() {
        blackList = new ArrayList<>();
    }
    public void add(String uuid) {
//        if (blackList == null)
//            blackList = new ArrayList<>();
        if (!containsIgnoreCase(uuid)) {
            blackList.add(uuid);
        }
    }

    public boolean containsIgnoreCase(String s) {
        return containsIgnoreCase(s, false);
    }

    public boolean containsIgnoreCase(String s, boolean isUUID) {
        if (blackList == null)
            return false;

        if(isUUID)
            s = s.replaceAll("-", ""); // if uuid, the stored uuid will not have - but incoming will

        for (String l : blackList)
            if (s.equalsIgnoreCase(l))
                return true;
        return false;
    }

    public void remove(String s) {
        if (blackList == null)
            return;
        for (int i = 0; i < blackList.size(); i++) {
            if (blackList.get(i).equalsIgnoreCase(s)) {
                blackList.remove(i);
                return;
            }
        }
    }

    public void clear() {
        blackList.clear();
    }

    public boolean isEmpty() {
        return blackList == null || blackList.isEmpty();
    }

}
