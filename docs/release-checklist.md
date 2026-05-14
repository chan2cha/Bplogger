# Pulse Log 릴리즈 전 체크리스트

## 1. 목적

릴리즈 또는 내부 테스트 배포 전에 기능, 데이터 보존, 알림, 내보내기, 빌드 산출물을 같은 기준으로 확인한다.

## 2. 자동 검증

Repository root에서 실행한다.

```powershell
.\scripts\check.ps1
```

또는:

```bash
bash ./scripts/check.sh
```

기기 또는 에뮬레이터 연결 상태에서 connected test까지 확인한다.

```powershell
$env:RUN_CONNECTED_TESTS='1'
.\scripts\check.ps1
```

또는:

```bash
RUN_CONNECTED_TESTS=1 bash ./scripts/check.sh
```

완료 기준:

- unit test 통과
- `lintDebug` 통과
- `assembleDebug` 통과
- `assembleDebugAndroidTest` 통과
- connected Android test 통과

## 3. 알림 수동 검증

Android 13 이상 기기에서는 알림 권한 요청까지 함께 확인한다.

- 설정 화면을 연다.
- 아침 또는 저녁 알림 시간을 현재 시간에서 가까운 값으로 설정한다.
- 설정 저장을 누른다.
- 알림 권한 요청이 뜨면 허용한다.
- 설정한 시간에 알림이 표시되는지 확인한다.
- 해당 날짜의 아침 또는 저녁 기록을 저장한 뒤 같은 알림이 생략되는지 확인한다.
- 재알림을 1회 이상으로 켠 뒤 첫 알림 이후 10분 간격 재알림이 예약되는지 확인한다.

남은 수동 리스크:

- 기기 제조사별 배터리 최적화 정책에 따라 `AlarmManager` 동작이 지연될 수 있다.
- 재부팅 후 재예약은 `BOOT_COMPLETED` 수신 가능 상태에서 별도 확인한다.

## 4. 데이터 보존 검증

- `app/schemas/com.pulselog.data.AppDatabase/3.json`과 `4.json`이 유지되는지 확인한다.
- `AppDatabaseMigrations.ALL`에 production migration이 등록되어 있는지 확인한다.
- `AppDatabaseMigrationTest`가 v3 -> v4 migration을 검증하는지 확인한다.
- destructive migration fallback이 production database builder에 없는지 확인한다.

## 5. 주요 UX 수동 검증

- 앱 첫 진입 시 오늘 날짜 기준 입력 탭이 열린다.
- 아침 혈압, 저녁 혈압, 체중을 저장/수정/삭제할 수 있다.
- 삭제는 확인 다이얼로그를 거친다.
- 캘린더에서 날짜 선택 후 입력 탭 이동이 자연스럽다.
- 그래프에서 날짜 선택 후 캘린더 이동이 선택 날짜를 유지한다.
- 설정 화면은 아침 알림, 저녁 알림, 재알림, 저장 카드가 분리되어 보인다.
- 시간 표시 버튼을 누르면 아침/저녁 범위에 맞는 시간 선택 다이얼로그가 열린다.
- CSV/PDF 공유가 Android 공유 시트를 연다.

## 6. 빌드 산출물 확인

- release AAB는 `app/build/outputs/bundle/release/app-release.aab`에서 생성한다.
- `app/release/app-release.aab` 같은 수동 복사 산출물은 의도한 배포 파일인지 확인한다.
- 의도한 산출물이 아니면 커밋에 포함하지 않는다.
- Play 업로드 전 `versionCode` 증가 여부를 확인한다.
- `keystore.properties`, `*.jks`, `*.keystore`가 git에 포함되지 않는지 확인한다.

## 7. 문서 확인

- README의 현재 기능/제한 사항이 실제 앱 상태와 맞는지 확인한다.
- `docs/requirements.md`의 구현 상태가 현재 릴리즈와 충돌하지 않는지 확인한다.
- `docs/play-internal-test-release.md` 절차에 따라 내부 테스트 업로드 산출물을 준비한다.
