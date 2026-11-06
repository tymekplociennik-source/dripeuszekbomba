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
        super(Text.literal("DripeuszekBomba Modern"));
    }

    @Override
    protected void init() {
        super.init();
        ModConfig config = ModConfig.get();

        if (!config.isLicensed) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;

            this.licenseField = new TextFieldWidget(this.textRenderer, centerX - 110, centerY - 15, 220, 22, Text.literal("Klucz"));
            this.licenseField.setMaxLength(256);
            this.licenseField.setText(config.licenseKey);
            this.addDrawableChild(this.licenseField);

            this.addDrawableChild(ButtonWidget.builder(Text.literal("§aZweryfikuj Licencję"), b -> {
                String enteredKey = licenseField.getText();
                String playerName = this.client != null && this.client.player != null ? this.client.player.getName().getString() : "Player";
                
                config.licenseKey = enteredKey;
                config.playerName = playerName;
                
                if (ModConfig.validateKey(enteredKey, playerName)) {
                    config.isLicensed = true;
                    ModConfig.save();
                    this.init();
                }
            }).dimensions(centerX - 110, centerY + 15, 220, 24).build());
            return;
        }

        int leftX = this.width / 2 - 160;
        int topY = this.height / 2 - 100;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§7> §fGracze"), b -> { currentTab = Tab.PLAYERS; this.init(); }).dimensions(leftX, topY + 40, 110, 22).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("§7> §fUstawienia"), b -> { currentTab = Tab.SETTINGS; this.init(); }).dimensions(leftX, topY + 68, 110, 22).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("§7> §fMotywy"), b -> { currentTab = Tab.THEMES; this.init(); }).dimensions(leftX, topY + 96, 110, 22).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("§7> §fLicencja"), b -> { currentTab = Tab.LICENSE; this.init(); }).dimensions(leftX, topY + 124, 110, 22).build());

        if (currentTab == Tab.SETTINGS) {
            int contentX = leftX + 130;
            int contentY = topY + 40;
            this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Tracers na atakowanych: " + (config.tracersEnabled ? "§aWŁĄCZONE" : "§cWYŁĄCZONE")),
                b -> {
                    config.tracersEnabled = !config.tracersEnabled;
                    ModConfig.save();
                    this.init();
                }
            ).dimensions(contentX, contentY, 180, 24).build());
        }

        if (currentTab == Tab.LICENSE && config.playerRank.equals("OWNER")) {
            int contentX = leftX + 130;
            int contentY = topY + 90;
            
            this.targetUserField = new TextFieldWidget(this.textRenderer, contentX, contentY, 180, 20, Text.literal("Nick gracza"));
            this.targetUserField.setMaxLength(32);
            this.targetUserField.setPlaceholder(Text.literal("Wpisz nick gracza..."));
            this.addDrawableChild(this.targetUserField);

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Generuj Klucz"), b -> {
                if (targetUserField != null && !targetUserField.getText().isEmpty()) {
                    generatedKeyResult = ModConfig.generateKeyForUser(targetUserField.getText(), "TWÓJ_TAJNY_SEKRET_ADMINA");
                }
            }).dimensions(contentX, contentY + 25, 180, 20).build());
        }

        if (currentTab == Tab.PLAYERS && selectedPlayer != null) {
            int contentX = leftX + 130;
            int contentY = topY + 140;
            UUID targetUuid = selectedPlayer.getUuid();
            long currentTime = System.currentTimeMillis();
            boolean onCooldown = attackCooldowns.containsKey(targetUuid) && attackCooldowns.get(targetUuid) > currentTime;
            boolean isAttacking = activeAttacks.containsKey(targetUuid) && activeAttacks.get(targetUuid) > currentTime;

            String btnText = onCooldown ? "§cCooldown..." : (isAttacking ? "§eATAK AKTYWNY (60s)" : "§4URUCHOM ATAK (60s)");
            
            ButtonWidget attackButton = ButtonWidget.builder(Text.literal(btnText), b -> {
                if (!onCooldown) {
                    activeAttacks.put(targetUuid, currentTime + 60000L);
                    attackCooldowns.put(targetUuid, currentTime + 120000L);
                    this.init();
                }
            }).dimensions(contentX, contentY, 180, 24).build();

            attackButton.active = !onCooldown;
            this.addDrawableChild(attackButton);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && currentTab == Tab.PLAYERS) {
            int leftX = this.width / 2 - 160;
            int topY = this.height / 2 - 100;
            int contentX = leftX + 135;
            int contentY = topY + 35 + 15;
            
            if (this.client != null && this.client.world != null) {
                int yOffset = contentY;
                for (PlayerEntity p : this.client.world.getPlayers()) {
                    if (p == this.client.player) continue;
                    if (mouseX >= contentX && mouseX <= contentX + 175 && mouseY >= yOffset && mouseY <= yOffset + 18) {
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
            context.fill(centerX - 130, centerY - 70, centerX + 130, centerY + 65, 0xF2121218);
            context.fill(centerX - 130, centerY - 70, centerX + 130, centerY - 45, 0xF21E1E2A);
            context.fill(centerX - 130, centerY - 70, centerX + 130, centerY - 69, 0xFFFF69B4);
            context.drawCenteredTextWithShadow(this.textRenderer, "§d§lDripeuszekBomba §7| Licencja", centerX, centerY - 58, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, "Wpisz klucz, aby odblokować mod:", centerX, centerY - 35, 0xFFAAAAAA);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        int leftX = this.width / 2 - 160;
        int topY = this.height / 2 - 100;
        int width = 320;
        int height = 210;

        context.fill(leftX, topY, leftX + width, topY + height, 0xF2121218);
        context.fill(leftX, topY, leftX + 125, topY + height, 0xF2181822);
        context.fill(leftX, topY, leftX + width, topY + 25, 0xF21E1E2A);
        context.fill(leftX + 125, topY, leftX + 126, topY + height, 0xFFFF69B4);
        context.fill(leftX, topY, leftX + width, topY + 1, 0xFFFF69B4);

        context.drawText(this.textRenderer, "§d§lDripeuszekBomba", leftX + 12, topY + 8, 0xFFFFFFFF, true);

        int contentX = leftX + 135;
        int contentY = topY + 35;

        if (currentTab == Tab.PLAYERS) {
            context.drawText(this.textRenderer, "§fOnline Gracze:", contentX, contentY, 0xFFFF69B4, true);
            if (this.client != null && this.client.world != null) {
                int yOffset = contentY + 15;
                for (PlayerEntity p : this.client.world.getPlayers()) {
                    if (p == this.client.player) continue;
                    boolean isSelected = (selectedPlayer == p);
                    int bgColor = isSelected ? 0x55FF69B4 : 0x22FFFFFF;
                    context.fill(contentX, yOffset, contentX + 175, yOffset + 18, bgColor);
                    context.drawText(this.textRenderer, "§f" + p.getName().getString(), contentX + 6, yOffset + 5, 0xFFFFFFFF, true);
                    yOffset += 22;
                }
                if (selectedPlayer != null) {
                    context.drawText(this.textRenderer, "Wybrano: §d" + selectedPlayer.getName().getString(), contentX, topY + 120, 0xFFFFFF, true);
                }
            }
        } 
        else if (currentTab == Tab.SETTINGS) {
            context.drawText(this.textRenderer, "§lUstawienia Globalne", contentX, contentY, 0xFFFF69B4, true);
        } 
        else if (currentTab == Tab.THEMES) {
            context.drawText(this.textRenderer, "§lMotywy i Styl", contentX, contentY, 0xFFFF69B4, true);
            context.drawText(this.textRenderer, "§7Aktywny motyw: §dDark Neon Pink", contentX, contentY + 15, 0xAAAAAA, true);
        } 
        else if (currentTab == Tab.LICENSE) {
            context.drawText(this.textRenderer, "§lPanel Licencji", contentX, contentY, 0xFFFF69B4, true);
            context.drawText(this.textRenderer, "Nick: §b" + config.playerName, contentX, contentY + 20, 0xFFFFFFFF, true);
            context.drawText(this.textRenderer, "Ranga: §d" + config.playerRank, contentX, contentY + 35, 0xFFFFFFFF, true);
            context.drawText(this.textRenderer, "Status: §aZweryfikowana", contentX, contentY + 50, 0xFFFFFFFF, true);

            if (config.playerRank.equals("OWNER")) {
                context.drawText(this.textRenderer, "§eGenerator Kluczy:", contentX, contentY + 68, 0xFFFFFF, true);
                if (!generatedKeyResult.isEmpty()) {
                    context.drawText(this.textRenderer, "Klucz: §a" + generatedKeyResult, contentX, contentY + 115, 0xFFFFFF, true);
                }
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
