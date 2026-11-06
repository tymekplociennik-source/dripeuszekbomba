package pl.dripeuszek.bomba;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import java.util.UUID;

public class DripeuszekBomba implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Rejestracja zdarzenia renderowania świata (Tracers i bomby nad głową)
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
                    // Tutaj w przyszłości dopiszemy rysowanie tekstu 3D nad głową gracza
                }
            }
        });
    }
}
