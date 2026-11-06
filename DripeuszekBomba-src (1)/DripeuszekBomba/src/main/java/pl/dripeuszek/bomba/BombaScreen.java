package pl.dripeuszek.bomba;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.UUID;

public class BombaScreen extends Screen {
    public enum Tab { PLAYERS, SETTINGS, THEMES, LICENSE }
    private Tab currentTab = Tab.PLAYERS;

    public static final HashMap<UUID, Long> attackCooldowns = new HashMap<>();
    public static final HashMap<UUID, Long> activeAttacks = new HashMap<>();
    private PlayerEntity selectedPlayer = null;

    private TextFieldWidget licenseField;
    private TextFieldWidget targetUserField;
    private String generatedKeyResult = "";
    private String errorMessage = "";
    private float animationProgress = 0.0f;

    public BombaScreen() {
        super(Text.literal("DripeuszekBomba Ultimate GUI"));
    }

    @Override
    protected void init() {
        super.init();
        this.clearChildren();
        animationProgress = 0.0f;
        
        ModConfig config = ModConfig.get();
        playClickSound(0.7f);

        if (!config.isLicensed) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;

            this.licenseField = new TextFieldWidget(this.textRenderer, centerX - 110, centerY - 15, 220, 22, Text.literal("Klucz"));
            this.licenseField.setMaxLength(256);
            this.licenseField.setPlaceholder(Text.literal("Wklej klucz (np. BOMBA-OWNER-KEY-2026)"));
            this.licenseField.setText(config.licenseKey);
            this.addDrawableChild(this.licenseField);

            this.addDrawableChild(ButtonWidget.builder(Text.literal("§a✔ ZATWIERDŹ LICENCJĘ"), b -> {
                playClickSound(1.2f);
                String enteredKey = licenseField.getText().trim();
                String playerName = (this.client != null && this.client.player != null) 
                    ? this.client.player.getName().getString() 
                    : "Player";
                
                config.licenseKey = enteredKey;
                config.playerName = playerName;
                
                if (ModConfig.validateKey(enteredKey, playerName)) {
                    config.isLicensed = true;
                    ModConfig.save();
                    errorMessage = "";
                    this.init();
                } else {
                    errorMessage = "§c✘ Błąd: Niepoprawny klucz lub zajęte IP!";
                }
            }).dimensions(centerX - 110, centerY + 15, 220, 24).build());
            return;
        }

        int panelWidth = 420;
        int panelHeight = 250;
        int leftX = (this.width - panelWidth) / 2;
        int topY = (this.height - panelHeight) / 2;

