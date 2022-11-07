package notstable.origincap;


import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

public class UUIDTools {

    // POST
    public static String playerNameToUUID(String name) throws IOException {
        URL url = new URL("https://api.mojang.com/profiles/minecraft");
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        http.setRequestProperty("Content-Type", "application/json");
        http.setRequestProperty("Authorization", "Bearer mt0dgHmLJMVQhvjpNXDyA83vA_PxH23Y");

        String data = "[ \"" + name + "\"]";

        // send
        byte[] out = data.getBytes(StandardCharsets.UTF_8);
        OutputStream stream = http.getOutputStream();
        stream.write(out);

        // response
        BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null)
            response.append(inputLine);
        in.close();
        String s = response.toString();

        http.disconnect();

        ArrayList<Map<String, String>> responseMap = new Gson().fromJson(s, ArrayList.class);
        if (responseMap.isEmpty())
            return "";
        return responseMap.get(0).get("id");
    }

    // GET
    public static String UUIDToPlayerName(String uuid) throws IOException {
        URL url = new URL("https://api.mojang.com/user/profile/" + uuid);

        StringBuilder result = new StringBuilder();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            for (String line; (line = reader.readLine()) != null; ) {
                result.append(line);
            }
        }

        ArrayList<Map<String, String>> response = new Gson().fromJson(result.toString(), ArrayList.class);
        return response.get(response.size() - 1).get("name");
    }
}
