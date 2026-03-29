# Pulse Log

Pulse Log는 Android용 혈압/체중 기록 앱입니다.
아침 혈압, 저녁 혈압, 체중, 날짜 메모를 날짜 단위로 저장하고 월별 캘린더와 최근 그래프로 확인하는 흐름을 제공합니다.

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
- 캘린더 중심 메인 화면
- 선택 날짜 기준 빠른 입력
- 날짜별 상세 관리 화면
- 최근 7일 / 최근 30일 그래프
- 알림 설정 저장 UI
- 저장/수정/삭제 메시지 스낵바 처리
- 메모가 있는 날짜 달력 별표 표시
- 선택 날짜 메모 요약 노출

## 현재 UX 방향

- 첫 화면 기본 탭은 캘린더다.
- 앱 첫 진입 시 오늘 날짜를 기본 선택한다.
- 메인 입력은 선택 날짜 기준으로 동작한다.
- 캘린더는 상태 확인 중심의 컴팩트한 월력 UI다.
- 날짜 탭의 기본 동작은 상세 진입이 아니라 입력 대상 날짜 변경이다.
- 상세 화면은 세부 수정과 관리용 보조 흐름이다.

## 현재 화면 구조

- 캘린더 탭
  - 프리미엄 톤 상단 월력 카드
  - 선택 날짜 요약
  - 메모 미리보기
  - 빠른 입력 카드
- 그래프 탭
  - 최근 7일 / 최근 30일 전환
  - 혈압 2라인 그래프
  - 체중 그래프
- 설정 탭
  - 아침/저녁 알림 사용 여부
  - 알림 시간 입력
  - 재알림 여부 / 횟수 입력
- 날짜별 상세 화면
  - 혈압/체중 수정 및 삭제
  - 메모 편집 및 삭제

## 코드 구조

- [MainActivity](/Users/chan/workspace/projects/Bplogger/app/src/main/java/com/example/bplogger/MainActivity.kt)
  - DB / Repository / ViewModel 연결
- [BloodPressureScreen.kt](/Users/chan/workspace/projects/Bplogger/app/src/main/java/com/example/bplogger/ui/BloodPressureScreen.kt)
  - 상단 헤더와 메인 탭 진입점
- [CalendarTab.kt](/Users/chan/workspace/projects/Bplogger/app/src/main/java/com/example/bplogger/ui/CalendarTab.kt)
  - 캘린더 탭 상태 조합
- [CalendarComponents.kt](/Users/chan/workspace/projects/Bplogger/app/src/main/java/com/example/bplogger/ui/CalendarComponents.kt)
  - 월력 카드, 날짜 셀, 상태 인디케이터
- [QuickEntryComponents.kt](/Users/chan/workspace/projects/Bplogger/app/src/main/java/com/example/bplogger/ui/QuickEntryComponents.kt)
  - 빠른 입력 카드, 입력 필드, 저장 버튼
- [DayDetailScreen.kt](/Users/chan/workspace/projects/Bplogger/app/src/main/java/com/example/bplogger/ui/DayDetailScreen.kt)
  - 날짜별 상세 관리 화면
- [GraphScreen.kt](/Users/chan/workspace/projects/Bplogger/app/src/main/java/com/example/bplogger/ui/GraphScreen.kt)
  - 그래프 탭
- [SettingsScreen.kt](/Users/chan/workspace/projects/Bplogger/app/src/main/java/com/example/bplogger/ui/SettingsScreen.kt)
  - 설정 탭
- [UiChrome.kt](/Users/chan/workspace/projects/Bplogger/app/src/main/java/com/example/bplogger/ui/UiChrome.kt)
  - 공통 헤더 / 포커스 해제 modifier
- [UiTokens.kt](/Users/chan/workspace/projects/Bplogger/app/src/main/java/com/example/bplogger/ui/UiTokens.kt)
  - 공통 색상 토큰, 포맷터, 상태 헬퍼

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

- 아침 혈압과 저녁 혈압은 독립 저장
- 혈압과 체중은 선택한 날짜에 신규 추가 또는 수정 가능
- 메모는 과거/오늘/미래 날짜 모두 편집 가능
- 수정 시 기존 측정 시간 유지
- 그래프는 최근 기준으로 최근 7일 / 최근 30일 제공
- 달력 상태는 `없음 / 아침만 / 저녁만 / 모두 기록` 4단계
- 메모가 있는 날짜는 달력에 별표 표시

## 실행 방법

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk
export PATH="$JAVA_HOME/bin:$PATH"
bash ./gradlew :app:compileDebugKotlin
```

APK 빌드는 아래 명령으로 가능합니다.

```bash
bash ./gradlew :app:assembleDebug
```

## 현재 미구현 / 제한 사항

- 실제 OS 알림 스케줄링은 아직 미구현
- DB는 `fallbackToDestructiveMigration()` 상태라 스키마 변경 시 기존 데이터 손실 가능
- 그래프는 Compose Canvas 기반의 단순 라인 차트라 축/범례/줌 기능 없음
- 앱 내부 표시명은 `Pulse Log`로 바뀌었지만 런처 앱명과 아이콘 정리는 별도 작업 필요

## 추가 보완 필요 사항

- Room 마이그레이션 전략 수립
- 알림 시간 유효성 검증 강화
- 설정 탭 시간 입력 UX 개선
- 상세 화면도 빠른 입력과 동일한 입력 컴포넌트 계열로 추가 통일
- UI 테스트 / ViewModel 테스트 추가
- 기기 회전과 프로세스 재생성 상태 복원 검증

## 관련 문서

- [요구사항](./docs/requirements.md)
- [기획](./docs/planning.md)
- [설계](./docs/design.md)
- [DB 스키마 초안](./docs/db-schema-draft.md)
- [이슈 정리](./docs/issues.md)
- [개발 TODO](./docs/development-todo.md)
- [정책 결정 기록](./docs/open-questions.md)
- [작업 소통 문서](./docs/communication-bridge.md)
