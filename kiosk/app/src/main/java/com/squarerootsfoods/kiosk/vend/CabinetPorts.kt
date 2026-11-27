package com.squarerootsfoods.kiosk.vend

import java.io.File

data class PortChoice(val path: String, val present: Boolean)

object CabinetPorts {
    val candidates = listOf(
        "/dev/ttyS1",
        "/dev/ttyS2",
        "/dev/ttyS3",
        "/dev/ttyS4",
        "/dev/ttyUSB0",
        "/dev/ttyAMA0",
    )

    fun survey(exists: (String) -> Boolean = { path -> File(path).exists() }): List<PortChoice> {
        return candidates.map { path -> PortChoice(path, exists(path)) }
    }
}
