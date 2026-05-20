# Pulse Log 설계 정리

## 1. 설계 요약

기준 설계는 `날짜 단위 건강 기록` 모델이다.
앱은 첫 진입 시 오늘 날짜를 기본 선택하고, 입력 탭에서 선택 날짜 기준으로 빠르게 입력하는 흐름을 우선한다.

별도 상세 화면은 제거됐고 현재 활성 사용자 흐름은 입력, 캘린더, 그래프 중심이다.
날짜 메모 기능과 관련 DB/API는 v3 -> v4 migration과 함께 제거한다.

## 2. 현재 화면 설계

### 2.1 메인 진입

- 상단 헤더
- 헤더 우측 내보내기 아이콘과 설정 톱니바퀴
- 커스텀 세그먼트 탭
- 탭 전환 애니메이션
- 결과 메시지는 스낵바

### 2.2 입력 탭

구성:

- 선택 날짜 기준 빠른 입력 카드
- 저장/삭제 액션
- 삭제 확인 다이얼로그
- 저장/삭제 아이콘 버튼은 입력 필드와 같은 행에 배치

동작:

- 선택 날짜는 캘린더/그래프와 공유한다.
- 입력 카드의 필드는 선택 날짜 저장값과 동기화된다.
- 저장 버튼 누름 시 포커스를 해제하고 키보드를 내린다.
- 삭제 버튼은 저장된 항목이 있을 때만 표시한다.
- 삭제는 확인 다이얼로그에서 확정한 뒤 실행한다.
- 아침/저녁 혈압과 체중의 저장/삭제 버튼은 입력란 기준선과 맞춰 같은 행에서 조작한다.

### 2.3 캘린더 탭

구성:

- 월력 카드
- 월력 카드 상단 선택 날짜 요약
- 선택 날짜 기록 요약
- 입력 탭 이동 액션

동작:

- 첫 진입 시 오늘 날짜를 기본 선택한다.
- 날짜 셀 탭의 1차 동작은 입력 대상 날짜 변경이다.
- 날짜 선택 시 월력 카드 상단 요약이 즉시 변경된다.

### 2.4 그래프 탭

- 최근 7일 / 최근 30일 전환
- 혈압 2라인 그래프
- 체중 그래프
- x축/y축 라벨
- 범례
- 포인트 표시
- 날짜 열 선택
- 선택 날짜 카드
- 혈압 기준선
- 중간 미기록 날짜 연결 점선
- 선택 날짜를 캘린더 탭으로 이동하는 액션
- 빈 상태 메시지

### 2.5 설정 화면

- 헤더 우측 톱니바퀴에서 진입
- 아침 알림 on/off와 시간 선택
- 저녁 알림 on/off와 시간 선택
- 아침, 저녁, 저장을 독립 카드로 구분
- 디버그 빌드에서 그래프 테스트 데이터 생성 액션

### 2.6 내보내기 화면

- 헤더 우측 공유 아이콘에서 진입
- 최근 30일 / 전체 기록 CSV 내보내기
- 최근 30일 / 전체 기록 PDF 요약본 내보내기
- PDF 요약본에 혈압/체중 최근 추이 그래프 포함
- Android 공유 시트로 이메일, 메신저, 드라이브 앱에 전달

## 3. UI 구조 설계

현재 UI는 역할별 파일로 분리한다.

- `BloodPressureScreen.kt`
  - 헤더, 내보내기/설정 진입, 세그먼트 탭, 탭 콘텐츠 전환
- `EntryScreen.kt`
  - 선택 날짜 기준 빠른 입력 화면
- `CalendarTab.kt`
  - 캘린더 탭 상태 조합
- `CalendarComponents.kt`
  - 월력 카드, 선택 날짜 요약, 날짜 셀
- `QuickEntryComponents.kt`
  - 빠른 입력 카드, 공용 입력 컴포넌트, 삭제 확인 다이얼로그
- `GraphScreen.kt`
  - 그래프 화면, Canvas 차트, 선택 날짜 카드
- `SettingsScreen.kt`
  - 헤더 톱니바퀴에서 열리는 설정 화면, 알림 설정, 디버그 테스트 데이터 액션
- `ExportScreen.kt`
  - 헤더 공유 아이콘에서 열리는 CSV/PDF 요약본 내보내기 화면
