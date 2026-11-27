package com.squarerootsfoods.kiosk.vend

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.delay

data class VendResult(
    val ok: Boolean,
    val confirmed: Boolean,
    val detail: String,
)

class VendDriver {
    suspend fun dispense(lane: Int, devicePath: String, baudRate: Int, simulate: Boolean): VendResult {
        if (simulate || devicePath.isBlank()) {
            delay(1200)
            return VendResult(ok = true, confirmed = true, detail = "Simulated dispense of lane $lane")
        }
        val device = File(devicePath)
        if (!device.exists()) {
            return VendResult(false, false, "Serial device $devicePath is not on this board")
        }
        return try {
            configurePort(devicePath, baudRate)
            val frame = VendeleFrame.dispense(lane)
            FileOutputStream(device).use { output ->
                output.write(frame)
                output.flush()
            }
            val reply = readReply(device)
            if (reply == null) {
                VendResult(
                    ok = true,
                    confirmed = false,
                    detail = "Dispense command sent to lane $lane. The board did not answer.",
                )
            } else {
                VendResult(ok = true, confirmed = true, detail = "Board replied ${reply.size} bytes for lane $lane")
            }
        } catch (error: Exception) {
            VendResult(ok = false, confirmed = false, detail = error.message ?: "Serial write failed")
        }
    }

    private fun configurePort(path: String, baudRate: Int) {
        try {
            Runtime.getRuntime().exec(arrayOf("sh", "-c", "stty -F $path $baudRate raw -echo")).waitFor()
        } catch (_: Exception) {
            // Vendor images that already set the baud rate can still accept the frame.
        }
    }

    private fun readReply(device: File): ByteArray? {
        return try {
            FileInputStream(device).use { input ->
                input.available().takeIf { it > 0 }?.let { available ->
                    val buffer = ByteArray(available)
                    val read = input.read(buffer)
                    if (read > 0) buffer.copyOf(read) else null
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
