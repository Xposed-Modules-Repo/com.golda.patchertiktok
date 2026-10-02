package com.golda.patchertiktok;

import de.robv.android.xposed.XposedBridge;

final class RuntimeLog {
    private RuntimeLog() { }

    static void log(String message) {
        XposedBridge.log("TiktokPatchXposed: " + message);
    }
}
