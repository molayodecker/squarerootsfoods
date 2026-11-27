package com.squarerootsfoods.kiosk

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "install_visits")
data class InstallVisit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val machineNo: String,
    val rows: Int,
    val coils: Int,
    val board: String,
    val serial: String,
    val baud: Int,
    val simulate: Boolean,
    val phase: String,
    val coilIndex: Int,
    val paymentLane: Int,
    val paymentReference: String,
    val paymentStatus: String,
    val status: String,
    val updatedAt: Long,
)

@Entity(
    tableName = "lane_marks",
    foreignKeys = [
        ForeignKey(
            entity = InstallVisit::class,
            parentColumns = ["id"],
            childColumns = ["visitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("visitId")],
)
data class LaneMark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val visitId: Long,
    val lane: Int,
    val row: Int,
    val coil: Int,
    val result: String,
    val detail: String,
)

@Entity(tableName = "install_events", indices = [Index("recordedAt")])
data class InstallEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val machineNo: String,
    val visitId: Long,
    val kind: String,
    val lane: Int,
    val row: Int,
    val coil: Int,
    val result: String,
    val detail: String,
    val serial: String,
    val baud: Int,
    val paymentReference: String,
    val paymentStatus: String,
    val recordedAt: Long,
)

data class InstallNote(
    val kind: String,
    val lane: Int = 0,
    val row: Int = 0,
    val coil: Int = 0,
    val result: String = "",
    val detail: String = "",
)