- `UiChrome.kt`
  - 헤더, 포커스 해제 modifier, 회색 ripple 없는 soft clickable
- `PulseLogTheme.kt`
  - 앱 컬러 스킴과 Material ripple 톤 통일
- `UiTokens.kt`
  - 색상 토큰, 포맷터, 상태 계산 헬퍼
- `domain/GraphPolicy.kt`
  - 그래프 날짜축과 값 범위 정책
- `domain/ExportPolicy.kt`
  - CSV 파일명, 헤더, 행 포맷, PDF 요약 데이터와 추이 포인트 정책
- `domain/ClockProvider.kt`
  - 날짜/시간 의존성 경계

## 4. 현재 시각 설계 원칙

- 전체 톤은 혈압 앱에 맞춘 밝은 핑크/적색 계열
- 캘린더, 빠른 입력, 그래프, 설정은 같은 디자인 언어를 사용
- 그래프 라인 색상은 수축기, 이완기, 체중을 명확히 구분한다.
- 기본 Material 컴포넌트 느낌을 그대로 두지 않고 카드/유리판/캡슐 구조로 조정한다.

## 5. 입력 설계

- 아침/저녁 혈압은 각각 2필드 + 저장 버튼
- 체중은 단일 필드 + 저장 버튼
- 저장된 항목에는 삭제 버튼을 함께 제공
- 입력 필드와 저장/삭제 버튼은 한 행에 배치해 터치 대상과 입력 대상의 관계를 명확히 한다.
- 저장 버튼은 아이콘 기반 단일 액션
- 삭제 버튼도 아이콘 기반이며 확인 다이얼로그를 거친다.
- 저장 누름 전/직후 상태 차이를 애니메이션으로 표현
- 빠른 입력에서는 필드 중심으로 현재값 확인과 수정을 수행한다.

## 6. 그래프 설계

- 그래프 기간은 최근 7일 또는 최근 30일이다.
- 기록이 없는 날짜도 날짜축에는 포함한다.
- 연속 기록 구간은 실선으로 표시한다.
- 중간 미기록 날짜를 건너는 연결 구간은 연한 점선으로 표시한다.
- 혈압 그래프는 수축기/이완기 일 평균 2라인이다.
- 아침/저녁 분리 그래프는 현재 범위에서 제외한다.
- 혈압 그래프에는 수축기 120, 이완기 80 기준선을 표시한다.
- 사용자가 그래프를 탭하면 가장 가까운 날짜 열을 선택한다.
- 선택 날짜 카드는 날짜, 값 목록, 캘린더 이동 액션을 함께 보여준다.

## 7. 피드백 설계

- 저장/수정/삭제 결과는 스낵바로 표시
- CSV/PDF 공유는 Android Sharesheet로 처리한다.
- 바깥쪽 터치 시 포커스 해제 및 키보드 닫힘
- 입력 화면 루트에는 `imePadding()`과 `animateContentSize()` 적용
- 그래프 선택은 점선 세로 가이드라인과 포인트 강조로 표시
- 기본 Material ripple은 앱 포인트 컬러 기반으로 통일한다.

## 8. 내보내기 설계

- 헤더 공유 아이콘에서 내보내기 화면을 열고 최근 30일 또는 전체 기록 CSV/PDF 요약본을 공유한다.
- CSV 생성과 PDF 요약 데이터 계산은 `ExportPolicy`가 담당한다.
- 파일은 `cacheDir/exports`에 임시 생성한다.
- 공유는 `FileProvider` URI와 `ACTION_SEND` intent로 처리한다.
- 한글 환경 엑셀 호환성을 위해 UTF-8 BOM을 포함한다.
- 빈 측정값은 빈 칸으로 둬서 실제 `0` 측정값으로 오해하지 않게 한다.
- PDF 요약본은 기록 기간, 기록 건수, 평균 혈압/체중, 최근 추이 그래프, 최근 기록 표를 포함한다.

## 9. 데이터 모델 설계

### 9.1 DailyHealthRecord

- `dateIso: String`
- `morningSystolic: Int?`
- `morningDiastolic: Int?`
- `morningMeasuredAtEpochMs: Long?`
- `eveningSystolic: Int?`
- `eveningDiastolic: Int?`
- `eveningMeasuredAtEpochMs: Long?`
- `weightKg: Double?`
- `weightMeasuredAtEpochMs: Long?`
- `createdAtEpochMs: Long`
- `updatedAtEpochMs: Long`

