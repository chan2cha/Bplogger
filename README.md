# Pulse Log

Pulse Log는 Android용 혈압/체중 기록 앱입니다.
선택한 날짜 기준으로 아침 혈압, 저녁 혈압, 체중을 빠르게 저장하고, 캘린더와 그래프로 상태를 확인하는 흐름을 제공합니다.

## 프로젝트 개요

- 플랫폼: Android
- 주요 언어: Kotlin
- UI: Jetpack Compose + Material 3
- 로컬 저장소: Room
- 상태 관리: Kotlin Coroutines + `StateFlow`
- 빌드: Gradle Kotlin DSL
- 코드 생성: KSP

## 현재 구현 상태

- 앱 첫 진입 시 오늘 날짜 자동 선택
- 상단 헤더와 커스텀 세그먼트 탭
- 입력 / 캘린더 / 그래프 3개 메인 탭
- 헤더 우측 내보내기/톱니바퀴 진입
- 선택 날짜 기준 빠른 입력
- 아침 혈압 / 저녁 혈압 / 체중 저장, 수정, 삭제
- 삭제 전 확인 다이얼로그
- 최근 7일 / 최근 30일 그래프
- 그래프 x축/y축, 범례, 포인트, 선택 가이드라인
- 미기록 날짜를 건너 연결하는 점선 구간 표시
- 그래프 선택 날짜 값 카드
- 그래프 선택 날짜를 캘린더 탭으로 연결
- Material 터치 피드백을 앱 컬러 톤으로 통일
- 디버그 빌드 그래프 더미 데이터 입력 액션
- 알림 설정 저장 UI
- 최근 30일 / 전체 기록 CSV 공유
- 최근 30일 / 전체 기록 PDF 요약본 공유
- 저장/수정/삭제 결과 스낵바 처리

## 현재 UX 방향

- 첫 화면 기본 탭은 입력이다.
- 앱 첫 진입 시 오늘 날짜를 기본 선택한다.
- 메인 입력은 선택 날짜 기준으로 동작한다.
- 캘린더는 상태 확인, 입력 대상 날짜 선택, 선택 날짜 요약 중심이다.
- 날짜 탭의 기본 동작은 입력 대상 날짜 변경이다.
- 별도 상세 화면과 날짜 메모는 현재 사용자 흐름에서 제외한다.
- 그래프에서 날짜를 확인한 뒤 캘린더 탭으로 이동해 해당 날짜를 수정한다.

## 현재 화면 구조

- 입력 탭
  - 선택 날짜 요약
  - 빠른 입력 카드
  - 입력란과 같은 행에 배치된 저장/삭제 아이콘 버튼
- 캘린더 탭
  - 월력 카드
  - 월력 카드 상단 선택 날짜 요약
  - 선택 날짜 기록 요약
  - 입력 탭 이동 액션
- 그래프 탭
  - 최근 7일 / 최근 30일 전환
  - 혈압 2라인 그래프
  - 체중 그래프
  - 선택 날짜 카드와 캘린더 이동 액션
- 설정 화면
  - 아침/저녁 알림 사용 여부
  - 알림 시간 입력
  - 재알림 여부 / 횟수 입력
  - 디버그 빌드 테스트 데이터 액션
- 내보내기 화면
  - 헤더 공유 아이콘에서 진입
  - 최근 30일 / 전체 기록 CSV 공유
  - 최근 추이 그래프가 포함된 병원 제출용 PDF 요약본 공유

## 코드 구조

- `MainActivity.kt`
  - DB / Repository / ViewModel 연결
- `BloodPressureScreen.kt`
  - 상단 헤더, 내보내기/설정 진입, 메인 탭 진입점
- `EntryScreen.kt`
  - 선택 날짜 기준 빠른 입력 탭
- `CalendarTab.kt`
  - 캘린더 탭 상태 조합
- `CalendarComponents.kt`
  - 월력 카드, 선택 날짜 요약, 날짜 셀, 상태 인디케이터
- `QuickEntryComponents.kt`
  - 빠른 입력 카드, 입력 필드, 저장/삭제 버튼, 삭제 확인 다이얼로그
- `GraphScreen.kt`
  - 그래프 탭, 차트 렌더링, 선택 날짜 카드
