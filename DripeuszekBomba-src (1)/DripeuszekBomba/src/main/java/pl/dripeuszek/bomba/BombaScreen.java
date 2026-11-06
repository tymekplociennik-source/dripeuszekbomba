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

    // System ataków i cooldownów (60s ataku / 60s cooldownu)
    private static final HashMap<UUID, Long> attackCooldowns = new HashMap<>();
    private static final HashMap<UUID, Long> activeAttacks = new HashMap<>();
    private PlayerEntity selectedPlayer = null;

    // Pola do generowania kluczy w zakładce Licencja (tylko dla Ownera)
    private TextFieldWidget targetUserField;
    private String generatedKeyResult = "";

    public BombaScreen() {
        super(Text.literal("DripeuszekBomba Modern"));
    }

    @Override
    protected void init() {
        super.init();
        int leftX = this.width / 2 - 160;
        int topY = this.height / 2 - 100;

        // Przyciski zakładek po lewej stronie (nowoczesny styl)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("§7> §fGracze"), b -> { currentTab = Tab.PLAYERS; this.init(); }).dimensions(leftX, topY + 40, 110, 22).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("§7> §fUstawienia"), b -> { currentTab = Tab.SETTINGS; this.init(); }).dimensions(leftX, topY + 68, 110, 22).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("§7> §fMotywy"), b -> { currentTab = Tab.THEMES; this.init(); }).dimensions(leftX, topY + 96, 110, 22).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("§7> §fLicencja"), b -> { currentTab = Tab.LICENSE; this.init(); }).dimensions(leftX, topY + 124, 110, 22).build());

        // Zakładka Ustawienia: Przycisk Tracers
        if (currentTab == Tab.SETTINGS) {
            int contentX = leftX + 130;
            int contentY = topY + 40;
            this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Tracers na atakowanych: " + (ModConfig.tracersEnabled ? "§aWŁĄCZONE" : "§cWYŁĄCZONE")),
                b -> {
                    ModConfig.tracersEnabled = !ModConfig.tracersEnabled;
                    ModConfig.save();
                    this.init();
                }
            ).dimensions(contentX, contentY, 180, 24).build());
        }

        // Zakładka Licencja: Generator kluczy (widoczny tylko jeśli jesteś OWNEREM)
        if (currentTab == Tab.LICENSE && ModConfig.playerRank.equals("OWNER")) {
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

        // Zakładka Gracze: Przycisk panelu ATAK dla wybranego gracza
        if (currentTab == Tab.PLAYERS && selectedPlayer != null) {
            int contentX = leftX + 130;
            int contentY = topY + 140;
            UUID targetUuid = selectedPlayer.getUuid();
            long currentTime = System.currentTimeMillis();
            boolean onCooldown = attackCooldowns.containsKey(targetUuid) && attackCooldowns.get(targetUuid) > currentTime;
            boolean isAttacking = activeAttacks.containsKey(targetUuid) && activeAttacks.get(targetUuid) > currentTime;

            String btnText = onCooldown ? "§cCooldown..." : (isAttacking ? "§eATAK AKTYWNY (60s)" : "§4URUCHOM ATAK (60s)");
            
            this.addDrawableChild(ButtonWidget.builder(Text.literal(btnText), b -> {
                if (!onCooldown) {
                    activeAttacks.put(targetUuid, currentTime + 60000L); // Atak na 60 sekund
                    attackCooldowns.put(targetUuid, currentTime + 120000L); // 60s ataku + 60s cooldownu
                    this.init();
                }
            }).dimensions(contentX, contentY, 180, 24).disabled(onCooldown).build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        int leftX = this.width / 2 - 160;
        int topY = this.height / 2 - 100;
        int width = 320;
        int height = 210;

        // Nowoczesne tło główne (ciemny matowy grafit z neonową ramką w stylu Twojego moda)
        context.fill(leftX, topY, leftX + width, topY + height, 0xF2121218); // Główne tło
        context.fill(leftX, topY, leftX + 125, topY + height, 0xF2181822); // Panel boczny
        context.fill(leftX, topY, leftX + width, topY + 25, 0xF21E1E2A); // Pasek górny

        // Linia rozdzielająca i akcenty
        context.fill(leftX + 125, topY, leftX + 126, topY + height, 0xFFFF69B4); // Różowa linia z Twojego motywu
        context.fill(leftX, topY, leftX + width, topY + 1, 0xFFFF69B4);

        // Nagłówek i tytuł
        context.drawText(this.textRenderer, "§d§lDripeuszekBomba", leftX + 12, topY + 8, 0xFFFFFFFF, true);

        int contentX = leftX + 135;
        int contentY = topY + 35;

        // RENDEROWANIE ZAWARTOŚCI W ZALEŻNOŚCI OD ZAKŁADKI
        if (currentTab == Tab.PLAYERS) {
            context.drawText(this.textRenderer, "§fOnline Gracze:", contentX, contentY, 0xFFFF69B4, true);
            
            if (this.client != null && this.client.world != null) {
                int yOffset = contentY + 15;
                for (PlayerEntity p : this.client.world.getPlayers()) {
                    if (p == this.client.player) continue;
                    boolean isSelected = (selectedPlayer == p);
                    
                    // Podświetlenie wybranego gracza
                    int bgColor = isSelected ? 0x55FF69B4 : 0x22FFFFFF;
                    context.fill(contentX, yOffset, contentX + 175, yOffset + 18, bgColor);
                    
                    context.drawText(this.textRenderer, "§f" + p.getName().getString(), contentX + 6, yOffset + 5, 0xFFFFFFFF, true);
                    
                    // Obsługa kliknięcia myszką w listę graczy
                    if (mouseX >= contentX && mouseX <= contentX + 175 && mouseY >= yOffset && mouseY <= yOffset + 18) {
                        if (this.client.mouse.wasButtonReleased(0)) {
                            selectedPlayer = p;
                            this.init();
                        }
                    }
                    yOffset += 22;
                }

                if (selectedPlayer != null) {
                    context.drawText(this.textRenderer, "Wybrano: §d" + selectedPlayer.getName().getString(), contentX, topY + 120, 0xFFFFFF, true);
                }
            }
        } 
        else if (currentTab == Tab.SETTINGS) {
            context.drawText(this.textRenderer, "§lUstawienia Globalne", contentX, contentY, 0xFFFF69B4, true);
            context.drawText(this.textRenderer, "§7Tutaj zarządzasz funkcjami pomocniczymi.", contentX, contentY + 15, 0x888888, true);
        } 
        else if (currentTab == Tab.THEMES) {
            context.drawText(this.textRenderer, "§lMotywy i Styl", contentX, contentY, 0xFFFF69B4, true);
            context.drawText(this.textRenderer, "§7Aktywny motyw: §dDark Neon Pink", contentX, contentY + 15, 0xAAAAAA, true);
        } 
        else if (currentTab == Tab.LICENSE) {
            context.drawText(this.textRenderer, "§lPanel Licencji", contentX, contentY, 0xFFFF69B4, true);
            context.drawText(this.textRenderer, "Nick: §b" + ModConfig.playerName, contentX, contentY + 20, 0xFFFFFFFF, true);
            context.drawText(this.textRenderer, "Ranga: §d" + ModConfig.playerRank, contentX, contentY + 35, 0xFFFFFFFF, true);
            context.drawText(this.textRenderer, "Status: §aZweryfikowana", contentX, contentY + 50, 0xFFFFFFFF, true);

            // Jeśli jesteś Ownerem, pokaż sekcję tworzenia kluczy
            if (ModConfig.playerRank.equals("OWNER")) {
                context.drawText(this.textRenderer, "§eGenerator Kluczy (Tylko Owner):", contentX, contentY + 68, 0xFFFFFF, true);
                if (!generatedKeyResult.isEmpty()) {
                    context.drawText(this.textRenderer, "Klucz: §a" + generatedKeyResult, contentX, contentY + 115, 0xFFFFFF, true);
                }
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
