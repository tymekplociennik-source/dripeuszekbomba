package pl.dripeuszek.bomba;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class DripeuszekBombaClient implements ClientModInitializer {
    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        // 1. Rejestracja klawisza (domyślnie litera 'B') w kategorii "DripeuszekBomba"
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dripeuszekbomba.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.dripeuszekbomba.main"
        ));

        // 2. Nasłuchiwanie wciśnięcia klawisza w każdej klatce gry
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                // Otwiera nasze GUI po wciśnięciu klawisza B
                client.setScreen(new BombaScreen());
            }
        });

        // 3. Rejestracja renderowania w świecie (Tracers i bomby)
        WorldRenderEvents.LAST.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null) return;
            
            ModConfig config = ModConfig.get();
            if (!config.isLicensed) return;

            long currentTime = System.currentTimeMillis();

            for (PlayerEntity player : client.world.getPlayers()) {
                if (player == client.player) continue;

                UUID uuid = player.getUuid();
                boolean isAttacking = BombaScreen.activeAttacks.containsKey(uuid) && BombaScreen.activeAttacks.get(uuid) > currentTime;

                if (isAttacking) {
                    long timeLeft = (BombaScreen.activeAttacks.get(uuid) - currentTime) / 1000;
                    // Tutaj w przyszłości wyświetlimy tekst nad głową
                }
            }
        });
    }
}
