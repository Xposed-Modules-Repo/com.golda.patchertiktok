package com.golda.patchertiktok;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Read-only access to TikTok's local streak records. The obfuscated data provider is found by a
 * log string it contains and captured when TikTok constructs it, so no per-version tables are needed.
 */
final class StreakApi {
    private static final String DATA = "com.ss.android.ugc.aweme.im.streak.api.StreakData";
    /** Log text inside the streak data provider. */
    private static final String PROVIDER_FINGERPRINT = "StreakServerDataSource triggerStreakCompare convId: ";
    private static volatile Object capturedProvider;

    private final Object provider;
    private final Method listMethod;
    private final List<Object> recordTypes = new ArrayList<>();
    private final Object accountService;
    private final Method accountGetter;
    private final Method loginGetter;
    private final Class<?> model;

    /** Hooks the provider's constructors so the singleton is available later. */
    static void capture(ClassLoader loader) {
        for (String name : Discovery.classesUsing(PROVIDER_FINGERPRINT)) {
            try {
                Class<?> type = Class.forName(name, false, loader);
                if (listMethod(type) == null) continue;
                Hooks.hookConstructors(type, new Hooks.Hook() {
                    @Override protected void after(Hooks.Call param) {
                        if (!param.hasThrowable()) capturedProvider = param.thisObject;
                    }
                });
                RuntimeLog.log("streak provider: " + name);
                return;
            } catch (Throwable ignored) { }
        }
        RuntimeLog.log("streak provider not found");
    }

    StreakApi(ClassLoader loader) throws ReflectiveOperationException {
        provider = capturedProvider;
        if (provider == null) throw new IllegalStateException("Streak provider not created yet");
        model = Class.forName(DATA, false, loader);
        for (String name : new String[]{"streak", "convType", "convId", "userStreak",
                "activeStart", "activeBefore", "endAt"}) field(model, name);
        listMethod = listMethod(provider.getClass());
        if (listMethod == null) throw new NoSuchMethodException("Streak list method");
        listMethod.setAccessible(true);
        Class<?> recordType = recordType(listMethod);
        if (recordType == null) throw new NoSuchFieldException("Streak record type");
        for (Object constant : recordType.getEnumConstants()) {
            String name = ((Enum<?>) constant).name();
            if ("USER".equals(name) || "CONVERSATION".equals(name)) recordTypes.add(constant);
        }
        if (recordTypes.size() != 2) throw new NoSuchFieldException("Streak record types unavailable");
        accountService = service(loader, "com.ss.android.ugc.aweme.framework.services.IUserService");
        accountGetter = method(accountService.getClass(), "getCurrentUserID");
        loginGetter = method(accountService.getClass(), "isLogin");
    }

