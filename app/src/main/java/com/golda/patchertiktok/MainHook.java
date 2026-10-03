package com.golda.patchertiktok;

import java.lang.reflect.Member;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Entry point for older LSPosed/Xposed builds without the modern API (assets/xposed_init).
 * Newer LSPosed loads {@link ModuleMain} instead. Only this class touches de.robv.
 */
public final class MainHook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!Module.targets(lpparam.packageName) || !Hooks.attach(new Legacy())) return;
        Module.load(lpparam.packageName, lpparam.processName, lpparam.classLoader);
    }

    private static final class Legacy implements Hooks.Backend {
        @Override public void hook(Member target, Hooks.Hook hook) {
            XposedBridge.hookMethod(target, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    Hooks.Call call = new Hooks.Call(param.method, param.thisObject, param.args);
                    param.setObjectExtra("call", call);
                    Hooks.before(hook, call);
                    if (!call.returnEarly) return;
                    if (call.hasThrowable()) param.setThrowable(call.getThrowable());
                    else param.setResult(call.getResult());
                }

                @Override protected void afterHookedMethod(MethodHookParam param) {
                    Object saved = param.getObjectExtra("call");
                    Hooks.Call call = saved instanceof Hooks.Call ? (Hooks.Call) saved
                            : new Hooks.Call(param.method, param.thisObject, param.args);
                    call.finish(param.getResult(), param.getThrowable());
                    Hooks.after(hook, call);
                    if (call.hasThrowable()) param.setThrowable(call.getThrowable());
                    else param.setResult(call.getResult());
                }
            });
        }

        @Override public void log(String message) {
            XposedBridge.log("TiktokPatchXposed: " + message);
        }
    }
}
