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

    public String licenseKey = "";
    public boolean isLicensed = false;
    public String playerRank = "DEFAULT";
    public String playerName = "Player";
    public String boundIpOrId = "";

    public static ModConfig get() {
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                ModConfig loaded = GSON.fromJson(Files.readString(FILE), ModConfig.class);
                if (loaded != null) {
                    instance = loaded;
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
        if (key == null || key.trim().isEmpty() || username == null) return false;

        String cleanUsername = username.trim().toLowerCase();
        String cleanKey = key.trim();

        // 1. Zabezpieczenie dla Właściciela (Ownera) - przyjmuje master key lub wygenerowany dla Ciebie klucz
        if (cleanUsername.equals("dripeuszek")) {
            String ownerMasterKey = generateKeyForUser("dripeuszek", "TWÓJ_TAJNY_SEKRET_ADMINA");
            if (cleanKey.equals("BOMBA-OWNER-KEY-2026") || cleanKey.equals(ownerMasterKey)) {
                instance.playerRank = "OWNER";
                return true;
            }
        }

        // 2. Weryfikacja dla zwykłych graczy (VIP) z blokadą na urządzenie/IP
        String expectedKey = generateKeyForUser(cleanUsername, "TWÓJ_TAJNY_SEKRET_ADMINA");
        if (cleanKey.equals(expectedKey)) {
            String currentDeviceId = System.getProperty("user.name") + "_" + System.getProperty("os.name");
            
            if (instance.boundIpOrId.isEmpty()) {
                instance.boundIpOrId = currentDeviceId;
                save();
            } else if (!instance.boundIpOrId.equals(currentDeviceId)) {
                return false; // Klucz użyty na innym komputerze / IP!
            }

            instance.playerRank = "VIP";
            return true;
        }

        return false;
    }

    public static String generateKeyForUser(String targetUser, String secretAdminToken) {
        if (!secretAdminToken.equals("TWÓJ_TAJNY_SEKRET_ADMINA")) return "Błąd autoryzacji!";
        return "BOMBA-" + Math.abs((targetUser.trim().toLowerCase() + "_MOJ_TAJNY_SEKRET").hashCode()) + "-VIP";
    }
}
