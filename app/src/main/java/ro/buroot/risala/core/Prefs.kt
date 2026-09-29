package ro.buroot.risala.core

/**
 * Central registry of every feature flag and preference key used by both the
 * Xposed hooks and the settings UI. Keeping keys in one place avoids typos
 * between the two processes, which read the same XSharedPreferences file.
 */
object Prefs {

    /** Shared-prefs file name. Must be world-readable for the hook process. */
    const val FILE = "risala_prefs"

    // ----- Privacy & security -----
    const val VAULT_ENABLED = "vault_enabled"
    const val VAULT_SECRET_CODE = "vault_secret_code"        // dialer code to open the vault
    const val BLOCK_SMS_READERS = "block_sms_readers"
    const val SMS_READER_ALLOWLIST = "sms_reader_allowlist"  // comma-separated packages
    const val SILENT_SMS_DETECT = "silent_sms_detect"
    const val READER_LOG = "reader_log_enabled"

    // ----- Messaging -----
    const val OTP_AUTO_COPY = "otp_auto_copy"
    const val OTP_AUTO_WIPE = "otp_auto_wipe"
    const val OTP_WIPE_MINUTES = "otp_wipe_minutes"
    const val SPAM_FILTER = "spam_filter"
    const val CUSTOM_LABELS = "custom_labels"                // JSON map number -> label
    const val SCHEDULE_ENABLED = "schedule_enabled"
    const val QUICK_REPLY = "quick_reply"

    // ----- System & SIM -----
    const val RADIO_BLOCK = "radio_block"
    const val RADIO_BLOCK_LIST = "radio_block_list"          // comma-separated numbers
    const val PER_CONVO_SIM = "per_convo_sim"
    const val SMSC_TOOLS = "smsc_tools"
    const val SIM_WATCH = "sim_watch"
    const val LAST_KNOWN_SIM = "last_known_sim_serials"

    // ----- Backup & export -----
    const val BACKUP_ENABLED = "backup_enabled"
    const val EXPORT_ENABLED = "export_enabled"
    const val RECOVER_ENABLED = "recover_enabled"

    // ----- UI -----
    const val GLASS_BLUR = "glass_blur_strength"             // 0..100

    /** Default values used when a key has never been written. */
    fun default(key: String): Any = when (key) {
        OTP_AUTO_COPY, SILENT_SMS_DETECT, SPAM_FILTER -> true
        OTP_WIPE_MINUTES -> 5
        GLASS_BLUR -> 60
        VAULT_SECRET_CODE -> "*#0000#"
        else -> false
    }
}
