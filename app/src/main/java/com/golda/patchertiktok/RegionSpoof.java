package com.golda.patchertiktok;

import android.telephony.TelephonyManager;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


/**
 * Optional region spoof. Installed only when a region is selected at startup, so turning it
 * off adds no hooks at all.
 *
 * TikTok decides the region from two places: the SIM (TelephonyManager, SubscriptionInfo,
 * system properties) and its own request parameters, which it fills from the system locale and
 * the network. Both are replaced; the language is left alone so recommendations stay in the
 * user's language.
 */
final class RegionSpoof {
    /** TikTok's feature store that supplies the common request parameters. */
    private static final String FEATURE_PRODUCER = "com.ss.ugc.clientai.core.api.FeatureProducer";
    private static volatile Regions.Region region;

    private RegionSpoof() { }

    static boolean active() { return region != null; }

    static void install(ClassLoader loader) {
        Regions.Region selected = Regions.find(Prefs.region());
        if (selected == null || region != null) return;
        region = selected;
        hookTelephony(selected);
        hookSubscriptionInfo(selected);
        hookSystemProperties(selected);
        hookRequestParams(loader, selected);
        RuntimeLog.log("region spoof: " + selected.iso + " " + selected.operator);
    }

    private static void hookTelephony(Regions.Region selected) {
        Map<String, Object> values = new HashMap<>();
        values.put("getSimCountryIso", selected.iso);
        values.put("getNetworkCountryIso", selected.iso);
        values.put("getSimOperator", selected.operator);
        values.put("getNetworkOperator", selected.operator);
        values.put("getSimOperatorName", selected.carrier);
        values.put("getNetworkOperatorName", selected.carrier);
        // A present, ready GSM SIM on LTE; otherwise TikTok may ignore the SIM country.
        values.put("getSimState", TelephonyManager.SIM_STATE_READY);
        values.put("hasIccCard", true);
        values.put("getPhoneType", TelephonyManager.PHONE_TYPE_GSM);
        values.put("getNetworkType", TelephonyManager.NETWORK_TYPE_LTE);
        values.put("getDataNetworkType", TelephonyManager.NETWORK_TYPE_LTE);
        values.put("getVoiceNetworkType", TelephonyManager.NETWORK_TYPE_LTE);
        for (Method method : TelephonyManager.class.getDeclaredMethods()) {
            Object value = values.get(method.getName());
            if (value == null || !compatible(method.getReturnType(), value)) continue;
            Hooks.hook(method, constant(value));
        }
    }

    private static void hookSubscriptionInfo(Regions.Region selected) {
        Class<?> type = Hooks.findClass("android.telephony.SubscriptionInfo", null);
        if (type == null) return;
        for (Method method : type.getDeclaredMethods()) {
            if (method.getParameterTypes().length != 0) continue;
            Object value;
            switch (method.getName()) {
                case "getCountryIso": value = selected.iso; break;
                case "getMccString": value = selected.mcc(); break;
                case "getMncString": value = selected.mnc(); break;
                case "getMcc": value = Integer.parseInt(selected.mcc()); break;
                case "getMnc": value = Integer.parseInt(selected.mnc()); break;
                case "getCarrierName":
                case "getDisplayName": value = selected.carrier; break;
                default: continue;
            }
            Hooks.hook(method, constant(value));
        }
    }

    private static void hookSystemProperties(Regions.Region selected) {
        Class<?> type = Hooks.findClass("android.os.SystemProperties", null);
        if (type == null) return;
        Hooks.Hook hook = new Hooks.Hook() {
            @Override protected void after(Hooks.Call param) {
                Object key = param.args[0];
                if (!(key instanceof String) || !((String) key).startsWith("gsm.")) return;
                switch ((String) key) {
                    case "gsm.operator.iso-country":
                    case "gsm.sim.operator.iso-country":
                        param.setResult(selected.iso);
                        break;
                    case "gsm.operator.numeric":
                    case "gsm.sim.operator.numeric":
                        param.setResult(selected.operator);
                        break;
                    case "gsm.operator.alpha":
                    case "gsm.sim.operator.alpha":
                        param.setResult(selected.carrier);
                        break;
                    default:
                        break;
                }
            }
        };
        try {
            Hooks.findAndHook(type, "get", String.class, hook);
            Hooks.findAndHook(type, "get", String.class, String.class, hook);
        } catch (Throwable error) {
            RuntimeLog.log("system properties spoof unavailable: " + error.getClass().getSimpleName());
        }
    }

    /**
     * Common request parameters ("region", "sys_region", "mcc_mnc"...) come from
     * FeatureProducer.getStringFeature("f_global_..."). sys_region and region otherwise
     * follow the system locale, which would still report the real country.
     */
    private static void hookRequestParams(ClassLoader loader, Regions.Region selected) {
        Class<?> producer = Hooks.findClass(FEATURE_PRODUCER, loader);
        if (producer == null) {
            RuntimeLog.log("request region params unavailable: FeatureProducer not found");
            return;
        }
        String country = selected.iso.toUpperCase(Locale.ROOT);
        Map<String, String> features = new HashMap<>();
        for (String name : new String[]{"f_global_region", "f_global_sys_region", "f_global_op_region",
                "f_global_carrier_region", "f_global_current_region", "f_global_residence"}) {
            features.put(name, country);
        }
        features.put("f_global_carrier_region_v2", selected.mcc());
        features.put("f_global_mcc_mnc", selected.operator);
        int hooks = 0;
        for (Method method : producer.getDeclaredMethods()) {
            if (!method.getName().startsWith("getStringFeature") || method.getReturnType() != String.class) continue;
            int nameIndex = -1;
            Class<?>[] p = method.getParameterTypes();
            for (int index = 0; index < p.length; index++) {
                if (p[index] == String.class) { nameIndex = index; break; }
            }
            if (nameIndex < 0) continue;
            int name = nameIndex;
            Hooks.hook(method, new Hooks.Hook() {
                @Override protected void after(Hooks.Call param) {
                    String value = features.get(param.args[name]);
                    if (value != null) param.setResult(value);
                }
            });
            hooks++;
        }
        RuntimeLog.log("request region params: " + hooks + " getters");
    }

    private static boolean compatible(Class<?> type, Object value) {
        if (value instanceof String) return type == String.class;
        if (value instanceof Integer) return type == int.class;
        if (value instanceof Boolean) return type == boolean.class;
        return false;
    }

    private static Hooks.Hook constant(Object value) {
        return new Hooks.Hook() {
            @Override protected void before(Hooks.Call param) { param.setResult(value); }
        };
    }
}
