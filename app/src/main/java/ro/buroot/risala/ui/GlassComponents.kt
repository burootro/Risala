package ro.buroot.risala.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared palette: dark base with a gold accent, under a frosted-glass card. */
object Glass {
    val Accent = Color(0xFFD4AF37)
    val CardStroke = Color(0x33FFFFFF)
    val cardBrush = Brush.verticalGradient(
        listOf(Color(0x22FFFFFF), Color(0x0DFFFFFF))
    )
    val TextPrimary = Color(0xFFF3F3F7)
    val TextSecondary = Color(0xFFB8B8C4)
}

/** A frosted translucent card. Real background blur is applied at window level. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Glass.cardBrush)
            .border(1.dp, Glass.CardStroke, RoundedCornerShape(22.dp))
            .padding(4.dp)
    ) { content() }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Glass.Accent,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
    )
}

/** A single feature row with a title, description and a toggle. */
@Composable
fun FeatureToggle(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Glass.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(description, color = Glass.TextSecondary, fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Glass.Accent,
                uncheckedTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}
