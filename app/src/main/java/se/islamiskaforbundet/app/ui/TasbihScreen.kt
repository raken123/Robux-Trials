package se.islamiskaforbundet.app.ui

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.islamiskaforbundet.app.data.Prefs

private data class Dhikr(val arabic: String, val latin: String, val swedish: String)

private val DHIKR = listOf(
    Dhikr("سُبْحَانَ ٱللَّٰهِ", "SubhanAllah", "Ära vare Allah"),
    Dhikr("ٱلْحَمْدُ لِلَّٰهِ", "Alhamdulillah", "All lov och pris tillkommer Allah"),
    Dhikr("ٱللَّٰهُ أَكْبَرُ", "Allahu Akbar", "Allah är störst"),
    Dhikr("لَا إِلَٰهَ إِلَّا ٱللَّٰهُ", "La ilaha illa Allah", "Det finns ingen gud utom Allah"),
    Dhikr("أَسْتَغْفِرُ ٱللَّٰهَ", "Astaghfirullah", "Jag ber Allah om förlåtelse"),
    Dhikr("سُبْحَانَ ٱللَّٰهِ وَبِحَمْدِهِ", "SubhanAllahi wa bihamdihi", "Ära vare Allah och all lov tillkommer Honom"),
)

@Composable
fun TasbihScreen() {
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context.getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)
    }
    var dhikr by remember { mutableStateOf(DHIKR[0]) }
    var target by remember { mutableIntStateOf(33) }
    var count by remember { mutableIntStateOf(0) }
    var rounds by remember { mutableIntStateOf(0) }
    var total by remember { mutableLongStateOf(Prefs.tasbihTotal) }
    val progress by animateFloatAsState(if (target == 0) 0f else (count % target) / target.toFloat(), label = "p")

    fun tap() {
        count++
        total++
        Prefs.tasbihTotal = total
        if (target > 0 && count % target == 0) {
            rounds++
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120), -1))
        } else {
            vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    Column(Modifier.fillMaxSize()) {
        HeroHeader("Tasbih", "Totalt: $total")
        LazyRow(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        ) {
            items(DHIKR) { d ->
                FilterChip(selected = d == dhikr, onClick = { dhikr = d; count = 0; rounds = 0 }, label = { Text(d.latin) })
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(33, 99, 100, 0).forEach { t ->
                FilterChip(selected = target == t, onClick = { target = t; count = 0; rounds = 0 },
                    label = { Text(if (t == 0) "∞" else "$t") })
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(dhikr.arabic, fontSize = 34.sp, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            Text(dhikr.swedish, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Box(Modifier.padding(24.dp).size(260.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 10.dp,
                    color = IfGold,
                    trackColor = IfGold.copy(alpha = 0.2f),
                )
                Box(
                    Modifier
                        .size(220.dp)
                        .background(Brush.radialGradient(listOf(IfNavy, IfNavyDark)), CircleShape)
                        .border(2.dp, IfGold, CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { tap() },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${if (target == 0) count else count % target}", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
                        Text(if (target == 0) "tryck" else "av $target · varv $rounds", color = IfGoldLight)
                    }
                }
            }
            OutlinedButton(onClick = { count = 0; rounds = 0 }) {
                Icon(Icons.Filled.Refresh, null)
                Text("  Nollställ")
            }
        }
    }
}
