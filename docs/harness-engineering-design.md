# Pulse Log 하네스 엔지니어링 설계

## 1. 목적

이 문서는 현재 소스와 문서 기준으로 Pulse Log를 하네스 엔지니어링 기반으로 개발하기 위한 구조 설계다.

여기서 하네스 엔지니어링은 기능 구현보다 먼저 다음 조건을 갖추는 개발 방식을 뜻한다.

- 시간, 저장소, 알림, DB 같은 외부 의존성을 교체 가능하게 만든다.
- 핵심 정책을 단위 테스트로 재현 가능하게 만든다.
- UI 주요 흐름을 Compose 테스트로 자동 검증한다.
- 로컬 명령 하나로 빌드, 테스트, 정적 검증을 반복 실행한다.
- 새 기능은 하네스가 먼저 실패하고, 구현 후 통과하는 흐름으로 개발한다.

## 2. 현재 상태 진단

### 2.1 구현 상태

현재 앱은 다음 기능을 이미 갖고 있다.

- 날짜 단위 건강 기록 모델
- 아침 혈압, 저녁 혈압, 체중 저장
- 캘린더 상태 표시
- 최근 7일 / 30일 그래프
- 알림 설정 저장 UI
- 스낵바 피드백
- 그래프 선택 날짜를 캘린더 탭으로 연결

### 2.2 하네스 관점의 부족한 점

현재 구조는 기능 구현 중심이며, 자동 검증 중심 구조는 아니다.

- `AppGraph`가 `Room`, `BpRepository`, `BpViewModel`, `ClockProvider`, `NotificationScheduler` 조립을 담당한다.
- `HealthRepository`, `ClockProvider`, `NotificationScheduler` 경계가 생겼고 ViewModel 테스트에서 fake로 교체 가능하다.
- 주요 정책과 ViewModel 일부 흐름은 단위 테스트로 검증한다.
- Compose UI smoke test와 빠른 입력 저장/삭제, 그래프 선택 후 캘린더 이동 테스트가 있다.
- 로컬 체크 스크립트가 `test`, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest`를 실행한다.
- Room `exportSchema = true`와 v4 schema snapshot은 추가됐고 destructive migration fallback은 제거됐다.
- `AppDatabaseMigrations.ALL`이 production DB와 migration test에서 함께 쓰는 migration registry다.

## 3. 목표 아키텍처

### 3.1 계층 구조

권장 구조는 다음과 같다.

```text
app
  MainActivity
  AppGraph
    AppDatabase
    HealthRepository
    NotificationScheduler
    ClockProvider
  data
    RoomHealthDataSource
    BpDao
    AppDatabase
    migrations
  domain
    HealthRepository
    ClockProvider
    NotificationScheduler
    ValidationPolicy
    CalendarPolicy
    GraphPolicy
  ui
    BpViewModel
    screens
    components
test
  fakes
  harness
  unit
androidTest
  compose
  room
  notification
```

### 3.2 핵심 원칙

- ViewModel은 `HealthRepository`, `ClockProvider`, `NotificationScheduler` 같은 인터페이스만 의존한다.
- Repository는 DB 접근과 데이터 조합을 담당하되, 현재 시각 계산을 직접 하지 않는다.
- 정책 함수는 가능한 한 순수 함수로 분리한다.
- UI 테스트는 텍스트보다 `testTag`와 semantics를 우선 사용한다.
- 실제 Android 리소스가 필요한 검증만 `androidTest`로 보낸다.

## 4. 교체 가능한 의존성 설계

### 4.1 ClockProvider

현재 문제:

- `BpViewModel`은 `LocalDate.now()`를 직접 사용한다.
- `BpRepository`는 `System.currentTimeMillis()`를 직접 사용한다.
- `UiTokens.formatDateTime()`은 시스템 타임존을 직접 사용한다.

권장 설계:

```kotlin
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
```

테스트 하네스:

```kotlin
class FakeClockProvider(
    private var date: LocalDate,
    private var epochMs: Long,
    private val zone: ZoneId = ZoneId.of("Asia/Seoul")
) : ClockProvider {
    override fun today(): LocalDate = date
    override fun nowEpochMs(): Long = epochMs
    override fun zoneId(): ZoneId = zone
}
```

검증할 정책:

- 앱 첫 진입 날짜가 fake clock의 오늘과 일치한다.
- 저장 시 측정 시간이 fake epoch로 저장된다.
- 그래프 기간 계산이 fake today 기준으로 계산된다.

### 4.2 HealthRepository

현재 문제:

- `BpRepository`가 concrete class라 ViewModel 테스트에서 DB 없이 교체하기 어렵다.

권장 설계:

```kotlin
interface HealthRepository {
    fun observeRecord(date: LocalDate): Flow<DailyHealthRecord?>
    fun observeMonthStatuses(month: YearMonth): Flow<List<CalendarDayStatus>>
    fun observeGraphPoints(days: Int): Flow<List<GraphPoint>>
    fun observeNotificationSettings(): Flow<NotificationSettings>

