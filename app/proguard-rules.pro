# Keep the Xposed entry point and all hook classes/methods intact so LSPosed
# can find them by name at runtime.
-keep class ro.buroot.risala.hook.** { *; }
-keepnames class ro.buroot.risala.hook.RisalaEntry

# Xposed API is provided by the framework.
-dontwarn de.robv.android.xposed.**

# Gson models used for backup/restore.
-keep class ro.buroot.risala.data.TelephonyRepo$Message { *; }