    /** {@code List<StreakData> x(List<? extends RecordType>)} on the provider. */
    private static Method listMethod(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            Class<?>[] p = method.getParameterTypes();
            if (Modifier.isStatic(method.getModifiers()) || p.length != 1 || p[0] != List.class
                    || method.getReturnType() != List.class) continue;
            if (recordType(method) != null) return method;
        }
        return null;
    }

    private static Class<?> recordType(Method method) {
        Type parameter = method.getGenericParameterTypes()[0];
        if (!(parameter instanceof ParameterizedType)) return null;
        Type argument = ((ParameterizedType) parameter).getActualTypeArguments()[0];
        if (argument instanceof WildcardType) argument = ((WildcardType) argument).getUpperBounds()[0];
        if (!(argument instanceof Class<?>) || !((Class<?>) argument).isEnum()) return null;
        boolean user = false;
        boolean conversation = false;
        for (Object constant : ((Class<?>) argument).getEnumConstants()) {
            String name = ((Enum<?>) constant).name();
            user |= "USER".equals(name);
            conversation |= "CONVERSATION".equals(name);
        }
        return user && conversation ? (Class<?>) argument : null;
    }

    String account() throws ReflectiveOperationException {
        if (!Boolean.TRUE.equals(loginGetter.invoke(accountService))) return null;
        Object id = accountGetter.invoke(accountService);
        return id instanceof String && StreakPeerPolicy.validId((String) id) ? (String) id : null;
    }

    List<?> items() throws ReflectiveOperationException {
        Object result = listMethod.invoke(provider, recordTypes);
        if (result == null) return null;
        if (!(result instanceof List<?>)) throw new IllegalStateException("Invalid streak list");
        List<?> items = new ArrayList<>((List<?>) result);
        for (Object item : items) {
            if (item != null && !model.isInstance(item)) throw new IllegalStateException("Invalid streak model");
        }
        return items;
    }

    Candidate candidate(Object item, String account, long now) throws ReflectiveOperationException {
        if (!model.isInstance(item) || number(item, "streak") <= 0) return null;
        Object users = field(model, "userStreak").get(item);
        if (!(users instanceof List<?>)) return null;
        List<String> ids = new ArrayList<>();
        for (Object user : (List<?>) users) {
            if (user == null) return null;
            Object value = field(user.getClass(), "uid").get(user);
            ids.add(value instanceof Number ? Long.toString(((Number) value).longValue())
                    : value instanceof String ? (String) value : null);
        }
        String peer = StreakPeerPolicy.directPeer(number(item, "convType"), account, ids);
        Object conversation = field(model, "convId").get(item);
        if (peer == null || !(conversation instanceof String) || ((String) conversation).isEmpty()) return null;
        long start = number(item, "activeStart");
        long before = number(item, "activeBefore");
        long end = number(item, "endAt");
        if (!StreakEligibility.isAtRisk(true, true, start, before, end, now)) return null;
        return new Candidate((String) conversation, peer, start, before, end);
    }

    /**
     * TikTok keeps several records per conversation: the current window and stale ones from
     * earlier windows. Only the newest window of each conversation counts; two records with the
     * same newest window that disagree make the conversation ambiguous, and it is skipped.
     */
    List<Candidate> candidates(List<?> items, String account, long now) throws ReflectiveOperationException {
        Map<String, Object> latest = new LinkedHashMap<>();
        Set<String> ambiguous = new HashSet<>();
        for (Object item : items) {
            if (!model.isInstance(item)) continue;
            Object id = field(model, "convId").get(item);
            if (!(id instanceof String) || ((String) id).isEmpty()) continue;
            String conversation = (String) id;
            Object previous = latest.get(conversation);
            long end = StreakKeys.seconds(number(item, "endAt"));
            long previousEnd = previous == null ? Long.MIN_VALUE : StreakKeys.seconds(number(previous, "endAt"));
            if (end > previousEnd) {
                latest.put(conversation, item);
                ambiguous.remove(conversation);
            } else if (end == previousEnd) {
                Candidate a = candidate(item, account, now);
                Candidate b = candidate(previous, account, now);
                if ((a == null) != (b == null) || (a != null && !a.peer.equals(b.peer))) ambiguous.add(conversation);
            }
        }
        List<Candidate> result = new ArrayList<>();
        for (Map.Entry<String, Object> entry : latest.entrySet()) {
            if (ambiguous.contains(entry.getKey())) continue;
            Candidate candidate = candidate(entry.getValue(), account, now);
            if (candidate != null) result.add(candidate);
        }
        return result;
    }

    /** "active=N healthy=N atRisk=N expired=N" for one-to-one streaks; used in the check log. */
    String summary(List<?> items, long now) throws ReflectiveOperationException {
        int active = 0;
        int healthy = 0;
        int atRisk = 0;
        int expired = 0;
        for (Object item : items) {
            if (!model.isInstance(item) || number(item, "streak") <= 0 || number(item, "convType") != 1) continue;
            active++;
            long before = StreakKeys.seconds(number(item, "activeBefore")) * 1000L;
            long end = StreakKeys.seconds(number(item, "endAt")) * 1000L;
            if (now < before) healthy++;
            else if (now < end) atRisk++;
            else expired++;
        }
        return "active=" + active + " healthy=" + healthy + " atRisk=" + atRisk + " expired=" + expired;
    }

    long nextRiskMillis(Object item, long now) throws ReflectiveOperationException {
        if (!model.isInstance(item) || number(item, "streak") <= 0 || number(item, "convType") != 1) return Long.MAX_VALUE;
        long before = StreakKeys.seconds(number(item, "activeBefore")) * 1000L;
        long end = StreakKeys.seconds(number(item, "endAt")) * 1000L;
        return before > now && end > before ? before : Long.MAX_VALUE;
    }

    static final class Candidate {
        final String conversation;
        final String peer;
        final long start;
        final long before;
        final long end;

        Candidate(String conversation, String peer, long start, long before, long end) {
            this.conversation = conversation;
            this.peer = peer;
            this.start = start;
            this.before = before;
            this.end = end;
        }

        boolean atRisk(long now) {
            return StreakEligibility.isAtRisk(true, true, start, before, end, now);
        }

        String window() {
            return StreakKeys.window(before, end);
        }
    }

    private long number(Object item, String name) throws ReflectiveOperationException {
        Object value = field(model, name).get(item);
        if (!(value instanceof Number)) throw new IllegalStateException("Invalid streak field " + name);
        return ((Number) value).longValue();
    }

    static Object service(ClassLoader loader, String name) throws ReflectiveOperationException {
        Class<?> managerType = Class.forName("com.ss.android.ugc.aweme.framework.services.ServiceManager", false, loader);
        Object manager = method(managerType, "get").invoke(null);
        Object service = method(manager.getClass(), "getService", Class.class)
                .invoke(manager, Class.forName(name, false, loader));
        if (service == null) throw new IllegalStateException("Service unavailable: " + name);
        return service;
    }

    static Field field(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(type.getName() + "#" + name);
    }

    static Method method(Class<?> type, String name, Class<?>... parameters) throws NoSuchMethodException {
        Method method = type.getMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }
}
