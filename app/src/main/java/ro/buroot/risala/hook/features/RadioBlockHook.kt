package ro.buroot.risala.hook.features

import android.content.Context
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Radio-level block: drops inbound SMS from user-listed numbers inside the
 * telephony stack, before the message is written to the provider or shown as a
 * notification. Unlike an app-level block, the message never reaches any app
 * and leaves no row behind.
 *
 * Numbers come from the user's own block list; nothing is added automatically.
 */
object RadioBlockHook {

    private var blocked: Set<String> = emptySet()

    fun install(lpparam: XC_LoadPackage.LoadPackageParam, prefs: XSharedPreferences) {
        blocked = prefs.getString(Prefs_RADIO_BLOCK_LIST, "")
            .orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        if (blocked.isEmpty()) return

        val cl = lpparam.classLoader
        val clazz = XposedHelpers.findClassIfExists(
            "com.android.internal.telephony.InboundSmsHandler", cl
        ) ?: return

        XposedBridge.hookAllMethods(clazz, "dispatchMessage", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val sms = param.args.firstOrNull() ?: return
                val from = runCatching {
                    XposedHelpers.callMethod(sms, "getOriginatingAddress") as? String
                }.getOrNull() ?: return

                if (matches(from)) {
                    XposedBridge.log("[Risala] Radio-blocked SMS from $from")
                    logDrop(param.thisObject, from)
                    // Swallow the message: return without invoking the original.
                    param.result = null
                }
            }
        })
    }

    private fun matches(number: String): Boolean {
        val norm = number.filter { it.isDigit() || it == '+' }
        return blocked.any { b ->
            val bn = b.filter { it.isDigit() || it == '+' }
            norm == bn || norm.endsWith(bn) || bn.endsWith(norm)
        }
    }

    private fun logDrop(handler: Any, from: String) {
        val ctx = XposedHelpers.getObjectField(handler, "mContext") as? Context ?: return
        ctx.sendBroadcast(
            android.content.Intent("ro.buroot.risala.RADIO_BLOCK").apply {
                setPackage("ro.buroot.risala")
                putExtra("from", from)
                putExtra("time", System.currentTimeMillis())
            }
        )
    }

    // Local copy of the key to avoid a cross-module import cycle in the hook path.
    private const val Prefs_RADIO_BLOCK_LIST = "radio_block_list"
}
