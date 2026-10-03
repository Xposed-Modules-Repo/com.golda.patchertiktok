package com.golda.patchertiktok;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.golda.patchertiktok.I18n.S;

/** Tells the user when TikTok did not accept an automatic streak. Posted as TikTok's own notification. */
final class StreakNotice {
    private static final String CHANNEL = "tiktokpatchxposed_streaks";
    private static final int ID = 0x7470_0001;

    private StreakNotice() { }

    // Runs inside TikTok, which holds its own POST_NOTIFICATIONS permission.
    @SuppressLint("NotificationPermission")
    static void failed(Context context) {
        try {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;
            manager.createNotificationChannel(new NotificationChannel(CHANNEL, I18n.get(S.STREAK_CHANNEL),
                    NotificationManager.IMPORTANCE_DEFAULT));
            Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            PendingIntent open = launch == null ? null : PendingIntent.getActivity(context, ID, launch,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            String text = I18n.get(S.STREAK_FAILED_TEXT);
            Notification notification = new Notification.Builder(context, CHANNEL)
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .setContentTitle(I18n.get(S.STREAK_FAILED_TITLE))
                    .setContentText(text)
                    .setStyle(new Notification.BigTextStyle().bigText(text))
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .build();
            manager.notify(ID, notification);
        } catch (RuntimeException error) {
            // Notifications may be disabled for TikTok; the check log still records the failure.
            RuntimeLog.log("streak notice unavailable: " + error.getClass().getSimpleName());
        }
    }
}
