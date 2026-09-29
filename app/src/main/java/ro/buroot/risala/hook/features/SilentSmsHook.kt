package ro.buroot.risala.hook.features

import android.content.Context
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Detects "silent" / stealth SMS (3GPP type-0 / class-0 flash messages that
 * carry no user-visible body). These are commonly used to silently ping a
 * handset and locate it without the owner's knowledge. Android normally drops
 * them without any trace; this hook intercepts the dispatch path in the
 * telephony stack and broadcasts an alert instead of staying silent.
 *
 * This is purely defensive: it only observes inbound PDUs and never blocks,
 * sends, or alters traffic.
 */
object SilentSmsHook {

    fun install(lpparam: XC_LoadPackage.LoadPackageParam, prefs: XSharedPreferences) {
        val cl = lpparam.classLoader

        // The inbound dispatcher differs slightly across AOSP/One UI versions;
        // try the common class names in order.
        val candidates = listOf(
            "com.android.internal.telephony.InboundSmsHandler",
            "com.android.internal.telephony.gsm.GsmInboundSmsHandler"
        )

        for (name in candidates) {
            val clazz = XposedHelpers.findClassIfExists(name, cl) ?: continue
            XposedBridge.hookAllMethods(clazz, "dispatchMessage", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        inspect(param)
                    } catch (t: Throwable) {
                        XposedBridge.log("[Risala] SilentSms inspect: ${t.message}")
                    }
                }
            })
        }
    }

    private fun inspect(param: XC_MethodHook.MethodHookParam) {
        val sms = param.args.firstOrNull() ?: return
        // SmsMessageBase exposes the protocol identifier and an empty body for
        // type-0 messages.
        val pid = runCatching {
            XposedHelpers.callMethod(sms, "getProtocolIdentifier") as? Int
        }.getOrNull()
        val body = runCatching {
            XposedHelpers.callMethod(sms, "getMessageBody") as? String
        }.getOrNull()
        val originating = runCatching {
            XposedHelpers.callMethod(sms, "getOriginatingAddress") as? String
        }.getOrNull() ?: "unknown"

        val isSilent = pid == 0x40 || (body.isNullOrEmpty() && pid != null)
        if (isSilent) {
            XposedBridge.log("[Risala] Silent SMS detected from $originating (pid=$pid)")
            val ctx = XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
            ctx?.sendBroadcast(
                android.content.Intent("ro.buroot.risala.SILENT_SMS").apply {
                    setPackage("ro.buroot.risala")
                    putExtra("from", originating)
                    putExtra("pid", pid ?: -1)
                    putExtra("time", System.currentTimeMillis())
                }
            )
        }
    }
}
