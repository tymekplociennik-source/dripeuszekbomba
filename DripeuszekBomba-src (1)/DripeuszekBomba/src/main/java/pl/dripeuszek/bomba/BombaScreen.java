package pl.dripeuszek.bomba;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.UUID;

public class BombaScreen extends Screen {
    public enum Tab { PLAYERS, SETTINGS, THEMES, LICENSE }
    private Tab currentTab = Tab.PLAYERS;

    private static final HashMap<UUID, Long> attackCooldowns = new HashMap<>();
    private static final HashMap<UUID, Long> activeAttacks = new HashMap<>();
    private PlayerEntity selectedPlayer = null;

    private TextFieldWidget licenseField;
    private TextFieldWidget targetUserField;
    private String generatedKeyResult = "";

    public BombaScreen() {
        super(Text.literal("DripeuszekBomba GUI"));
    }

    @Override
    protected void init() {
        super.init();
        this.clearChildren();
        
        ModConfig config = ModConfig.get();

        if (!config.isLicensed) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;

            this.licenseField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY - 20, 200, 20, Text.literal("Klucz"));
            this.licenseField.setMaxLength(256);
            this.licenseField.setText(config.licenseKey);
            this.addDrawableChild(this.licenseField);

            this.addDrawableChild(ButtonWidget.builder(Text.literal("§aZAKTYWUJ LICENCJĘ"), b -> {
                String enteredKey = licenseField.getText().trim();
                String playerName = (this.client != null && this.client.player != null) 
                    ? this.client.player.getName().getString() 
                    : "Player";
                
                config.licenseKey = enteredKey;
                config.playerName = playerName;
                
                if (ModConfig.validateKey(enteredKey, playerName)) {
                    config.isLicensed = true;
                    ModConfig.save();
                    this.init();
                }
            }).dimensions(centerX - 100, centerY + 10, 200, 24).build());
            return;
        }

        int panelWidth = 380;
        int panelHeight = 230;
        int leftX = (this.width - panelWidth) / 2;
        int topY = (this.height - panelHeight) / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.literal(currentTab == Tab.PLAYERS ? "§d> Gracze" : "§fGracze"), 
            b -> { currentTab = Tab.PLAYERS; this.init(); }).dimensions(leftX + 10, topY + 40, 100, 22).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal(currentTab == Tab.SETTINGS ? "§d> Ustawienia" : "§fUstawienia"), 
            b -> { currentTab = Tab.SETTINGS; this.init(); }).dimensions(leftX + 10, topY + 68, 100, 22).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal(currentTab == Tab.THEMES ? "§d> Motywy" : "§fMotywy"), 
            b -> { currentTab = Tab.THEMES; this.init(); }).dimensions(leftX + 10, topY + 96, 100, 22).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.literal(currentTab == Tab.LICENSE ? "§d> Licencja" : "§fLicencja"), 
            b -> { currentTab = Tab.LICENSE; this.init(); }).dimensions(leftX + 10, topY + 124, 100, 22).build());

        if (currentTab == Tab.SETTINGS) {
            int contentX = leftX + 125;
            int contentY = topY + 40;
            this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Tracers: " + (config.tracersEnabled ? "§aWŁĄCZONE" : "§cWYŁĄCZONE")),
                b -> {
                    config.tracersEnabled = !config.tracersEnabled;
                    ModConfig.save();
                    this.init();
                }
            ).dimensions(contentX, contentY, 230, 24).build());
        }

        if (currentTab == Tab.LICENSE && config.playerRank.equalsIgnoreCase("OWNER")) {
            int contentX = leftX + 125;
            int contentY = topY + 95;
            
            this.targetUserField = new TextFieldWidget(this.textRenderer, contentX, contentY, 230, 20, Text.literal("Nick gracza"));
            this.targetUserField.setMaxLength(32);
            this.targetUserField.setPlaceholder(Text.literal("Wpisz nick gracza..."));
            this.addDrawableChild(this.targetUserField);

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Generuj Klucz Licencyjny"), b -> {
                if (targetUserField != null && !targetUserField.getText().isEmpty()) {
                    generatedKeyResult = ModConfig.generateKeyForUser(targetUserField.getText().trim(), "TWÓJ_TAJNY_SEKRET_ADMINA");
                    
                    // Automatyczne kopiowanie klucza do schowka systemowego
                    if (this.client != null) {
                        this.client.keyboard.setClipboard(generatedKeyResult);
                    }
                    this.init();
                }
            }).dimensions(contentX, contentY + 26, 230, 22).build());
        }

        if (currentTab == Tab.PLAYERS && selectedPlayer != null) {
            int contentX = leftX + 125;
            int contentY = topY + 150;
            UUID targetUuid = selectedPlayer.getUuid();
            long currentTime = System.currentTimeMillis();
            boolean onCooldown = attackCooldowns.containsKey(targetUuid) && attackCooldowns.get(targetUuid) > currentTime;
            boolean isAttacking = activeAttacks.containsKey(targetUuid) && activeAttacks.get(targetUuid) > currentTime;

            String btnText = onCooldown ? "§cCOOLDOWN (CZEKAJ)" : (isAttacking ? "§eATAK W TOKU (60s)" : "§4URUCHOM ATAK (60s)");
            
            ButtonWidget attackBtn = ButtonWidget.builder(Text.literal(btnText), btn -> {
                if (!onCooldown) {
                    activeAttacks.put(targetUuid, currentTime + 60000L);
                    attackCooldowns.put(targetUuid, currentTime + 120000L);
                    this.init();
                }
            }).dimensions(contentX, contentY, 230, 24).build();

            attackBtn.active = !onCooldown;
            this.addDrawableChild(attackBtn);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && currentTab == Tab.PLAYERS) {
            int panelWidth = 380;
            int panelHeight = 230;
            int leftX = (this.width - panelWidth) / 2;
            int topY = (this.height - panelHeight) / 2;
            int contentX = leftX + 125;
            int contentY = topY + 45;

            if (this.client != null && this.client.world != null) {
                int yOffset = contentY;
                for (PlayerEntity p : this.client.world.getPlayers()) {
                    if (p == this.client.player) continue;
                    if (mouseX >= contentX && mouseX <= contentX + 230 && mouseY >= yOffset && mouseY <= yOffset + 18) {
                        selectedPlayer = p;
                        this.init();
                        return true;
                    }
                    yOffset += 22;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        ModConfig config = ModConfig.get();

        if (!config.isLicensed) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            
            context.fill(centerX - 130, centerY - 60, centerX + 130, centerY + 50, 0xF2121218);
            context.fill(centerX - 130, centerY - 60, centerX + 130, centerY - 35, 0xF21E1E2A);
            context.fill(centerX - 130, centerY - 60, centerX + 130, centerY - 58, 0xFFFF69B4);

            context.drawCenteredTextWithShadow(this.textRenderer, "§d§lDripeuszekBomba §7| Weryfikacja", centerX, centerY - 50, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, "Wklej klucz licencyjny:", centerX, centerY - 32, 0xFFAAAAAA);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        int panelWidth = 380;
        int panelHeight = 230;
        int leftX = (this.width - panelWidth) / 2;
        int topY = (this.height - panelHeight) / 2;

        context.fill(leftX, topY, leftX + panelWidth, topY + panelHeight, 0xF2121218);
        context.fill(leftX, topY, leftX + 120, topY + panelHeight, 0xF2181822);
        context.fill(leftX, topY, leftX + panelWidth, topY + 28, 0xF21E1E2A);
        context.fill(leftX + 120, topY, leftX + 121, topY + panelHeight, 0xFFFF69B4);
        context.fill(leftX, topY, leftX + panelWidth, topY + 1, 0xFFFF69B4);

        context.drawText(this.textRenderer, "§d§lDripeuszekBomba", leftX + 12, topY + 10, 0xFFFFFFFF, true);

        int contentX = leftX + 125;
        int contentY = topY + 35;

        if (currentTab == Tab.PLAYERS) {
            context.drawText(this.textRenderer, "§fLista graczy online:", contentX, contentY, 0xFFFF69B4, true);
            if (this.client != null && this.client.world != null) {
                int yOffset = contentY + 12;
                for (PlayerEntity p : this.client.world.getPlayers()) {
                    if (p == this.client.player) continue;
                    boolean isSelected = (selectedPlayer == p);
                    int bgColor = isSelected ? 0x55FF69B4 : 0x22FFFFFF;
                    
                    context.fill(contentX, yOffset, contentX + 230, yOffset + 18, bgColor);
                    context.drawText(this.textRenderer, "§f" + p.getName().getString(), contentX + 6, yOffset + 5, 0xFFFFFFFF, true);
                    yOffset += 22;
                }
                if (selectedPlayer != null) {
                    context.drawText(this.textRenderer, "Wybrany cel: §d" + selectedPlayer.getName().getString(), contentX, topY + 135, 0xFFFFFF, true);
                }
            }
        } 
        else if (currentTab == Tab.SETTINGS) {
            context.drawText(this.textRenderer, "§lUstawienia Globalne", contentX, contentY, 0xFFFF69B4, true);
        } 
        else if (currentTab == Tab.THEMES) {
            context.drawText(this.textRenderer, "§lMotywy i Styl", contentX, contentY, 0xFFFF69B4, true);
            context.drawText(this.textRenderer, "Motyw: §dDark Neon Pink", contentX, contentY + 20, 0xAAAAAA, true);
        } 
        else if (currentTab == Tab.LICENSE) {
            context.drawText(this.textRenderer, "§lStatus Licencji", contentX, contentY, 0xFFFF69B4, true);
            context.drawText(this.textRenderer, "Użytkownik: §b" + config.playerName, contentX, contentY + 18, 0xFFFFFFFF, true);
            context.drawText(this.textRenderer, "Ranga: §d" + config.playerRank, contentX, contentY + 32, 0xFFFFFFFF, true);

            if (config.playerRank.equalsIgnoreCase("OWNER")) {
                context.drawText(this.textRenderer, "§eGenerator Licencji (Panel Ownera):", contentX, contentY + 50, 0xFFFFFF, true);
                if (!generatedKeyResult.isEmpty()) {
                    context.drawText(this.textRenderer, "Wygenerowany klucz:", contentX, contentY + 98, 0xAAAAAA, true);
                    context.drawText(this.textRenderer, "§a" + generatedKeyResult, contentX, contentY + 110, 0xFFFFFF, true);
                    context.drawText(this.textRenderer, "§a✔ Skopiowano do schowka!", contentX, contentY + 124, 0x55FF55, true);
                }
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
