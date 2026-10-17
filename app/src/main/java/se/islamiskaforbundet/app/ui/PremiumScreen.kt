package se.islamiskaforbundet.app.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.islamiskaforbundet.app.Config
import se.islamiskaforbundet.app.data.Billing

@Composable
fun PremiumScreen() {
    val activity = LocalContext.current as Activity
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HeroHeader("Premium", "${Config.PREMIUM_ZAKAT_PERCENT}% av pengarna går till zakat")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (Billing.premium) {
                IfCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = IfGold, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Text("Du är Premium-medlem", fontWeight = FontWeight.Bold)
                            Text("Jazak Allahu khayran för ditt stöd!", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Text(
                "${Config.PREMIUM_ZAKAT_PERCENT}%",
                fontSize = 64.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "av intäkterna från Premium går till zakat och hjälper behövande.",
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Benefit(Icons.Filled.Block, "Inga reklamer", "Appen blir helt fri från annonser.")
            Benefit(Icons.Filled.OndemandVideo, "Fler videor", "Lås upp alla videokategorier: sira, tajwid, barn och mer.")
            Benefit(Icons.Filled.VolunteerActivism, "Ge varje månad", "Din prenumeration blir en löpande gåva.")
            if (!Billing.premium) {
                Button(
                    onClick = { Billing.buy(activity) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = IfGold, contentColor = IfNavyDark),
                ) {
                    Text(
                        Billing.price?.let { "Bli Premium · $it/månad" } ?: "Bli Premium",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Billing.status?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Text(
                "Prenumerationen hanteras av Google Play och kan avslutas när som helst under Google Play → Prenumerationer.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun Benefit(icon: ImageVector, title: String, text: String) {
    IfCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.size(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(text, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
