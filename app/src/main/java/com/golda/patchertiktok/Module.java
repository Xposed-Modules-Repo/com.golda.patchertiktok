package com.golda.patchertiktok;

import android.app.Application;
import android.content.Context;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;

/** Everything the module does once TikTok's classes are available, for either entry point. */
final class Module {
    private static final String[] TARGETS = {"com.zhiliaoapp.musically", "com.ss.android.ugc.trill"};
    private static final AtomicBoolean LOADED = new AtomicBoolean();
    private static final AtomicBoolean STARTED = new AtomicBoolean();

    private Module() { }

    static boolean targets(String packageName) {
        return TARGETS[0].equals(packageName) || TARGETS[1].equals(packageName);
    }

    static void load(String packageName, String processName, ClassLoader loader) {
        if (!targets(packageName) || !LOADED.compareAndSet(false, true)) return;
        boolean main = packageName.equals(processName);

        if (main) {
            // These hooks read their toggle on every call, so they are safe before settings load.
            run("feed filter", () -> FeedFilter.install(loader));
            run("LIVE button", () -> LiveButton.install(loader));
            run("downloads", () -> Downloads.install(loader));
            run("clean links", CleanLinks::install);
            run("rendered ads", () -> RenderedAdSkip.install(loader));
            run("google login", () -> googleLoginFix(loader));
        }
        // Every process: the settings screen and the restart trampoline are swapped in here.
        run("settings host", SettingsEntry::installHost);
        Hooks.findAndHook(Application.class, "attach", Context.class, new Hooks.Hook() {
            @Override protected void after(Hooks.Call param) {
                if (STARTED.compareAndSet(false, true)) startup((Application) param.thisObject, loader, main);
            }
        });
    }

    /** Runs once the Application context exists, before TikTok's own onCreate. */
    private static void startup(Application app, ClassLoader loader, boolean main) {
        Prefs.load(app);
        run("region", () -> RegionSpoof.install(loader));
        if (RegionSpoof.active()) run("recommendation", () -> Recommendation.install(loader));
        if (!main) return;
        I18n.use(app.getResources().getConfiguration().getLocales().get(0));
        Discovery.init(app);
        run("A/B overrides", () -> AbOverrides.install(loader));
        run("startup ads", () -> StartupAds.install(loader));
        run("seekbar", () -> Seekbar.install(loader));
        run("settings row", () -> SettingsEntry.install(app, loader));
        run("auto streaks", () -> AutoStreakManager.install(app, loader));
        Discovery.release();
        RuntimeLog.log(BuildConfig.VERSION_NAME + " started in " + app.getPackageName());
    }

    private static void run(String name, Runnable task) {
        try {
            task.run();
        } catch (Throwable error) {
            RuntimeLog.log(name + " unavailable: " + error);
        }
    }

    /** TikTok's Google sign-in breaks on devices it considers modified; keep the standard flow. */
    private static void googleLoginFix(ClassLoader loader) {
        for (String name : new String[]{"com.bytedance.lobby.google.GoogleAuth", "com.bytedance.lobby.google.GoogleOneTapAuth"}) {
            Class<?> type = Hooks.findClass(name, loader);
            if (type == null) continue;
            for (Method method : type.getDeclaredMethods()) {
                if (method.getParameterTypes().length == 0 && method.getReturnType() == boolean.class
                        && Modifier.isFinal(method.getModifiers()) && Modifier.isPublic(method.getModifiers())) {
                    Hooks.hook(method, Hooks.constant(false));
                }
            }
        }
    }
}
