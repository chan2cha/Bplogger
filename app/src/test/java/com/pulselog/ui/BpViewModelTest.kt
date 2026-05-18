package com.pulselog.ui

import com.pulselog.data.NotificationSettings
import com.pulselog.domain.ExportRange
import com.pulselog.test.FakeClockProvider
import com.pulselog.test.FakeHealthRepository
import com.pulselog.test.FakeNotificationScheduler
import com.pulselog.test.SaveEveningCall
import com.pulselog.test.SaveMorningCall
import com.pulselog.test.SaveWeightCall
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun saveEvening_validInputDelegatesSelectedDateToRepository() = runTest {
        val repo = FakeHealthRepository()
        val viewModel = BpViewModel(
            repo = repo,
            clockProvider = FakeClockProvider(date = LocalDate.parse("2026-05-12"))
        )
        viewModel.setSelectedDate(LocalDate.parse("2026-05-11"))

        viewModel.saveEvening("124", "82")
        advanceUntilIdle()

        assertEquals(
            SaveEveningCall(LocalDate.parse("2026-05-11"), systolic = 124, diastolic = 82),
            repo.saveEveningCalls.single()
        )
    }

    @Test
    fun saveWeight_validInputRoundsAndDelegatesSelectedDateToRepository() = runTest {
        val repo = FakeHealthRepository()
        val viewModel = BpViewModel(
            repo = repo,
            clockProvider = FakeClockProvider(date = LocalDate.parse("2026-05-12"))
        )
        viewModel.setSelectedDate(LocalDate.parse("2026-05-09"))

        viewModel.saveWeight("65.555")
        advanceUntilIdle()

        assertEquals(
            SaveWeightCall(LocalDate.parse("2026-05-09"), weightKg = 65.56),
            repo.saveWeightCalls.single()
        )
    }

    @Test
    fun saveWeight_invalidInputDoesNotCallRepository() = runTest {
        val repo = FakeHealthRepository()
        val viewModel = BpViewModel(repo = repo)

        viewModel.saveWeight("abc")
        advanceUntilIdle()

        assertEquals(emptyList<SaveWeightCall>(), repo.saveWeightCalls)
    }

    @Test
    fun deleteMorningAndEveningDelegateSelectedDateToRepository() = runTest {
        val repo = FakeHealthRepository()
        val viewModel = BpViewModel(
            repo = repo,
            clockProvider = FakeClockProvider(date = LocalDate.parse("2026-05-12"))
        )
        viewModel.setSelectedDate(LocalDate.parse("2026-05-08"))

        viewModel.deleteMorning()
        viewModel.deleteEvening()
        advanceUntilIdle()

        assertEquals(listOf(LocalDate.parse("2026-05-08")), repo.deleteMorningCalls)
        assertEquals(listOf(LocalDate.parse("2026-05-08")), repo.deleteEveningCalls)
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

    @Test
    fun saveNotificationSettings_validInputSavesAndAppliesScheduler() = runTest {
        val repo = FakeHealthRepository()
        val scheduler = FakeNotificationScheduler()
        val viewModel = BpViewModel(
            repo = repo,
            notificationScheduler = scheduler
        )

        viewModel.saveNotificationSettings(
            morningEnabled = true,
            morningTime = "08:00",
            eveningEnabled = true,
            eveningTime = "20:30"
        )
        advanceUntilIdle()

        val savedSettings = repo.savedNotificationSettings.single()
        val expected = savedSettings.copy(
            morningEnabled = true,
            morningTime = "08:00",
            eveningEnabled = true,
            eveningTime = "20:30",
            repeatEnabled = false,
            repeatCount = 0
        )
        assertEquals(expected, savedSettings)
        assertEquals(expected, scheduler.appliedSettings.single())
        assertEquals("알림 설정을 저장했습니다.", viewModel.message.value)
    }

    @Test
    fun saveNotificationSettings_invalidTimeDoesNotSaveOrApplyScheduler() = runTest {
        val repo = FakeHealthRepository()
        val scheduler = FakeNotificationScheduler()
        val viewModel = BpViewModel(
            repo = repo,
            notificationScheduler = scheduler
        )

        viewModel.saveNotificationSettings(
            morningEnabled = true,
            morningTime = "8am",
            eveningEnabled = true,
            eveningTime = "20:30"
        )
        advanceUntilIdle()

        assertEquals(emptyList<NotificationSettings>(), repo.savedNotificationSettings)
        assertEquals(emptyList<NotificationSettings>(), scheduler.appliedSettings)
        assertEquals("알림 시간은 HH:mm 형식이어야 합니다.", viewModel.message.value)
    }

    @Test
    fun exportCsv_emptyResultDoesNotInvokeReadyCallback() = runTest {
        val repo = FakeHealthRepository().apply {
            exportCsvResult = "empty.csv" to "date"
        }
        val viewModel = BpViewModel(repo = repo)
        var ready: Pair<String, String>? = null

        viewModel.exportCsv(ExportRange.RECENT_30_DAYS) { fileName, csv ->
            ready = fileName to csv
        }
        advanceUntilIdle()

        assertEquals(listOf(ExportRange.RECENT_30_DAYS), repo.exportCsvCalls)
        assertNull(ready)
    }

    @Test
    fun exportCsv_nonEmptyResultInvokesReadyCallback() = runTest {
        val repo = FakeHealthRepository().apply {
            exportCsvResult = "records.csv" to "date\n2026-05-12\n"
        }
        val viewModel = BpViewModel(repo = repo)
        var ready: Pair<String, String>? = null

        viewModel.exportCsv(ExportRange.ALL) { fileName, csv ->
            ready = fileName to csv
        }
        advanceUntilIdle()

        assertEquals(listOf(ExportRange.ALL), repo.exportCsvCalls)
        assertEquals("records.csv" to "date\n2026-05-12\n", ready)
    }

    @Test
    fun exportPdfSummary_emptySummaryDoesNotInvokeReadyCallback() = runTest {
        val repo = FakeHealthRepository().apply {
            exportSummaryResult = "empty.pdf" to FakeHealthRepository.emptyExportSummary()
        }
        val viewModel = BpViewModel(repo = repo)
        var readyFileName: String? = null

        viewModel.exportPdfSummary(ExportRange.RECENT_30_DAYS) { fileName, _ ->
            readyFileName = fileName
        }
        advanceUntilIdle()

        assertEquals(listOf(ExportRange.RECENT_30_DAYS), repo.exportSummaryCalls)
        assertNull(readyFileName)
    }

    @Test
    fun exportPdfSummary_nonEmptySummaryInvokesReadyCallback() = runTest {
        val summary = FakeHealthRepository.emptyExportSummary(ExportRange.ALL).copy(totalRecordDays = 1)
        val repo = FakeHealthRepository().apply {
            exportSummaryResult = "summary.pdf" to summary
        }
        val viewModel = BpViewModel(repo = repo)
        var readyFileName: String? = null
        var readyTotalRecordDays: Int? = null

        viewModel.exportPdfSummary(ExportRange.ALL) { fileName, exportSummary ->
            readyFileName = fileName
            readyTotalRecordDays = exportSummary.totalRecordDays
        }
        advanceUntilIdle()

        assertEquals(listOf(ExportRange.ALL), repo.exportSummaryCalls)
        assertEquals("summary.pdf", readyFileName)
        assertEquals(1, readyTotalRecordDays)
    }
}
