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
        // Rejestracja klawisza B do otwierania GUI
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dripeuszekbomba.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.dripeuszekbomba.main"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                client.setScreen(new BombaScreen());
            }
        });

        // Rejestracja renderowania świata (Tracers / Stan ataku)
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
                    // Aktywny cel bomby – tutaj renderujemy tracers / etykietę nad głową
                }
            }
        });
    }
}
