package com.fieldrecord.presentation.map

import org.junit.Assert.assertEquals
import org.junit.Test

class TemporaryGuidePinTest {
    @Test
    fun firstTapSelectsPinAndSecondTapRequestsDeletion() {
        assertEquals(TemporaryGuidePinTapAction.SELECT, temporaryGuidePinTapAction(null, 42L))
        assertEquals(TemporaryGuidePinTapAction.SELECT, temporaryGuidePinTapAction(7L, 42L))
        assertEquals(TemporaryGuidePinTapAction.REQUEST_DELETE, temporaryGuidePinTapAction(42L, 42L))
    }
}
