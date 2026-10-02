package com.golda.patchertiktok;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;

/**
 * Restarts TikTok so settings that are read once at startup take effect.
 *
 * A dying process cannot relaunch itself, and alarms are deferred or blocked from the
 * background. Instead the main process starts one of TikTok's own activities that runs in its
 * separate ":safemode" process; {@link SettingsEntry} swaps it for {@link Trampoline}, which waits
 * for the main process to exit and then opens TikTok again.
 */
final class AppRestart {
    static final String HOST = "com.bytedance.ies.safemode.SafeModeBlankActivity";
    static final String EXTRA = "com.golda.patchertiktok.RESTART";

    private AppRestart() { }

    static void restart(Activity activity) {
        Intent trampoline = new Intent().setClassName(activity.getPackageName(), HOST)
                .putExtra(EXTRA, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try {
            activity.startActivity(trampoline);
        } catch (RuntimeException error) {
            RuntimeLog.log("restart trampoline unavailable: " + error.getClass().getSimpleName());
        }
        activity.getWindow().getDecorView().postDelayed(() -> {
            Process.killProcess(Process.myPid());
            System.exit(0);
        }, 150);
    }

    /** Runs in TikTok's ":safemode" process. */
    public static final class Trampoline extends Activity {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private long started;

        @Override
        protected void onCreate(Bundle saved) {
            super.onCreate(saved);
            started = SystemClock.uptimeMillis();
            handler.post(this::waitForMainProcess);
        }

        private void waitForMainProcess() {
            if (mainProcessAlive() && SystemClock.uptimeMillis() - started < 3_000) {
                handler.postDelayed(this::waitForMainProcess, 100);
                return;
            }
            Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(launch);
            }
            finish();
            overridePendingTransition(0, 0);
            handler.postDelayed(() -> Process.killProcess(Process.myPid()), 500);
        }

        private boolean mainProcessAlive() {
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (manager == null || manager.getRunningAppProcesses() == null) return false;
            for (ActivityManager.RunningAppProcessInfo info : manager.getRunningAppProcesses()) {
                if (getPackageName().equals(info.processName)) return true;
            }
            return false;
        }
    }
}
