# Pulse Log DB 스키마 초안

## 1. 목적

현재 구현과 정책 기준으로 Room DB 구조를 유지/보완하기 위한 문서다.

## 2. 권장 테이블 구성

### 2.1 `daily_health_records`

- `date_iso TEXT PRIMARY KEY`
- `morning_systolic INTEGER NULL`
- `morning_diastolic INTEGER NULL`
- `morning_measured_at_epoch_ms INTEGER NULL`
- `evening_systolic INTEGER NULL`
- `evening_diastolic INTEGER NULL`
- `evening_measured_at_epoch_ms INTEGER NULL`
- `weight_kg REAL NULL`
- `weight_measured_at_epoch_ms INTEGER NULL`
- `created_at_epoch_ms INTEGER NOT NULL`
- `updated_at_epoch_ms INTEGER NOT NULL`

제약:

- 날짜당 1건
- 아침 혈압은 수축기/이완기가 함께 존재하거나 함께 NULL 이어야 한다.
- 저녁 혈압도 같은 규칙을 따른다.
- 체중은 nullable
- 수정 시 측정 시간은 기존 값을 유지해야 한다.

### 2.2 `daily_notes`

- `date_iso TEXT PRIMARY KEY`
- `note TEXT NOT NULL`
- `created_at_epoch_ms INTEGER NOT NULL`
- `updated_at_epoch_ms INTEGER NOT NULL`

비고:

- 메모는 혈압/체중과 독립적으로 저장한다.
- 메모 존재 여부는 캘린더 별표 마커 계산에도 사용된다.

### 2.3 `notification_settings`

- `id INTEGER PRIMARY KEY`
- `morning_enabled INTEGER NOT NULL`
- `morning_time TEXT NOT NULL`
- `evening_enabled INTEGER NOT NULL`
- `evening_time TEXT NOT NULL`
- `repeat_enabled INTEGER NOT NULL`
- `repeat_count INTEGER NOT NULL`
- `updated_at_epoch_ms INTEGER NOT NULL`

## 3. 조회 모델

### 3.1 캘린더 셀 조회 모델

- `dateIso`
- `status`
- `hasNote`

역할:

- 월력 셀 상태 색상 계산
- 메모 별표 표시
- 접근성 라벨 생성

### 3.2 그래프 포인트 조회 모델

- `dateIso`
- `morningSystolic`
- `morningDiastolic`
- `eveningSystolic`
- `eveningDiastolic`
- `weightKg`

## 4. 구현 시 주의점

- 선택한 날짜에 row가 없으면 신규 생성할 수 있어야 한다.
- 선택한 날짜에 row가 있으면 update 로 처리해야 한다.
- 메모는 미래 날짜도 허용된다.
- UI는 빠른 입력 중심으로 바뀌어도 데이터 모델은 날짜 단위 레코드 구조를 유지한다.
- 알림 설정은 현재 저장만 하고 있으며, 실제 스케줄링은 별도 계층에서 처리해야 한다.

## 5. 추가 보완 필요 사항

- Room migration 스크립트 설계
- 메모와 기록을 함께 읽는 월별 조인 쿼리 성능 점검
- 알림 시간 형식 검증을 DB 저장 전 단계에서 강제할지 검토
