package com.golda.patchertiktok;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** LSPosed's "module settings" button: opens the settings screen inside TikTok. No UI of its own. */
public final class OpenInTikTok extends Activity {
    private static final String[] TARGETS = {"com.zhiliaoapp.musically", "com.ss.android.ugc.trill"};

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        for (String target : TARGETS) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(target);
            if (launch == null) continue;
            // A distinct action makes Android deliver a new intent even if TikTok is already running.
            launch.setAction(SettingsEntry.OPEN_EXTRA).removeCategory(Intent.CATEGORY_LAUNCHER);
            launch.putExtra(SettingsEntry.OPEN_EXTRA, true);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(launch);
                break;
            } catch (RuntimeException ignored) { }
        }
        finish();
    }
}
