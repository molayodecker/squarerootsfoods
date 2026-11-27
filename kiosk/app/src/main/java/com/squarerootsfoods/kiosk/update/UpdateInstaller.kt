package com.squarerootsfoods.kiosk.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.FileProvider
import com.squarerootsfoods.kiosk.BuildConfig
import com.squarerootsfoods.kiosk.data.AppRelease
import com.squarerootsfoods.kiosk.data.MachineApi
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object UpdateInstaller {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    @Volatile
    var running: Boolean = false
        private set

    fun isNewer(release: AppRelease, currentName: String = BuildConfig.VERSION_NAME, currentCode: Int = BuildConfig.VERSION_CODE): Boolean {
        if (release.url.isBlank() || release.sha256.isBlank()) return false
        if (release.versionCode <= currentCode) return false
        if (release.requiredVersion.isNotBlank() && release.requiredVersion != currentName) return false
        return true
    }

    suspend fun install(context: Context, api: MachineApi, release: AppRelease, jobNumber: String) {
        if (running) {
            report(api, jobNumber, release, "4", 0, "Update already running")
            return
        }
        if (!isNewer(release)) {
            report(api, jobNumber, release, "4", 0, "This build is not the required version, or the release is not newer")
            return
        }
        running = true
        try {
            report(api, jobNumber, release, "2", 0, "Downloading")
            val apk = download(context, release.url) { progress ->
                report(api, jobNumber, release, "2", progress, "Downloading")
            }
            val actual = sha256(apk)
            if (!actual.equals(release.sha256, ignoreCase = true)) {
                apk.delete()
                report(api, jobNumber, release, "4", 100, "SHA-256 did not match")
                return
            }
            report(api, jobNumber, release, "3", 100, "Downloaded")
            commitInstall(context, apk)
        } catch (error: Exception) {
            report(api, jobNumber, release, "4", 0, error.message ?: "Update failed")
        } finally {
            running = false
        }
    }

    private suspend fun download(context: Context, url: String, onProgress: suspend (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.getExternalFilesDir(null), "updates")
            dir.mkdirs()
            val apk = File(dir, "kiosk-update.apk")
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IllegalStateException("Download failed (${response.code})")
                val body = response.body ?: throw IllegalStateException("Empty download")
                val total = body.contentLength()
                body.byteStream().use { input ->
                    apk.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var readTotal = 0L
                        var lastPercent = -1
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            readTotal += count
                            if (total > 0) {
                                val percent = ((readTotal * 100) / total).toInt()
                                if (percent != lastPercent && percent % 5 == 0) {
                                    lastPercent = percent
                                    onProgress(percent)
                                }
                            }
                        }
                    }
                }
            }
            apk
        }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun commitInstall(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        if (Build.VERSION.SDK_INT >= 31) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("kiosk", 0, apk.length()).use { output ->
                apk.inputStream().use { input -> input.copyTo(output) }
                session.fsync(output)
            }
            val intent = Intent(context, InstallResultReceiver::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val pending = PendingIntent.getBroadcast(context, sessionId, intent, flags)
            session.commit(pending.intentSender)
        }
    }

    fun openSystemInstaller(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private suspend fun report(
        api: MachineApi,
        number: String,
        release: AppRelease,
        state: String,
        progress: Int,
        error: String,
    ) {
        try {
            api.reportUpgrade(number, BuildConfig.VERSION_NAME, release.versionName, state, progress, error)
        } catch (_: Exception) {
            // The cabinet should still install if the status call fails.
        }
    }
}
