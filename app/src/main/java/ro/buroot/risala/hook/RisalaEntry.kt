package ro.buroot.risala.hook

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import ro.buroot.risala.core.Prefs
import ro.buroot.risala.hook.features.BlockSmsReadersHook
import ro.buroot.risala.hook.features.CustomLabelHook
import ro.buroot.risala.hook.features.DualSimHook
import ro.buroot.risala.hook.features.GlassUiHook
import ro.buroot.risala.hook.features.OtpHook
import ro.buroot.risala.hook.features.QuickReplyHook
import ro.buroot.risala.hook.features.RadioBlockHook
import ro.buroot.risala.hook.features.SilentSmsHook
import ro.buroot.risala.hook.features.SimWatchHook
import ro.buroot.risala.hook.features.SpamFilterHook

/**
 * Module entry point declared in assets/xposed_init.
 *
 * The hooks fall into two groups:
 *  - Framework hooks (loaded when the "android" / telephony package is in scope):
 *    silent-SMS detection, radio-level block, and the guard that blocks other
 *    apps from reading the SMS provider.
 *  - App hooks (loaded inside Google Messages): glass UI, OTP handling, spam
 *    tabs, custom labels, dual-SIM, quick reply.
 *
 * Every feature reads its own flag from XSharedPreferences and does nothing when
 * disabled, so the module is inert until the user turns something on.
 */
class RisalaEntry : IXposedHookLoadPackage, IXposedHookZygoteInit {

    private lateinit var prefs: XSharedPreferences

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        prefs = XSharedPreferences(BuildConfigPkg.PKG, Prefs.FILE).apply {
            makeWorldReadable()
        }
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Ensure prefs exist even if initZygote was skipped on some ROMs.
        if (!::prefs.isInitialized) {
            prefs = XSharedPreferences(BuildConfigPkg.PKG, Prefs.FILE)
        }
        prefs.reload()

        when (lpparam.packageName) {
            "android", "com.android.phone" -> handleFramework(lpparam)
            GOOGLE_MESSAGES -> handleMessagesApp(lpparam)
        }
    }

    private fun handleFramework(lpparam: XC_LoadPackage.LoadPackageParam) {
        safe("SilentSms") {
            if (prefs.getBoolean(Prefs.SILENT_SMS_DETECT, true)) {
                SilentSmsHook.install(lpparam, prefs)
            }
        }
        safe("RadioBlock") {
            if (prefs.getBoolean(Prefs.RADIO_BLOCK, false)) {
                RadioBlockHook.install(lpparam, prefs)
            }
        }
        safe("BlockSmsReaders") {
            if (prefs.getBoolean(Prefs.BLOCK_SMS_READERS, false)) {
                BlockSmsReadersHook.install(lpparam, prefs)
            }
        }
    }

    private fun handleMessagesApp(lpparam: XC_LoadPackage.LoadPackageParam) {
        safe("GlassUi") { GlassUiHook.install(lpparam, prefs) }
        safe("Otp") {
            if (prefs.getBoolean(Prefs.OTP_AUTO_COPY, true) ||
                prefs.getBoolean(Prefs.OTP_AUTO_WIPE, false)
            ) OtpHook.install(lpparam, prefs)
        }
        safe("SpamFilter") {
            if (prefs.getBoolean(Prefs.SPAM_FILTER, true)) SpamFilterHook.install(lpparam, prefs)
        }
        safe("CustomLabel") {
            if (prefs.getBoolean(Prefs.CUSTOM_LABELS, false)) CustomLabelHook.install(lpparam, prefs)
        }
        safe("DualSim") {
            if (prefs.getBoolean(Prefs.PER_CONVO_SIM, false)) DualSimHook.install(lpparam, prefs)
        }
        safe("QuickReply") {
            if (prefs.getBoolean(Prefs.QUICK_REPLY, false)) QuickReplyHook.install(lpparam, prefs)
        }
        safe("SimWatch") {
            if (prefs.getBoolean(Prefs.SIM_WATCH, false)) SimWatchHook.install(lpparam, prefs)
        }
    }

    /** Runs a hook installer, logging (never crashing the host) on failure. */
    private inline fun safe(tag: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            XposedBridge.log("[Risala] $tag hook failed: ${t.message}")
        }
    }

    companion object {
        const val GOOGLE_MESSAGES = "com.google.android.apps.messaging"
    }
}

/** Holds this module's own package name so hooks can locate the prefs file. */
object BuildConfigPkg {
    const val PKG = "ro.buroot.risala"
}
