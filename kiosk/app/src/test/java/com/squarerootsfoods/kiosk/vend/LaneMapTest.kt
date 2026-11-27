package com.squarerootsfoods.kiosk.vend

import org.junit.Assert.assertEquals
import org.junit.Test

class LaneMapTest {
    @Test
    fun countsAcrossARowThenDown() {
        val lanes = LaneMap.addresses(2, 3)
        assertEquals(listOf(1, 2, 3, 4, 5, 6), lanes.map { it.lane })
        assertEquals(LaneAddress(2, 1, 4), lanes[3])
    }

    @Test
    fun emptyGridHasNoLanes() {
        assertEquals(emptyList<LaneAddress>(), LaneMap.addresses(0, 8))
    }
}
