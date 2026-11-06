package pl.dripeuszek.bomba;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class DripeuszekBombaClient implements ClientModInitializer {
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        ModConfig.load();

        // Klawisz "." (kropka) otwiera menu
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dripeuszekbomba.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_PERIOD,
                "key.categories.dripeuszekbomba"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new BombaScreen());
                }
            }
        });

        // Proste HUD sterowane z zakładki "Ustawienia"
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            ModConfig cfg = ModConfig.get();
            if (mc.player == null || mc.options.hudHidden) return;

            int color = Theme.byName(cfg.theme).accent;
            int y = 4;
            if (cfg.showFps) {
                ctx.drawTextWithShadow(mc.textRenderer, "FPS: " + mc.getCurrentFps(), 4, y, color);
                y += 10;
            }
            if (cfg.showCoords) {
                String xyz = String.format("XYZ: %.1f / %.1f / %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
                ctx.drawTextWithShadow(mc.textRenderer, xyz, 4, y, color);
            }
        });
    }
}
