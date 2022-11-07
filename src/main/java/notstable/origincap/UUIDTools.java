package notstable.origincap;


import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class UUIDTools {

    public static String playerNameToUUID(String name, MinecraftServer server) throws IOException {
       Optional<GameProfile> profile =  server.getUserCache().findByName(name);
        return profile.isPresent() ? profile.get().getId().toString() : "No Player Found";
    }

    public static String UUIDToPlayerName(String uuid, MinecraftServer server) throws IOException {
        Optional<GameProfile> profile =  server.getUserCache().getByUuid(UUID.fromString(uuid));
        return profile.isPresent() ? profile.get().getName() : "No Player Found";
    }
}
