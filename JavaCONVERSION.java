import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

public class PlayerStatusTracker {
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1318211521524011179/b9rFC_MsWfZndQ0aoeUlol4q1O5Z4c8Alz7umWFSCdT1dHw-kUv23gXy0PDkODp4rr4y";
    private static final List<String> PLAYER_UUIDS = Arrays.asList(
            "572133d1-a28e-4e8f-9af4-f43167f672a3", "28237084-488c-4cf0-a05b-947b9edd579e", "c25be85a-0d8a-478a-956e-9839ebd58cb3", "001ec66e-f729-4d7d-ace0-a243d49a857b", "0d768882-6006-4db5-a9c5-e9c96b00262d", "171e54c1-1918-44bc-8d2f-5beafb794358", "0c90fb66-49c5-4305-9b46-50bdf26aa4ff", "a1dcd9be-63a1-4728-aa6e-0bc60a341ea8", "2f14c1f2-8a80-4a44-801c-acd0df53db56", "74f70623-8a16-45bc-9002-f798fa08833b", "bb194f5a-6ca4-4f25-8393-4b0730b7207f", "68f9872a-f7bf-499e-94ed-ec0627a37698", "72eda40c-bc36-47cd-a03d-edddd14d0407", "90e58b5e-6044-4080-952b-8c2eb4f3399a", "e8c5415e-2a9b-4c47-aa5f-2ec2eaabc435", "43c05878-94ce-4650-ba6c-8f7fa783f1e2", "36ee3dc5-678c-4291-94f1-bcb098f64309", "faf778ca-ffae-4f8a-b7b9-b67a479ad071", "b3990d64-f6b6-4571-acea-185f8c529036"
    );
    private static final List<String> PERSONAL_OPPS = Arrays.asList(
            "572133d1-a28e-4e8f-9af4-f43167f672a3"
    );
    private static final Map<String, Map<String, Object>> FRIEND_DATA = new HashMap<>();
    static {
        for (String uuid : PLAYER_UUIDS) {
            FRIEND_DATA.put(uuid, new HashMap<String, Object>() {{
                put("online", false);
                put("last_logout", null);
            }});
        }
    }
    private static boolean FIRST_RUN = true;
    private static boolean LOG_API_DURATION = true;

    static {
        System.out.println("Started Tracking Player Status");
    }

    private static boolean handleRateLimit(Response response) {
        if (response.code() == 429) { // Too Many Requests
            long resetTime = response.headers().get("X-RateLimit-Reset") == null ? System.currentTimeMillis() + 10000 : Long.parseLong(response.headers().get("X-RateLimit-Reset"));
            long waitTime = resetTime - System.currentTimeMillis() + 1;
            System.out.printf("Rate limited. Waiting for %.2f seconds...%n", waitTime / 1000.0);
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private static void checkOnlineStatus() {
        OkHttpClient client = new OkHttpClient();
        for (String uuid : PLAYER_UUIDS) {
            try {
                long startTime = System.currentTimeMillis();

                Request request = new Request.Builder()
                        .url(String.format("https://pitpanda.rocks/api/players/%s", uuid))
                        .build();

                // rate limit handler for response from the API
                Response response;
                while (handleRateLimit(response = client.newCall(request).execute())) {}

                JSONObject data = new JSONObject(response.body().string());

                long endTime = System.currentTimeMillis();
                double apiCallDuration = (endTime - startTime) / 1000.0;

                if (LOG_API_DURATION) {
                    System.out.printf("%s - API call duration for the UUID %s: %.2f seconds%n", Instant.now(), uuid, apiCallDuration);
                }

                if (data.getBoolean("success")) {
                    JSONObject playerData = data.getJSONObject("data");
                    String username = playerData.getString("name");
                    boolean isOnline = playerData.getBoolean("online");
                    Long lastLogoutTimestamp = playerData.getLong("lastLogout");

                    if (FIRST_RUN || isOnline != (boolean) FRIEND_DATA.get(uuid).get("online"))
