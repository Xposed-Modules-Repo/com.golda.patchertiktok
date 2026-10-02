package com.golda.patchertiktok;

import android.content.Context;
import android.content.res.Resources;
import android.telephony.TelephonyManager;

import java.util.Locale;

final class DeviceSignals {
    private static final ThreadLocal<Boolean> READING_DEVICE = new ThreadLocal<>();
    private static volatile RecommendationProfile profile =
            new RecommendationProfile(deviceLocale(), "", "");

    private DeviceSignals() {
    }

    static RecommendationProfile current() {
        return profile;
    }

    static boolean readingDevice() {
        return Boolean.TRUE.equals(READING_DEVICE.get());
    }

    static Locale deviceLocale() {
        return Resources.getSystem().getConfiguration().getLocales().get(0);
    }

    static Locale systemLocale(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            android.app.LocaleManager manager = context.getSystemService(android.app.LocaleManager.class);
            if (manager != null && !manager.getSystemLocales().isEmpty()) return manager.getSystemLocales().get(0);
        }
        return deviceLocale();
    }

    static void refresh(Context context) {
        String country = "";
        String operator = "";
        READING_DEVICE.set(true);
        try {
            TelephonyManager telephony = context.getSystemService(TelephonyManager.class);
            if (telephony != null) {
                country = telephony.getSimCountryIso();
                operator = telephony.getSimOperator();
            }
        } catch (RuntimeException ignored) {
            // No SIM or unavailable telephony: do not invent a carrier identity.
        } finally {
            READING_DEVICE.remove();
        }
        profile = new RecommendationProfile(systemLocale(context), country, operator);
    }
}
