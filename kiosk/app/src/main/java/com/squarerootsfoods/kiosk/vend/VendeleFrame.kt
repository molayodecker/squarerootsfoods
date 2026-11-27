package com.squarerootsfoods.kiosk.vend

import java.util.concurrent.atomic.AtomicInteger

/**
 * Cabinet frame used by the Vendele / WX board driver in the 3.1.80 app.
 *
 * Layout, as built by VendeleCommandUtil.command():
 * 57 58 | address | 66 | id (2 bytes) | command | length (2 decimal digits) | data | checksum.
 * Checksum is the sum of the preceding bytes, mod 256.
 * Command 03 is CMD_SLOT_DRIVER_TRANSFOR (dispense a lane).
 */
object VendeleFrame {
    private val nextId = AtomicInteger(0)

    fun dispense(lane: Int): ByteArray {
        val id = nextId.getAndUpdate { current -> if (current >= 0xFFFF) 0 else current + 1 }
        val data = "%04x".format(lane.coerceAtLeast(0))
        return frame(command = "03", dataHex = data, id = id)
    }

    fun frame(command: String, dataHex: String, id: Int): ByteArray {
        require(command.length == 2) { "command must be one byte of hex" }
        require(dataHex.length % 2 == 0) { "data must be whole bytes" }
        val byteCount = dataHex.length / 2
        val lengthField = if (byteCount < 10) "0$byteCount" else byteCount.toString()
        val body = "5758" + "00" + "66" + "%04x".format(id and 0xFFFF) + command + lengthField + dataHex
        val sum = body.chunked(2).sumOf { it.toInt(16) }
        val checksum = "%02x".format(sum % 256)
        return hexToBytes(body + checksum)
    }

    fun hexToBytes(hex: String): ByteArray {
        val clean = hex.lowercase()
        return ByteArray(clean.length / 2) { index ->
            clean.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }
}
