package org.notdev.origincap.server;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.notdev.origincap.cap.OriginCap;
import org.notdev.origincap.global.Config;

public class SaveLoadCap {

    // static class
    private SaveLoadCap() {
    }


    /**
     * Load origin cap from save/load file and returns it
     *
     * @return loaded origin cap
     */
    public static OriginCap load() {
        // De-serializing origin cap
        if (!Files.exists(Path.of(Config.ORIGIN_CAP_SAVE_LOAD_FILE_PATH)))
            return new OriginCap(Config.DEFAULT_CAP_MAX_SIZE);
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(Config.ORIGIN_CAP_SAVE_LOAD_FILE_PATH))) {

            return (OriginCap) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("class OriginCap not found or IO Exception from " + Config.ORIGIN_CAP_SAVE_LOAD_FILE_PATH);
            throw new RuntimeException(e);
        }
    }

    /**
     * saves the origin cap to save/load file
     *
     * @param originCap the origin cap to save
     */
    public static void save(OriginCap originCap) {
        // create save file and directory
        try {
            tryCreateFilesAndDirs();
        } catch (IOException e) {
            System.err.println("Failed to save origin cap at " + Config.ORIGIN_CAP_SAVE_LOAD_FILE_PATH);
            throw new RuntimeException(e);
        }

        // Serializing origin cap
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(Config.ORIGIN_CAP_SAVE_LOAD_FILE_PATH))) {
            oos.writeObject(originCap);
        } catch (IOException e) {
            System.err.println("error reading or general IO error " + Config.ORIGIN_CAP_SAVE_LOAD_FILE_PATH);
            throw new RuntimeException(e);
        }
    }

    private static void tryCreateFilesAndDirs() throws IOException {
        if (!Files.exists(Path.of(Config.ORIGIN_CAP_CONFIG_DIRECTORY)))
            Files.createDirectories(Path.of(Config.ORIGIN_CAP_CONFIG_DIRECTORY));
        else if (!Files.exists(Path.of(Config.ORIGIN_CAP_SAVE_LOAD_FILE_PATH)))
            Files.createFile(Path.of(Config.ORIGIN_CAP_SAVE_LOAD_FILE_PATH));
    }
}
