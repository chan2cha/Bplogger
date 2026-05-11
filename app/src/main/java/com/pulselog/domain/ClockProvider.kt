package com.pulselog.domain

import java.time.LocalDate
import java.time.ZoneId

interface ClockProvider {
    fun today(): LocalDate
    fun nowEpochMs(): Long
    fun zoneId(): ZoneId
}

class SystemClockProvider : ClockProvider {
    override fun today(): LocalDate = LocalDate.now()

    override fun nowEpochMs(): Long = System.currentTimeMillis()

    override fun zoneId(): ZoneId = ZoneId.systemDefault()
}
