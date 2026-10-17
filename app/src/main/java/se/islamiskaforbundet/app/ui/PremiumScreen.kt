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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import se.islamiskaforbundet.app.data.PremiumCodes
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
    var scanning by remember { mutableStateOf(false) }
    var codeMessage by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HeroHeader("Premium", "${Config.PREMIUM_ZAKAT_PERCENT}% av pengarna går till zakat")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (Billing.premium) {
                IfCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = IfGold, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.size(12.dp))
                        Column {
                            if (Billing.purchased) {
                                Text("Du är Premium-medlem", fontWeight = FontWeight.Bold)
                                Text("Jazak Allahu khayran för ditt stöd!", style = MaterialTheme.typography.bodySmall)
                            } else {
                                val days = ((Billing.trialUntil - System.currentTimeMillis()) / 86_400_000L + 1).coerceAtLeast(1)
                                val end = Instant.ofEpochMilli(Billing.trialUntil).atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("sv-SE")))
                                Text("Gratis Premium – $days ${if (days == 1L) "dag" else "dagar"} kvar", fontWeight = FontWeight.Bold)
                                Text("Gäller till och med $end. Inget kort behövs.", style = MaterialTheme.typography.bodySmall)
                            }
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
            Benefit(Icons.Filled.OndemandVideo, "Fler videor", "Lås upp alla animerade videor: Hajj, Ramadan, Zakat och bönetiderna.")
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
            if (!Billing.purchased) {
                OutlinedButton(onClick = { codeMessage = null; scanning = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.QrCodeScanner, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Skanna kod för gratis Premium")
                }
            }
            codeMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
            Billing.status?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Text(
                "Prenumerationen hanteras av Google Play och kan avslutas när som helst under Google Play → Prenumerationer.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (scanning) {
        Dialog(onDismissRequest = { scanning = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(24.dp)).background(IfNavy).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Skanna Premium-koden", color = Color.White, fontWeight = FontWeight.Bold)
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(Color.Black)) {
                    BarcodeScanner(Modifier.fillMaxSize()) { code ->
                        if (!scanning) return@BarcodeScanner
                        when (val r = PremiumCodes.redeem(code)) {
                            is PremiumCodes.Result.Granted -> {
                                codeMessage = "🎉 Premium är aktiverat i ${r.days} dagar!"
                                scanning = false
                            }
                            PremiumCodes.Result.AlreadyUsed -> {
                                codeMessage = "Den här koden har redan använts på telefonen."
                                scanning = false
                            }
                            PremiumCodes.Result.Invalid -> codeMessage = "Ogiltig kod – försök igen."
                        }
                    }
                }
                codeMessage?.let { Text(it, color = IfGoldLight) }
                TextButton(onClick = { scanning = false }) { Text("Avbryt", color = IfGold) }
            }
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
