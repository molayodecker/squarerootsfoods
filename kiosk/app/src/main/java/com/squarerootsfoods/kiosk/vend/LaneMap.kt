package com.squarerootsfoods.kiosk.vend

data class LaneAddress(val row: Int, val coil: Int, val lane: Int)

object LaneMap {
    fun addresses(rows: Int, coils: Int): List<LaneAddress> {
        if (rows <= 0 || coils <= 0) return emptyList()
        return (1..rows).flatMap { row ->
            (1..coils).map { coil ->
                LaneAddress(row = row, coil = coil, lane = (row - 1) * coils + coil)
            }
        }
    }
}
