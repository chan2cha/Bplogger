# Pulse Log iOS 배포 설계

## 1. 목표

Android에서 배포 중인 Pulse Log와 같은 앱을 iOS에서도 배포한다. 첫 iOS 버전은 Android 앱의 핵심 경험을 최대한 동일하게 제공하는 것을 목표로 한다.

가장 중요한 제약은 다음과 같다.

- Android 소스, Gradle 설정, Room migration, Play 배포 흐름은 iOS 작업으로 변경하지 않는다.
- iOS 앱은 독립적으로 빌드, 테스트, 배포할 수 있어야 한다.
- 두 플랫폼은 기능과 UX 기준을 문서로 동기화한다.

## 2. 권장 접근

첫 iOS 배포는 Kotlin Multiplatform `shared` 모듈을 만든 뒤, iOS 앱이 그 공통 로직을 사용하는 구조를 권장한다.

이유:

- Android 앱은 이미 배포 중이므로 구조 변경 리스크를 줄여야 한다.
- Kotlin Multiplatform은 Android와 iOS 사이에서 공통 business logic을 공유하는 데 적합하다.
- 검증, 날짜, 그래프, 내보내기, 알림 표시 판단은 플랫폼 API 없이 공유할 수 있다.
- UI와 OS 기능까지 한 번에 공유하면 Android 변경 폭이 커지므로, 첫 단계에서는 domain/usecase/presentation state를 공유한다.
- UI 공유는 첫 iOS 배포 이후 Compose Multiplatform으로 점진 검토한다.

## 3. 프로젝트 구조

기본안:

```text
shared/
  src/commonMain/
  src/commonTest/
  src/androidMain/
  src/iosMain/

ios/
  PulseLog/
    PulseLog.xcodeproj
    PulseLog/
      App/
      Features/
      Domain/
      Data/
      DesignSystem/
      Resources/
    PulseLogTests/
    PulseLogUITests/
```

대안:

- iOS 앱을 별도 저장소에서 관리한다.
- 이 저장소에는 문서, 화면 기준, 릴리즈 체크리스트만 유지한다.

선택 기준:

- 같은 저장소에 두면 디자인/요구사항 문서와 함께 관리하기 쉽다.
- 별도 저장소에 두면 Android 배포 산출물과 iOS 산출물이 섞일 위험이 더 낮다.

현재 권장안은 `shared/` KMP 모듈과 `ios/` Xcode 프로젝트를 같은 저장소에 둔다. 단, Android app 모듈은 phase별로 작게 연결하고 기존 Android 동작을 보존한다.

## 4. iOS 기술 스택

- 언어: Swift
- UI: SwiftUI
- 상태 관리: `ObservableObject` 또는 Swift Observation
- 공통 로직: Kotlin Multiplatform shared framework
- 저장소: SwiftData, SQLite wrapper, 또는 단순 로컬 JSON/SQLite 중 선택
- 알림: `UNUserNotificationCenter`
- 공유: `ShareLink` 또는 `UIActivityViewController`
- 테스트: XCTest, XCUITest

첫 배포에서는 저장소 복잡도를 낮추기 위해 iOS native 저장소 또는 KMP 저장소 중 하나를 선택한다. Android 기존 Room DB는 유지한다. 의료 기록 성격상 추후 schema migration 가능성이 필요하므로 단순 JSON은 프로토타입에만 적합하다.

## 5. 화면 동등성

iOS 화면은 Android의 현재 화면 구조를 기준으로 한다.

### 5.1 메인

- 상단 헤더
- 우측 내보내기 아이콘
- 우측 설정 아이콘
- 입력 / 캘린더 / 그래프 전환
- 저장, 삭제, 내보내기 결과 피드백

### 5.2 입력

- 선택 날짜 요약
- 아침 혈압 카드: 수축기, 이완기, 저장, 삭제
- 저녁 혈압 카드: 수축기, 이완기, 저장, 삭제
- 체중 카드: 체중, 저장, 삭제
- 삭제 확인 다이얼로그

### 5.3 캘린더

- 월 이동
- 날짜별 상태 표시
- 오늘과 선택 날짜 구분
- 선택 날짜 요약
- 입력 화면 이동 액션

### 5.4 그래프

- 최근 7일 / 최근 30일 전환
- 혈압 2라인 그래프
- 체중 그래프
- x/y축 라벨과 범례
- 기준선
- 선택 날짜 카드

### 5.5 설정

- 아침 알림 on/off
- 아침 알림 시간 선택
- 저녁 알림 on/off
- 저녁 알림 시간 선택
- 저장 시 알림 예약 갱신
- 재알림 사용/횟수 설정은 제공하지 않음

### 5.6 내보내기

- 최근 30일 CSV
- 전체 CSV
- 최근 30일 PDF 요약본
- 전체 PDF 요약본
- iOS 공유 시트로 전달

