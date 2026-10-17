package se.islamiskaforbundet.app.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.GeomagneticField
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import se.islamiskaforbundet.app.data.Prefs

private const val KAABA_LAT = 21.422487
private const val KAABA_LNG = 39.826206

fun qiblaBearing(lat: Double, lng: Double): Double {
    val phi = Math.toRadians(lat)
    val phiK = Math.toRadians(KAABA_LAT)
    val dLng = Math.toRadians(KAABA_LNG - lng)
    val b = Math.toDegrees(atan2(sin(dLng), cos(phi) * kotlin.math.tan(phiK) - sin(phi) * cos(dLng)))
    return (b + 360) % 360
}

@Composable
fun QiblaScreen() {
    val context = LocalContext.current
    val lat = Prefs.latitude
    val lng = Prefs.longitude
    val qibla = remember(lat, lng) { qiblaBearing(lat, lng).toFloat() }
    val distanceKm = remember(lat, lng) {
        val r = FloatArray(1)
        android.location.Location.distanceBetween(lat, lng, KAABA_LAT, KAABA_LNG, r)
        (r[0] / 1000).roundToInt()
    }
    var heading by remember { mutableFloatStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }
    var lowAccuracy by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val declination = GeomagneticField(lat.toFloat(), lng.toFloat(), 0f, System.currentTimeMillis()).declination
        val rot = FloatArray(9)
        val orient = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rot, e.values)
                SensorManager.getOrientation(rot, orient)
                val az = (Math.toDegrees(orient[0].toDouble()).toFloat() + declination + 360) % 360
                // Low-pass filter to keep the needle steady.
                var diff = az - heading
                if (diff > 180) diff -= 360
                if (diff < -180) diff += 360
                heading = (heading + diff * 0.15f + 360) % 360
            }
            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {
                lowAccuracy = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
            }
        }
        if (sensor == null) hasSensor = false
        else sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }

    // Angle of the Qibla arrow relative to the top of the phone.
    val target = (qibla - heading + 360) % 360
    val animated by animateFloatAsState(target, label = "qibla")
    val aligned = hasSensor && (target < 4 || target > 356)
    val gold = IfGold
    val ring = MaterialTheme.colorScheme.primary

    Column(Modifier.fillMaxSize()) {
        HeroHeader("Qibla", "${Prefs.placeName} · ${distanceKm} km till Mecka")
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                if (!hasSensor) "Telefonen saknar kompass"
                else if (aligned) "Du är vänd mot Qibla ✓" else "Vrid telefonen tills pilen pekar uppåt",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (aligned) gold else MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Canvas(Modifier.size(300.dp)) {
                val c = Offset(size.width / 2, size.height / 2)
                val r = size.minDimension / 2 - 8.dp.toPx()
                drawCircle(ring.copy(alpha = 0.12f), r, c)
                drawCircle(if (aligned) gold else ring, r, c, style = Stroke(4.dp.toPx()))
                // Compass ticks rotate with the world (north follows heading).
                rotate(-heading, c) {
                    for (i in 0 until 72) {
                        val a = Math.toRadians(i * 5.0)
                        val len = if (i % 18 == 0) 18.dp.toPx() else if (i % 2 == 0) 10.dp.toPx() else 5.dp.toPx()
                        val o = Offset(c.x + (r * sin(a)).toFloat(), c.y - (r * cos(a)).toFloat())
                        val inner = Offset(c.x + ((r - len) * sin(a)).toFloat(), c.y - ((r - len) * cos(a)).toFloat())
                        drawLine(if (i == 0) Color.Red else ring, o, inner, strokeWidth = if (i % 18 == 0) 3.dp.toPx() else 1.5f.dp.toPx())
                    }
                }
                rotate(animated, c) {
                    val arrow = Path().apply {
                        moveTo(c.x, c.y - r + 28.dp.toPx())
                        lineTo(c.x - 18.dp.toPx(), c.y + 10.dp.toPx())
                        lineTo(c.x, c.y)
                        lineTo(c.x + 18.dp.toPx(), c.y + 10.dp.toPx())
                        close()
                    }
                    drawPath(arrow, gold)
                    // Kaaba marker
                    val k = 16.dp.toPx()
                    drawRect(Color.Black, Offset(c.x - k / 2, c.y - r + 6.dp.toPx()), androidx.compose.ui.geometry.Size(k, k))
                    drawRect(gold, Offset(c.x - k / 2, c.y - r + 9.dp.toPx()), androidx.compose.ui.geometry.Size(k, 2.dp.toPx()))
                }
                drawCircle(gold, 6.dp.toPx(), c)
            }
            Text("Qibla: ${qibla.roundToInt()}° från norr", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Riktning just nu: ${heading.roundToInt()}°", style = MaterialTheme.typography.bodyMedium)
            if (lowAccuracy) {
                Text(
                    "Kompassen behöver kalibreras: rör telefonen i en åtta några gånger.",
                    color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center,
                )
            }
            Text(
                "Håll telefonen plant och undan från metall och magneter. Platsen ändras under Bön → Plats.",
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
            )
        }
    }
}
