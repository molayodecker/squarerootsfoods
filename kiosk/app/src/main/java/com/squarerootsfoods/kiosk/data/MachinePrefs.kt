package com.squarerootsfoods.kiosk.data

import android.content.Context

class MachinePrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("machine", Context.MODE_PRIVATE)

    var setupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP, value).apply()

    var machineNo: String
        get() = prefs.getString(KEY_MACHINE, "VM-001").orEmpty()
        set(value) = prefs.edit().putString(KEY_MACHINE, value.trim()).apply()

    var serverBase: String
        get() = prefs.getString(KEY_SERVER, "http://10.0.2.2:8080").orEmpty().trimEnd('/')
        set(value) = prefs.edit().putString(KEY_SERVER, value.trim().trimEnd('/')).apply()

    var currency: String
        get() {
            val stored = prefs.getString(KEY_CURRENCY, null)
            val value = if (stored.isNullOrBlank() || stored.equals("NGN", ignoreCase = true)) "GHS" else stored
            if (stored != value) prefs.edit().putString(KEY_CURRENCY, value).apply()
            return value
        }
        set(value) = prefs.edit().putString(KEY_CURRENCY, value.trim().uppercase()).apply()

    var operatorPin: String
        get() = prefs.getString(KEY_PIN, "258046").orEmpty()
        set(value) = prefs.edit().putString(KEY_PIN, value).apply()

    var serialDevice: String
        get() = prefs.getString(KEY_SERIAL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_SERIAL, value.trim()).apply()

    var baudRate: Int
        get() = prefs.getInt(KEY_BAUD, 9600)
        set(value) = prefs.edit().putInt(KEY_BAUD, value).apply()

    var simulateVend: Boolean
        get() = prefs.getBoolean(KEY_SIMULATE, true)
        set(value) = prefs.edit().putBoolean(KEY_SIMULATE, value).apply()

    var installDraft: String
        get() = prefs.getString(KEY_INSTALL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_INSTALL, value).apply()

    var mqttHost: String
        get() = prefs.getString(KEY_MQTT_HOST, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_MQTT_HOST, value.trim()).apply()

    var mqttPort: Int
        get() = prefs.getInt(KEY_MQTT_PORT, 1883)
        set(value) = prefs.edit().putInt(KEY_MQTT_PORT, value).apply()

    var mqttUser: String
        get() = prefs.getString(KEY_MQTT_USER, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_MQTT_USER, value).apply()

    var mqttPassword: String
        get() = prefs.getString(KEY_MQTT_PASSWORD, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_MQTT_PASSWORD, value).apply()

    fun snapshot(): Snapshot = Snapshot(
        machineNo = machineNo,
        serverBase = serverBase,
        currency = currency,
        operatorPin = operatorPin,
        serialDevice = serialDevice,
        baudRate = baudRate,
        simulateVend = simulateVend,
        mqttHost = mqttHost,
        mqttPort = mqttPort,
        mqttUser = mqttUser,
        mqttPassword = mqttPassword,
    )

    data class Snapshot(
        val machineNo: String,
        val serverBase: String,
        val currency: String,
        val operatorPin: String,
        val serialDevice: String,
        val baudRate: Int,
        val simulateVend: Boolean,
        val mqttHost: String,
        val mqttPort: Int,
        val mqttUser: String,
        val mqttPassword: String,
    )

    private companion object {
        const val KEY_SETUP = "setup"
        const val KEY_MACHINE = "machine_no"
        const val KEY_SERVER = "server"
        const val KEY_CURRENCY = "currency"
        const val KEY_PIN = "pin"
        const val KEY_SERIAL = "serial"
        const val KEY_BAUD = "baud"
        const val KEY_SIMULATE = "simulate_vend"
        const val KEY_INSTALL = "install_draft"
        const val KEY_MQTT_HOST = "mqtt_host"
        const val KEY_MQTT_PORT = "mqtt_port"
        const val KEY_MQTT_USER = "mqtt_user"
        const val KEY_MQTT_PASSWORD = "mqtt_password"
    }
}
