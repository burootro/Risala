package ro.buroot.risala.hook.features

import android.content.Context
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * SIM / device-identity change watch: records the active SIM serials and warns
 * when they change unexpectedly (e.g. a SIM swap while you weren't looking).
 * It only reads identifiers the user's own app is entitled to and stores a
 * hash locally for comparison — nothing is transmitted anywhere.
 */
object SimWatchHook {

    fun install(lpparam: XC_LoadPackage.LoadPackageParam, prefs: XSharedPreferences) {
        XposedBridge.log("[Risala] SIM watch active")
    }

    /** Called by the settings app / boot receiver to compare current SIMs. */
    fun checkForChange(ctx: Context, prefs: android.content.SharedPreferences) {
        val current = currentSimSerials(ctx)
        val last = prefs.getString("last_known_sim_serials", null)
        if (last != null && last != current) {
            ctx.sendBroadcast(
                android.content.Intent("ro.buroot.risala.SIM_CHANGED").apply {
                    setPackage("ro.buroot.risala")
                    putExtra("time", System.currentTimeMillis())
                }
            )
        }
        prefs.edit().putString("last_known_sim_serials", current).apply()
    }

    private fun currentSimSerials(ctx: Context): String {
        return runCatching {
            val sm = ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                as android.telephony.SubscriptionManager
            @Suppress("MissingPermission")
            val list = sm.activeSubscriptionInfoList ?: emptyList()
            list.joinToString("|") { "${it.subscriptionId}:${it.carrierName}" }
                .hashCode().toString()
        }.getOrDefault("")
    }
}
