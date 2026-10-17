package se.islamiskaforbundet.app.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.print.PrintHelper
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.launch
import se.islamiskaforbundet.app.data.LocationHelper
import se.islamiskaforbundet.app.data.Prefs
import se.islamiskaforbundet.app.data.SWEDISH_CITIES
import se.islamiskaforbundet.app.prayer.AlarmScheduler
import se.islamiskaforbundet.app.prayer.AlarmService
import se.islamiskaforbundet.app.prayer.CalcMethod
import se.islamiskaforbundet.app.prayer.Prayer

@Composable
fun SalahScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Bumped whenever a setting changes so the times and toggles recompute.
    var version by remember { mutableIntStateOf(0) }
    fun changed() { version++; AlarmScheduler.scheduleNext(context) }

    val today = LocalDate.now()
    val times = remember(version, today) { AlarmScheduler.today(today) }
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    var locating by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var showScanner by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res.values.any { it }) {
            locating = true
            scope.launch {
                val loc = LocationHelper.current(context)
                locating = false
                if (loc == null) {
                    locationError = "Kunde inte hitta din position. Välj stad istället."
                } else {
                    Prefs.latitude = loc.latitude
                    Prefs.longitude = loc.longitude
                    Prefs.placeName = LocationHelper.describe(loc.latitude, loc.longitude)
                    locationError = null
                    changed()
                }
            }
        } else {
            locationError = "Platsåtkomst nekades. Välj stad istället."
        }
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { version++ }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HeroHeader("Salah", "Bönetider för ${Prefs.placeName}")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Permissions the alarm depends on
            val needsNotif = Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            val needsFullScreen = Build.VERSION.SDK_INT >= 34 &&
                !context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
            if (needsNotif || needsFullScreen) {
                IfCard {
                    Text("Tillåt larmet", fontWeight = FontWeight.Bold)
                    Text("För att telefonen ska ringa vid bönetid behöver appen få visa aviseringar i helskärm.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.size(8.dp))
                    if (needsNotif) {
                        Button(onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Tillåt aviseringar") }
                    } else {
                        Button(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
                            )
                        }) { Text("Tillåt helskärmslarm") }
                    }
                }
            }

            SectionTitle("Idag")
            IfCard {
                Prayer.entries.forEachIndexed { i, p ->
                    if (i == 1) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("Soluppgång", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(times.sunrise?.format(fmt) ?: "–", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(96.dp))
                        }
                    }
                    val prayed = remember(version) { Prefs.isPrayed(today, p) }
                    val alarm = remember(version) { Prefs.alarmEnabled(p) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.sv, fontWeight = FontWeight.Bold)
                            Text(p.ar, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(times[p]?.format(fmt) ?: "–", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { Prefs.setAlarmEnabled(p, !alarm); changed() }) {
                            Icon(
                                if (alarm) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                                contentDescription = if (alarm) "Larm på" else "Larm av",
                                tint = if (alarm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            )
                        }
                        Checkbox(checked = prayed, onCheckedChange = { Prefs.setPrayed(today, p, it); version++ })
                    }
                    if (i < Prayer.entries.size - 1) HorizontalDivider()
                }
                Text(
                    "🔔 = larm (Fajr är alltid på från början)   ☑ = bön förrättad",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            SectionTitle("Senaste 7 dagarna")
            IfCard {
                val (count, streak) = remember(version) { stats() }
                Text("$count av 35 böner förrättade", fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { count / 35f }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                Row {
                    (6 downTo 0).forEach { d ->
                        val day = today.minusDays(d.toLong())
                        val n = Integer.bitCount(Prefs.prayedMask(day))
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.forLanguageTag("sv-SE")), style = MaterialTheme.typography.labelSmall)
                            Text("$n/5", fontWeight = if (n == 5) FontWeight.Bold else FontWeight.Normal,
                                color = if (n == 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Text("Svit: $streak ${if (streak == 1) "dag" else "dagar"} med alla fem bönerna", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }

            SectionTitle("Bönematta")
            IfCard {
                val registered = remember(version) { Prefs.matCode != null }
                Text(
                    if (registered) "✅ Din bönematta är registrerad" else "Ingen bönematta registrerad",
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "När larmet ringer stängs det bara av när du skannar koden på din bönematta. Fäst en QR-kod (skriv ut appens kod) eller en streckkod på mattan och registrera den här.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.size(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showScanner = true }) {
                        Icon(Icons.Filled.QrCodeScanner, null); Spacer(Modifier.width(6.dp)); Text("Registrera")
                    }
                    OutlinedButton(onClick = { showQr = true }) {
                        Icon(Icons.Filled.QrCode2, null); Spacer(Modifier.width(6.dp)); Text("Skapa QR-kod")
                    }
                }
                TextButton(onClick = {
                    val t = AlarmScheduler.nextPrayer()?.first ?: Prayer.FAJR
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, AlarmService::class.java)
                            .putExtra(AlarmScheduler.EXTRA_PRAYER, t.name)
                            .putExtra(AlarmScheduler.EXTRA_TIME, System.currentTimeMillis()),
                    )
                }) { Text("Testa larmet nu") }
            }

            SectionTitle("Plats och beräkning")
            IfCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Prefs.placeName, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    var open by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { open = true }) { Text("Välj stad") }
                        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                            SWEDISH_CITIES.forEach { c ->
                                DropdownMenuItem(text = { Text(c.name) }, onClick = {
                                    Prefs.latitude = c.lat; Prefs.longitude = c.lng; Prefs.placeName = c.name
                                    open = false; changed()
                                })
                            }
                        }
                    }
                }
                OutlinedButton(
                    enabled = !locating,
                    onClick = {
                        locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                    },
                ) {
                    Icon(Icons.Filled.MyLocation, null); Spacer(Modifier.width(6.dp))
                    Text(if (locating) "Söker position…" else "Använd min position")
                }
                locationError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.size(8.dp))
                var methodOpen by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Beräkningsmetod", style = MaterialTheme.typography.bodySmall)
                        Text(remember(version) { Prefs.method.label })
                    }
                    Box {
                        TextButton(onClick = { methodOpen = true }) { Text("Ändra") }
                        DropdownMenu(expanded = methodOpen, onDismissRequest = { methodOpen = false }) {
                            CalcMethod.entries.forEach { m ->
                                DropdownMenuItem(text = { Text(m.label) }, onClick = { Prefs.method = m; methodOpen = false; changed() })
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hanafi Asr", Modifier.weight(1f))
                    Switch(checked = remember(version) { Prefs.hanafiAsr }, onCheckedChange = { Prefs.hanafiAsr = it; changed() })
                }
            }
        }
    }

    if (showScanner) {
        Dialog(onDismissRequest = { showScanner = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(24.dp)).background(IfNavy).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Skanna koden på din bönematta", color = Color.White, fontWeight = FontWeight.Bold)
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(Color.Black)) {
                    BarcodeScanner(Modifier.fillMaxSize()) { code ->
                        if (showScanner) {
                            Prefs.matCode = code
                            showScanner = false
                            version++
                        }
                    }
                }
                TextButton(onClick = { showScanner = false }) { Text("Avbryt", color = IfGold) }
            }
        }
    }

    if (showQr) {
        val code = remember { "IF-BONEMATTA-" + UUID.randomUUID().toString().take(8).uppercase() }
        val bitmap = remember(code) { qrBitmap(code, 640) }
        AlertDialog(
            onDismissRequest = { showQr = false },
            title = { Text("QR-kod för bönematta") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(bitmap.asImageBitmap(), null, Modifier.size(220.dp))
                    Text(code, style = MaterialTheme.typography.labelSmall)
                    Text(
                        "Skriv ut koden och fäst den på din bönematta. Tryck sedan på \"Använd koden\".",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = {
                        PrintHelper(context).apply { scaleMode = PrintHelper.SCALE_MODE_FIT }
                            .printBitmap("Bönematta QR", bitmap)
                    }) { Text("Skriv ut") }
                }
            },
            confirmButton = {
                Button(onClick = { Prefs.matCode = code; showQr = false; version++ }) { Text("Använd koden") }
            },
            dismissButton = { TextButton(onClick = { showQr = false }) { Text("Stäng") } },
        )
    }
}

private fun stats(): Pair<Int, Int> {
    val today = LocalDate.now()
    val count = (0L..6L).sumOf { Integer.bitCount(Prefs.prayedMask(today.minusDays(it))) }
    var streak = 0
    // Today counts only once all five are done; otherwise start from yesterday.
    var day = if (Integer.bitCount(Prefs.prayedMask(today)) == 5) today else today.minusDays(1)
    while (Integer.bitCount(Prefs.prayedMask(day)) == 5 && streak < 3650) { streak++; day = day.minusDays(1) }
    return count to streak
}

private fun qrBitmap(text: String, size: Int): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) for (y in 0 until size) {
        bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    }
    return bmp
}
