package pl.dripeuszek.bomba;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class BombaScreen extends Screen {
    private enum Tab {
        GRACZE("Gracze"),
        USTAWIENIA("Ustawienia"),
        MOTYWY("Motywy"),
        LICENCJA("Licencja");

        final String label;
        Tab(String label) { this.label = label; }
    }

    private static final int SIDEBAR = 110;
    private static final String LICENSE =
            "MIT License\n\n"
          + "Copyright (c) 2026 DripeuszekBomba\n\n"
          + "Permission is hereby granted, free of charge, to any person obtaining a copy of this software "
          + "and associated documentation files (the \"Software\"), to deal in the Software without restriction, "
          + "including without limitation the rights to use, copy, modify, merge, publish, distribute, "
          + "sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is "
          + "furnished to do so, subject to the following conditions:\n\n"
          + "The above copyright notice and this permission notice shall be included in all copies or "
          + "substantial portions of the Software.\n\n"
          + "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT "
          + "NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND "
          + "NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, "
          + "DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, "
          + "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.";

    private static Tab currentTab = Tab.GRACZE;

    private double scroll = 0;
    private double maxScroll = 0;
    private int px, py, pw, ph;

    public BombaScreen() {
        super(Text.literal("DripeuszekBomba"));
    }

    @Override
    protected void init() {
        pw = Math.min(width - 20, 440);
        ph = Math.min(height - 20, 280);
        px = (width - pw) / 2;
        py = (height - ph) / 2;

        // Zakładki po lewej
        int by = py + 34;
        for (Tab t : Tab.values()) {
            String label = (t == currentTab ? "> " : "") + t.label;
            addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> {
                currentTab = t;
                scroll = 0;
                clearAndInit();
            }).dimensions(px + 6, by, SIDEBAR - 12, 20).build());
            by += 24;
        }

        int cx = px + SIDEBAR + 12;
        int cy = py + 34;
        int cw = pw - SIDEBAR - 24;
        ModConfig cfg = ModConfig.get();

        switch (currentTab) {
            case USTAWIENIA -> {
                addToggle(cx, cy, cw, "Pokaż FPS", () -> cfg.showFps, v -> cfg.showFps = v);
                addToggle(cx, cy + 24, cw, "Pokaż koordynaty", () -> cfg.showCoords, v -> cfg.showCoords = v);
            }
            case MOTYWY -> {
                int ty = cy;
                for (Theme th : Theme.values()) {
                    boolean selected = th.name().equals(cfg.theme);
                    Text label = Text.literal((selected ? "> " : "") + th.label + (selected ? " <" : ""))
                            .withColor(th.accent & 0xFFFFFF);
                    addDrawableChild(ButtonWidget.builder(label, btn -> {
                        cfg.theme = th.name();
                        ModConfig.save();
                        clearAndInit();
                    }).dimensions(cx, ty, cw, 20).build());
                    ty += 24;
                }
            }
            default -> { }
        }
    }

    private void addToggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
        addDrawableChild(ButtonWidget.builder(toggleText(label, get.getAsBoolean()), btn -> {
            boolean now = !get.getAsBoolean();
            set.accept(now);
            btn.setMessage(toggleText(label, now));
            ModConfig.save();
        }).dimensions(x, y, w, 20).build());
    }

    private static Text toggleText(String label, boolean value) {
        return Text.literal(label + ": " + (value ? "WŁ." : "WYŁ."));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // własne tło rysujemy w render()
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Theme th = Theme.byName(ModConfig.get().theme);

        ctx.fill(0, 0, width, height, 0x90000000);
        ctx.fill(px, py, px + pw, py + ph, th.bg);
        ctx.fill(px, py, px + SIDEBAR, py + ph, th.sidebar);
        ctx.fill(px, py, px + pw, py + 2, th.accent);
        ctx.fill(px + SIDEBAR - 1, py, px + SIDEBAR, py + ph, th.accent);

        ctx.drawText(textRenderer, "DripeuszekBomba", px + 8, py + 12, th.accent, true);

        int cx = px + SIDEBAR + 12;
        int cw = pw - SIDEBAR - 24;
        int top = py + 34;
        int bottom = py + ph - 8;
        ctx.drawText(textRenderer, currentTab.label, cx, py + 12, th.accent, true);

        switch (currentTab) {
            case GRACZE -> renderPlayers(ctx, th, cx, top, cw, bottom);
            case USTAWIENIA -> ctx.drawText(textRenderer, "Zmiany zapisują się automatycznie.",
                    cx, top + 56, th.subText, false);
            case MOTYWY -> ctx.drawText(textRenderer, "Wybierz kolor interfejsu.",
                    cx, top + 24 * Theme.values().length + 4, th.subText, false);
            case LICENCJA -> renderLicense(ctx, th, cx, top, cw, bottom);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderPlayers(DrawContext ctx, Theme th, int x, int top, int w, int bottom) {
        List<PlayerListEntry> players = List.of();
        if (client != null && client.getNetworkHandler() != null) {
            players = client.getNetworkHandler().getPlayerList().stream()
                    .sorted(Comparator.comparing(e -> e.getProfile().getName().toLowerCase()))
                    .toList();
        }

        ctx.drawText(textRenderer, "Online: " + players.size(), x + 70, py + 12, th.subText, false);

        final int rowH = 14;
        int areaH = bottom - top;
        maxScroll = Math.max(0, players.size() * rowH - areaH);
        scroll = MathHelper.clamp(scroll, 0, maxScroll);

        ctx.enableScissor(x, top, x + w, bottom);
        int y = top - (int) scroll;
        int i = 0;
        for (PlayerListEntry e : players) {
            if ((i++ & 1) == 0) ctx.fill(x, y, x + w, y + rowH, 0x30FFFFFF);
            ctx.drawText(textRenderer, e.getProfile().getName(), x + 4, y + 3, th.text, false);
            String ping = e.getLatency() + " ms";
            ctx.drawText(textRenderer, ping, x + w - 4 - textRenderer.getWidth(ping), y + 3, th.subText, false);
            y += rowH;
        }
        ctx.disableScissor();

        if (players.isEmpty()) {
            ctx.drawText(textRenderer, "Brak graczy (wejdź do świata).", x, top + 2, th.subText, false);
        }
    }

    private void renderLicense(DrawContext ctx, Theme th, int x, int top, int w, int bottom) {
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(LICENSE), w - 6);
        final int lineH = 10;
        int areaH = bottom - top;
        maxScroll = Math.max(0, lines.size() * lineH - areaH);
        scroll = MathHelper.clamp(scroll, 0, maxScroll);

        ctx.enableScissor(x, top, x + w, bottom);
        int y = top - (int) scroll;
        for (OrderedText line : lines) {
            ctx.drawText(textRenderer, line, x, y, th.text, false);
            y += lineH;
        }
        ctx.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = MathHelper.clamp(scroll - verticalAmount * 14, 0, maxScroll);
        return true;
    }
}
