package com.pulselog.test

import com.pulselog.domain.ClockProvider
import java.time.LocalDate
import java.time.ZoneId

class FakeClockProvider(
    private val date: LocalDate = LocalDate.parse("2026-05-12"),
    private val epochMs: Long = 1L,
    private val zone: ZoneId = ZoneId.of("Asia/Seoul")
) : ClockProvider {
    override fun today(): LocalDate = date
    override fun nowEpochMs(): Long = epochMs
    override fun zoneId(): ZoneId = zone
}
