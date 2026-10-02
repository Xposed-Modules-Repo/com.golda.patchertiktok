# Entry point listed in assets/xposed_init.
-keep class com.golda.patchertiktok.MainHook { <init>(); }
-repackageclasses
-allowaccessmodification
