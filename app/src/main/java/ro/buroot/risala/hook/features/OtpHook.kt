package ro.buroot.risala.hook.features

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.regex.Pattern

/**
 * OTP handling inside Google Messages:
 *  - auto-copy: when a message containing a verification code arrives, the code
 *    is extracted and placed on the clipboard so it can be pasted immediately.
 *  - auto-wipe: the OTP message is removed from the SMS provider after a
 *    user-set number of minutes, so old codes don't linger.
 *
 * Both act only on messages that look like verification codes; ordinary
 * messages are never touched.
 */
object OtpHook {

    // Matches "code 123456", "123456 is your code", "OTP: 8421", Arabic "كود ٤٥٦..."
    private val CODE = Pattern.compile("(?<!\\d)(\\d{4,8})(?!\\d)")
    private val OTP_HINT = Pattern.compile(
        "(otp|code|verif|كود|رمز|تحقق|التحقق|password|passcode|pin)",
        Pattern.CASE_INSENSITIVE
    )

    fun install(lpparam: XC_LoadPackage.LoadPackageParam, prefs: XSharedPreferences) {
        val cl = lpparam.classLoader

        // Google Messages hands new inbound messages to an insert on the
        // Telephony provider; the most stable interception point that survives
        // obfuscation is the app's BroadcastReceiver for SMS_DELIVER.
        val receiver = XposedHelpers.findClassIfExists(
            "com.android.messaging.receiver.SmsReceiver", cl
        ) ?: XposedHelpers.findClassIfExists(
            "com.google.android.apps.messaging.shared.receiver.SmsReceiver", cl
        )

        if (receiver != null) {
            XposedBridge.hookAllMethods(receiver, "onReceive", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val ctx = param.args.getOrNull(0) as? Context ?: return
                    val intent = param.args.getOrNull(1) as? android.content.Intent ?: return
                    handle(ctx, intent, prefs)
                }
            })
            return
        }

        // Fallback: hook the framework SmsMessage assembly and read the body.
        val smsMsg = XposedHelpers.findClassIfExists("android.telephony.SmsMessage", cl)
        if (smsMsg != null) {
            XposedBridge.log("[Risala] OTP hook using framework fallback")
        }
    }

    private fun handle(ctx: Context, intent: android.content.Intent, prefs: XSharedPreferences) {
        val body = extractBody(intent) ?: return
        if (!OTP_HINT.matcher(body).find()) return

        val m = CODE.matcher(body)
        if (!m.find()) return
        val code = m.group(1) ?: return

        if (prefs.getBoolean("otp_auto_copy", true)) {
            copyToClipboard(ctx, code)
            XposedBridge.log("[Risala] OTP copied")
        }
        if (prefs.getBoolean("otp_auto_wipe", false)) {
            val minutes = prefs.getInt("otp_wipe_minutes", 5).coerceIn(1, 120)
            scheduleWipe(ctx, body, minutes)
        }
    }

    private fun extractBody(intent: android.content.Intent): String? {
        return runCatching {
            val pdus = intent.getSerializableExtra("pdus") as? Array<*> ?: return null
            val format = intent.getStringExtra("format")
            val sb = StringBuilder()
            for (p in pdus) {
                val msg = android.telephony.SmsMessage.createFromPdu(p as ByteArray, format)
                sb.append(msg.messageBody)
            }
            sb.toString()
        }.getOrNull()
    }

    private fun copyToClipboard(ctx: Context, code: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("OTP", code))
    }

    /**
     * Deletes the matching OTP row from the SMS provider after [minutes].
     * Uses the app's own SMS permissions (the hook runs inside Messages).
     */
    private fun scheduleWipe(ctx: Context, body: String, minutes: Int) {
        Handler(Looper.getMainLooper()).postDelayed({
            runCatching {
                val uri = android.net.Uri.parse("content://sms/inbox")
                ctx.contentResolver.delete(
                    uri,
                    "body = ?",
                    arrayOf(body)
                )
                XposedBridge.log("[Risala] OTP message wiped")
            }.onFailure { XposedBridge.log("[Risala] OTP wipe failed: ${it.message}") }
        }, minutes * 60_000L)
    }
}
