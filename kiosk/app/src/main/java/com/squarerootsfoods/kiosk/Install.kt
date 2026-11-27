package com.squarerootsfoods.kiosk

import com.squarerootsfoods.kiosk.vend.PortChoice

data class CoilCheck(
    val row: Int,
    val coil: Int,
    val lane: Int,
    val result: String = "untested",
    val detail: String = "",
)

data class InstallUi(
    val rows: Int = 6,
    val coils: Int = 10,
    val board: String = "vendele",
    val serial: String = "",
    val baud: Int = 9600,
    val simulate: Boolean = true,
    val ports: List<PortChoice> = emptyList(),
    val checks: List<CoilCheck> = emptyList(),
    val index: Int = 0,
    val busy: Boolean = false,
    val message: String? = null,
    val phase: String = "setup",
    val paymentLane: Int = 0,
    val paymentReference: String = "",
    val paymentStatus: String = "",
)
