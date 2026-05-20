# Pulse Log 개발 TODO

이 문서는 Android 배포 유지 작업과 iOS 별도 배포 작업을 분리해서 관리한다. Android 앱은 배포 중이므로 iOS 작업 때문에 Android 소스, Gradle 설정, Room migration, Play 배포 산출물을 변경하지 않는다.

## P0 - Android 배포 안정성 유지

- 실제 기기에서 입력, 캘린더, 그래프, 설정, 내보내기 주요 흐름을 수동 검증한다.
- `scripts/check.ps1` 또는 `scripts/check.sh`로 로컬 체크가 실행되는지 확인한다.
- Android 알림 권한, 알림 예약, 기록 후 알림 생략 동작을 기기에서 확인한다.
- Room migration 테스트와 schema snapshot을 유지한다.
- Android 배포 산출물은 iOS 작업과 분리해서 관리한다.

완료 기준:

- 기존 Android 기능에 회귀가 없다.
- Play 배포용 산출물과 iOS 산출물이 섞이지 않는다.
- Android 문서와 iOS 문서의 책임 범위가 명확하다.

## P1 - iOS 배포 준비

- Apple Developer Program 가입 상태를 확인한다.
- macOS와 Xcode 빌드 환경을 준비한다.
- App Store Connect 앱 레코드, Bundle ID, Team, signing 설정을 준비한다.
- `shared` Kotlin Multiplatform 모듈 추가 위치를 확정한다.
- iOS 앱 구현 위치를 결정한다.
  - 기본안: `shared/` 공통 모듈 + `ios/` 디렉터리 Xcode 프로젝트
  - 대안: iOS 앱은 별도 저장소, shared framework는 별도 artifact로 배포
- 앱 이름, 아이콘, 스플래시, 개인정보 처리 문구를 Android와 맞춘다.
- TestFlight 내부 테스트 계획을 만든다.

완료 기준:

- iOS 앱을 Android 앱과 독립적으로 빌드할 수 있다.
- Android 앱 소스와 배포 설정에는 변경이 없다.
- TestFlight 업로드 전 필요한 Apple 계정/서명 정보가 준비된다.

## P1 - iOS 기능 동등성 MVP

- `shared` 공통 정책을 먼저 연결한다.
- 오늘 날짜 기본 선택
- 입력 탭: 아침 혈압, 저녁 혈압, 체중 저장/수정/삭제
- 삭제 확인 다이얼로그
- 캘린더 탭: 월력, 날짜 선택, 선택 날짜 요약
- 그래프 탭: 최근 7일 / 30일 혈압 및 체중 그래프
- 설정 화면: 아침/저녁 알림 on/off 및 시간 선택
- 내보내기 화면: CSV 공유
- 로컬 저장소: iOS 로컬 DB 또는 파일 기반 저장

완료 기준:

- Android의 핵심 사용자 흐름과 화면 구성이 iOS에서 동일하게 동작한다.
- iOS 앱은 네트워크 없이 주요 기능을 사용할 수 있다.
- iOS 데이터는 기기 로컬에만 저장된다.

## P2 - iOS 하네스와 테스트

- 순수 정책 테스트를 `shared/commonTest`에 작성한다.
- iOS wrapper 상태 테스트는 XCTest로 작성한다.
- 저장소 테스트를 로컬 DB 계층에 추가한다.
- ViewModel 또는 상태 모델 테스트를 추가한다.
- 주요 UI 요소에 accessibility identifier를 부여한다.
- XCUITest smoke test를 추가한다.

완료 기준:

- 저장, 삭제, 캘린더 상태, 그래프 기간 계산, 알림 표시 조건을 자동 테스트로 확인한다.
- UI 테스트는 텍스트 문자열보다 안정적인 identifier를 우선 사용한다.

## P2 - iOS 알림과 내보내기 완성

- `UNUserNotificationCenter` 기반 로컬 알림을 구현한다.
- 알림 권한 요청 흐름을 설정 저장 흐름과 연결한다.
- 이미 해당 날짜의 아침/저녁 기록이 있으면 알림 표시를 생략한다.
- PDF 요약본 내보내기를 추가한다.
- iOS 공유 시트로 CSV/PDF를 전달한다.

완료 기준:

- 알림 예약, 권한, 표시 생략 동작이 실제 iOS 기기에서 검증된다.
- CSV/PDF 공유가 Mail, Files, Drive 등 일반 공유 대상에서 동작한다.

## P3 - App Store 심사 준비

- 개인정보 처리방침을 iOS 포함 문구로 갱신한다.
- App Privacy 응답을 로컬 저장, 사용자 입력 건강 데이터 기준으로 작성한다.
- 앱 설명, 스크린샷, 키워드, 지원 URL을 준비한다.
- TestFlight 내부 테스트 후 외부 테스트 또는 App Review를 진행한다.

완료 기준:

- App Store Connect 제출 정보가 앱 실제 동작과 일치한다.
- 건강 관련 데이터가 서버로 전송되지 않는다는 설명이 앱, 문서, 개인정보 처리방침에서 일관된다.
