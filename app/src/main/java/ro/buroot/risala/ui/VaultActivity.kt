package ro.buroot.risala.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ro.buroot.risala.data.VaultManager
import org.json.JSONArray

/**
 * The hidden vault. Requires biometric unlock before decrypting and showing the
 * conversations the user has moved out of the main app.
 *
 * Uses FragmentActivity because BiometricPrompt needs a FragmentActivity host.
 */
class VaultActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VaultScreen(::promptBiometric) }
    }

    private fun promptBiometric(onSuccess: () -> Unit, onFail: () -> Unit) {
        val canAuth = BiometricManager.from(this)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            onFail(); return
        }
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: AuthenticationResult) = onSuccess()
                override fun onAuthenticationError(code: Int, msg: CharSequence) = onFail()
            })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock vault")
                .setSubtitle("Authenticate to view hidden conversations")
                .setNegativeButtonText("Cancel")
                .build()
        )
    }
}

@Composable
private fun VaultScreen(
    prompt: (onSuccess: () -> Unit, onFail: () -> Unit) -> Unit
) {
    var unlocked by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf<List<String>>(emptyList()) }
    val ctx = androidx.compose.ui.platform.LocalContext.current

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0B12), Color(0xFF16121A)))),
        contentAlignment = Alignment.Center
    ) {
        if (!unlocked) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Vault locked", color = Glass.TextPrimary, fontSize = 22.sp)
                Text(
                    "Tap to unlock with biometrics",
                    color = Glass.Accent,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            // Auto-trigger the prompt on first composition.
            androidx.compose.runtime.LaunchedEffect(Unit) {
                prompt({
                    val json = runCatching { VaultManager.load(ctx) }.getOrDefault("[]")
                    val arr = runCatching { JSONArray(json) }.getOrDefault(JSONArray())
                    items = (0 until arr.length()).map { arr.getString(it) }
                    unlocked = true
                }, { /* stay locked */ })
            }
        } else {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Text("Vault", color = Glass.Accent, fontSize = 24.sp)
                if (items.isEmpty()) {
                    Text("No hidden conversations yet.",
                        color = Glass.TextSecondary, modifier = Modifier.padding(top = 16.dp))
                } else {
                    items.forEach {
                        GlassCard(Modifier.padding(vertical = 6.dp)) {
                            Text(it, color = Glass.TextPrimary,
                                modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}
