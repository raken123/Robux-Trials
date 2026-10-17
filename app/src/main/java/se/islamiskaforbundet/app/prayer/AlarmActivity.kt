package se.islamiskaforbundet.app.prayer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import se.islamiskaforbundet.app.R
import se.islamiskaforbundet.app.data.Prefs
import se.islamiskaforbundet.app.ui.BarcodeScanner
import se.islamiskaforbundet.app.ui.IfGold
import se.islamiskaforbundet.app.ui.IfNavy
import se.islamiskaforbundet.app.ui.IfTheme

/** Full-screen alarm: only a scan of the registered prayer mat turns it off. */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* back must not silence the alarm */ }
        })

        val prayer = runCatching { Prayer.valueOf(intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER) ?: "") }
            .getOrDefault(Prayer.FAJR)
        val millis = intent.getLongExtra(AlarmScheduler.EXTRA_TIME, System.currentTimeMillis())
        val zoned = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        val day: LocalDate = zoned.toLocalDate()
        val timeText = zoned.format(DateTimeFormatter.ofPattern("HH:mm"))

        setContent {
            IfTheme {
                var done by remember { mutableStateOf(false) }
                var wrongCode by remember { mutableStateOf(false) }
                val matCode = Prefs.matCode

                fun finishPrayer() {
                    Prefs.setPrayed(day, prayer, true)
                    AlarmService.stop(this)
                    done = true
                }

                LaunchedEffect(done) {
                    if (done) { delay(2500); finish() }
                }
                LaunchedEffect(wrongCode) {
                    if (wrongCode) { delay(2000); wrongCode = false }
                }

                Column(
                    Modifier.fillMaxSize().background(IfNavy).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Spacer(Modifier.height(24.dp))
                    Image(painterResource(R.drawable.logo_glyph), null, Modifier.size(72.dp))
                    Text(prayer.ar, color = IfGold, fontSize = 40.sp)
                    Text("Dags för ${prayer.sv}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(timeText, color = Color.White.copy(alpha = 0.8f), fontSize = 20.sp)

                    when {
                        done -> {
                            Spacer(Modifier.height(32.dp))
                            Text(
                                "Må Allah acceptera din bön 🤲",
                                color = IfGold, fontSize = 24.sp, textAlign = TextAlign.Center,
                            )
                        }
                        matCode == null -> {
                            Text(
                                "Du har inte registrerat någon bönematta ännu. Registrera den under Bön → Bönematta så att larmet bara kan stängas av vid mattan.",
                                color = Color.White, textAlign = TextAlign.Center,
                            )
                            Button(
                                onClick = { finishPrayer() },
                                colors = ButtonDefaults.buttonColors(containerColor = IfGold, contentColor = IfNavy),
                            ) { Text("Stäng av larmet") }
                        }
                        else -> {
                            Text(
                                if (wrongCode) "Det där är inte din bönematta" else "Gå till din bönematta och skanna koden på den",
                                color = if (wrongCode) Color(0xFFFFB4AB) else Color.White,
                                textAlign = TextAlign.Center,
                            )
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(Color.Black)
                            ) {
                                BarcodeScanner(Modifier.fillMaxSize()) { code ->
                                    if (done) return@BarcodeScanner
                                    if (code == matCode) finishPrayer() else wrongCode = true
                                }
                            }
                            TextButton(onClick = {
                                // Emergency silence without logging the prayer; the alarm screen stays until dismissed.
                                AlarmService.stop(this@AlarmActivity)
                                finish()
                            }) { Text("Nödstopp (räknas inte som bön)", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun intent(context: Context, prayer: Prayer, time: Long): Intent =
            Intent(context, AlarmActivity::class.java)
                .putExtra(AlarmScheduler.EXTRA_PRAYER, prayer.name)
                .putExtra(AlarmScheduler.EXTRA_TIME, time)
    }
}
