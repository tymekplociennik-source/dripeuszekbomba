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
    public String boundIpOrId = ""; // Zabezpieczenie: klucz przypisany do konkretnego urządzenia/IP

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
        if (key == null || key.isEmpty()) return false;

        // Twoje konto OWNER ma zawsze dostęp
        if (username.equalsIgnoreCase("dripeuszek")) {
            instance.playerRank = "OWNER";
            return true;
        }

        // Sprawdzenie poprawności klucza wygenerowanego dla gracza
        String expectedKey = "BOMBA-" + Math.abs((username + "_MOJ_TAJNY_SEKRET").hashCode()) + "-VIP";
        if (key.equals(expectedKey)) {
            // Blokada jednego IP / urządzenia: jeśli klucz nie był jeszcze nigdzie przypisany lub jest przypisany do nas
            String currentDeviceId = System.getProperty("user.name") + "_" + System.getProperty("os.name");
            
            if (instance.boundIpOrId.isEmpty()) {
                instance.boundIpOrId = currentDeviceId; // Przypisuje do pierwszego lepszego IP/urządzenia
                save();
            } else if (!instance.boundIpOrId.equals(currentDeviceId)) {
                // Klucz był już użyty na innym urządzeniu/IP!
                return false;
            }

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
