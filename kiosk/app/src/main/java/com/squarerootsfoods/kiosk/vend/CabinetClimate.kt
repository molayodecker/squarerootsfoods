package com.squarerootsfoods.kiosk.vend

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

data class ClimateSample(
    val temperatureC: Float?,
    val humidityPercent: Float?,
)

class CabinetClimate(context: Context) {
    private val manager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    suspend fun read(): ClimateSample {
        return ClimateSample(
            temperatureC = read(Sensor.TYPE_AMBIENT_TEMPERATURE),
            humidityPercent = read(Sensor.TYPE_RELATIVE_HUMIDITY),
        )
    }

    private suspend fun read(type: Int): Float? {
        val sensors = manager ?: return null
        val sensor = sensors.getDefaultSensor(type) ?: return null
        return withTimeoutOrNull(1_500) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        sensors.unregisterListener(this)
                        val value = event.values.firstOrNull()
                        if (continuation.isActive) continuation.resume(value)
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                val registered = sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                if (!registered) {
                    if (continuation.isActive) continuation.resume(null)
                    return@suspendCancellableCoroutine
                }
                continuation.invokeOnCancellation { sensors.unregisterListener(listener) }
            }
        }
    }
}
