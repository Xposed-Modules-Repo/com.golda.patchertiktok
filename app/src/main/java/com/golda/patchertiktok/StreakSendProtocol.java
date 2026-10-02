package com.golda.patchertiktok;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

final class StreakSendProtocol {
    private StreakSendProtocol() {
    }

    static Method sendMethod(Class<?> type) throws NoSuchMethodException {
        Method match = null;
        for (Method method : type.getMethods()) {
            Class<?>[] p = method.getParameterTypes();
            if (Modifier.isStatic(method.getModifiers()) || method.getReturnType() != void.class
                    || p.length != 11 || p[0] != String.class || p[1] != String.class
                    || p[2] != String.class || p[3] != Map.class || p[4] != Map.class
                    || !p[9].isInterface()) continue;
            boolean nullable = true;
            for (int index = 5; index < p.length; index++) nullable &= !p[index].isPrimitive();
            if (!nullable) continue;
            if (match != null && !match.equals(method)) throw new NoSuchMethodException("Ambiguous send API");
            match = method;
        }
        if (match == null) throw new NoSuchMethodException("Streak send API unavailable");
        callbackMessageType(match.getParameterTypes()[9]);
        match.setAccessible(true);
        return match;
    }

    static Class<?> callbackMessageType(Class<?> callback) throws NoSuchMethodException {
        if (!callback.isInterface()) throw new NoSuchMethodException("Callback is not an interface");
        Class<?> message = null;
        boolean failure = false;
        boolean batch = false;
        for (Method method : callback.getMethods()) {
            if (method.getDeclaringClass() == Object.class || Modifier.isStatic(method.getModifiers())) continue;
            if (method.getReturnType() != void.class) throw new NoSuchMethodException("Unexpected callback result");
            Class<?>[] p = method.getParameterTypes();
            if (p.length < 2) continue;
            if (p.length == 3 && List.class.isAssignableFrom(p[1]) && Map.class.isAssignableFrom(p[2])) {
                batch = true;
                continue;
            }
            if (List.class.isAssignableFrom(p[1])) continue;
            Class<?> candidate = p[1];
            validateMessage(candidate);
            if (message != null && message != candidate) throw new NoSuchMethodException("Ambiguous callback message");
            message = candidate;
            failure |= p.length == 3;
        }
        if (message == null || !failure || !batch) throw new NoSuchMethodException("Unsupported callback protocol");
        return message;
    }

    private static void validateMessage(Class<?> type) throws NoSuchMethodException {
        if (type.getMethod("isSuccessStatus").getReturnType() != boolean.class
                || type.getMethod("getConversationId").getReturnType() != String.class
                || type.getMethod("getUuid").getReturnType() != String.class) {
            throw new NoSuchMethodException("Unsupported receipt identity");
        }
        for (String name : new String[]{"getSender", "getMsgId"}) {
            Class<?> result = type.getMethod(name).getReturnType();
            if (result != long.class && result != Long.class && result != String.class) {
                throw new NoSuchMethodException("Unsupported receipt field: " + name);
            }
        }
    }
}
