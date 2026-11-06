package pl.dripeuszek.bomba;

public enum Theme {
    CIEMNY("Ciemny", 0xFF55AAFF, 0xF0161616, 0xF00E0E0E, 0xFFFFFFFF, 0xFFAAAAAA),
    JASNY("Jasny", 0xFF2266CC, 0xF0EEEEEE, 0xF0D4D4D4, 0xFF202020, 0xFF555555),
    FIOLETOWY("Fioletowy", 0xFFB266FF, 0xF01A1226, 0xF0120C1C, 0xFFFFFFFF, 0xFFBBAADD),
    ROZOWY("Różowy", 0xFFFF66B2, 0xF0261219, 0xF01C0C12, 0xFFFFFFFF, 0xFFDDAABB),
    ZIELONY("Zielony", 0xFF55FF77, 0xF0102014, 0xF00A160D, 0xFFFFFFFF, 0xFFAADDB5);

    public final String label;
    public final int accent, bg, sidebar, text, subText;

    Theme(String label, int accent, int bg, int sidebar, int text, int subText) {
        this.label = label;
        this.accent = accent;
        this.bg = bg;
        this.sidebar = sidebar;
        this.text = text;
        this.subText = subText;
    }

    public static Theme byName(String name) {
        for (Theme t : values()) {
            if (t.name().equals(name)) return t;
        }
        return CIEMNY;
    }
}
