package com.golda.patchertiktok;

import android.app.Application;
import android.content.Context;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class MainHook implements IXposedHookLoadPackage {
    private static final String[] TARGETS = {"com.zhiliaoapp.musically", "com.ss.android.ugc.trill"};
    private final AtomicBoolean started = new AtomicBoolean();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGETS[0].equals(lpparam.packageName) && !TARGETS[1].equals(lpparam.packageName)) return;
        boolean main = lpparam.packageName.equals(lpparam.processName);
        ClassLoader loader = lpparam.classLoader;

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
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (started.compareAndSet(false, true)) startup((Application) param.thisObject, loader, main);
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
            Class<?> type = XposedHelpers.findClassIfExists(name, loader);
            if (type == null) continue;
            for (Method method : type.getDeclaredMethods()) {
                if (method.getParameterTypes().length == 0 && method.getReturnType() == boolean.class
                        && Modifier.isFinal(method.getModifiers()) && Modifier.isPublic(method.getModifiers())) {
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false));
                }
            }
        }
    }
}
