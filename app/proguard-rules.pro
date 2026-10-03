# Entry points: META-INF/xposed/java_init.list (modern API) and assets/xposed_init (legacy).
# Each entry and its nested classes reference one API flavour only; keeping them stops R8 from
# merging them into shared code that would then fail to load under the other framework.
-keep class com.golda.patchertiktok.ModuleMain* { *; }
-keep class com.golda.patchertiktok.MainHook* { *; }
-repackageclasses
-allowaccessmodification
