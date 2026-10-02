package com.golda.patchertiktok;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Cached reflective access; feed filtering touches every item, so lookups must not repeat. */
final class Reflect {
    private static final Object MISSING = new Object();
    private static final Map<String, Object> METHODS = new ConcurrentHashMap<>();
    private static final Map<String, Object> FIELDS = new ConcurrentHashMap<>();

    private Reflect() { }

    static Method method(Class<?> type, String name) {
        String key = type.getName() + '#' + name;
        Object cached = METHODS.get(key);
        if (cached == null) {
            cached = MISSING;
            for (Class<?> current = type; current != null && cached == MISSING; current = current.getSuperclass()) {
                try {
                    Method method = current.getDeclaredMethod(name);
                    method.setAccessible(true);
                    cached = method;
                } catch (Throwable ignored) { }
            }
            METHODS.put(key, cached);
        }
        return cached == MISSING ? null : (Method) cached;
    }

    static Field field(Class<?> type, String name) {
        String key = type.getName() + '.' + name;
        Object cached = FIELDS.get(key);
        if (cached == null) {
            cached = MISSING;
            for (Class<?> current = type; current != null && cached == MISSING; current = current.getSuperclass()) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    cached = field;
                } catch (Throwable ignored) { }
            }
            FIELDS.put(key, cached);
        }
        return cached == MISSING ? null : (Field) cached;
    }

    static Object call(Object target, String name) {
        if (target == null) return null;
        Method method = method(target.getClass(), name);
        if (method == null) return null;
        try {
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static boolean callBoolean(Object target, String name) {
        return Boolean.TRUE.equals(call(target, name));
    }

    static Object get(Object target, String name) {
        if (target == null) return null;
        Field field = field(target.getClass(), name);
        if (field == null) return null;
        try {
            return field.get(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static boolean getBoolean(Object target, String name) {
        return Boolean.TRUE.equals(get(target, name));
    }
}
