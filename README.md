# Pulse Log

Pulse Log는 날짜별 아침 혈압, 저녁 혈압, 체중을 빠르게 기록하고 캘린더와 그래프로 확인하는 건강 기록 앱입니다.

현재 배포 기준 앱은 Android 네이티브 앱입니다. iOS 배포는 Android 소스와 배포 흐름에 영향을 주지 않는 별도 트랙으로 설계합니다.

## 현재 Android 앱

- 플랫폼: Android
- 언어: Kotlin
- UI: Jetpack Compose + Material 3
- 로컬 저장소: Room
- 상태 관리: Kotlin Coroutines + `StateFlow`
- 빌드: Gradle Kotlin DSL
- 코드 생성: KSP

## 현재 기능

- 첫 진입 시 오늘 날짜 자동 선택
- 입력, 캘린더, 그래프 중심의 메인 탭
- 선택 날짜 기준 아침 혈압, 저녁 혈압, 체중 저장/수정/삭제
- 삭제 확인 다이얼로그
- 월별 캘린더 상태 표시
- 최근 7일 / 최근 30일 혈압 및 체중 그래프
- 그래프에서 선택한 날짜를 캘린더와 공유
- 아침/저녁 알림 on/off 및 시간 설정
- 실제 OS 알림 예약
- 이미 해당 날짜 기록이 있으면 해당 알림 표시 생략
- 최근 30일 / 전체 기록 CSV 내보내기
- 최근 30일 / 전체 기록 PDF 요약본 내보내기

## iOS 배포 방향

iOS 앱은 Android 앱과 같은 사용자 경험을 목표로 하되, 장기 유지보수 비용을 줄이기 위해 Kotlin Multiplatform 공통 모듈을 둡니다.

- Android 앱은 기존 Kotlin/Compose/Room 구조를 유지합니다.
- `shared` 모듈에 모델, 검증, 캘린더, 그래프, 내보내기, 알림 판단 정책을 둡니다.
- iOS 앱은 `shared` 공통 로직을 호출합니다.
- UI는 첫 단계에서 플랫폼별로 유지하고, 이후 Compose Multiplatform 공유를 검토합니다.
- UI 흐름, 화면 구성, 컬러 톤, 문구, 데이터 모델은 Android 앱을 기준으로 맞춥니다.
- Android Room DB migration과 Play Store 배포 흐름은 iOS 작업 범위에서 제외합니다.

자세한 계획은 [iOS 배포 설계](./docs/ios-distribution-plan.md)를 기준으로 합니다.
공통 코드 전략은 [Android/iOS 공통 코드 설계](./docs/shared-kmp-architecture.md)를 기준으로 합니다.

## 문서

- [요구사항](./docs/requirements.md)
- [설계](./docs/design.md)
- [iOS 배포 설계](./docs/ios-distribution-plan.md)
- [Android/iOS 공통 코드 설계](./docs/shared-kmp-architecture.md)
- [하네스 엔지니어링 설계](./docs/harness-engineering-design.md)
- [개발 TODO](./docs/development-todo.md)
- [이슈 정리](./docs/issues.md)
- [오픈 질문](./docs/open-questions.md)
- [릴리즈 체크리스트](./docs/release-checklist.md)
- [Play 내부 테스트 배포 절차](./docs/play-internal-test-release.md)
- [Play Store 등록 정보](./docs/store-listing.md)
- [개인정보처리방침](./docs/privacy-policy.md)

## Android 로컬 체크

Repository root에서 실행합니다.

```powershell
.\scripts\check.ps1
```

또는:

```bash
bash ./scripts/check.sh
```

Gradle 체크 전 `JAVA_HOME`이 유효한 JDK를 가리키고 `java`가 `PATH`에 있어야 합니다.

## 현재 제한 사항

- iOS 앱은 아직 구현되지 않았습니다.
- iOS 배포에는 Apple Developer Program, macOS, Xcode, App Store Connect 설정이 필요합니다.
- 실제 Android 알림은 기기 제조사별 배터리 정책에 따라 지연될 수 있어 수동 검증이 필요합니다.
- Android Room v1/v2 schema artifact는 없고 v3 이후 migration을 기준으로 보존합니다.
