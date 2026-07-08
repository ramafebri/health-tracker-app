package com.rama.health.domain.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalDateFormatsTest {

    @Test
    fun storageRoundTrip_preservesDateValue() {
        val original = LocalDate.of(2026, 7, 8)
        val encoded = LocalDateFormats.toStorageString(original)
        val decoded = LocalDateFormats.parseStorageString(encoded)

        assertEquals(original, decoded)
    }
}
