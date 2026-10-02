package com.golda.patchertiktok;

/** Colors matching TikTok's own settings screens for its light and dark app themes. */
final class Palette {
    final boolean dark;
    final int background;
    final int statusBar;
    final int card;
    final int pressed;
    final int textPrimary;
    final int textSecondary;
    final int textTertiary;
    final int chevron;
    final int field;
    final int accent;
    final int trackOn;
    final int trackOff;
    final int thumb;

    private Palette(boolean dark) {
        this.dark = dark;
        if (dark) {
            background = 0xFF000000;
            statusBar = 0xFF121212;
            card = 0xFF1E1E1E;
            pressed = 0xFF2C2C2C;
            textPrimary = 0xFFF6F6F6;
            textSecondary = 0xFF999999;
            textTertiary = 0xFF787878;
            chevron = 0xFF787878;
            field = 0xFF2A2A2A;
            trackOff = 0xFF484848;
        } else {
            background = 0xFFF5F5F5;
            statusBar = 0xFFFFFFFF;
            card = 0xFFFFFFFF;
            pressed = 0xFFEDEDED;
            textPrimary = 0xFF161823;
            textSecondary = 0xFF757575;
            textTertiary = 0xFF8A8B91;
            chevron = 0xFFA8A8A8;
            field = 0xFFF1F1F2;
            trackOff = 0xFFE0E0E0;
        }
        accent = 0xFFFE2C55;
        trackOn = 0xFF20D5EC;
        thumb = 0xFFFFFFFF;
    }

    private static final Palette DARK = new Palette(true);
    private static final Palette LIGHT = new Palette(false);

    static Palette of(boolean dark) { return dark ? DARK : LIGHT; }
}
