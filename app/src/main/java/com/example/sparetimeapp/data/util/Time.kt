package com.example.sparetimeapp.util

import java.time.*
import java.time.format.DateTimeFormatter

fun todayKey(clock: Clock = Clock.systemDefaultZone()): String =
    LocalDate.now(clock).format(DateTimeFormatter.ISO_DATE) // z.B. 2025-08-29

fun midnightMillis(clock: Clock = Clock.systemDefaultZone()): Long {
    val tomorrowStart = LocalDate.now(clock).plusDays(1)
        .atStartOfDay(ZoneId.systemDefault())
    return tomorrowStart.toInstant().toEpochMilli()
}

