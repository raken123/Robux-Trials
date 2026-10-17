package se.islamiskaforbundet.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import se.islamiskaforbundet.app.Config

private data class Cause(val name: String, val description: String)

private val CAUSES = listOf(
    Cause("Allmän gåva", "Stöd Islamiska Förbundets verksamhet där behovet är störst."),
    Cause("Zakat", "Din obligatoriska zakat fördelas till de berättigade grupperna."),
    Cause("Sadaqa", "Frivillig välgörenhet till behövande."),
    Cause("Moské och lokaler", "Drift och underhåll av moskéer och samlingslokaler."),
    Cause("Utbildning och ungdom", "Koranskola, föreläsningar och ungdomsverksamhet."),
    Cause("Ramadan & iftar", "Iftar-måltider och Ramadan-aktiviteter."),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonateScreen() {
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    var cause by remember { mutableStateOf(CAUSES[0]) }
    var amount by remember { mutableStateOf("200") }
    var message by remember { mutableStateOf<String?>(null) }
    val amountValue = amount.toIntOrNull()?.takeIf { it > 0 }

    fun swish() {
        if (Config.SWISH_NUMBER.isBlank()) {
            uri.openUri(Config.WEBSITE)
            return
        }
        // Swish "payment request from app" deep link: prefilled payee, amount and message.
        val data = JSONObject().apply {
            put("version", 1)
            put("payee", JSONObject().put("value", Config.SWISH_NUMBER.filter { it.isDigit() }))
            put("amount", JSONObject().put("value", amountValue ?: 0).put("editable", amountValue == null))
            put("message", JSONObject().put("value", cause.name.take(50)).put("editable", false))
        }
        val link = Uri.parse("swish://payment?data=" + Uri.encode(data.toString()))
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, link))
        } catch (e: ActivityNotFoundException) {
            message = "Swish är inte installerat på telefonen."
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HeroHeader("Donera", "Stöd Islamiska Förbundet")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "\"Den som ger i Guds väg är som ett sädeskorn som skjuter sju ax, och i varje ax finns hundra korn.\" (2:261)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            SectionTitle("Ändamål")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CAUSES.forEach { c -> FilterChip(selected = c == cause, onClick = { cause = c }, label = { Text(c.name) }) }
            }
            Text(cause.description, style = MaterialTheme.typography.bodySmall)

            SectionTitle("Belopp")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(100, 200, 500, 1000, 2500).forEach { a ->
                    FilterChip(selected = amount == "$a", onClick = { amount = "$a" }, label = { Text("$a kr") })
                }
            }
            OutlinedTextField(
                value = amount,
                onValueChange = { v -> amount = v.filter { it.isDigit() }.take(7) },
                label = { Text("Eget belopp (kr)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { swish() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = IfGold, contentColor = IfNavyDark),
            ) {
                Text(
                    if (Config.SWISH_NUMBER.isBlank()) "Donera via webbplatsen"
                    else "Swisha ${amountValue ?: ""} kr",
                    fontWeight = FontWeight.Bold,
                )
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (Config.SWISH_NUMBER.isNotBlank()) {
                Text("Swish-nummer: ${Config.SWISH_NUMBER}", style = MaterialTheme.typography.bodySmall)
            }
            if (Config.BANKGIRO.isNotBlank()) {
                IfCard {
                    Text("Bankgiro", fontWeight = FontWeight.Bold)
                    Text(Config.BANKGIRO)
                    Text("Märk betalningen med \"${cause.name}\".", style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton(onClick = { uri.openUri(Config.WEBSITE) }, modifier = Modifier.fillMaxWidth()) {
                Text("Fler sätt att ge på islamiskaforbundet.se")
            }
        }
    }
}