## 6. 데이터 모델

iOS 데이터 모델은 Android 의미와 맞춘다.

```text
DailyHealthRecord
  dateIso: String
  morningSystolic: Int?
  morningDiastolic: Int?
  morningMeasuredAtEpochMs: Int64?
  eveningSystolic: Int?
  eveningDiastolic: Int?
  eveningMeasuredAtEpochMs: Int64?
  weightKg: Double?
  weightMeasuredAtEpochMs: Int64?
  createdAtEpochMs: Int64
  updatedAtEpochMs: Int64

NotificationSettings
  id: Int
  morningEnabled: Bool
  morningTime: String
  eveningEnabled: Bool
  eveningTime: String
  updatedAtEpochMs: Int64
```

iOS에는 `repeatEnabled`, `repeatCount`를 새 기능으로 만들지 않는다. Android에는 기존 DB 호환 때문에 필드가 남아있지만 앱에서는 false/0으로 저장한다.

## 7. 알림 설계

- 설정 저장 시 기존 pending notification을 취소하고 아침/저녁 알림을 다시 예약한다.
- 권한이 없고 알림이 켜져 있으면 iOS 알림 권한을 요청한다.
- 알림 발화 시점에 해당 날짜 기록 여부를 확인할 수 없는 구조라면, 앱 실행 또는 설정 저장 시점에 다음 알림을 재계산하는 정책을 둔다.
- 가능하면 notification action보다는 앱 열기 중심으로 단순화한다.

검증 기준:

- 권한 거부 상태에서 저장 흐름이 깨지지 않는다.
- 권한 허용 후 알림이 표시된다.
- 아침 기록이 있으면 아침 알림은 표시되지 않는다.
- 저녁 기록이 있으면 저녁 알림은 표시되지 않는다.

## 8. App Store 준비

필수 준비:

- Apple Developer Program 가입
- macOS + Xcode 환경
- Bundle ID
- App Store Connect 앱 레코드
- 앱 아이콘과 스크린샷
- 개인정보 처리방침 URL
- App Privacy 응답
- TestFlight 내부 테스트

Apple Developer Program은 공식 안내 기준 연 99 USD 멤버십이다. TestFlight는 App Store Connect에서 베타 테스트를 관리하는 Apple 공식 배포 경로다. 건강 및 개인정보 데이터는 App Store Review Guidelines와 App Privacy 문구가 앱 실제 동작과 일치해야 한다.

참고:

- Apple Developer Program: https://developer.apple.com/programs/
- TestFlight: https://developer.apple.com/testflight/
- App Store Review Guidelines: https://developer.apple.com/app-store/review/guidelines/

## 9. iOS 하네스

첫 iOS 구현부터 `shared`에 다음 경계를 둔다.

- `ClockProviding`
- `HealthRepository`
- `NotificationScheduling`
- `ValidationPolicy`
- `CalendarPolicy`
- `GraphPolicy`
- `ExportPolicy`

테스트 우선순위:

1. 입력 검증 정책
2. 날짜와 캘린더 상태 정책
3. 그래프 기간 계산
4. 저장/삭제 repository 동작
5. 알림 표시 생략 정책
6. ViewModel 상태 전이
7. 주요 UI smoke test

상세 공통화 전략은 [Android/iOS 공통 코드 설계](./shared-kmp-architecture.md)를 기준으로 한다.

## 10. 단계별 계획

### Phase iOS-0. 준비

- Apple 계정과 Xcode 환경 준비
- iOS 프로젝트 위치 확정
- `shared` KMP 모듈 추가 계획 확정
- 앱 이름, Bundle ID, signing 설정
- Android 화면 기준 스크린샷 수집

### Phase iOS-1. 공통 로직

- `shared` 모듈 생성
- 순수 정책과 common test 이동
- shared model과 repository 계약 정의
- Android 앱이 shared 정책을 참조하도록 점진 연결

### Phase iOS-2. UI 껍데기

- SwiftUI 또는 Compose Multiplatform iOS 앱 생성
- 디자인 토큰 정의
- 입력, 캘린더, 그래프, 설정, 내보내기 화면 골격 구현
- Android와 같은 화면 흐름 구성

### Phase iOS-3. 로컬 저장

- iOS 저장소 선택 및 구현
- 기록 저장/수정/삭제 연결
- 캘린더 상태와 그래프 데이터 연결

### Phase iOS-4. 알림과 내보내기

- 로컬 알림 권한과 예약
- CSV 내보내기
- PDF 요약본 내보내기
- 공유 시트 연결

### Phase iOS-5. 테스트와 TestFlight

- 단위 테스트와 UI smoke test 추가
- 실제 기기 수동 검증
- TestFlight 내부 테스트 배포

### Phase iOS-6. App Store 제출

- 개인정보 처리방침 갱신
- 스크린샷과 설명 작성
- App Privacy 작성
- App Review 제출
