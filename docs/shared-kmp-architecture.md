# Pulse Log Android/iOS 공통 코드 설계

## 1. 목표

Android와 iOS를 함께 운영할 때 같은 정책과 데이터 규칙을 두 번 구현하지 않도록 Kotlin Multiplatform 기반 공통 모듈을 둔다.

목표는 다음과 같다.

- Android 배포 앱의 동작을 보존한다.
- iOS 앱은 공통 Kotlin 모듈을 사용한다.
- 혈압/체중 검증, 날짜 정책, 그래프 정책, 알림 표시 판단, 내보내기 포맷을 한 곳에서 관리한다.
- UI는 단계적으로 공유 범위를 늘린다.
- Android Room migration과 기존 배포 산출물은 안전하게 유지한다.

## 2. 결론

권장 구조는 `shared` Kotlin Multiplatform 모듈 + Android 앱 + iOS 앱이다.

첫 단계에서 공유할 것:

- 순수 domain policy
- data model
- repository interface
- use case
- ViewModel에 준하는 presentation state reducer
- CSV export formatter
- 알림 표시 판단 정책

첫 단계에서 공유하지 않을 것:

- Android Activity와 권한 요청
- Android `AlarmManager`
- iOS `UNUserNotificationCenter`
- Android 공유 시트와 iOS 공유 시트
- Android Compose 화면 전체
- Room migration 이력

이유:

- Kotlin Multiplatform은 공통 business logic 공유에 적합하다.
- Compose Multiplatform iOS는 사용할 수 있지만, 기존 Android Compose 화면을 그대로 옮기면 Android 앱 구조 변경 폭이 커질 수 있다.
- iOS 첫 배포의 리스크를 낮추려면 UI 공유는 2단계 이후로 미루고, 정책과 상태 로직부터 공유한다.

## 3. 목표 구조

```text
settings.gradle.kts
build.gradle.kts

shared/
  build.gradle.kts
  src/
    commonMain/kotlin/com/pulselog/shared/
      model/
      domain/
      repository/
      usecase/
      presentation/
      export/
    commonTest/kotlin/com/pulselog/shared/
    androidMain/kotlin/com/pulselog/shared/
    iosMain/kotlin/com/pulselog/shared/

app/
  기존 Android 앱

iosApp/
  Xcode/SwiftUI 앱 또는 Compose Multiplatform iOS entry
```

## 4. 공유 대상 상세

### 4.1 Model

공유한다.

```kotlin
data class DailyHealthRecord(...)
data class NotificationSettings(...)
data class CalendarDayStatus(...)
data class GraphPoint(...)
```

주의:

- Android Room entity annotation은 shared model에 직접 붙이지 않는다.
- Android Room entity는 Android app/data 계층에 남기거나 adapter를 둔다.
- shared model은 플랫폼 저장소와 분리된 순수 Kotlin 모델이어야 한다.

### 4.2 Domain Policy

공유한다.

- `ValidationPolicy`
- `CalendarPolicy`
- `GraphPolicy`
- `ExportPolicy`
- `NotificationSchedulePolicy`
- `NotificationReminderPolicy`

이 영역은 가장 먼저 옮긴다. Android 테스트를 shared common test로 이전하면 양쪽 플랫폼이 같은 규칙을 사용한다.

### 4.3 Repository Interface

공유한다.

```kotlin
interface HealthRepository {
    fun observeRecord(date: LocalDate): Flow<DailyHealthRecord?>
    fun observeMonthStatuses(month: YearMonth): Flow<List<CalendarDayStatus>>
    fun observeGraphPoints(days: Int): Flow<List<GraphPoint>>
    fun observeNotificationSettings(): Flow<NotificationSettings>

    suspend fun saveMorning(...)
    suspend fun saveEvening(...)
    suspend fun saveWeight(...)
    suspend fun deleteMorning(...)
    suspend fun deleteEvening(...)
    suspend fun deleteWeight(...)
    suspend fun saveNotificationSettings(settings: NotificationSettings)
}
```

Android 구현:

- 기존 Room 기반 repository를 adapter로 연결한다.
- 처음에는 기존 Android repository를 유지하고 shared interface를 구현하게 만든다.

iOS 구현:

- KMP 저장소를 쓰면 Kotlin 구현으로 둔다.
- SwiftData를 쓰면 Swift repository가 shared interface를 직접 구현하기 어렵기 때문에 KMP wrapper 또는 expect/actual 경계를 둔다.

### 4.4 Presentation State

부분 공유한다.

공유하기 좋은 것:

- selected date
- current month
- graph range
- input validation result
- screen state DTO
- save/delete intent 처리

플랫폼에 남길 것:

- Android `ViewModel`
- SwiftUI `ObservableObject`
- Compose snackbar
- iOS alert
- 권한 요청 launcher

권장 방식:

```text
shared presentation reducer/usecase
Android BpViewModel -> shared usecase 호출
iOS ObservableObject -> shared usecase 호출
```

이렇게 하면 UI 상태 규칙은 공유하고, 플랫폼 생명주기와 UI 이벤트 처리는 각 플랫폼에 남길 수 있다.

### 4.5 Database

두 가지 선택지가 있다.

#### 선택 A. Room KMP

장점:

- Android 기존 Room 지식을 유지할 수 있다.
- Android Developers 공식 문서가 KMP Room 구성을 제공한다.

주의:

- 기존 Android Room DB를 바로 shared Room으로 옮기는 것은 migration 리스크가 있다.
- 첫 단계에서는 Android 기존 Room을 유지하고 iOS 저장소만 새로 두는 편이 안전하다.

