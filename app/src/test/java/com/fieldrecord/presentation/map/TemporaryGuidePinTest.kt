package com.fieldrecord.presentation.map

import org.junit.Assert.assertEquals
import org.junit.Test

class TemporaryGuidePinTest {
    @Test
    fun createsGoogleNavigationUriFromPinnedCoordinates() {
        assertEquals(
            "google.navigation:q=-6.1234,106.9876",
            googleNavigationUri(-6.1234, 106.9876)
        )
    }
}
