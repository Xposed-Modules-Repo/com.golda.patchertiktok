package com.golda.patchertiktok;

import android.util.Log;

import java.lang.reflect.Executable;
import java.lang.reflect.Member;

import io.github.libxposed.api.XposedModule;

/** Entry point for LSPosed with the modern libxposed API (META-INF/xposed/java_init.list). */
public final class ModuleMain extends XposedModule {
    private volatile String processName = "";

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        processName = param.getProcessName();
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!param.isFirstPackage() || !Module.targets(param.getPackageName())) return;
        if (!Hooks.attach(new Modern())) return;
        Module.load(param.getPackageName(), processName, param.getClassLoader());
    }

    private final class Modern implements Hooks.Backend {
        @Override public void hook(Member target, Hooks.Hook hook) {
            ModuleMain.this.hook((Executable) target).intercept(new Interceptor(target, hook));
        }

        @Override public void log(String message) {
            ModuleMain.this.log(Log.INFO, "TiktokPatchXposed", message);
        }
    }

    /** Runs a {@link Hooks.Hook} around the original call, as the legacy bridge did. */
    private static final class Interceptor implements Hooker {
        private final Member target;
        private final Hooks.Hook hook;

        Interceptor(Member target, Hooks.Hook hook) {
            this.target = target;
            this.hook = hook;
        }

        @Override public Object intercept(Chain chain) throws Throwable {
            Hooks.Call call = new Hooks.Call(target, chain.getThisObject(), chain.getArgs().toArray());
            Hooks.before(hook, call);
            if (call.returnEarly) {
                call.finish(call.getResult(), call.getThrowable());
            } else {
                Object result = null;
                Throwable error = null;
                try {
                    result = chain.proceed(call.args);
                } catch (Throwable thrown) {
                    error = thrown;
                }
                call.finish(result, error);
            }
            Hooks.after(hook, call);
            if (call.hasThrowable()) throw call.getThrowable();
            return call.getResult();
        }
    }
}
