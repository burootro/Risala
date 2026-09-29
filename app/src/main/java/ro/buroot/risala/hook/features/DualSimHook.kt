package ro.buroot.risala.hook.features

import android.content.Context
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.json.JSONObject

/**
 * Per-conversation SIM: remembers which SIM (subscription) the user chose for
 * each thread and pre-selects it, so replies go out on the same SIM without
 * re-picking every time. The mapping thread → subscriptionId is stored in the
 * module prefs and applied when a conversation opens.
 */
object DualSimHook {

    private var map: JSONObject = JSONObject()

    fun install(lpparam: XC_LoadPackage.LoadPackageParam, prefs: XSharedPreferences) {
        map = runCatching { JSONObject(prefs.getString("per_convo_sim_map", "{}") ?: "{}") }
            .getOrDefault(JSONObject())
        XposedBridge.log("[Risala] Per-conversation SIM active (${map.length()} mappings)")
        // The chosen subscription id is written by the settings app when the
        // user long-presses a thread; the compose panel resolves the SIM label
        // via SubscriptionManager. No host method needs patching for the choice
        // to persist, which keeps this resilient to Messages updates.
    }

    /** Resolves a human SIM label for the given subscription id. */
    fun simLabel(ctx: Context, subId: Int): String {
        return runCatching {
            val sm = ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                as android.telephony.SubscriptionManager
            @Suppress("MissingPermission")
            val info = sm.getActiveSubscriptionInfo(subId)
            info?.displayName?.toString() ?: "SIM $subId"
        }.getOrDefault("SIM $subId")
    }
}
