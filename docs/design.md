# Pulse Log 설계 정리

## 1. 설계 요약

기준 설계는 `날짜 단위 건강 기록` 모델이다.
앱은 첫 진입 시 오늘 날짜를 기본 선택하고, 캘린더 탭 안에서 선택 날짜 기준으로 빠르게 입력하는 흐름을 우선한다.

## 2. 현재 화면 설계

### 2.1 메인 진입

- 상단 헤더
- 커스텀 세그먼트 탭
- 탭 전환 애니메이션
- 결과 메시지는 스낵바

### 2.2 캘린더 탭

구성:

- 월력 카드
- 선택 날짜 요약
- 메모 미리보기
- 메모 별표가 포함된 날짜 셀
- 빠른 입력 카드

동작:

- 첫 진입 시 오늘 날짜를 기본 선택한다.
- 날짜 셀 탭의 1차 동작은 상세 진입이 아니라 입력 대상 날짜 변경이다.
- 메인 입력 카드의 필드는 선택 날짜 저장값과 동기화된다.
- 저장 버튼 누름 시 포커스를 해제하고 키보드를 내린다.

### 2.3 상세 화면

역할:

- 선택 날짜 아침 혈압 관리
- 선택 날짜 저녁 혈압 관리
- 선택 날짜 체중 관리
- 날짜 메모 확인/수정/삭제
- 측정 시간 확인

성격:

- 주 입력 화면이 아니라 세부 관리 화면

### 2.4 그래프 탭

- 최근 7일 / 최근 30일 전환
- 혈압 2라인 그래프
- 체중 그래프
- 빈 상태 메시지 처리

### 2.5 설정 탭

- 아침 알림 on/off와 시간 입력
- 저녁 알림 on/off와 시간 입력
- 재알림 on/off와 횟수 입력
- 빠른 입력 카드와 동일한 디자인 계열 재사용

## 3. UI 구조 설계

현재 UI는 역할별 파일로 분리한다.

- [BloodPressureScreen.kt](/Users/chan/workspace/projects/Bplogger/app/src/main/java/com/example/bplogger/ui/BloodPressureScreen.kt)
  - 헤더, 세그먼트 탭, 탭 콘텐츠 전환
- [CalendarTab.kt](/Users/chan/workspace/projects/Bplogger/app/src/main/java/com/example/bplogger/ui/CalendarTab.kt)
  - 캘린더 탭 상태 조합
- [CalendarComponents.kt](/Users/chan/workspace/projects/Bplogger/app/src/main/java/com/example/bplogger/ui/CalendarComponents.kt)
  - 월력 카드와 날짜 셀
- [QuickEntryComponents.kt](/Users/chan/workspace/projects/Bplogger/app/src/main/java/com/example/bplogger/ui/QuickEntryComponents.kt)
  - 빠른 입력 카드와 공용 입력 컴포넌트
- [GraphScreen.kt](/Users/chan/workspace/projects/Bplogger/app/src/main/java/com/example/bplogger/ui/GraphScreen.kt)
  - 그래프 화면
- [SettingsScreen.kt](/Users/chan/workspace/projects/Bplogger/app/src/main/java/com/example/bplogger/ui/SettingsScreen.kt)
  - 설정 화면
- [DayDetailScreen.kt](/Users/chan/workspace/projects/Bplogger/app/src/main/java/com/example/bplogger/ui/DayDetailScreen.kt)
  - 날짜별 상세 화면
- [UiChrome.kt](/Users/chan/workspace/projects/Bplogger/app/src/main/java/com/example/bplogger/ui/UiChrome.kt)
  - 헤더, 포커스 해제 modifier
- [UiTokens.kt](/Users/chan/workspace/projects/Bplogger/app/src/main/java/com/example/bplogger/ui/UiTokens.kt)
  - 색상 토큰, 포맷터, 상태 계산 헬퍼

## 4. 현재 시각 설계 원칙

- 전체 톤은 혈압 앱에 맞춘 밝은 핑크/적색 계열
- 캘린더, 빠른 입력, 그래프, 설정은 같은 디자인 언어를 사용
- 기본 Material 컴포넌트 느낌을 그대로 두지 않고, 카드/유리판/캡슐 구조로 조정
- 메모 상태는 혈압 상태와 충돌하지 않도록 보조 마커로 표현

## 5. 입력 설계

- 아침/저녁 혈압은 각각 2필드 + 저장 버튼
- 체중은 단일 필드 + 저장 버튼
- 저장 버튼은 아이콘 기반 단일 액션
- 저장 누름 전/직후 상태 차이를 애니메이션으로 표현
- 빠른 입력에서는 측정 시간과 현재값 텍스트를 최소화하고 필드 중심으로 구성
- 상세 화면에서는 측정 시간과 삭제까지 포함

## 6. 피드백 설계

- 저장/수정/삭제 결과는 스낵바로 표시
- 바깥쪽 터치 시 포커스 해제 및 키보드 닫힘
- 입력 화면 루트에는 `imePadding()`과 `animateContentSize()`를 적용

## 7. 데이터 모델 설계

### 7.1 DailyHealthRecord

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

### 7.2 DailyNote

- `dateIso: String`
- `note: String`
- `createdAtEpochMs: Long`
- `updatedAtEpochMs: Long`

### 7.3 NotificationSettings

- `id: Int`
- `morningEnabled: Boolean`
- `morningTime: String`
- `eveningEnabled: Boolean`
- `eveningTime: String`
- `repeatEnabled: Boolean`
- `repeatCount: Int`
- `updatedAtEpochMs: Long`

## 8. 현재 설계상 남은 리스크

- 실제 알림 스케줄링은 아직 연결되지 않았다.
- 설정 화면 시간 입력은 자유 텍스트라 형식 오류 가능성이 남아 있다.
- 상세 화면 입력 스타일이 빠른 입력 대비 덜 정리되어 있다.
- 현재 차트는 단순 캔버스 기반이라 확장성이 낮다.
- DB는 destructive migration 상태라 운영 데이터 보존 전략이 필요하다.
