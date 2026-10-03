package com.golda.patchertiktok;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The few hook primitives the module needs, independent of the Xposed API flavour.
 * {@link ModuleMain} plugs in the modern libxposed API, {@link MainHook} the legacy one
 * for older LSPosed builds; feature code only ever talks to this class.
 */
final class Hooks {
    interface Backend {
        void hook(Member target, Hook hook);

        void log(String message);
    }

    /** Before/after callbacks around the original method, with the familiar Xposed semantics. */
    abstract static class Hook {
        protected void before(Call call) throws Throwable { }

        protected void after(Call call) throws Throwable { }
    }

    /** Replaces the method body; whatever {@link #replace} returns or throws is the result. */
    abstract static class Replace extends Hook {
        protected abstract Object replace(Call call) throws Throwable;

        @Override protected final void before(Call call) {
            try {
                call.setResult(replace(call));
            } catch (Throwable error) {
                call.setThrowable(error);
            }
        }
    }

    static final class Call {
        final Member method;
        final Object thisObject;
        final Object[] args;
        private Object result;
        private Throwable throwable;
        boolean returnEarly;

        Call(Member method, Object thisObject, Object[] args) {
            this.method = method;
            this.thisObject = thisObject;
            this.args = args == null ? new Object[0] : args;
        }

        Object getResult() { return result; }

        Throwable getThrowable() { return throwable; }

        boolean hasThrowable() { return throwable != null; }

        /** In a before-callback this skips the original method. */
        void setResult(Object value) {
            result = value;
            throwable = null;
            returnEarly = true;
        }

        void setThrowable(Throwable error) {
            throwable = error;
            result = null;
            returnEarly = true;
        }

        /** Records what the original (or an early return) produced, before the after-callback runs. */
        void finish(Object value, Throwable error) {
            result = value;
            throwable = error;
            returnEarly = false;
        }
    }

    private static final String TAG = "TiktokPatchXposed";
    private static final Map<Object, Map<String, Object>> EXTRAS = Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile Backend backend;

    private Hooks() { }

    /** First entry point wins; the other one stays idle if a framework loads both. */
    static synchronized boolean attach(Backend implementation) {
        if (backend != null || System.getProperties().putIfAbsent("tiktokpatchxposed.entry", TAG) != null) {
            return false;
        }
        backend = implementation;
        return true;
    }

    static void hook(Member target, Hook hook) {
        Backend active = backend;
        if (active == null) throw new IllegalStateException("No Xposed backend");
        active.hook(target, hook);
    }

    /** Hooks every non-abstract declared method with this name. */
    static int hookAll(Class<?> type, String name, Hook hook) {
        int count = 0;
        for (Method method : type.getDeclaredMethods()) {
            if (!method.getName().equals(name) || Modifier.isAbstract(method.getModifiers())) continue;
            hook(method, hook);
            count++;
        }
        return count;
    }

    static int hookConstructors(Class<?> type, Hook hook) {
        int count = 0;
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            hook(constructor, hook);
            count++;
        }
        return count;
    }

    /** {@code findAndHook(type, "name", ParamType.class, ..., hook)} for a method declared by {@code type}. */
    static void findAndHook(Class<?> type, String name, Object... typesAndHook) {
        Class<?>[] parameters = new Class<?>[typesAndHook.length - 1];
        for (int i = 0; i < parameters.length; i++) parameters[i] = (Class<?>) typesAndHook[i];
        try {
            hook(type.getDeclaredMethod(name, parameters), (Hook) typesAndHook[parameters.length]);
        } catch (NoSuchMethodException missing) {
            throw new NoSuchMethodError(type.getName() + "#" + name);
        }
    }

    static Hook constant(Object value) {
        return new Hook() {
            @Override protected void before(Call call) { call.setResult(value); }
        };
    }

    static Class<?> findClass(String name, ClassLoader loader) {
        try {
            return Class.forName(name, false, loader == null ? ClassLoader.getSystemClassLoader() : loader);
        } catch (ClassNotFoundException | LinkageError missing) {
            return null;
        }
    }

    static void setField(Object target, String name, Object value) {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException next) {
                // Keep looking in the superclass.
            } catch (IllegalAccessException error) {
                throw new IllegalStateException(error);
            }
        }
        throw new NoSuchFieldError(name);
    }

    /** Values the module remembers for a TikTok object, dropped together with the object. */
    static Object getExtra(Object target, String key) {
        synchronized (EXTRAS) {
            Map<String, Object> values = EXTRAS.get(target);
            return values == null ? null : values.get(key);
        }
    }

    static void setExtra(Object target, String key, Object value) {
        synchronized (EXTRAS) {
            Map<String, Object> values = EXTRAS.get(target);
            if (values == null) EXTRAS.put(target, values = new HashMap<>());
            values.put(key, value);
        }
    }

    static void log(String message) {
        Backend active = backend;
        if (active != null) active.log(message);
        else Log.i(TAG, message);
    }

    /** Callback errors are logged and never reach TikTok, like in the legacy bridge. */
    static void before(Hook hook, Call call) {
        try {
            hook.before(call);
        } catch (Throwable error) {
            call.returnEarly = false;
            log("hook failed: " + error);
        }
    }

    static void after(Hook hook, Call call) {
        Object result = call.getResult();
        Throwable throwable = call.getThrowable();
        try {
            hook.after(call);
        } catch (Throwable error) {
            call.finish(result, throwable);
            log("hook failed: " + error);
        }
    }
}
