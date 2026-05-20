# Pulse Log 릴리즈 체크리스트

Android와 iOS 릴리즈는 서로 독립적으로 검증한다. Android 앱은 배포 중이므로 iOS 작업으로 Android 산출물, 서명, Gradle 설정, Room migration을 변경하지 않는다.

## 1. 공통 문서 확인

- README의 플랫폼 상태가 실제와 일치한다.
- `docs/requirements.md`가 Android와 iOS 범위를 분리해서 설명한다.
- `docs/design.md`가 현재 UI 흐름과 맞다.
- `docs/ios-distribution-plan.md`가 iOS 배포 계획의 기준이다.
- 개인정보처리방침이 현재 배포 플랫폼과 데이터 처리 방식을 반영한다.

## 2. Android 자동 검증

Repository root에서 실행한다.

```powershell
.\scripts\check.ps1
```

또는:

```bash
bash ./scripts/check.sh
```

완료 기준:

- unit test 통과
- `lintDebug` 통과
- `assembleDebug` 통과
- `assembleDebugAndroidTest` 통과

기기 또는 에뮬레이터 연결 상태에서는 connected test도 확인한다.

```powershell
$env:RUN_CONNECTED_TESTS='1'
.\scripts\check.ps1
```

## 3. Android 수동 검증

- 첫 진입 시 오늘 날짜 기준 입력 탭이 열린다.
- 아침 혈압, 저녁 혈압, 체중 저장/수정/삭제가 동작한다.
- 삭제는 확인 다이얼로그를 거친다.
- 캘린더 날짜 선택이 입력과 그래프에 반영된다.
- 그래프에서 날짜 선택 후 캘린더 이동 시 같은 날짜가 선택된다.
- 설정 화면에서 아침/저녁 알림 on/off와 시간 선택이 가능하다.
- 저장 시 알림 예약이 갱신된다.
- Android 13 이상에서 알림 권한 요청 흐름이 동작한다.
- CSV/PDF 공유가 Android 공유 시트를 연다.

## 4. Android 데이터 보존 검증

- `app/schemas/com.pulselog.data.AppDatabase/3.json`과 `4.json`이 유지된다.
- `AppDatabaseMigrations.ALL`이 production database builder에 연결되어 있다.
- `AppDatabaseMigrationTest`가 v3 -> v4 migration을 검증한다.
- destructive migration fallback이 production database builder에 없다.
- 기존 기록 삭제를 유발하는 migration 변경이 없다.

## 5. Android 배포 산출물 확인

- release AAB 생성 위치를 확인한다.
- Play 업로드 전 `versionCode` 증가 여부를 확인한다.
- `keystore.properties`, `*.jks`, `*.keystore`가 git에 포함되지 않는다.
- `app/release`의 수동 복사 산출물은 의도한 배포 파일인지 확인한다.

## 6. iOS TestFlight 전 검증

- Apple Developer Program 가입과 Team 설정이 준비됐다.
- macOS와 Xcode에서 iOS 프로젝트가 열린다.
- Bundle ID와 signing 설정이 맞다.
- `shared` Kotlin Multiplatform 모듈의 common test가 통과한다.
- iOS 앱은 Android 배포 동작을 깨지 않고 빌드된다.
- 핵심 단위 테스트가 통과한다.
- 저장소 테스트가 통과한다.
- UI smoke test가 통과한다.
- 실제 iPhone에서 입력, 캘린더, 그래프, 설정, 내보내기를 수동 검증했다.
- 알림 권한 요청과 로컬 알림 예약을 수동 검증했다.

## 7. iOS App Store 제출 전 검증

- TestFlight 내부 테스트 피드백을 확인했다.
- 앱 이름, 아이콘, 스크린샷, 설명, 키워드를 준비했다.
- 개인정보처리방침 URL을 준비했다.
- App Privacy 응답이 실제 데이터 처리와 일치한다.
- 건강 관련 데이터가 자체 서버로 전송되지 않는다는 설명이 앱 설명과 개인정보처리방침에서 일관된다.
- CSV/PDF 공유 시 사용자가 선택한 외부 앱으로 데이터가 전달될 수 있음을 설명했다.

## 8. 릴리즈 차단 조건

- Android 릴리즈에서 기존 로컬 데이터가 손실될 수 있는 migration 변경이 테스트 없이 포함된 경우
- iOS 릴리즈에서 건강 데이터 처리 설명과 실제 동작이 다른 경우
- 알림 권한 거부 상태에서 앱 핵심 기능이 막히는 경우
- 저장/삭제/내보내기 중 앱이 종료되거나 데이터가 깨지는 경우
- Android 배포 산출물과 iOS 산출물이 같은 경로에서 혼동되는 경우
