package com.rama.health.domain.util

import java.time.LocalDate

object LocalDateFormats {
    fun toStorageString(date: LocalDate): String = date.toString()

    fun parseStorageString(value: String): LocalDate = LocalDate.parse(value)
}
