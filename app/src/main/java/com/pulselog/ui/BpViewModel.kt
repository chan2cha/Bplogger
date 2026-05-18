package com.pulselog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulselog.BuildConfig
import com.pulselog.data.GraphPoint
import com.pulselog.data.NotificationSettings
import com.pulselog.domain.ClockProvider
import com.pulselog.domain.ExportRange
import com.pulselog.domain.ExportSummary
import com.pulselog.domain.HealthRepository
import com.pulselog.domain.NoOpNotificationScheduler
import com.pulselog.domain.NotificationScheduler
import com.pulselog.domain.SystemClockProvider
import com.pulselog.domain.ValidationPolicy
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BpViewModel(
    private val repo: HealthRepository,
    clockProvider: ClockProvider = SystemClockProvider(),
    private val notificationScheduler: NotificationScheduler = NoOpNotificationScheduler()
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(clockProvider.today())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _currentMonth = MutableStateFlow(YearMonth.from(clockProvider.today()))
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    private val _graphRangeDays = MutableStateFlow(7)
    val graphRangeDays: StateFlow<Int> = _graphRangeDays

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    val selectedRecord = selectedDate
        .flatMapLatest { repo.observeRecord(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val monthStatuses = currentMonth
        .flatMapLatest { repo.observeMonthStatuses(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val graphPoints: StateFlow<List<GraphPoint>> = graphRangeDays
        .flatMapLatest { repo.observeGraphPoints(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notificationSettings = repo.observeNotificationSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationSettings())

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        _currentMonth.value = YearMonth.from(date)
    }

    fun previousMonth() {
        val newMonth = currentMonth.value.minusMonths(1)
        _currentMonth.value = newMonth
        _selectedDate.value = alignDateToMonth(_selectedDate.value, newMonth)
    }

    fun nextMonth() {
        val newMonth = currentMonth.value.plusMonths(1)
        _currentMonth.value = newMonth
        _selectedDate.value = alignDateToMonth(_selectedDate.value, newMonth)
    }

    fun setGraphRangeDays(days: Int) {
        _graphRangeDays.value = days
    }

    fun clearMessage() {
        _message.value = null
    }

    fun showNotificationPermissionRequired() {
        _message.value = "알림 권한을 허용한 뒤 다시 저장하세요."
    }

    fun showNotificationPermissionGranted() {
        _message.value = "알림 권한이 허용되었습니다. 설정을 다시 저장하세요."
    }

    fun saveMorning(systolicInput: String, diastolicInput: String) {
        val rawSystolic = systolicInput.toIntOrNull()
        val rawDiastolic = diastolicInput.toIntOrNull()
        if (rawSystolic == null || rawDiastolic == null) {
            _message.value = "아침 혈압 값을 올바르게 입력하세요."
            return
        }
        val systolic = ValidationPolicy.parsePressure(systolicInput)
        val diastolic = ValidationPolicy.parsePressure(diastolicInput)
        if (systolic == null || diastolic == null) {
            _message.value = "혈압은 50부터 200 사이여야 합니다."
            return
        }

        viewModelScope.launch {
            repo.saveMorning(selectedDate.value, systolic, diastolic)
            _message.value = "아침 혈압을 저장했습니다."
        }
    }

    fun saveEvening(systolicInput: String, diastolicInput: String) {
        val rawSystolic = systolicInput.toIntOrNull()
        val rawDiastolic = diastolicInput.toIntOrNull()
        if (rawSystolic == null || rawDiastolic == null) {
            _message.value = "저녁 혈압 값을 올바르게 입력하세요."
            return
        }
        val systolic = ValidationPolicy.parsePressure(systolicInput)
        val diastolic = ValidationPolicy.parsePressure(diastolicInput)
        if (systolic == null || diastolic == null) {
            _message.value = "혈압은 50부터 200 사이여야 합니다."
            return
        }

        viewModelScope.launch {
            repo.saveEvening(selectedDate.value, systolic, diastolic)
            _message.value = "저녁 혈압을 저장했습니다."
        }
    }

    fun saveWeight(weightInput: String) {
        val rawWeight = weightInput.toDoubleOrNull()
        if (rawWeight == null) {
            _message.value = "체중 값을 올바르게 입력하세요."
            return
        }
        val rounded = ValidationPolicy.parseWeightKg(weightInput)
        if (rounded == null) {
            _message.value = "체중은 0부터 100kg 사이여야 합니다."
            return
        }

        viewModelScope.launch {
            repo.saveWeight(selectedDate.value, rounded)
            _message.value = "체중을 저장했습니다."
        }
    }

    fun deleteMorning() {
        viewModelScope.launch {
            repo.deleteMorning(selectedDate.value)
            _message.value = "아침 혈압을 삭제했습니다."
        }
    }

    fun deleteEvening() {
        viewModelScope.launch {
            repo.deleteEvening(selectedDate.value)
            _message.value = "저녁 혈압을 삭제했습니다."
        }
    }

    fun deleteWeight() {
        viewModelScope.launch {
            repo.deleteWeight(selectedDate.value)
            _message.value = "체중을 삭제했습니다."
        }
    }

    fun saveNotificationSettings(
        morningEnabled: Boolean,
        morningTime: String,
        eveningEnabled: Boolean,
        eveningTime: String
    ) {
        if (!ValidationPolicy.isValidTime(morningTime) || !ValidationPolicy.isValidTime(eveningTime)) {
            _message.value = "알림 시간은 HH:mm 형식이어야 합니다."
            return
        }

        viewModelScope.launch {
            val settings = NotificationSettings(
                morningEnabled = morningEnabled,
                morningTime = morningTime,
                eveningEnabled = eveningEnabled,
                eveningTime = eveningTime,
                repeatEnabled = false,
                repeatCount = 0
            )
            repo.saveNotificationSettings(settings)
            notificationScheduler.apply(settings)
            _message.value = "알림 설정을 저장했습니다."
        }
    }

    fun seedGraphDemoData() {
        if (!BuildConfig.DEBUG) return

        viewModelScope.launch {
            repo.seedGraphDemoData()
            _message.value = "그래프 테스트 데이터를 추가했습니다."
        }
    }

    fun exportCsv(
        range: ExportRange,
        onReady: (fileName: String, csv: String) -> Unit
    ) {
        viewModelScope.launch {
            val (fileName, csv) = repo.exportCsv(range)
            if (csv.lineSequence().count() <= 1) {
                _message.value = "내보낼 기록이 없습니다."
                return@launch
            }
            onReady(fileName, csv)
            _message.value = "${range.label} 기록 CSV를 준비했습니다."
        }
    }

    fun exportPdfSummary(
        range: ExportRange,
        onReady: (fileName: String, summary: ExportSummary) -> Unit
    ) {
        viewModelScope.launch {
            val (fileName, summary) = repo.exportSummary(range)
            if (summary.totalRecordDays == 0) {
                _message.value = "내보낼 기록이 없습니다."
                return@launch
            }
            onReady(fileName, summary)
            _message.value = "${range.label} 기록 PDF 요약본을 준비했습니다."
        }
    }

    private fun alignDateToMonth(date: LocalDate, month: YearMonth): LocalDate {
        return month.atDay(date.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
    }
}