    suspend fun saveMorning(date: LocalDate, systolic: Int, diastolic: Int)
    suspend fun saveEvening(date: LocalDate, systolic: Int, diastolic: Int)
    suspend fun saveWeight(date: LocalDate, weightKg: Double)
    suspend fun deleteMorning(date: LocalDate)
    suspend fun deleteEvening(date: LocalDate)
    suspend fun deleteWeight(date: LocalDate)
    suspend fun saveNotificationSettings(settings: NotificationSettings)
}
```

`BpRepository`는 `RoomHealthRepository` 또는 `DefaultHealthRepository`로 이름을 바꾸고 이 인터페이스를 구현한다.

테스트 하네스:

- `FakeHealthRepository`
- 내부 상태는 `MutableStateFlow<Map<LocalDate, DailyHealthRecord>>`, `MutableStateFlow<NotificationSettings>`로 유지한다.
- 저장/삭제 호출 내역을 기록해 ViewModel 동작을 검증한다.

### 4.3 NotificationScheduler

현재 문제:

- 알림 설정은 DB에 저장되지만 OS 알림 스케줄링 계층이 없다.

권장 설계:

```kotlin
interface NotificationScheduler {
    suspend fun apply(settings: NotificationSettings)
    suspend fun cancelAll()
}
```

구현체:

- 1차: `NoOpNotificationScheduler`
- 2차: `AlarmManagerNotificationScheduler` 또는 `WorkManagerNotificationScheduler`

테스트 하네스:

- `FakeNotificationScheduler`
- `lastAppliedSettings`
- `applyCallCount`
- `cancelAllCallCount`

검증할 정책:

- 설정 저장 성공 시 scheduler가 호출된다.
- 잘못된 시간 입력이면 scheduler가 호출되지 않는다.
- 알림 off 상태도 명시적으로 scheduler에 반영된다.

## 5. 순수 정책 모듈 분리

### 5.1 ValidationPolicy

현재 `BpViewModel` 내부에 있는 검증을 분리한다.

대상:

- 혈압 50..200
- 체중 0.0..100.0
- 체중 둘째 자리 반올림
- 시간 `HH:mm`

권장 API:

```kotlin
object ValidationPolicy {
    fun parsePressure(value: String): Int?
    fun parseWeight(value: String): Double?
    fun parseTime(value: String): LocalTime?
}
```

테스트:

- `49`, `50`, `200`, `201`
- 빈 문자열, 공백, 소수 혈압
- `0`, `100`, `100.001`, `-1`
- `65.555`는 `65.56`
- `8:00` 허용 여부를 정책으로 확정
- `08:00`, `23:59`, `24:00`, `12:60`

### 5.2 CalendarPolicy

현재 `buildCalendarCells`, `statusText`, 월별 상태 조합 일부가 UI/Repository에 섞여 있다.

권장 분리:

- 6주 42칸 생성
- 선택 날짜와 월 이동 시 날짜 보정
- record에서 calendar status 생성

테스트:

- 2026년 5월은 42칸을 생성한다.
- 월 이동 시 31일에서 30일 달로 이동하면 마지막 날짜로 보정된다.
- 기록 상태 4단계가 정확히 계산된다.
- 아침/저녁 기록 조합 4상태가 정확하다.

### 5.3 GraphPolicy

현재 그래프 기간은 `BpRepository.observeGraphPoints()`가 `GraphPolicy.buildGraphPoints()`에 위임하고, 기준일은 `ClockProvider`를 통해 주입한다.

권장 분리:

- 최근 N일 시작일 계산
- 잘못된 `dateIso` 제외
- 날짜순 정렬
- 아침/저녁 평균 계산
- 미기록 날짜를 건너는 연결 구간 판정

테스트:

- fake today 기준 최근 7일만 포함한다.
- 미래 날짜는 제외한다.
- 잘못된 날짜 문자열은 제외한다.
- systolic/diastolic 평균 계산이 정확하다.

## 6. 테스트 하네스 구성

### 6.1 Unit Test

위치:

```text
app/src/test/java/com/pulselog/
```

필요 의존성:

- `kotlinx-coroutines-test`
- `app.cash.turbine:turbine` 또는 Flow 수집용 자체 헬퍼
- JUnit 4 유지 가능

우선 추가할 테스트:

- `ValidationPolicyTest`
- `CalendarPolicyTest`
- `GraphPolicyTest`
- `BpViewModelTest`
- `FakeHealthRepositoryTest`

### 6.2 Room Test

위치:

```text
app/src/androidTest/java/com/pulselog/data/
```

하네스:

- `Room.inMemoryDatabaseBuilder`
- `allowMainThreadQueries()`는 테스트에서만 허용
- DAO upsert, delete, observe 검증

우선 추가할 테스트:

- `BpDaoTest`
- `AppDatabaseMigrationTest`

### 6.3 Compose UI Test

위치:

```text
app/src/androidTest/java/com/pulselog/ui/
```

전제 작업:

- 주요 노드에 `Modifier.testTag()` 추가
- 저장 버튼, 삭제 버튼, 날짜 셀, 탭, 입력 탭 이동 버튼, 입력 필드 태그화
- 삭제 버튼과 삭제 확인 다이얼로그 태그화

권장 태그:

```text
main.tab.calendar
main.tab.graph
main.header.settings
calendar.month.previous
calendar.month.next
calendar.day.2026-05-11
calendar.selected.open_entry
quick.morning.systolic
quick.morning.diastolic
quick.morning.save
quick.morning.delete
quick.evening.systolic
quick.evening.diastolic
quick.evening.save
quick.evening.delete
quick.weight.value
quick.weight.save
quick.weight.delete
quick.delete.confirm
quick.delete.cancel
settings.morning.time
settings.evening.time
settings.save
```

우선 검증 흐름:

- 첫 진입 시 오늘 날짜가 선택된다.
- 날짜 셀 탭 시 입력 대상 날짜가 바뀐다.
- 날짜 셀 탭 시 월력 카드 상단 선택 날짜 요약이 바뀐다.
- 아침 혈압 저장 후 캘린더 상태가 아침만 기록으로 바뀐다.
- 저녁 혈압까지 저장하면 완료 상태가 된다.
- 삭제 버튼은 확인 다이얼로그를 띄우고, 확인 후 해당 항목이 삭제된다.
- 그래프에서 선택한 날짜가 캘린더 탭 선택 날짜로 반영된다.
- 설정에서 잘못된 시간 저장 시 오류 스낵바가 보인다.

## 7. 앱 조립 하네스

### 7.1 AppGraph

현재 `MainActivity`가 모든 의존성을 직접 만든다. 이를 앱 조립 클래스로 이동한다.

```kotlin
class AppGraph(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "bp-db"
    )
        .addMigrations(...)
        .build()

    val clockProvider: ClockProvider = SystemClockProvider()
    val repository: HealthRepository = RoomHealthRepository(db.bpDao(), clockProvider)
    val notificationScheduler: NotificationScheduler = NoOpNotificationScheduler()

    fun createViewModel(): BpViewModel {
        return BpViewModel(
            repo = repository,
            clockProvider = clockProvider,
            notificationScheduler = notificationScheduler
        )
    }
}
```

효과:

- Production graph와 Test graph를 분리할 수 있다.
- Activity는 앱 시작 역할만 가진다.
- UI 테스트에서 fake graph를 주입하기 쉬워진다.

### 7.2 ViewModel Factory

현재 `BpViewModel(repo)`를 Activity에서 직접 생성한다. 테스트와 생명주기 안정성을 위해 `ViewModelProvider.Factory`를 둔다.

## 8. 마이그레이션 하네스

현재 리스크:

- `AppDatabase.version = 4`
- `exportSchema = true`
- `app/schemas`에 version 3, 4 snapshot 보존
- `fallbackToDestructiveMigration(dropAllTables = true)` 제거됨
- v3 schema snapshot을 baseline으로 유지하고 v4에서 `daily_notes`를 제거
- future migration은 `AppDatabaseMigrations.ALL`에 추가

권장 변경:

- version 1 -> 2 -> 3 migration은 schema artifact가 확보될 때만 복원한다.
- 현 시점부터 version 3 기준 스냅샷을 baseline으로 고정하고 version 4 migration으로 메모 테이블 제거를 검증한다.

테스트:

- `MigrationTestHelper` 사용
- 이전 스키마 DB 생성
- 샘플 레코드 삽입
- migration 실행
- 데이터 보존 확인

## 9. 로컬 체크 하네스

권장 스크립트:

```text
scripts/check.ps1
scripts/check.sh
```

1차 체크:

```text
./gradlew.bat test
./gradlew.bat lintDebug
./gradlew.bat assembleDebug
```

Android 기기 연결 시 추가:

```text
./gradlew.bat connectedDebugAndroidTest
```

현재 환경 선행 조건:

- `JAVA_HOME` 설정 필요
- `java`가 PATH에 있어야 한다.

## 10. 개발 단계 계획

### Phase 0. 실행 환경 고정

목표:

- `JAVA_HOME` 설정
- `./gradlew.bat test` 실행 가능
- 샘플 테스트 통과 확인

완료 조건:

- 로컬에서 Gradle test가 실행된다.

### Phase 1. 순수 정책 테스트 하네스

작업:

- `domain` 패키지 생성
- `ValidationPolicy`, `CalendarPolicy`, `GraphPolicy` 분리
- 각 정책 테스트 추가

완료 조건:

- DB나 Android 없이 핵심 정책 테스트가 통과한다.

### Phase 2. Repository / Clock 하네스

작업:

- `HealthRepository` 인터페이스 추가
- `BpRepository`가 인터페이스 구현
- `ClockProvider` 추가
- Repository와 ViewModel에서 현재 시각 직접 사용 제거
- `FakeHealthRepository`, `FakeClockProvider` 추가

완료 조건:

- ViewModel 저장/삭제/검증 테스트가 DB 없이 통과한다.

### Phase 3. 알림 하네스

작업:

- `NotificationScheduler` 인터페이스 추가
- `NoOpNotificationScheduler`, `FakeNotificationScheduler` 추가
- 설정 저장 시 scheduler 호출
- `AlarmManager` 기반 production scheduler 추가
- 부팅/앱 업데이트 후 저장된 설정 재예약
- 미기록 여부를 기준으로 알림 표시 여부를 판단하는 순수 정책 추가

완료 조건:

- 실제 OS 알림 구현 전에도 설정 저장과 스케줄링 계약을 테스트한다.
- 실제 OS 알림 구현은 `NotificationSchedulePolicy` 단위 테스트와 로컬 체크로 최소 검증한다.

### Phase 4. UI 하네스

작업:

- 주요 컴포넌트에 `testTag` 추가
- Compose UI 테스트 추가
- Fake ViewModel 또는 Fake Repository 기반 테스트 구성

완료 조건:

- 캘린더, 빠른 입력, 그래프, 설정의 핵심 흐름을 자동으로 검증한다.

### Phase 5. DB 마이그레이션 하네스

작업:

- Room schema export 켜기
- destructive migration 제거
- migration 테스트 추가

완료 조건:

- 스키마 변경 시 데이터 보존 테스트가 실패/성공으로 검증된다.

### Phase 6. 로컬/CI 체크 하네스

작업:

- `scripts/check.ps1` 또는 `scripts/check.sh` 추가
- GitHub Actions 또는 다른 CI 구성

완료 조건:

- PR 또는 로컬 작업 전 같은 검증 명령을 실행한다.

## 11. 다음 구현 우선순위

가장 먼저 할 작업은 앱 기능 추가가 아니라 테스트 가능한 경계 생성이다.

권장 순서:

1. `JAVA_HOME` 설정 후 현재 테스트 실행 상태 확인
2. `ValidationPolicy` 분리와 테스트 추가
3. `ClockProvider` 도입
4. `HealthRepository` 인터페이스와 fake repository 추가
5. ViewModel 테스트 추가
6. `NotificationScheduler` 인터페이스 추가
7. Compose testTag 추가
8. Room migration 하네스 추가

이 순서가 좋은 이유는 알림, 마이그레이션, 미사용 상세 코드 정리 같은 다음 기능 작업의 회귀 위험을 먼저 줄이기 때문이다.