#### 선택 B. SQLDelight

장점:

- KMP에서 오래 쓰인 SQLite 기반 접근이다.
- schema와 query를 공통화하기 쉽다.

주의:

- Android 기존 Room schema와 migration을 SQLDelight로 옮기는 작업이 필요하다.
- 이미 배포된 Android 앱의 DB migration 전략을 새로 설계해야 한다.

권장:

- 1단계: Android Room 유지, iOS는 KMP 또는 Swift native 저장소 구현
- 2단계: 공통 저장소 필요성이 커지면 Room KMP 또는 SQLDelight migration 검토

## 5. UI 공유 전략

### 옵션 A. UI는 플랫폼별, 로직만 공유

권장 첫 단계.

- Android: 기존 Jetpack Compose 유지
- iOS: SwiftUI 또는 Compose Multiplatform 중 선택
- shared: domain/usecase/presentation state

장점:

- Android 변경 폭이 작다.
- iOS 네이티브 품질을 맞추기 쉽다.
- App Store 심사와 iOS 관례 대응이 쉽다.

단점:

- 화면 배치는 양쪽에서 구현해야 한다.

### 옵션 B. Compose Multiplatform으로 UI까지 공유

2단계 이후 검토.

- 기존 Android Compose 컴포넌트를 shared Compose UI로 점진 이동한다.
- iOS entry에서 같은 composable을 띄운다.

장점:

- UI 수정도 한 번만 할 가능성이 커진다.
- 디자인 동등성이 가장 높다.

단점:

- 기존 Android app 모듈 구조 변경이 커진다.
- iOS 네이티브 느낌, 텍스트 입력, 권한, 공유, 알림 같은 플랫폼 차이를 별도 처리해야 한다.
- Android 배포 중인 앱에는 더 큰 회귀 리스크가 있다.

권장:

- 첫 iOS 배포는 옵션 A
- 안정화 후 반복 수정이 많은 화면부터 옵션 B 검토

## 6. 단계별 이행 계획

### Phase KMP-0. 준비

- `shared` 모듈 추가
- Android app은 기존 동작 유지
- common test 실행 경로 추가
- 문서 기준 모델과 정책을 확정

완료 기준:

- `shared:allTests` 또는 equivalent common test가 실행된다.
- Android 앱 빌드는 기존과 동일하게 통과한다.

### Phase KMP-1. 순수 정책 이동

- `ValidationPolicy`
- `CalendarPolicy`
- `GraphPolicy`
- `ExportPolicy`
- `NotificationSchedulePolicy`
- `NotificationReminderPolicy`

완료 기준:

- 기존 Android unit test와 같은 내용이 shared common test에서 통과한다.
- Android 앱은 shared policy를 참조한다.

### Phase KMP-2. 모델과 repository 계약 공유

- shared model 추가
- shared `HealthRepository` 계약 추가
- Android Room entity와 shared model mapper 추가
- Android repository가 shared 계약을 구현

완료 기준:

- Android ViewModel 동작이 기존과 같다.
- Android Room schema는 변경하지 않는다.

### Phase KMP-3. UseCase 공유

- save morning/evening/weight use case
- delete use case
- notification settings save use case
- graph load use case
- export use case

완료 기준:

- Android ViewModel은 직접 정책 조합을 줄이고 shared usecase를 호출한다.
- iOS 앱은 같은 usecase를 호출할 준비가 된다.

### Phase KMP-4. iOS 앱 연결

- iOS target 추가
- shared framework를 Xcode에 연결
- SwiftUI `ObservableObject`가 shared usecase를 호출
- iOS repository 구현 추가

완료 기준:

- iOS에서 입력 저장/조회가 동작한다.
- Android 앱 소스 변경은 shared 연동 범위 안에서만 발생한다.

### Phase KMP-5. UI 공유 검토

- 반복 수정이 많은 화면을 분석한다.
- 설정 화면 또는 그래프처럼 공유 이득이 큰 화면부터 Compose Multiplatform 이동을 검토한다.
- Android/iOS 접근성, 입력, 공유, 알림 차이를 확인한다.

완료 기준:

- UI 공유가 실제 유지보수 비용을 줄이는 경우에만 진행한다.

## 7. 리스크와 대응

### Android 배포 회귀

대응:

- shared 이동은 순수 정책부터 시작한다.
- 한 phase마다 Android local check를 실행한다.
- Room schema 변경은 별도 phase 전에는 금지한다.

### iOS 빌드 환경 복잡도

대응:

- iOS target 추가 전 shared common test를 먼저 안정화한다.
- Xcode signing과 KMP framework 연결은 별도 phase에서 처리한다.

### UI 공유 과욕

대응:

- 첫 목표는 business logic 공유다.
- UI 공유는 첫 iOS 배포 이후 결정한다.

### DB 공통화 리스크

대응:

- Android 기존 Room DB는 유지한다.
- DB 공통화는 iOS 출시 후 필요성이 확실할 때 진행한다.

## 8. 최종 방향

Pulse Log의 장기 구조는 다음이 가장 적합하다.

```text
공유 Kotlin:
  모델, 정책, repository 계약, usecase, export formatter, 알림 판단

Android:
  기존 Compose UI, Android ViewModel wrapper, Room 구현, AlarmManager, Android share sheet

iOS:
  SwiftUI 또는 Compose UI, iOS state wrapper, iOS repository 구현, UNUserNotificationCenter, iOS share sheet
```

이 구조는 업데이트 때 검증/정책/데이터 의미를 한 번만 수정하게 만들고, 플랫폼별 OS 기능은 각 플랫폼에서 안정적으로 처리하게 한다.
