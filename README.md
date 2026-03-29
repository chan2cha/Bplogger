# Bplogger

Bplogger는 Android용 혈압/체중 기록 앱입니다.  
아침 혈압, 저녁 혈압, 체중, 날짜 메모를 날짜 단위로 저장하고, 월별 캘린더와 최근 그래프로 상태를 확인하는 것을 목표로 합니다. 앱 첫 진입 시 오늘 날짜가 기본 선택되며, 메인 화면에서는 선택 날짜 기준으로 빠르게 입력하는 흐름을 지향합니다.

## 한눈에 보기

- 플랫폼: Android
- 주요 개발 언어: Kotlin
- UI: Jetpack Compose + Material 3
- 로컬 저장소: Room
- 비동기/상태 처리: Kotlin Coroutines + `StateFlow`
- 빌드 시스템: Gradle Kotlin DSL
- 코드 생성: KSP

## 현재 구현 범위

- 월별 캘린더 화면
- 메인 빠른 입력 화면
- 날짜별 4상태 표시
- 선택 날짜 상세 관리 화면
- 최근 7일 / 최근 30일 그래프 탭
- 알림 설정 저장 UI

## 현재 UX 방향

- 앱 첫 진입 시 오늘 날짜 기본 선택
- 메인 화면은 빠른 입력 중심
- 캘린더는 상태 확인 중심의 컴팩트한 월력 UI
- 날짜 탭의 1차 동작은 입력 대상 날짜 변경
- 상세 화면은 세부 수정과 관리용 보조 흐름

## 소스 기준 기술 스택

- Kotlin `2.2.10`
- Android Gradle Plugin `9.1.0`
- Jetpack Compose
- Material 3
- Room `2.7.0`
- Retrofit `2.11.0`
- Moshi `1.15.1`
- OkHttp `4.12.0`

## 현재 아키텍처

- `MainActivity`
  - DB / Repository / ViewModel 생성
- `BpViewModel`
  - 첫 진입 기본 날짜 상태
  - 선택 날짜 상태
  - 월 상태
  - 그래프 범위
  - 저장/수정/삭제 액션
- `BpRepository`
  - Room DAO 래핑
  - 월 상태 계산
  - 그래프 조회 데이터 변환
- `BpDao`
  - 날짜별 기록 / 메모 / 설정 저장

## 주요 정책

- 아침 혈압과 저녁 혈압은 독립 저장
- 앱 첫 진입 시 오늘 날짜 기본 선택
- 선택한 날짜에 기록이 없으면 신규 저장 가능
- 선택한 날짜에 기록이 있으면 수정 가능
- 메모는 과거/오늘/미래 날짜 모두 편집 가능
- 수정 시 기존 측정 시간 유지
- 그래프 범위는 최근 기준 이동 구간

## 실행 방법

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk
export PATH="$JAVA_HOME/bin:$PATH"
bash ./gradlew :app:compileDebugKotlin
```

## 관련 문서

- [요구사항](./docs/requirements.md)
- [기획](./docs/planning.md)
- [설계](./docs/design.md)
- [DB 스키마 초안](./docs/db-schema-draft.md)
- [이슈 정리](./docs/issues.md)
- [개발 TODO](./docs/development-todo.md)
- [정책 결정 기록](./docs/open-questions.md)
- [작업 소통 문서](./docs/communication-bridge.md)
