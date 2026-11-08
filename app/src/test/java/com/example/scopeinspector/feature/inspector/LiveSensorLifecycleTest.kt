package com.example.scopeinspector.feature.inspector

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSensor

/** Los sensores solo se escuchan con la pantalla en primer plano: en segundo plano gastarían batería. */
@RunWith(AndroidJUnit4::class)
class LiveSensorLifecycleTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val sensorManager get() = composeRule.activity.getSystemService<SensorManager>()!!
    private val listeners get() = shadowOf(sensorManager).listeners.size

    @Before
    fun addSensors() {
        shadowOf(sensorManager).addSensor(ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER))
        shadowOf(sensorManager).addSensor(ShadowSensor.newInstance(Sensor.TYPE_LIGHT))
    }

    @Test
    fun listensWhileResumedAndStopsWhenPausedThenResumesAgain() {
        composeRule.setContent { rememberLiveSensorReadings() }
        composeRule.waitForIdle()
        assertEquals(2, listeners)

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.waitForIdle()
        assertEquals("con la app en pausa no debe quedar ningún sensor escuchando", 0, listeners)

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
        assertEquals(2, listeners)
    }
}
