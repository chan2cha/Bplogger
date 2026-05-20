# Pulse Log 하네스 엔지니어링 설계

## 1. 목적

Pulse Log의 기능을 안전하게 유지하고 확장하기 위한 테스트 가능한 경계를 정의한다. 현재 Android 앱은 배포 중이므로 Android 하네스는 기존 동작 보호에 집중하고, iOS 하네스는 Android 소스와 독립된 신규 구현의 품질 기준으로 둔다.

## 2. 핵심 원칙

- Android 배포 중인 소스는 iOS 작업 때문에 변경하지 않는다.
- 시간, 저장소, 알림, 내보내기 같은 외부 의존성은 인터페이스 뒤에 둔다.
- 순수 정책은 플랫폼과 무관하게 단위 테스트 가능해야 한다.
- OS 알림, DB, 공유 시트, 파일 시스템은 플랫폼별 구현으로 분리한다.
- UI 테스트는 화면 문구보다 안정적인 태그 또는 accessibility identifier를 우선 사용한다.
- 데이터 migration은 삭제가 아니라 보존 전략과 테스트를 먼저 둔다.

## 3. 현재 Android 하네스 상태

Android 앱에는 다음 경계가 이미 있다.

- `ClockProvider`
- `HealthRepository`
- `NotificationScheduler`
- `ValidationPolicy`
- `CalendarPolicy`
- `GraphPolicy`
- `ExportPolicy`
- `NotificationSchedulePolicy`
- `NotificationReminderPolicy`

현재 Android 체크는 repository root에서 실행한다.

```powershell
.\scripts\check.ps1
```

또는:

```bash
bash ./scripts/check.sh
```

Android 하네스의 우선순위는 배포 중인 앱의 회귀 방지다. iOS 구현을 위해 Android 구조를 재편하지 않는다.

## 4. Android 하네스 유지 규칙

- 로직 변경은 `app/src/test` 단위 테스트를 동반한다.
- Room 또는 migration 변경은 `app/src/androidTest` 테스트를 동반한다.
- UI 변경은 `Modifier.testTag()`를 추가하거나 유지한다.
- 알림 변경은 `NotificationScheduler` 경계를 통과해야 한다.
- 실제 OS 알림 변경은 scheduler fake 테스트와 기기 수동 검증을 함께 둔다.
- Android Room schema는 `app/schemas`에 보존한다.
- destructive migration fallback은 다시 추가하지 않는다.

## 5. iOS 하네스 목표 구조

iOS 앱은 Android와 같은 `shared` Kotlin Multiplatform 경계를 사용한다.

```text
shared
  commonMain
    model
    domain
    repository
    usecase
    presentation
  commonTest

ios
  PulseLog
    App
    Features
      Entry
      Calendar
      Graph
      Settings
      Export
    SharedBridge
    Data
      LocalHealthRepository
    Notifications
      NotificationScheduling
      LocalNotificationScheduler
    DesignSystem
  PulseLogTests
  PulseLogUITests
```

## 6. iOS 경계 설계

공통 경계는 가능한 한 `shared/commonMain`에 둔다. iOS 프로젝트에는 SwiftUI 상태 wrapper, 저장소 구현, 알림 구현, 공유 시트 구현만 남긴다.

### 6.1 ClockProviding

목적:

- 오늘 날짜와 현재 시각을 테스트에서 고정한다.
- 그래프 기간, 저장 시각, 알림 시각 계산을 재현 가능하게 만든다.

권장 shared API:

```kotlin
protocol ClockProviding {
    fun today(): LocalDate
    fun nowEpochMs(): Long
    fun zoneId(): TimeZoneId
}
```

### 6.2 HealthRepository

목적:

- UI 상태 모델이 저장소 구현을 직접 알지 않게 한다.
- SwiftData, SQLite, in-memory fake를 교체 가능하게 한다.

필수 동작:

- 날짜별 기록 관찰 또는 조회
- 월별 상태 조회
- 그래프 포인트 조회
- 알림 설정 조회
- 아침/저녁 혈압 저장과 삭제
- 체중 저장과 삭제
- 알림 설정 저장

### 6.3 NotificationScheduling

목적:

- iOS `UNUserNotificationCenter` 직접 호출을 앱 로직에서 분리한다.
- 권한, 예약, 취소를 테스트 가능한 계약으로 만든다.

필수 동작:

- 설정 적용
- 전체 예약 취소
- 권한 상태 조회
- 권한 요청

### 6.4 순수 정책

다음 정책은 플랫폼 API 없이 테스트 가능해야 한다.

- 혈압, 체중, 시간 입력 검증
- 월력 셀 생성
- 날짜 선택과 월 이동 보정
- 캘린더 상태 계산
- 그래프 기간과 축 범위 계산
- CSV 행 포맷
- 알림 표시 생략 판단

상세 shared 모듈 구조는 [Android/iOS 공통 코드 설계](./shared-kmp-architecture.md)를 따른다.

## 7. iOS 테스트 계획

### 7.1 Unit Test

위치:

```text
ios/PulseLogTests
```

우선 테스트:

- 혈압 범위 `49`, `50`, `200`, `201`
- 체중 범위 `0`, `100`, `100.001`, `-1`
- 체중 반올림
- 시간 형식 `08:00`, `23:59`, `24:00`, `12:60`
- 최근 7일 / 30일 그래프 기간
- 기록 상태 없음/아침만/저녁만/모두
- 알림 표시 생략 정책

### 7.2 Repository Test

검증:

- 날짜별 1건 저장
- 아침/저녁/체중 독립 저장
- 수정 시 측정 시각 유지
- 삭제 시 해당 항목만 삭제
- 빈 기록 정리 정책
- 알림 설정 저장

### 7.3 UI Test

위치:

```text
ios/PulseLogUITests
```

권장 accessibility identifier:

```text
main.tab.entry
main.tab.calendar
main.tab.graph
main.header.settings
main.header.export
entry.morning.systolic
entry.morning.diastolic
entry.morning.save
entry.morning.delete
entry.evening.systolic
entry.evening.diastolic
entry.evening.save
entry.weight.value
entry.weight.save
calendar.month.previous
calendar.month.next
calendar.day.<yyyy-mm-dd>
graph.range.7
graph.range.30
settings.morning.enabled
settings.morning.time
settings.evening.enabled
settings.evening.time
settings.save
export.csv.30
export.csv.all
```

## 8. iOS 완료 기준

첫 TestFlight 전:

- 핵심 단위 테스트 통과
- 저장소 테스트 통과
- UI smoke test 통과
- 실제 iPhone에서 입력, 캘린더, 그래프, 설정, 내보내기 수동 검증 완료
- 알림 권한 요청과 예약 수동 검증 완료

App Store 제출 전:

- 개인정보 처리방침이 iOS 포함 상태로 갱신됨
- App Privacy 응답이 실제 데이터 처리와 일치함
- TestFlight 피드백 중 치명 이슈 없음
- Android 배포 산출물과 iOS 산출물이 섞이지 않음

## 9. 작업 순서

Android 유지:

1. 로컬 Gradle checks 실행 가능 상태 유지
2. 정책 단위 테스트 유지
3. Room migration 테스트 유지
4. 알림 수동 검증 유지

iOS 신규:

1. iOS 프로젝트 생성
2. 순수 정책과 fake clock 작성
3. 로컬 repository와 fake repository 작성
4. 입력 화면과 저장 흐름 구현
5. 캘린더와 그래프 구현
6. 알림 scheduler 구현
7. 내보내기 구현
8. XCUITest와 TestFlight 준비
