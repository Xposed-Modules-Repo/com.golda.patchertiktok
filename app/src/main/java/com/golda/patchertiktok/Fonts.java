package com.golda.patchertiktok;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;

/** TikTok Sans from TikTok's own font split, with system fallbacks. */
final class Fonts {
    private static final String ASSET = "font/TikTokSans-VF.otf";
    private static volatile Typeface regular;
    private static volatile Typeface semibold;
    private static volatile Typeface bold;

    private Fonts() { }

    static Typeface regular(Context context) {
        if (regular == null) regular = load(context, 400);
        return regular;
    }

    static Typeface semibold(Context context) {
        if (semibold == null) semibold = load(context, 600);
        return semibold;
    }

    static Typeface bold(Context context) {
        if (bold == null) bold = load(context, 700);
        return bold;
    }

    private static Typeface load(Context context, int weight) {
        try {
            Typeface typeface = new Typeface.Builder(context.getAssets(), ASSET)
                    .setFontVariationSettings("'wght' " + weight)
                    .setWeight(weight)
                    .build();
            if (typeface != null) return typeface;
        } catch (RuntimeException ignored) {
            // The font split is optional; fall through to the system font.
        }
        if (Build.VERSION.SDK_INT >= 28) return Typeface.create(Typeface.SANS_SERIF, weight, false);
        return weight >= 600 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT;
    }
}