제약:

- 날짜당 1건
- 아침 혈압은 수축기/이완기가 함께 존재하거나 함께 비어 있어야 한다.
- 저녁 혈압도 같은 규칙을 따른다.
- 체중은 nullable
- 수정 시 기존 측정 시간은 유지한다.

### 9.2 NotificationSettings

- `id: Int`
- `morningEnabled: Boolean`
- `morningTime: String`
- `eveningEnabled: Boolean`
- `eveningTime: String`
- `repeatEnabled: Boolean`은 기존 DB 호환을 위해 유지하되 앱에서는 false로 저장
- `repeatCount: Int`는 기존 DB 호환을 위해 유지하되 앱에서는 0으로 저장
- `updatedAtEpochMs: Long`

## 10. 현재 설계상 남은 리스크

- 실제 알림 스케줄링은 연결됐지만 기기 제조사별 배터리 정책에 따른 지연 가능성은 수동 검증이 필요하다.
- 설정 화면 시간 입력은 선택 다이얼로그로 교체됐고, 저장 전 ViewModel 검증은 안전망으로 유지한다.
- destructive migration fallback은 제거됐고 v3 -> v4 migration은 테스트로 검증한다.
- 미사용 상세 화면 코드는 제거됐다.

## 11. iOS 동등 설계

iOS 앱은 Android 앱과 같은 사용자 경험을 목표로 하되 Android 배포 흐름에 영향을 주지 않는 Kotlin Multiplatform 공통 모듈을 사용한다.

### 11.1 구현 원칙

- Android Kotlin/Compose/Room 코드는 iOS 작업 범위에서 수정하지 않는다.
- 모델, 정책, use case, repository 계약은 `shared` KMP 모듈로 이동한다.
- iOS는 `shared` 모듈을 호출하는 SwiftUI 앱 또는 Compose Multiplatform 앱으로 구현한다.
- 화면 순서, 정보 구조, 주요 문구, 색상 톤, 카드 구조는 Android 기준을 따른다.
- 플랫폼 네이티브 차이는 허용하되 사용자 흐름은 동일하게 유지한다.
- iOS DB와 Android Room DB의 파일 호환성은 첫 배포 목표가 아니다.

### 11.2 iOS 화면 구조

- 메인: 헤더, 내보내기, 설정, 입력/캘린더/그래프 전환
- 입력: 선택 날짜 요약, 아침 혈압, 저녁 혈압, 체중, 저장/삭제, 삭제 확인
- 캘린더: 월 이동, 날짜 상태, 선택 날짜 요약, 입력 이동
- 그래프: 7일/30일 전환, 혈압 2라인, 체중 그래프, 선택 날짜 카드
- 설정: 아침/저녁 알림 on/off와 시간 선택, 저장
- 내보내기: 최근 30일/전체 CSV, 최근 30일/전체 PDF 요약본 공유

### 11.3 iOS 디자인 동등성

- 전체 톤은 Android와 같은 밝은 핑크/적색 계열을 사용한다.
- iOS 컨트롤은 SwiftUI 네이티브 컨트롤을 사용하되 Android의 카드 분리, 아이콘 액션, 피드백 구조를 유지한다.
- 저장/삭제/공유/설정 같은 명령은 아이콘과 접근성 라벨을 함께 제공한다.
- 캘린더와 그래프의 색상 의미는 두 플랫폼에서 동일해야 한다.

### 11.4 iOS 제외 범위

- Android Room migration 공유
- Android Play 배포 설정 변경
- 재알림 사용/횟수 기능 재도입
- 서버 동기화
- Android와 iOS 간 자동 데이터 이전

### 11.5 공통 코드 범위

공유:

- `DailyHealthRecord`, `NotificationSettings`, `CalendarDayStatus`, `GraphPoint`
- 입력 검증 정책
- 캘린더 상태 정책
- 그래프 기간과 값 계산 정책
- CSV/PDF 요약 데이터 정책
- 알림 표시 생략 정책
- repository 계약과 use case

플랫폼별 유지:

- Android Compose 화면과 iOS 화면 entry
- Android Room entity와 migration
- iOS 로컬 저장소 구현
- Android `AlarmManager`와 iOS `UNUserNotificationCenter`
- Android/iOS 공유 시트
