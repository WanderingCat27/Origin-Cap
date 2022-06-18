package notstable.origincap;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public class Blacklist {

    private final File file;
    public ArrayList<String> blackList;

    public Blacklist(File f) {
        blackList = new ArrayList<String>();
        file = f;
        load();
    }

    private void load() {
        try {
            if (!file.exists())
                file.createNewFile();
            blackList = new Gson().fromJson(Files.readString(file.toPath()), ArrayList.class);
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void add(String uuid) {
        if (blackList == null)
            blackList = new ArrayList<>();
        if (!containsIgnoreCase(uuid)) {
            blackList.add(uuid);
            save();
        }
    }

    private void save() {
        try {
            if (!file.exists())
                file.createNewFile();


            FileWriter flwr = new FileWriter(file);
            if (blackList == null)
                flwr.write("");
            else
                flwr.write(new Gson().toJson(blackList));
            flwr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public boolean containsIgnoreCase(String s) {
        if (blackList == null)
            return false;

        s = s.replaceAll("-", ""); // if uuid, the stored uuid will not have - but incoming will
        System.out.println(s);

        for (String l : blackList) {
            System.out.println(l);
            if (s.equalsIgnoreCase(l))
                return true;
        }
        return false;
    }

    public void remove(String s) {
        if (blackList == null)
            return;
        for (int i = 0; i < blackList.size(); i++) {
            if (blackList.get(i).equalsIgnoreCase(s)) {
                blackList.remove(i);
                save();
                return;
            }
        }
    }

    public void clear() {
        if (!file.exists())
            return;

        try {
            FileWriter flwr = new FileWriter(file);
            flwr.write("");
            blackList.clear();
            flwr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean isEmpty() {
        return blackList == null || blackList.isEmpty();
    }

}
