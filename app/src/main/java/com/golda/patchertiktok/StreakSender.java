package com.golda.patchertiktok;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

final class StreakSender {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final java.util.concurrent.Executor LATE = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final Object service;
    private final Method sendMethod;
    private final Class<?> callbackType;
    private final Class<?> messageType;
    private final DeliveryLedger ledger;
    private final SharedPreferences legacy;
    private final SharedPreferences friendLedger;

    StreakSender(Context context, ClassLoader loader) throws ReflectiveOperationException {
        Object resolved;
        try {
            resolved = StreakApi.service(loader,
                    "com.ss.android.ugc.aweme.im.lightinteract.api.platform.service.ILightInteractionPlatformService");
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            Class<?> manager = Class.forName(
                    "com.ss.android.ugc.aweme.im.lightinteract.impl.serviceimpl.LightInteractionManager", false, loader);
            resolved = null;
            for (Field field : manager.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && manager.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    resolved = field.get(null);
                    if (resolved != null) break;
                }
            }
            if (resolved == null) throw new IllegalStateException("Light interaction service unavailable");
        }
        service = resolved;
        sendMethod = StreakSendProtocol.sendMethod(service.getClass());
        callbackType = sendMethod.getParameterTypes()[9];
        messageType = StreakSendProtocol.callbackMessageType(callbackType);
        SharedPreferences preferences = context.getSharedPreferences("streak_delivery_v315", Context.MODE_PRIVATE);
        ledger = new DeliveryLedger(new PreferenceStore(preferences));
        legacy = context.getSharedPreferences("ttmod_settings", Context.MODE_PRIVATE);
        friendLedger = context.getSharedPreferences("ttmod_streak_delivery_v314", Context.MODE_PRIVATE);
    }

    String send(String account, StreakApi.Candidate candidate, String day, BooleanSupplier allowed) {
        if (Looper.myLooper() == Looper.getMainLooper()) throw new IllegalStateException("Worker required");
        if (!allowed.getAsBoolean()) return "CANCELLED";
        String window = candidate.window();
        if (window == null) return "INELIGIBLE";
        if (day.equals(legacy.getString("auto_streak_nudge_sent_date", ""))
                && legacy.getStringSet("auto_streak_nudge_sent_conversations", java.util.Collections.emptySet())
                .contains(candidate.conversation)) return "PREVIOUS_ATTEMPT";
        String oldKey = StreakKeys.hash("window_v1_", account, candidate.conversation, window);
        if (friendLedger.contains(oldKey)) return "PREVIOUS_ATTEMPT";
        String key = StreakKeys.hash("conversation_v2_", account, candidate.conversation);
        DeliveryLedger.Record reservation = ledger.reserve(key, day, window, System.currentTimeMillis());
        if (reservation == null) return "PREVIOUS_ATTEMPT";

        Receipt callback = new Receipt(key, reservation, account, candidate.conversation);
        Object[] args = new Object[11];
        args[0] = "spark_v1";
        args[1] = candidate.conversation;
        args[2] = candidate.peer;
        // Same message extras as a flame tapped in a chat; no made-up analytics.
        args[3] = ext();
        args[9] = Proxy.newProxyInstance(callbackType.getClassLoader(), new Class<?>[]{callbackType}, callback);
        boolean posted = MAIN.post(() -> {
            try {
                if (!allowed.getAsBoolean()) {
                    callback.beforeDispatchFailure();
                    return;
                }
                if (!callback.dispatch.compareAndSet(0, 1)) return;
                sendMethod.invoke(service, args);
            } catch (IllegalAccessException | IllegalArgumentException rejected) {
                callback.beforeDispatchFailure();
            } catch (Throwable unknown) {
                callback.done.countDown();
                RuntimeLog.log("send result unknown: " + unknown.getClass().getSimpleName());
            }
        });
        if (!posted) callback.beforeDispatchFailure();
        try {
            callback.done.await(45, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            // Cancel an unstarted main-thread task after a timeout or settings change.
            if (callback.dispatch.compareAndSet(0, 2)) callback.retry = true;
            callback.finished = true;
        }
        DeliveryLedger.State state = callback.confirmed ? DeliveryLedger.State.CONFIRMED
                : callback.retry ? DeliveryLedger.State.RETRY : DeliveryLedger.State.UNKNOWN;
        ledger.finish(key, reservation, state, System.currentTimeMillis());
        return state.name();
    }

    /**
     * Extras of a flame sent from a chat's action bar, as captured from TikTok itself:
     * the source button, the chat entrance and a fresh chat-session id.
     */
    private static Map<String, String> ext() {
        Map<String, String> values = new HashMap<>();
        values.put("a:src", "action_bar:spark");
        values.put("a:entrance_type", "1");
        values.put("a:process_id", java.util.UUID.randomUUID().toString());
        return values;
    }

    private final class Receipt implements InvocationHandler {
        final String key;
        final DeliveryLedger.Record attempt;
        final String account;
        final String conversation;
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicInteger dispatch = new AtomicInteger();
        volatile boolean confirmed;
        volatile boolean retry;
        volatile boolean finished;
        private String uuid;
        private boolean invalid;

        Receipt(String key, DeliveryLedger.Record attempt, String account, String conversation) {
            this.key = key;
            this.attempt = attempt;
            this.account = account;
            this.conversation = conversation;
        }

        @Override
        public synchronized Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getDeclaringClass() == Object.class) {
                if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                if ("equals".equals(method.getName())) return args != null && args.length == 1 && proxy == args[0];
                return "StreakReceipt";
            }
            try {
                if (args != null) {
                    for (Object arg : args) {
                        if (messageType.isInstance(arg)) observe(arg);
                        else if (arg instanceof List<?>) {
                            for (Object message : (List<?>) arg) {
                                if (messageType.isInstance(message)) observe(message);
                            }
                        }
                    }
                    // A failure callback is terminal, but never proof that retrying is safe.
                    if (args.length == 3) {
                        RuntimeLog.log("streak send failed: " + reason(args));
                        done.countDown();
                    }
                }
            } catch (Throwable error) {
                invalid = true;
                done.countDown();
            }
            return null;
        }

        /** TikTok's error object (code and server message), without the chat message itself. */
        private String reason(Object[] args) {
            StringBuilder out = new StringBuilder();
            for (Object arg : args) {
                if (arg == null || messageType.isInstance(arg) || arg instanceof List<?>) continue;
                String text = String.valueOf(arg);
                if (out.length() > 0) out.append(' ');
                out.append(text.length() > 300 ? text.substring(0, 300) : text);
            }
            return out.length() == 0 ? "no details" : out.toString();
        }

        private void observe(Object message) throws ReflectiveOperationException {
            if (invalid) return;
            Object actualConversation = StreakApi.method(messageType, "getConversationId").invoke(message);
            Object sender = StreakApi.method(messageType, "getSender").invoke(message);
            Object actualUuid = StreakApi.method(messageType, "getUuid").invoke(message);
            if (!conversation.equals(actualConversation) || !account.equals(String.valueOf(sender))
                    || !(actualUuid instanceof String) || ((String) actualUuid).isEmpty()
                    || ((String) actualUuid).length() > 256 || (uuid != null && !uuid.equals(actualUuid))) {
                invalid = true;
                done.countDown();
                return;
            }
            uuid = (String) actualUuid;
            Object id = StreakApi.method(messageType, "getMsgId").invoke(message);
            boolean serverId = StreakPeerPolicy.validId(String.valueOf(id));
            if (serverId && Boolean.TRUE.equals(StreakApi.method(messageType, "isSuccessStatus").invoke(message))) {
                confirmed = true;
                done.countDown();
                if (finished) LATE.execute(() -> {
                    try {
                        ledger.finish(key, attempt, DeliveryLedger.State.CONFIRMED, System.currentTimeMillis());
                    } catch (RuntimeException error) {
                        RuntimeLog.log("late confirmation storage failed");
                    }
                });
            }
        }

        void beforeDispatchFailure() {
            dispatch.set(2);
            retry = true;
            done.countDown();
        }
    }

    private static final class PreferenceStore implements DeliveryLedger.Store {
        private final SharedPreferences preferences;

        PreferenceStore(SharedPreferences preferences) {
            this.preferences = preferences;
        }

        @Override
        public DeliveryLedger.Record read(String key) {
            String saved = preferences.getString(key, null);
            if (saved == null) return null;
            try {
                JSONObject value = new JSONObject(saved);
                if (value.getInt("schema") != 1) throw new JSONException("Unknown ledger schema");
                if (!value.getString("day").matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")
                        || !value.getString("window").matches("[0-9]+:[0-9]+")
                        || value.getString("token").isEmpty() || value.getInt("attempts") < 1
                        || value.getInt("attempts") > 3 || value.getLong("retryAt") < 0) {
                    throw new JSONException("Invalid delivery record");
                }
                return new DeliveryLedger.Record(value.getString("day"), value.getString("window"),
                        value.getString("token"), DeliveryLedger.State.valueOf(value.getString("state")),
                        value.getInt("attempts"), value.getLong("retryAt"));
            } catch (JSONException | IllegalArgumentException error) {
                throw new IllegalStateException("Unreadable delivery record; sending blocked", error);
            }
        }

        @Override
        public boolean write(String key, DeliveryLedger.Record record) {
            try {
                JSONObject value = new JSONObject();
                value.put("schema", 1).put("day", record.day).put("window", record.window)
                        .put("token", record.token).put("state", record.state.name())
                        .put("attempts", record.attempts).put("retryAt", record.retryAt);
                return preferences.edit().putString(key, value.toString()).commit();
            } catch (JSONException error) {
                return false;
            }
        }
    }
}