        // Zakładki boczne
        this.addDrawableChild(ButtonWidget.builder(Text.literal(currentTab == Tab.PLAYERS ? "§d⚡ Gracze" : "§fGracze"), 
            b -> { playClickSound(1.0f); currentTab = Tab.PLAYERS; this.init(); }).dimensions(leftX + 12, topY + 45, 100, 24).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal(currentTab == Tab.SETTINGS ? "§d⚡ Ustawienia" : "§fUstawienia"), 
            b -> { playClickSound(1.0f); currentTab = Tab.SETTINGS; this.init(); }).dimensions(leftX + 12, topY + 75, 100, 24).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal(currentTab == Tab.THEMES ? "§d⚡ Motywy" : "§fMotywy"), 
            b -> { playClickSound(1.0f); currentTab = Tab.THEMES; this.init(); }).dimensions(leftX + 12, topY + 105, 100, 24).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal(currentTab == Tab.LICENSE ? "§d⚡ Licencja" : "§fLicencja"), 
            b -> { playClickSound(1.0f); currentTab = Tab.LICENSE; this.init(); }).dimensions(leftX + 12, topY + 135, 100, 24).build());

        // Ustawienia
        if (currentTab == Tab.SETTINGS) {
            int contentX = leftX + 125;
            int contentY = topY + 45;
            this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Tracers na cele: " + (config.tracersEnabled ? "§aWŁĄCZONE" : "§cWYŁĄCZONE")),
                b -> {
                    playClickSound(1.1f);
                    config.tracersEnabled = !config.tracersEnabled;
                    ModConfig.save();
                    this.init();
                }
            ).dimensions(contentX, contentY, 280, 24).build());
        }

        // Generator kluczy (Owner)
        if (currentTab == Tab.LICENSE && config.playerRank.equalsIgnoreCase("OWNER")) {
            int contentX = leftX + 125;
            int contentY = topY + 90;
            
            this.targetUserField = new TextFieldWidget(this.textRenderer, contentX, contentY, 280, 20, Text.literal("Nick gracza"));
            this.targetUserField.setMaxLength(32);
            this.targetUserField.setPlaceholder(Text.literal("Wpisz nick gracza docelowego..."));
            this.addDrawableChild(this.targetUserField);

            this.addDrawableChild(ButtonWidget.builder(Text.literal("§dGeneruj i Skopiuj Klucz VIP"), b -> {
                playClickSound(1.4f);
                if (targetUserField != null && !targetUserField.getText().isEmpty()) {
                    generatedKeyResult = ModConfig.generateKeyForUser(targetUserField.getText().trim(), "TWÓJ_TAJNY_SEKRET_ADMINA");
                    if (this.client != null) {
                        this.client.keyboard.setClipboard(generatedKeyResult);
                    }
                    this.init();
                }
            }).dimensions(contentX, contentY + 26, 280, 22).build());
        }

        // Przycisk ataku w prawym panelu
        if (currentTab == Tab.PLAYERS && selectedPlayer != null) {
            int rightPanelX = leftX + 265;
            int contentY = topY + 130;
            UUID targetUuid = selectedPlayer.getUuid();
            long currentTime = System.currentTimeMillis();
            boolean onCooldown = attackCooldowns.containsKey(targetUuid) && attackCooldowns.get(targetUuid) > currentTime;
            boolean isAttacking = activeAttacks.containsKey(targetUuid) && activeAttacks.get(targetUuid) > currentTime;

            String btnText = onCooldown ? "§c⏳ COOLDOWN" : (isAttacking ? "§e⚠ ATAK W TOKU" : "§4🔥 WYŚLIJ BOMBE");
            
            ButtonWidget attackBtn = ButtonWidget.builder(Text.literal(btnText), btn -> {
                if (!onCooldown) {
                    if (this.client != null && this.client.player != null) {
                        this.client.player.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 0.5f, 1.0f);
                    }
                    activeAttacks.put(targetUuid, currentTime + 60000L); // 60s bomby
                    attackCooldowns.put(targetUuid, currentTime + 120000L); // 120s cooldownu
                    this.init();
                }
            }).dimensions(rightPanelX, contentY, 145, 28).build();

            attackBtn.active = !onCooldown;
            this.addDrawableChild(attackBtn);
        }
    }

    private void playClickSound(float pitch) {
        if (this.client != null && this.client.player != null) {
            this.client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4f, pitch);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && ModConfig.get().isLicensed && currentTab == Tab.PLAYERS) {
            int panelWidth = 420;
            int panelHeight = 250;
            int leftX = (this.width - panelWidth) / 2;
            int topY = (this.height - panelHeight) / 2;
            int listX = leftX + 120;
            int listY = topY + 45;

            if (this.client != null && this.client.world != null) {
                int yOffset = listY;
                for (PlayerEntity p : this.client.world.getPlayers()) {
                    if (p == this.client.player) continue;
                    if (mouseX >= listX && mouseX <= listX + 135 && mouseY >= yOffset && mouseY <= yOffset + 20) {
                        playClickSound(1.5f);
                        selectedPlayer = p;
                        this.init();
                        return true;
                    }
                    yOffset += 24;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (animationProgress < 1.0f) {
            animationProgress += delta * 0.15f;
            if (animationProgress > 1.0f) animationProgress = 1.0f;
        }

        this.renderBackground(context, mouseX, mouseY, delta);
        ModConfig config = ModConfig.get();

        if (!config.isLicensed) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            
            context.fill(centerX - 150, centerY - 75, centerX + 150, centerY + 75, 0xF20B0B10);
            context.fill(centerX - 150, centerY - 75, centerX + 150, centerY - 45, 0xF2181824);
            context.fill(centerX - 150, centerY - 75, centerX + 150, centerY - 74, 0xFFFF69B4);

            context.drawCenteredTextWithShadow(this.textRenderer, "§d§l⚡ DRIPEUSZEKBOMBA ⚡", centerX, centerY - 62, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, "Wklej swój klucz licencyjny (1 IP):", centerX, centerY - 38, 0xFFAAAAAA);

            if (!errorMessage.isEmpty()) {
                context.drawCenteredTextWithShadow(this.textRenderer, errorMessage, centerX, centerY + 52, 0xFFFF5555);
            }

            super.render(context, mouseX, mouseY, delta);
            return;
        }

        int panelWidth = 420;
        int panelHeight = 250;
        int leftX = (this.width - panelWidth) / 2;
        int topY = (this.height - panelHeight) / 2;

        context.fill(leftX - 2, topY - 2, leftX + panelWidth + 2, topY + panelHeight + 2, 0x33FF69B4);
        context.fill(leftX, topY, leftX + panelWidth, topY + panelHeight, 0xF50D0D14);
        context.fill(leftX, topY, leftX + 115, topY + panelHeight, 0xF814141E);
        context.fill(leftX, topY, leftX + panelWidth, topY + 32, 0xF81A1A26);
        context.fill(leftX + 115, topY, leftX + 116, topY + panelHeight, 0xFFFF69B4);
        context.fill(leftX, topY, leftX + panelWidth, topY + 1, 0xFFFF69B4);

        context.drawText(this.textRenderer, "§d§l⚡ DripeuszekBomba", leftX + 12, topY + 11, 0xFFFFFFFF, true);
        context.drawText(this.textRenderer, "§7v1.21.4", leftX + 125, topY + 13, 0xFF888888, true);

        int contentX = leftX + 125;
        int contentY = topY + 45;

        if (currentTab == Tab.PLAYERS) {
            context.drawText(this.textRenderer, "§f🎯 Gracze:", contentX, contentY - 10, 0xFFFF69B4, true);
            if (this.client != null && this.client.world != null) {
                int yOffset = contentY + 5;
                for (PlayerEntity p : this.client.world.getPlayers()) {
                    if (p == this.client.player) continue;
                    boolean isSelected = (selectedPlayer == p);
                    int bgColor = isSelected ? 0x66FF69B4 : 0x22FFFFFF;
                    
                    context.fill(contentX, yOffset, contentX + 135, yOffset + 20, bgColor);
                    context.drawText(this.textRenderer, "§f👤 " + p.getName().getString(), contentX + 6, yOffset + 6, 0xFFFFFFFF, true);
                    yOffset += 24;
                }

                // Prawy panel informacyjny
                int rightPanelX = leftX + 265;
                context.fill(rightPanelX - 5, contentY - 15, rightPanelX + 145, topY + 230, 0x331E1E2A);
                
                if (selectedPlayer != null) {
                    UUID targetUuid = selectedPlayer.getUuid();
                    long currentTime = System.currentTimeMillis();
                    boolean isAttacking = activeAttacks.containsKey(targetUuid) && activeAttacks.get(targetUuid) > currentTime;
                    long timeLeft = isAttacking ? (activeAttacks.get(targetUuid) - currentTime) / 1000 : 0;

                    context.drawText(this.textRenderer, "§d§l[ INFORMACJE ]", rightPanelX, contentY, 0xFFFFFFFF, true);
                    context.drawText(this.textRenderer, "Cel: §b" + selectedPlayer.getName().getString(), rightPanelX, contentY + 20, 0xFFFFFF, true);
                    context.drawText(this.textRenderer, "Status: " + (isAttacking ? "§c§lBOMBA ( " + timeLeft + "s )" : "§aBEZPIECZNY"), rightPanelX, contentY + 35, 0xFFFFFF, true);
                    
                    double dist = this.client.player != null ? this.client.player.distanceTo(selectedPlayer) : 0;
                    context.drawText(this.textRenderer, "Dystans: §e" + String.format("%.1f", dist) + "m", rightPanelX, contentY + 50, 0xFFFFFF, true);
                } else {
                    context.drawText(this.textRenderer, "§7Wybierz gracza", rightPanelX, contentY, 0x888888, true);
                    context.drawText(this.textRenderer, "§7z listy obok,", rightPanelX, contentY + 12, 0x888888, true);
                    context.drawText(this.textRenderer, "§7aby zobaczyć", rightPanelX, contentY + 24, 0x888888, true);
                    context.drawText(this.textRenderer, "§7szczegóły i atak.", rightPanelX, contentY + 36, 0x888888, true);
                }
            }
        } 
        else if (currentTab == Tab.SETTINGS) {
            context.drawText(this.textRenderer, "§l⚙ Ustawienia Zaawansowane", contentX, contentY - 10, 0xFFFF69B4, true);
            context.drawText(this.textRenderer, "§7Zarządzaj funkcjami wspomagającymi rozgrywkę.", contentX, contentY + 35, 0xFFAAAAAA, true);
        } 
        else if (currentTab == Tab.THEMES) {
            context.drawText(this.textRenderer, "§l🎨 Motywy i Personalizacja", contentX, contentY - 10, 0xFFFF69B4, true);
            context.drawText(this.textRenderer, "Aktywny motyw: §dDark Neon Pink (Ultimate)", contentX, contentY + 10, 0xFFFFFFFF, true);
        } 
        else if (currentTab == Tab.LICENSE) {
            context.drawText(this.textRenderer, "§l🔒 Status Licencji i System", contentX, contentY - 10, 0xFFFF69B4, true);
            context.drawText(this.textRenderer, "Konto: §b" + config.playerName, contentX, contentY + 10, 0xFFFFFFFF, true);
            context.drawText(this.textRenderer, "Ranga: §d" + config.playerRank, contentX, contentY + 24, 0xFFFFFFFF, true);

            if (config.playerRank.equalsIgnoreCase("OWNER")) {
                context.drawText(this.textRenderer, "§e⚡ Panel Generowania Kluczy VIP:", contentX, contentY + 45, 0xFFFFFF, true);
                if (!generatedKeyResult.isEmpty()) {
                    context.drawText(this.textRenderer, "Wygenerowany klucz:", contentX, contentY + 115, 0xAAAAAA, true);
                    context.drawText(this.textRenderer, "§a" + generatedKeyResult, contentX, contentY + 127, 0xFFFFFF, true);
                    context.drawText(this.textRenderer, "§a✔ Skopiowano do schowka!", contentX, contentY + 142, 0x55FF55, true);
                }
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
