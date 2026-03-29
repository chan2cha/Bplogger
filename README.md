# Bplogger

Bplogger는 Android용 혈압/체중 기록 앱입니다.  
아침 혈압, 저녁 혈압, 체중, 날짜 메모를 날짜 단위로 저장하고, 월별 캘린더와 최근 그래프로 상태를 확인하는 것을 목표로 합니다.

현재 소스는 Android 단일 모듈 프로젝트이며, UI는 Jetpack Compose로 작성되어 있습니다.

## 한눈에 보기

- 플랫폼: Android
- 주요 개발 언어: Kotlin
- UI: Jetpack Compose + Material 3
- 로컬 저장소: Room
- 비동기/상태 처리: Kotlin Coroutines + `StateFlow`
- 빌드 시스템: Gradle Kotlin DSL (`build.gradle.kts`)
- 코드 생성: KSP
- 최소 SDK: Android 8.0 (`minSdk 26`)
- 타깃 SDK: Android 16 계열 SDK (`targetSdk 36`)

## 현재 구현 범위

- Jetpack Compose 기반 앱 화면
- Room 기반 로컬 저장
- 월별 캘린더 화면
- 날짜별 4상태 표시
  - 기록 없음
  - 아침만 기록
  - 저녁만 기록
  - 아침/저녁 모두 기록
- 선택 날짜 상세 화면
  - 아침 혈압 저장/수정/삭제
  - 저녁 혈압 저장/수정/삭제
  - 체중 저장/수정/삭제
  - 날짜 메모 저장/수정/삭제
- 최근 7일 / 최근 30일 그래프 탭
- 알림 설정 저장 UI

## 아직 미구현인 항목

- 실제 알림 스케줄링
  - 현재는 알림 설정 저장 UI와 DB 저장까지만 구현되어 있습니다.
- 정식 DB 마이그레이션
  - 현재는 `fallbackToDestructiveMigration()` 상태입니다.
- 테스트 코드 보강
  - 단위 테스트와 계측 테스트가 거의 없는 상태입니다.
- 서버 시간 연동
  - Retrofit `TimeApi` 인터페이스는 있으나 실제 사용되지는 않습니다.

## 소스 기준 기술 스택

### 언어 및 빌드

- Kotlin `2.2.10`
- Android Gradle Plugin `9.1.0`
- Gradle Kotlin DSL
- KSP `2.0.21-1.0.25`

### Android / UI

- Jetpack Compose BOM `2024.09.00`
- Material 3
- `androidx.activity:activity-compose`
- `androidx.lifecycle:lifecycle-runtime-ktx`

### 데이터 계층

- Room `2.7.0`
- Room KTX
- KSP 기반 Room Compiler

### 네트워크 계층

- Retrofit `2.11.0`
- Moshi `1.15.1`
- OkHttp `4.12.0`

현재 네트워크 계층은 소스에 존재하지만, 앱의 핵심 동작은 로컬 저장 기준으로만 동작합니다.

## 현재 아키텍처

현재 구조는 전형적인 Android MVVM에 가까운 단순 구조입니다.

- `MainActivity`
  - Room DB 생성
  - Repository 생성
  - ViewModel 생성
  - Compose 화면 진입
- `BpViewModel`
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
- `BloodPressureScreen`
  - 캘린더 / 그래프 / 설정 UI

의존성 주입 프레임워크(Hilt, Koin 등)는 아직 사용하지 않고 있습니다.

## 데이터 모델 개요

현재 저장 구조는 `날짜 단위 레코드` 기준입니다.

- `DailyHealthRecord`
  - 날짜별 건강 기록
  - 아침 혈압
  - 저녁 혈압
  - 체중
  - 각 항목의 측정 시각
- `DailyNote`
  - 날짜별 메모
- `NotificationSettings`
  - 아침/저녁 알림 사용 여부
  - 아침/저녁 알림 시간
  - 재알림 여부와 횟수

## 주요 정책

- 아침 혈압과 저녁 혈압은 묶음 세트가 아니라 각각 독립 저장
- 선택한 날짜에 기록이 없으면 신규 저장 가능
- 선택한 날짜에 기록이 있으면 수정 가능
- 메모는 과거/오늘/미래 날짜 모두 편집 가능
- 혈압 허용 범위: `50..200`
- 체중 허용 범위: `0..100kg`
- 체중은 소수점 둘째 자리 반올림
- 수정 시 기존 측정 시간 유지
- 그래프 범위는 최근 기준 이동 구간
  - 최근 7일
  - 최근 30일

## 프로젝트 구조

```text
.
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/bplogger/
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── AppDatabase.kt
│       │   │   ├── BpDao.kt
│       │   │   └── BpRecord.kt
│       │   ├── network/
│       │   │   └── TimeApi.kt
│       │   └── ui/
│       │       ├── BloodPressureScreen.kt
│       │       ├── BpRepository.kt
│       │       ├── BpViewModel.kt
│       │       └── TimeUtils.kt
│       └── res/
├── docs/
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 실행 환경

이 프로젝트를 로컬에서 실행하려면 아래 환경이 필요합니다.

- Java
  - 현재 확인 기준: Homebrew OpenJDK `25`
- Android SDK
  - `platforms;android-36`
  - `build-tools;36.0.0`
  - `platform-tools`
- Android Studio
  - 설치되어 있으면 가장 쉽게 실행할 수 있습니다.

## 실행 방법

### 1. Android Studio로 실행

1. `Android Studio`에서 이 폴더를 엽니다.
2. Gradle Sync를 완료합니다.
3. 에뮬레이터 또는 실제 Android 기기를 선택합니다.
4. Run으로 실행합니다.

### 2. 터미널에서 빌드

`JAVA_HOME`이 설정되지 않았다면 먼저 지정합니다.

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk
export PATH="$JAVA_HOME/bin:$PATH"
```

디버그 컴파일:

```bash
bash ./gradlew :app:compileDebugKotlin
```

디버그 APK 생성:

```bash
bash ./gradlew :app:assembleDebug
```

기기 설치:

```bash
bash ./gradlew :app:installDebug
```

## AndroidManifest 기준 참고 사항

- `INTERNET` 권한이 선언되어 있습니다.
- 현재 핵심 기능은 로컬 저장만 사용합니다.
- 따라서 이 권한은 향후 시간 API 또는 서버 연동 준비 흔적으로 보는 편이 맞습니다.

## 관련 문서

- [요구사항](./docs/requirements.md)
- [기획](./docs/planning.md)
- [설계](./docs/design.md)
- [DB 스키마 초안](./docs/db-schema-draft.md)
- [이슈 정리](./docs/issues.md)
- [개발 TODO](./docs/development-todo.md)
- [정책 결정 기록](./docs/open-questions.md)

## 현재 확인 상태

- `:app:compileDebugKotlin` 성공
- Android Studio 설치 완료
- Android SDK command line tools 설치 완료
- Java 설치 및 빌드 가능 상태 확인

## 참고

이 저장소는 현재 Android 프로젝트입니다.  
iOS 앱 소스는 포함되어 있지 않습니다. 따라서 Xcode나 CocoaPods를 설치하더라도, 이 저장소 자체는 Android 앱 기준으로만 바로 실행할 수 있습니다.
