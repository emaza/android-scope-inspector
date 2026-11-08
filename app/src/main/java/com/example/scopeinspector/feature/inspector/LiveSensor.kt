package com.example.scopeinspector.feature.inspector

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.scopeinspector.R
import java.util.Locale

/** Sensores que se leen en vivo sin permiso, con su unidad. */
enum class LiveSensor(val type: Int, @StringRes val labelRes: Int, val unit: String, val axes: Int) {
    Accelerometer(Sensor.TYPE_ACCELEROMETER, R.string.sensor_accelerometer, "m/s²", 3),
    Gyroscope(Sensor.TYPE_GYROSCOPE, R.string.sensor_gyroscope, "rad/s", 3),
    Magnetometer(Sensor.TYPE_MAGNETIC_FIELD, R.string.sensor_magnetometer, "µT", 3),
    Light(Sensor.TYPE_LIGHT, R.string.sensor_light, "lx", 1),
    Proximity(Sensor.TYPE_PROXIMITY, R.string.sensor_proximity, "cm", 1),
    Pressure(Sensor.TYPE_PRESSURE, R.string.sensor_pressure, "hPa", 1),
    ;

    /** Texto de una lectura: "x 0,12 · y 9,81 · z 0,03 m/s²" o "312 lx". */
    fun format(values: FloatArray, locale: Locale = Locale.getDefault()): String {
        val shown = values.take(axes)
        val text = if (axes == 1) {
            shown.joinToString { String.format(locale, "%.1f", it) }
        } else {
            shown.mapIndexed { i, v -> String.format(locale, "%s %.2f", AXES[i], v) }.joinToString(" · ")
        }
        return "$text $unit"
    }

    private companion object {
        val AXES = listOf("x", "y", "z")
    }
}

/**
 * Escucha los sensores presentes solo mientras la pantalla está en primer plano (RESUMED): al pausar
 * la app se dejan de escuchar, aunque la composición siga viva, y se vuelven a registrar al volver.
 * Devuelve la última lectura de cada uno. Ritmo `SENSOR_DELAY_UI`: por encima de 200 Hz Android exige `HIGH_SAMPLING_RATE_SENSORS`.
 */
@Composable
fun rememberLiveSensorReadings(): Map<LiveSensor, FloatArray> {
    val context = LocalContext.current
    val readings = remember { mutableStateMapOf<LiveSensor, FloatArray>() }
    LifecycleResumeEffect(context) {
        val manager = context.getSystemService<SensorManager>()
        val listeners = LiveSensor.entries.mapNotNull { live ->
            val sensor = manager?.getDefaultSensor(live.type) ?: return@mapNotNull null
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    readings[live] = event.values.copyOf()
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            listener
        }
        onPauseOrDispose { listeners.forEach { manager?.unregisterListener(it) } }
    }
    return readings
}

/** Qué sensores de la lista en vivo tiene el dispositivo (aunque aún no hayan dado lectura). */
fun availableLiveSensors(manager: SensorManager?): List<LiveSensor> =
    LiveSensor.entries.filter { manager?.getDefaultSensor(it.type) != null }
