package pl.dripeuszek.bomba;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("dripeuszekbomba.json");
    private static ModConfig instance = new ModConfig();

    public boolean showFps = false;
    public boolean showCoords = false;
    public boolean tracersEnabled = false;
    public String theme = Theme.CIEMNY.name();

    // Pola licencji
    public String licenseKey = "";
    public boolean isLicensed = false;
    public String playerRank = "DEFAULT";
    public String playerName = "Player";

    public static ModConfig get() {
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                ModConfig loaded = GSON.fromJson(Files.readString(FILE), ModConfig.class);
                if (loaded != null) {
                    instance = loaded;
                    // Automatyczna walidacja przy wczytaniu
                    instance.isLicensed = validateKey(instance.licenseKey, instance.playerName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(instance));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean validateKey(String key, String username) {
        // Jeśli to Ty, zawsze dostajesz OWNER i pełny dostęp
        if (username.equalsIgnoreCase("dripeuszek")) {
            instance.playerRank = "OWNER";
            return true;
        }

        // Walidacja klucza dla innych graczy
        String expectedKey = "BOMBA-" + Math.abs((username + "_MOJ_TAJNY_SEKRET").hashCode()) + "-VIP";
        if (key.equals(expectedKey)) {
            instance.playerRank = "DEFAULT";
            return true;
        }
        return false;
    }

    public static String generateKeyForUser(String targetUser, String secretAdminToken) {
        if (!secretAdminToken.equals("TWÓJ_TAJNY_SEKRET_ADMINA")) return "Błąd autoryzacji!";
        return "BOMBA-" + Math.abs((targetUser + "_MOJ_TAJNY_SEKRET").hashCode()) + "-VIP";
    }
}
