package com.squarerootsfoods.kiosk.vend

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class VendeleFrameTest {
    @Test
    fun firstDispenseFrameMatchesChecksum() {
        val frame = VendeleFrame.frame(command = "03", dataHex = "0001", id = 0)
        val expected = VendeleFrame.hexToBytes("575800660000030200011b")
        assertArrayEquals(expected, frame)
    }
}
