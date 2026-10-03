package com.golda.patchertiktok;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.SystemClock;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


/**
 * Adds a native "TiktokPatchXposed" row at the top of TikTok's Settings and privacy screen
 * and hosts the module's own screen inside TikTok's settings activity.
 *
 * The row is a regular TikTok privacy cell: a proxy item implementing the privacy-item
 * interface is appended to the settings list, and {@code PrivacyItemVM.defaultState} returns
 * our title, icon and click handler for its key. All obfuscated classes are reached from
 * stable ones (fragment, view models) through generic signatures and call sites.
 */
final class SettingsEntry {
    static final String HOST_ACTIVITY = "com.ss.android.ugc.aweme.setting.ui.SettingContainerActivity";
    static final String OPEN_EXTRA = "com.golda.patchertiktok.OPEN_SETTINGS";
    private static final String KEY = "tiktokpatchxposed_settings";
    private static final String FRAGMENT = "com.ss.android.ugc.aweme.setting.ui.rvmpcompose.SettingsComposeRvmpFragment";
    private static final String PRIVACY_GROUP = "com.ss.android.ugc.aweme.setting.ui.rvmpcompose.group.privacy.PrivacyGroupVM";
    private static final String PRIVACY_ITEM = "com.ss.android.ugc.aweme.setting.ui.rvmpcompose.group.privacy.cell.PrivacyItemVM";

    private static volatile Object item;
    private static volatile Object state;
    private static volatile WeakReference<Activity> resumed = new WeakReference<>(null);
    private static volatile long lastOpen;
    private static volatile boolean openPending;

    private SettingsEntry() { }

    /** Swaps in {@link ModSettingsActivity} when TikTok's settings activity is started with our extra. */
    static void installHost() {
        Hooks.hookAll(Instrumentation.class, "newActivity", new Hooks.Hook() {
            @Override protected void before(Hooks.Call param) {
                if (param.args.length != 3 || !(param.args[2] instanceof Intent)) return;
                Intent intent = (Intent) param.args[2];
                if (HOST_ACTIVITY.equals(param.args[1]) && intent.getBooleanExtra(ModSettingsActivity.EXTRA, false)) {
                    param.setResult(new ModSettingsActivity());
                } else if (AppRestart.HOST.equals(param.args[1]) && intent.getBooleanExtra(AppRestart.EXTRA, false)) {
                    param.setResult(new AppRestart.Trampoline());
                }
            }
        });
    }

    static void install(Application app, ClassLoader loader) {
        trackActivities(app);
        try {
            injectRow(loader);
        } catch (Throwable error) {
            RuntimeLog.log("settings row unavailable: " + error);
        }
    }

    static void open(Context context) {
        long now = SystemClock.uptimeMillis();
        if (now - lastOpen < 600) return;
        lastOpen = now;
        Context host = activityOf(context);
        if (host == null) host = resumed.get();
        if (host == null) host = context;
        if (host == null) return;
        boolean dark = (host.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        host.startActivity(ModSettingsActivity.intent(host, dark));
    }

    // ---- row ---------------------------------------------------------------------------

    private static void injectRow(ClassLoader loader) throws Exception {
        Class<?> itemType = typeArgument(Class.forName(PRIVACY_GROUP, false, loader), 0);
        if (itemType == null || !itemType.isInterface()) throw new IllegalStateException("privacy item type");
        item = Proxy.newProxyInstance(itemType.getClassLoader(), new Class<?>[]{itemType}, SettingsEntry::itemMethod);

        Method combiner = null;
        Method factory = null;
        for (String call : Discovery.invokedBy(FRAGMENT, "onCreate")) {
            String[] parts = call.split("#", 2);
            Class<?> owner = Hooks.findClass(parts[0], loader);
            if (owner == null) continue;
            for (Method method : owner.getDeclaredMethods()) {
                if (!method.getName().equals(parts[1]) || !Modifier.isStatic(method.getModifiers())) continue;
                Class<?>[] p = method.getParameterTypes();
                if (p.length == 2 && p[0] == Iterable.class && List.class.isAssignableFrom(method.getReturnType())) {
                    combiner = method;
                } else if (p.length == 2 && p[1].isAssignableFrom(itemType) && method.getReturnType() == void.class) {
                    factory = method;
                }
            }
        }
        if (combiner == null || factory == null) throw new IllegalStateException("settings list helpers");

        Hooks.hook(combiner, new Hooks.Hook() {
            @Override protected void after(Hooks.Call param) {
                Object result = param.getResult();
                if (!(result instanceof List<?>) || ((List<?>) result).contains(item)) return;
                List<Object> list = new ArrayList<>((List<?>) result);
                list.add(0, item);
                param.setResult(list);
            }
        });
        // The cell factory's class also holds the sort key: a static (item) -> int method.
        int sortHooks = 0;
        Class<?> baseItem = factory.getParameterTypes()[1];
        for (Method method : factory.getDeclaringClass().getDeclaredMethods()) {
            Class<?>[] p = method.getParameterTypes();
            if (Modifier.isStatic(method.getModifiers()) && method.getReturnType() == int.class
                    && p.length == 1 && p[0] == baseItem) {
                Hooks.hook(method, new Hooks.Hook() {
                    @Override protected void before(Hooks.Call param) {
                        if (param.args[0] == item) param.setResult(-100);
                    }
                });
                sortHooks++;
            }
        }

        Class<?> privacyItem = Class.forName(PRIVACY_ITEM, false, loader);
        Field keyField = null;
        for (Field field : privacyItem.getDeclaredFields()) {
            if (field.getType() == String.class && !Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                keyField = field;
                break;
            }
        }
        Class<?> stateType = typeArgument(privacyItem, 0);
        if (keyField == null || stateType == null) throw new IllegalStateException("privacy cell shape");
        Field key = keyField;
        Hooks.hookAll(privacyItem, "defaultState", new Hooks.Hook() {
            @Override protected void before(Hooks.Call param) {
                try {
                    if (!KEY.equals(key.get(param.thisObject))) return;
                    if (state == null) state = buildState(stateType, loader);
                    param.setResult(state);
                } catch (Throwable error) {
                    RuntimeLog.log("settings row state: " + error);
                }
            }
        });
        RuntimeLog.log("settings row installed; sort=" + sortHooks + " item=" + itemType.getName());
    }

    private static Object itemMethod(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
            case "equals": return args != null && args.length == 1 && proxy == args[0];
            case "hashCode": return KEY.hashCode();
            case "toString": return "TiktokPatchXposedItem";
            case "getContentType": return "cellDisclosure";
            case "getHeadingItem": return false;
            default: break;
        }
        Class<?> type = method.getReturnType();
        if (type == String.class) return KEY;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }

