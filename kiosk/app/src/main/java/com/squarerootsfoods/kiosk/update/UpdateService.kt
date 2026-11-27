package com.squarerootsfoods.kiosk.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.squarerootsfoods.kiosk.admin.KioskDeviceAdmin
import com.squarerootsfoods.kiosk.data.AppRelease
import com.squarerootsfoods.kiosk.data.MachineApi
import com.squarerootsfoods.kiosk.data.MachinePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject

class UpdateService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mqtt: MqttClient? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        startInForeground()
        scope.launch { pollLoop() }
        scope.launch { mqttLoop() }
    }

    override fun onDestroy() {
        mqtt?.let { client ->
            try {
                if (client.isConnected) client.disconnect()
            } catch (_: Exception) {
            }
        }
        super.onDestroy()
    }

    private fun startInForeground() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, "Machine updates", NotificationManager.IMPORTANCE_MIN)
            manager.createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL)
                .setContentTitle("Vending machine")
                .setContentText("Watching for payments and updates")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Vending machine")
                .setContentText("Watching for payments and updates")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .build()
        }
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private suspend fun pollLoop() {
        delay(20_000)
        while (true) {
            try {
                val prefs = MachinePrefs(this)
                if (prefs.setupComplete) {
                    val api = MachineApi(prefs)
                    val release = api.latestRelease()
                    if (release != null && UpdateInstaller.isNewer(release)) {
                        UpdateInstaller.install(this, api, release, "poll")
                    }
                }
            } catch (error: Exception) {
                Log.w(TAG, "version poll failed: ${error.message}")
            }
            delay(15 * 60 * 1000L)
        }
    }

    private suspend fun mqttLoop() {
        while (true) {
            val prefs = MachinePrefs(this)
            val host = prefs.mqttHost
            if (!prefs.setupComplete || host.isBlank()) {
                delay(30_000)
                continue
            }
            try {
                connectMqtt(prefs)
                while (mqtt?.isConnected == true) delay(5_000)
            } catch (error: Exception) {
                Log.w(TAG, "mqtt failed: ${error.message}")
            }
            delay(15_000)
        }
    }

    private fun connectMqtt(prefs: MachinePrefs) {
        val uri = "tcp://${prefs.mqttHost}:${prefs.mqttPort}"
        val client = MqttClient(uri, "kiosk-${prefs.machineNo}", MemoryPersistence())
        val options = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = true
            if (prefs.mqttUser.isNotBlank()) {
                userName = prefs.mqttUser
                password = prefs.mqttPassword.toCharArray()
            }
        }
        client.setCallback(object : org.eclipse.paho.client.mqttv3.MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Log.w(TAG, "mqtt lost: ${cause?.message}")
            }

            override fun messageArrived(topic: String?, message: org.eclipse.paho.client.mqttv3.MqttMessage?) {
                val text = message?.payload?.toString(Charsets.UTF_8) ?: return
                scope.launch { handleCommand(text) }
            }

            override fun deliveryComplete(token: org.eclipse.paho.client.mqttv3.IMqttDeliveryToken?) = Unit
        })
        client.connect(options)
        client.subscribe("machines/${prefs.machineNo}", 1)
        mqtt = client
        Log.i(TAG, "mqtt subscribed machines/${prefs.machineNo}")
    }

    private suspend fun handleCommand(text: String) {
        val json = try {
            JSONObject(text)
        } catch (_: Exception) {
            return
        }
        when (json.optString("method")) {
            "refresh_goods" -> sendBroadcast(Intent(ACTION_REFRESH).setPackage(packageName))
            "reboot" -> rebootIfOwner()
            "app_upgrade" -> {
                val release = AppRelease(
                    versionName = json.optString("version"),
                    versionCode = json.optInt("version_code"),
                    url = json.optString("url"),
                    sha256 = json.optString("sha256"),
                    requiredVersion = json.optString("required_version", json.optString("require_version")),
                    notes = json.optString("notes"),
                )
                val api = MachineApi(MachinePrefs(this))
                UpdateInstaller.install(this, api, release, json.optString("number", "mqtt"))
            }
        }
    }

    private fun rebootIfOwner() {
        if (Build.VERSION.SDK_INT < 24) return
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (!dpm.isDeviceOwnerApp(packageName)) return
        dpm.reboot(ComponentName(this, KioskDeviceAdmin::class.java))
    }

    companion object {
        const val ACTION_REFRESH = "com.squarerootsfoods.kiosk.REFRESH_GOODS"
        private const val CHANNEL = "kiosk_updates"
        private const val NOTIFICATION_ID = 42
        private const val TAG = "UpdateService"

        fun start(context: android.content.Context) {
            val intent = Intent(context, UpdateService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
