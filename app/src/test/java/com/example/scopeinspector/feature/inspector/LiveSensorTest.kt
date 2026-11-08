package com.example.scopeinspector.feature.inspector

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveSensorTest {

    private val spanish: Locale = Locale.forLanguageTag("es-ES")

    @Test
    fun threeAxisSensorsShowXYZWithTheirUnit() {
        assertEquals(
            "x 0,12 · y 9,81 · z -0,03 m/s²",
            LiveSensor.Accelerometer.format(floatArrayOf(0.12f, 9.81f, -0.03f, 7f), spanish),
        )
    }

    @Test
    fun singleValueSensorsIgnoreExtraValues() {
        assertEquals("312,4 lx", LiveSensor.Light.format(floatArrayOf(312.4f, 0f, 0f), spanish))
    }

    @Test
    fun sensorTypesAndLabelsAreUnique() {
        assertEquals(LiveSensor.entries.size, LiveSensor.entries.map { it.type }.toSet().size)
        assertEquals(LiveSensor.entries.size, LiveSensor.entries.map { it.labelRes }.toSet().size)
    }
}