- `SettingsScreen.kt`
  - 헤더 톱니바퀴에서 열리는 설정 화면, 알림 설정, 디버그 테스트 데이터 액션
- `ExportScreen.kt`
  - 헤더 공유 아이콘에서 열리는 CSV/PDF 요약본 내보내기 화면
- `BpViewModel.kt`
  - 화면 상태와 저장 액션
- `BpRepository.kt`
  - Room 기반 데이터 접근과 그래프 더미 데이터 생성
- `domain/GraphPolicy.kt`
  - 그래프 날짜축과 차트 범위 계산
- `domain/ExportPolicy.kt`
  - CSV 파일명, 헤더, 행 포맷, PDF 요약 데이터와 추이 포인트 생성
- `domain/ClockProvider.kt`
  - 테스트 가능한 날짜/시간 의존성 경계

## 기술 스택

- Kotlin `2.2.10`
- Android Gradle Plugin `9.1.0`
- Jetpack Compose
- Material 3
- Room `2.7.0`
- Retrofit `2.11.0`
- Moshi `1.15.1`
- OkHttp `4.12.0`

## 주요 정책

- 아침 혈압과 저녁 혈압은 독립 저장한다.
- 혈압과 체중은 선택한 날짜에 신규 추가 또는 수정 가능하다.
- 입력된 아침 혈압, 저녁 혈압, 체중은 항목별로 삭제 가능하며 삭제 전 확인한다.
- 수정 시 기존 측정 시간은 유지한다.
- 그래프는 최근 기준으로 최근 7일 / 최근 30일을 제공한다.
- 그래프에서 연속 기록은 실선, 중간 미기록 날짜를 건너는 연결은 점선으로 표시한다.
- 내보내기는 헤더 공유 아이콘에서 열고 최근 30일 또는 전체 기록을 CSV 원본 파일 또는 그래프 포함 PDF 요약본으로 생성한다.
- 달력 상태는 `없음 / 아침만 / 저녁만 / 모두 기록` 4단계다.
- 날짜 메모와 상세 화면은 현재 UI 흐름에서 사용하지 않는다.
- 기존 `daily_notes` 테이블과 관련 코드는 마이그레이션 리스크를 줄이기 위해 일단 보존한다.

## 실행 방법

PowerShell:

```powershell
powershell.exe -ExecutionPolicy Bypass -File .\scripts\check.ps1
```

또는 Gradle 직접 실행:

```powershell
.\gradlew.bat test
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

## 현재 미구현 / 제한 사항

- 실제 OS 알림 스케줄링은 아직 미구현
- DB는 `fallbackToDestructiveMigration(dropAllTables = true)` 상태라 스키마 변경 시 기존 데이터 손실 가능
- 날짜 메모와 상세 화면 코드는 남아 있으나 현재 UI에서는 사용하지 않음
- 런처 앱명은 `Pulse Log`이며 하트 로고 기반 런처 아이콘을 사용
- Play Store 등록용 아이콘과 Feature Graphic은 `docs/store-assets`에 생성됨
- KSP 생성 소스 호환을 위해 `android.disallowKotlinSourceSets=false` 설정이 남아 있음
- Play 내부 테스트용 패키지명은 `com.pulselog`로 정리됨

## 추가 보완 필요 사항

- Room 마이그레이션 전략 수립
- 실제 알림 스케줄링 구현
- 설정 시간 입력 UX 개선
- ViewModel 테스트 추가
- Compose UI 테스트 추가
- 기기 회전과 프로세스 재생성 상태 복원 검증

## 관련 문서

- [요구사항](./docs/requirements.md)
- [기획](./docs/planning.md)
- [설계](./docs/design.md)
- [DB 스키마 초안](./docs/db-schema-draft.md)
- [이슈 정리](./docs/issues.md)
- [개발 TODO](./docs/development-todo.md)
- [정책 결정 기록](./docs/open-questions.md)
- [하네스 엔지니어링 설계](./docs/harness-engineering-design.md)
- [Play 내부 테스트 배포 절차](./docs/play-internal-test-release.md)
- [Play Store 등록 정보](./docs/store-listing.md)
- [개인정보처리방침](./docs/privacy-policy.md)
