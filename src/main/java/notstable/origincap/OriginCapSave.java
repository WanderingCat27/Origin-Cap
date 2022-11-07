package notstable.origincap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OriginCapSave {

    private static final String dir = "config/origin-cap/";

    private static final File originsCapsFile = new File(dir + "originCap.json");

    private static final String originsCapBackupFolderLoc = dir + "cap-backups/";

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd---HH-mm-ss";

    public static void createCapBackup() {
        try {
            String date = new SimpleDateFormat(DATE_FORMAT_NOW).format(new Date());
            Files.createDirectories(Path.of(originsCapBackupFolderLoc));
            Files.copy(originsCapsFile.toPath(), Path.of(originsCapBackupFolderLoc + "cap-backup-" + date + ".json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void save(OriginCapStruct cap) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Writer writer = Files.newBufferedWriter(Paths.get("src/test.json"));
        gson.toJson(cap, writer);
        writer.close();
    }

    private static OriginCapStruct load() throws IOException {
        if(!originsCapsFile.exists()) return new OriginCapStruct();

        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get("user.json"));
        OriginCapStruct ocl = gson.fromJson(reader, OriginCapStruct.class);
        reader.close();
        return ocl;
    }
}
