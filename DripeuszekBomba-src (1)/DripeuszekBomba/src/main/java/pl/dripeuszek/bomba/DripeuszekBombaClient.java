package pl.dripeuszek.bomba;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class DripeuszekBombaClient implements ClientModInitializer {
    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        // Ładowanie konfiguracji i sprawdzenie licencji przy starcie
        ModConfig.load();

        // Klawisz otwierający menu (np. kropka '.')
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dripeuszekbomba.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_PERIOD,
                "category.dripeuszekbomba.main"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.player != null) {
                    client.setScreen(new BombaScreen());
                }
            }
        });
    }
}
