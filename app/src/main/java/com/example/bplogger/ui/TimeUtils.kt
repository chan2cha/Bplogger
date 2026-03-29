package com.example.bplogger.ui

import java.time.LocalDate
import java.time.YearMonth

fun todayIso(): String = LocalDate.now().toString()

fun currentYearMonth(): YearMonth = YearMonth.now()
