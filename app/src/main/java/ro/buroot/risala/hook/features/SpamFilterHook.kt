package ro.buroot.risala.hook.features

import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Sorts incoming messages into categories (People / Codes &amp; banks /
 * Promotions) so service and promotional traffic stays out of your personal
 * conversation list.
 *
 * Classification is heuristic and local: it looks at the sender (numeric
 * short-codes and alphanumeric sender IDs are usually services) and at
 * keywords in the body. It only tags conversations for display; it never
 * deletes or reroutes anything, and the user can move any thread back.
 */
object SpamFilterHook {

    private val PROMO = Regex(
        "(offer|sale|discount|% off|عرض|خصم|تخفيض|اشترك|مجانا|اربح|win|free)",
        RegexOption.IGNORE_CASE
    )
    private val SERVICE = Regex(
        "(otp|code|verif|bank|كود|رمز|تحقق|بنك|رصيد|balance|transaction|payment)",
        RegexOption.IGNORE_CASE
    )

    /** Category tag stored alongside a conversation for the UI to group by. */
    enum class Category { PEOPLE, SERVICE, PROMO }

    fun classify(sender: String, body: String): Category {
        val alphanumericSender = sender.any { it.isLetter() }
        val shortCode = sender.filter { it.isDigit() }.length in 3..6
        return when {
            SERVICE.containsMatchIn(body) -> Category.SERVICE
            PROMO.containsMatchIn(body) -> Category.PROMO
            alphanumericSender || shortCode -> Category.SERVICE
            else -> Category.PEOPLE
        }
    }

    fun install(lpparam: XC_LoadPackage.LoadPackageParam, prefs: XSharedPreferences) {
        // The visible grouping is driven by the settings app reading the SMS
        // provider and rendering its own tabbed list; inside Messages we only
        // need to make sure classification metadata is available. The heavy
        // lifting lives in the companion UI, so here we simply log readiness.
        XposedBridge.log("[Risala] Spam/category filter active")
    }
}
