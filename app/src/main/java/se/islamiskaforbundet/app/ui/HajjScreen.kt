package se.islamiskaforbundet.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

private enum class Trip(val label: String, val days: Int, val pkg: Int, val hady: Int) {
    HAJJ("Hajj", 21, 75000, 2500),
    UMRAH("Umrah", 10, 18000, 0),
}

@Composable
fun HajjScreen() {
    var trip by remember { mutableStateOf(Trip.HAJJ) }
    val fields = remember(trip) {
        listOf(
            Field("Antal personer", "1"),
            Field("Resepaket per person (flyg, hotell, transport)", trip.pkg.toString()),
            Field("Visum och avgifter per person", "2000"),
            Field("Antal dagar", trip.days.toString()),
            Field("Mat per person och dag", "250"),
            Field("Offerdjur (hady) per person", trip.hady.toString()),
            Field("Fickpengar och gåvor per person", "3000"),
            Field("Månader kvar till resan", "12"),
            Field("Redan sparat", "0"),
        ).map { mutableStateOf(it) }
    }
    fun v(i: Int) = fields[i].value.value.toLongOrNull() ?: 0L

    val persons = v(0).coerceAtLeast(0)
    val perPerson = v(1) + v(2) + v(3) * v(4) + v(5) + v(6)
    val total = perPerson * persons
    val remaining = (total - v(8)).coerceAtLeast(0)
    val months = v(7)
    val monthly = if (months > 0) (remaining + months - 1) / months else remaining
    val sek = remember { NumberFormat.getInstance(Locale.forLanguageTag("sv-SE")) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HeroHeader("Pengarkalkyl", "Planera och spara till din resa")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Trip.entries.forEach { t -> FilterChip(selected = trip == t, onClick = { trip = t }, label = { Text(t.label) }) }
            }
            IfCard {
                Text("Totalt för ${trip.label}", style = MaterialTheme.typography.labelLarge)
                Text("${sek.format(total)} kr", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("${sek.format(perPerson)} kr per person", style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Spara ${sek.format(monthly)} kr i månaden", fontWeight = FontWeight.Bold)
                Text(
                    if (months > 0) "för att ha ${sek.format(remaining)} kr kvar att spara ihop om $months månader"
                    else "Ange antal månader kvar för en sparplan",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            fields.forEach { state ->
                val f = state.value
                OutlinedTextField(
                    value = f.value,
                    onValueChange = { new -> state.value = f.copy(value = new.filter { it.isDigit() }.take(9)) },
                    label = { Text(f.label) },
                    suffix = { if (!f.label.startsWith("Antal") && !f.label.startsWith("Månader")) Text("kr") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                "Förifyllda belopp är ungefärliga uppskattningar. Kontrollera aktuella priser hos en godkänd resebyrå. Hajj är en plikt för den som har råd – planera utan lån med ränta.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private data class Field(val label: String, val value: String)
