package ro.buroot.risala.hook.features

import android.widget.TextView
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.json.JSONObject

/**
 * Custom number labels: lets the user attach their own readable name to any
 * number (e.g. a shortcode → "My Bank"). The label is shown in the conversation
 * list and header as an ADDITION to the real number — it never hides or
 * replaces the underlying address, so a message is never made to look like it
 * came from someone it didn't.
 */
object CustomLabelHook {

    private var labels: JSONObject = JSONObject()

    fun install(lpparam: XC_LoadPackage.LoadPackageParam, prefs: XSharedPreferences) {
        labels = runCatching { JSONObject(prefs.getString("custom_labels", "{}") ?: "{}") }
            .getOrDefault(JSONObject())
        if (labels.length() == 0) return

        // Append the user's label to conversation title TextViews as they bind.
        XposedBridge.hookAllMethods(TextView::class.java, "setText", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val text = param.args.getOrNull(0)?.toString() ?: return
                val normalized = text.filter { it.isDigit() || it == '+' }
                if (normalized.length < 3) return
                val label = lookup(normalized) ?: return
                if (!text.contains(label)) {
                    // Show "<label> · <number>" so the origin stays visible.
                    param.args[0] = "$label · $text"
                }
            }
        })
    }

    private fun lookup(number: String): String? {
        val keys = labels.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val kn = k.filter { it.isDigit() || it == '+' }
            if (kn.isNotEmpty() && (number == kn || number.endsWith(kn) || kn.endsWith(number))) {
                return labels.optString(k, null)
            }
        }
        return null
    }
}