    /** PrivacyItemState(icon, searchIcon, rightLabel, key, title, eventSink, action). */
    private static Object buildState(Class<?> stateType, ClassLoader loader) throws Exception {
        Constructor<?> constructor = null;
        for (Constructor<?> candidate : stateType.getDeclaredConstructors()) {
            Class<?>[] p = candidate.getParameterTypes();
            if (p.length == 7 && p[2] == String.class && p[3] == String.class && p[4] == String.class) {
                constructor = candidate;
                break;
            }
        }
        if (constructor == null) throw new NoSuchMethodException("PrivacyItemState constructor");
        constructor.setAccessible(true);
        Class<?>[] p = constructor.getParameterTypes();
        Object icon = icon(p[0], loader);
        Object eventSink = function(p[5], loader);
        Object action = function(p[6], loader);
        Activity activity = resumed.get();
        if (activity != null) I18n.use(activity.getResources().getConfiguration().getLocales().get(0));
        return constructor.newInstance(icon, icon, "", KEY, I18n.get(I18n.S.APP_NAME), eventSink, action);
    }

    /** TikTok's VectorResource(resId) with the Tux puzzle icon; the id differs per build, the name does not. */
    @android.annotation.SuppressLint("DiscouragedApi")
    private static Object icon(Class<?> type, ClassLoader loader) {
        Application app = currentApplication();
        if (app == null) return null;
        int id = app.getResources().getIdentifier("icon_puzzle", "raw", app.getPackageName());
        if (id == 0) return null;
        try {
            Constructor<?> constructor = type.getDeclaredConstructor(int.class);
            constructor.setAccessible(true);
            return constructor.newInstance(id);
        } catch (Throwable error) {
            return null;
        }
    }

    /** A Kotlin function proxy that opens our screen with whatever context the click supplies. */
    private static Object function(Class<?> type, ClassLoader loader) {
        Object unit = kotlinUnit(loader);
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                if ("equals".equals(method.getName())) return args != null && proxy == args[0];
                if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                return "TiktokPatchXposedClick";
            }
            if ("invoke".equals(method.getName())) {
                Context context = null;
                if (args != null) {
                    for (Object arg : args) {
                        context = arg instanceof Context ? (Context) arg : contextField(arg);
                        if (context != null) break;
                    }
                }
                open(context);
            }
            return unit;
        };
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Context contextField(Object event) {
        if (event == null) return null;
        for (Class<?> type = event.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!Context.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(event);
                    if (value instanceof Context) return (Context) value;
                } catch (Throwable ignored) { }
            }
        }
        return null;
    }

    private static Object kotlinUnit(ClassLoader loader) {
        try {
            Class<?> unit = Class.forName("kotlin.Unit", false, loader);
            for (Field field : unit.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.getType() == unit) {
                    field.setAccessible(true);
                    return field.get(null);
                }
            }
        } catch (Throwable ignored) { }
        return null;
    }

    private static Class<?> typeArgument(Class<?> type, int index) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            Type generic = current.getGenericSuperclass();
            if (generic instanceof ParameterizedType) {
                Type[] arguments = ((ParameterizedType) generic).getActualTypeArguments();
                if (arguments.length > index && arguments[index] instanceof Class<?>) return (Class<?>) arguments[index];
            }
        }
        return null;
    }

    // ---- activities --------------------------------------------------------------------

    private static void trackActivities(Application app) {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity activity) {
                resumed = new WeakReference<>(activity);
                // TikTok's splash activity finishes immediately, so open from the next real screen.
                if (openPending && !activity.isFinishing() && !(activity instanceof ModSettingsActivity)) {
                    openPending = false;
                    activity.getWindow().getDecorView().postDelayed(() -> open(activity), 250);
                }
            }

            @Override public void onActivityCreated(Activity activity, Bundle state) {
                // Opened from LSPosed's module settings button.
                Intent intent = activity.getIntent();
                if (state == null && intent != null && intent.getBooleanExtra(OPEN_EXTRA, false)) {
                    intent.removeExtra(OPEN_EXTRA);
                    openPending = true;
                }
            }

            @Override public void onActivityStarted(Activity activity) { }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivityStopped(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });
    }

    private static Activity activityOf(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) return (Activity) current;
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }

    private static Application currentApplication() {
        try {
            return (Application) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null);
        } catch (Throwable error) {
            return null;
        }
    }
}
