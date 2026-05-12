package com.pulselog.ui

import com.pulselog.data.CalendarDayStatus
import com.pulselog.data.DailyHealthRecord
import com.pulselog.data.DailyNote
import com.pulselog.data.GraphPoint
import com.pulselog.data.NotificationSettings
import com.pulselog.domain.ClockProvider
import com.pulselog.domain.ExportRange
import com.pulselog.domain.ExportSummary
import com.pulselog.domain.HealthRepository
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BpViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsFromClockProviderDate() {
        val viewModel = BpViewModel(
            repo = FakeHealthRepository(),
            clockProvider = FakeClockProvider(date = LocalDate.parse("2026-05-12"))
        )

        assertEquals(LocalDate.parse("2026-05-12"), viewModel.selectedDate.value)
        assertEquals(YearMonth.parse("2026-05"), viewModel.currentMonth.value)
    }

    @Test
    fun saveMorning_validInputDelegatesToRepositoryAndShowsMessage() = runTest {
        val repo = FakeHealthRepository()
        val viewModel = BpViewModel(
            repo = repo,
            clockProvider = FakeClockProvider(date = LocalDate.parse("2026-05-12"))
        )

        viewModel.saveMorning("121", "79")
        advanceUntilIdle()

        assertEquals(
            SaveMorningCall(LocalDate.parse("2026-05-12"), systolic = 121, diastolic = 79),
            repo.saveMorningCalls.single()
        )
        assertEquals("아침 혈압을 저장했습니다.", viewModel.message.value)
    }

    @Test
    fun saveMorning_invalidInputDoesNotCallRepository() = runTest {
        val repo = FakeHealthRepository()
        val viewModel = BpViewModel(repo = repo)

        viewModel.saveMorning("abc", "79")
        advanceUntilIdle()

        assertEquals(emptyList<SaveMorningCall>(), repo.saveMorningCalls)
        assertEquals("아침 혈압 값을 올바르게 입력하세요.", viewModel.message.value)
    }

    @Test
    fun deleteWeightDelegatesSelectedDateAndShowsMessage() = runTest {
        val repo = FakeHealthRepository()
        val viewModel = BpViewModel(
            repo = repo,
            clockProvider = FakeClockProvider(date = LocalDate.parse("2026-05-12"))
        )
        viewModel.setSelectedDate(LocalDate.parse("2026-05-10"))

        viewModel.deleteWeight()
        advanceUntilIdle()

        assertEquals(listOf(LocalDate.parse("2026-05-10")), repo.deleteWeightCalls)
        assertEquals("체중을 삭제했습니다.", viewModel.message.value)
    }

    private data class SaveMorningCall(
        val date: LocalDate,
        val systolic: Int,
        val diastolic: Int
    )

    private class FakeClockProvider(
        private val date: LocalDate = LocalDate.parse("2026-05-12")
    ) : ClockProvider {
        override fun today(): LocalDate = date
        override fun nowEpochMs(): Long = 1L
        override fun zoneId(): ZoneId = ZoneId.of("Asia/Seoul")
    }

    private class FakeHealthRepository : HealthRepository {
        val saveMorningCalls = mutableListOf<SaveMorningCall>()
        val deleteWeightCalls = mutableListOf<LocalDate>()

        override fun observeRecord(date: LocalDate): Flow<DailyHealthRecord?> = MutableStateFlow(null)

        override fun observeNote(date: LocalDate): Flow<DailyNote?> = MutableStateFlow(null)

        override fun observeMonthStatuses(month: YearMonth): Flow<List<CalendarDayStatus>> {
            return MutableStateFlow(emptyList())
        }

        override fun observeGraphPoints(days: Int): Flow<List<GraphPoint>> {
            return MutableStateFlow(emptyList())
        }

        override fun observeNotificationSettings(): Flow<NotificationSettings> {
            return MutableStateFlow(NotificationSettings())
        }

        override suspend fun exportCsv(range: ExportRange): Pair<String, String> = "test.csv" to ""

        override suspend fun exportSummary(range: ExportRange): Pair<String, ExportSummary> {
            return "test.pdf" to ExportSummary(
                rangeLabel = range.label,
                generatedDateIso = "2026-05-12",
                startDateIso = null,
                endDateIso = null,
                totalRecordDays = 0,
                morningCount = 0,
                eveningCount = 0,
                weightCount = 0,
                averageMorningSystolic = null,
                averageMorningDiastolic = null,
                averageEveningSystolic = null,
                averageEveningDiastolic = null,
                averageWeightKg = null,
                latestRows = emptyList(),
                trendPoints = emptyList()
            )
        }

        override suspend fun saveMorning(date: LocalDate, systolic: Int, diastolic: Int) {
            saveMorningCalls += SaveMorningCall(date, systolic, diastolic)
        }

        override suspend fun saveEvening(date: LocalDate, systolic: Int, diastolic: Int) = Unit

        override suspend fun saveWeight(date: LocalDate, weightKg: Double) = Unit

        override suspend fun deleteMorning(date: LocalDate) = Unit

        override suspend fun deleteEvening(date: LocalDate) = Unit

        override suspend fun deleteWeight(date: LocalDate) {
            deleteWeightCalls += date
        }

        override suspend fun saveNote(date: LocalDate, note: String) = Unit

        override suspend fun deleteNote(date: LocalDate) = Unit

        override suspend fun saveNotificationSettings(settings: NotificationSettings) = Unit

        override suspend fun seedGraphDemoData() = Unit
    }
}
