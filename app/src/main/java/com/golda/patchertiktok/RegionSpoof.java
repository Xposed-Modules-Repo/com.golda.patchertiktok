package com.golda.patchertiktok;

import android.telephony.TelephonyManager;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Optional SIM-country spoof. Installed only when a region is selected at startup,
 * so the default configuration adds no telephony hooks at all.
 */
final class RegionSpoof {
    private static volatile Regions.Region region;

    private RegionSpoof() { }

    static boolean active() { return region != null; }

    static Regions.Region region() { return region; }

    static void install() {
        Regions.Region selected = Regions.find(Prefs.region());
        if (selected == null || region != null) return;
        region = selected;
        String operator = selected.operator;
        hookTelephony("getSimCountryIso", selected.iso);
        hookTelephony("getNetworkCountryIso", selected.iso);
        hookTelephony("getSimOperator", operator);
        hookTelephony("getNetworkOperator", operator);
        hookTelephony("getSimOperatorName", selected.carrier);
        hookTelephony("getNetworkOperatorName", selected.carrier);
        hookSubscriptionInfo(selected);
        hookSystemProperties(selected);
        RuntimeLog.log("region spoof: " + selected.iso + " " + operator);
    }

    private static void hookTelephony(String name, Object value) {
        for (Method method : TelephonyManager.class.getDeclaredMethods()) {
            if (!name.equals(method.getName()) || method.getReturnType() != String.class) continue;
            XposedBridge.hookMethod(method, constant(value));
        }
    }

    private static void hookSubscriptionInfo(Regions.Region selected) {
        Class<?> type = XposedHelpers.findClassIfExists("android.telephony.SubscriptionInfo", null);
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
            XposedBridge.hookMethod(method, constant(value));
        }
    }

    private static void hookSystemProperties(Regions.Region selected) {
        Class<?> type = XposedHelpers.findClassIfExists("android.os.SystemProperties", null);
        if (type == null) return;
        XC_MethodHook hook = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                Object key = param.args[0];
                if (!(key instanceof String) || !((String) key).startsWith("gsm.") || DeviceSignals.readingDevice()) return;
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
            XposedHelpers.findAndHookMethod(type, "get", String.class, hook);
            XposedHelpers.findAndHookMethod(type, "get", String.class, String.class, hook);
        } catch (Throwable error) {
            RuntimeLog.log("system properties spoof unavailable: " + error.getClass().getSimpleName());
        }
    }

    private static XC_MethodHook constant(Object value) {
        return new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (!DeviceSignals.readingDevice()) param.setResult(value);
            }
        };
    }
}
