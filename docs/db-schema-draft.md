# Bplogger DB 스키마 초안

## 1. 목적

최신 정책 기준으로 Room DB 재설계 방향을 정리한다.
현재 UX가 메인 입력 우선으로 바뀌더라도 데이터 모델은 날짜 단위 구조를 유지한다.

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
- 아침 혈압은 수축기/이완기가 함께 존재하거나 함께 NULL 이어야 한다
- 저녁 혈압도 같은 규칙을 따른다
- 체중은 nullable

비고:

- 기록이 없는 날짜에만 신규 생성하고, 기록이 있는 날짜는 update 로 처리하는 정책은 도메인 서비스에서 처리한다.
- 수정 시 측정 시간은 기존 값을 유지해야 한다.
- 앱 첫 진입 시 오늘 날짜를 기본 선택하는 정책은 DB가 아니라 UI 상태 계층에서 관리한다.

### 2.2 `daily_notes`

- `date_iso TEXT PRIMARY KEY`
- `note TEXT NOT NULL`
- `created_at_epoch_ms INTEGER NOT NULL`
- `updated_at_epoch_ms INTEGER NOT NULL`

### 2.3 `notification_settings`

- `id INTEGER PRIMARY KEY`
- `morning_enabled INTEGER NOT NULL`
- `morning_time TEXT NOT NULL`
- `evening_enabled INTEGER NOT NULL`
- `evening_time TEXT NOT NULL`
- `repeat_enabled INTEGER NOT NULL`
- `repeat_count INTEGER NOT NULL`
- `updated_at_epoch_ms INTEGER NOT NULL`

## 3. 구현 시 주의점

- 선택한 날짜에 row가 없으면 신규 생성할 수 있어야 한다.
- 선택한 날짜에 row가 있으면 update 로 처리해야 한다.
- 수정 시 기존 측정 시간은 유지해야 한다.
- 메모는 미래 날짜도 허용된다.
- 빠른 입력 중심 UX로 바뀌어도 데이터 저장 모델 자체는 날짜 단위 레코드 구조를 유지한다.
