package com.golda.patchertiktok;

final class RuntimeLog {
    private RuntimeLog() { }

    static void log(String message) {
        Hooks.log(message);
    }
}
