package ro.buroot.risala.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ro.buroot.risala.core.Prefs

/**
 * The module's control panel. A scrolling glass sheet of feature toggles,
 * grouped into Privacy, Messaging, System &amp; SIM, and Backup. Every toggle
 * writes straight to the shared prefs the hook process reads.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RisalaSettingsScreen() }
    }
}

@Composable
private fun RisalaSettingsScreen() {
    val ctx = LocalContext.current
    // Local mirror of toggle state, seeded from prefs.
    val state = rememberFeatureState()

    fun toggle(key: String): (Boolean) -> Unit = { v ->
        state[key] = v
        SettingsPrefs.setBool(ctx, key, v)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0B0B12), Color(0xFF16121A)))
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(48.dp))
            Text(
                "الرسائل",
                color = Glass.TextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
            Text(
                "Glass enhancements for Google Messages",
                color = Glass.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )

            SectionTitle("Privacy & Security")
            GlassCard {
                FeatureToggle("Hidden vault", "Biometric-locked private conversations.",
                    state[Prefs.VAULT_ENABLED] == true, toggle(Prefs.VAULT_ENABLED))
                FeatureToggle("Block apps from reading SMS", "Deny other apps access to your messages.",
                    state[Prefs.BLOCK_SMS_READERS] == true, toggle(Prefs.BLOCK_SMS_READERS))
                FeatureToggle("Silent-SMS detection", "Alert on stealth tracking messages.",
                    state[Prefs.SILENT_SMS_DETECT] == true, toggle(Prefs.SILENT_SMS_DETECT))
                FeatureToggle("SMS access log", "Record which apps read your messages.",
                    state[Prefs.READER_LOG] == true, toggle(Prefs.READER_LOG))
            }

            SectionTitle("Messaging")
            GlassCard {
                FeatureToggle("Auto-copy verification codes", "Copy OTP codes on arrival.",
                    state[Prefs.OTP_AUTO_COPY] == true, toggle(Prefs.OTP_AUTO_COPY))
                FeatureToggle("Auto-delete codes", "Remove OTP messages after a few minutes.",
                    state[Prefs.OTP_AUTO_WIPE] == true, toggle(Prefs.OTP_AUTO_WIPE))
                FeatureToggle("Separate banks & promotions", "Sort service and promo messages into tabs.",
                    state[Prefs.SPAM_FILTER] == true, toggle(Prefs.SPAM_FILTER))
                FeatureToggle("Custom number labels", "Give any number a clear name of your own.",
                    state[Prefs.CUSTOM_LABELS] == true, toggle(Prefs.CUSTOM_LABELS))
                FeatureToggle("Reliable scheduled send", "Send later via a system alarm.",
                    state[Prefs.SCHEDULE_ENABLED] == true, toggle(Prefs.SCHEDULE_ENABLED))
                FeatureToggle("Reply from notification", "Reply, mark read and delete without opening.",
                    state[Prefs.QUICK_REPLY] == true, toggle(Prefs.QUICK_REPLY))
            }

            SectionTitle("System & SIM")
            GlassCard {
                FeatureToggle("Radio-level block", "Drop messages/calls before they are logged.",
                    state[Prefs.RADIO_BLOCK] == true, toggle(Prefs.RADIO_BLOCK))
                FeatureToggle("Per-conversation SIM", "Pin each thread to a SIM.",
                    state[Prefs.PER_CONVO_SIM] == true, toggle(Prefs.PER_CONVO_SIM))
                FeatureToggle("Message centre (SMSC)", "View and set the SMSC per SIM.",
                    state[Prefs.SMSC_TOOLS] == true, toggle(Prefs.SMSC_TOOLS))
                FeatureToggle("SIM / IMEI change watch", "Alert when SIM or device identity changes.",
                    state[Prefs.SIM_WATCH] == true, toggle(Prefs.SIM_WATCH))
            }

            SectionTitle("Backup & Export")
            GlassCard {
                FeatureToggle("Full backup & restore", "Complete SMS/MMS backup.",
                    state[Prefs.BACKUP_ENABLED] == true, toggle(Prefs.BACKUP_ENABLED))
                FeatureToggle("Readable export", "Export to HTML, JSON or PDF.",
                    state[Prefs.EXPORT_ENABLED] == true, toggle(Prefs.EXPORT_ENABLED))
                FeatureToggle("Recover deleted messages", "Best-effort scan for deleted rows.",
                    state[Prefs.RECOVER_ENABLED] == true, toggle(Prefs.RECOVER_ENABLED))
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun rememberFeatureState() = mutableStateMapOf<String, Boolean>().apply {
    val ctx = LocalContext.current
    val keys = listOf(
        Prefs.VAULT_ENABLED, Prefs.BLOCK_SMS_READERS, Prefs.SILENT_SMS_DETECT, Prefs.READER_LOG,
        Prefs.OTP_AUTO_COPY, Prefs.OTP_AUTO_WIPE, Prefs.SPAM_FILTER, Prefs.CUSTOM_LABELS,
        Prefs.SCHEDULE_ENABLED, Prefs.QUICK_REPLY, Prefs.RADIO_BLOCK, Prefs.PER_CONVO_SIM,
        Prefs.SMSC_TOOLS, Prefs.SIM_WATCH, Prefs.BACKUP_ENABLED, Prefs.EXPORT_ENABLED,
        Prefs.RECOVER_ENABLED
    )
    for (k in keys) put(k, SettingsPrefs.bool(ctx, k))
}
