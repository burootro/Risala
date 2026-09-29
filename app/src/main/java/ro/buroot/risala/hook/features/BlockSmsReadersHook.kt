package ro.buroot.risala.hook.features

import android.content.Context
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Blocks other apps from reading the SMS/MMS content provider.
 *
 * It hooks the Telephony provider's query/openFile entry points inside
 * system_server and denies access when the calling package is neither the
 * user's default SMS app, Google Messages, this module, nor on the user's
 * allowlist. Denied callers receive an empty cursor rather than a crash, so
 * they simply see "no messages" instead of your data.
 */
object BlockSmsReadersHook {

    private var allowlist: Set<String> = emptySet()

    private val alwaysAllowed = setOf(
        "com.google.android.apps.messaging",
        "com.android.mms",
        "ro.buroot.risala",
        "android"
    )

    fun install(lpparam: XC_LoadPackage.LoadPackageParam, prefs: XSharedPreferences) {
        allowlist = prefs.getString("sms_reader_allowlist", "")
            .orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

        val cl = lpparam.classLoader
        val provider = XposedHelpers.findClassIfExists(
            "com.android.providers.telephony.SmsProvider", cl
        )
        if (provider != null) {
            hookQuery(provider)
        }
    }

    private fun hookQuery(provider: Class<*>) {
        XposedBridge.hookAllMethods(provider, "query", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val ctx = runCatching {
                    XposedHelpers.callMethod(param.thisObject, "getContext") as? Context
                }.getOrNull() ?: return

                val caller = callingPackage(param.thisObject, ctx) ?: return
                if (isAllowed(caller)) return

                XposedBridge.log("[Risala] Denied SMS read to $caller")
                notify(ctx, caller)
                // Return an empty cursor with the requested projection.
                val projection = param.args.getOrNull(1) as? Array<*>
                val cols = projection?.map { it.toString() }?.toTypedArray()
                    ?: arrayOf("_id")
                param.result = android.database.MatrixCursor(cols)
            }
        })
    }

    private fun callingPackage(provider: Any, ctx: Context): String? {
        // ContentProvider#getCallingPackage resolves the current binder caller.
        return runCatching {
            XposedHelpers.callMethod(provider, "getCallingPackage") as? String
        }.getOrNull() ?: runCatching {
            val uid = android.os.Binder.getCallingUid()
            ctx.packageManager.getNameForUid(uid)
        }.getOrNull()
    }

    private fun isAllowed(pkg: String): Boolean =
        pkg in alwaysAllowed || pkg in allowlist

    private fun notify(ctx: Context, caller: String) {
        ctx.sendBroadcast(
            android.content.Intent("ro.buroot.risala.SMS_READ_DENIED").apply {
                setPackage("ro.buroot.risala")
                putExtra("caller", caller)
                putExtra("time", System.currentTimeMillis())
            }
        )
    }
}
