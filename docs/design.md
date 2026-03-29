# Bplogger 설계 정리본

## 1. 설계 요약

`open-questions.md` 반영 후 기준 설계는 `아침+저녁 묶음 세트` 모델이 아니다.
권장 구조는 `날짜 단위 건강 기록` 1건 안에 아침 혈압, 저녁 혈압, 체중을 각각 독립 필드로 보관하고, 날짜 메모는 별도 테이블로 분리하는 방식이다.

핵심 설계 원칙:

- 저장 단위는 날짜 단위다.
- 아침 혈압과 저녁 혈압은 독립적으로 저장/수정/삭제된다.
- 체중은 선택 입력이며 날짜 단위로 저장된다.
- 메모는 별도 날짜 단위 리소스다.
- 혈압/체중은 선택한 날짜에 기록이 없으면 신규 저장, 있으면 수정한다.
- 수정 시 기존 측정 시간은 유지한다.

## 2. 현재 구조와 요구사항의 차이

현재 구현은 `오늘`, `AM/PM 슬롯`, `혈압 중심 단일 화면` 구조다.
새 정책을 만족하려면 아래가 바뀌어야 한다.

- 슬롯 중심 저장에서 날짜 단위 건강 기록 모델로 전환
- 단일 화면 구조에서 캘린더/상세/그래프/설정 구조로 확장
- 오늘 고정 조회에서 월별/일별 탐색 기반 조회로 전환
- 값 단순 저장에서 검증, 수정, 삭제, 알림 설정까지 포함하는 구조로 전환

## 3. 권장 화면 구조

### 3.1 메인 화면

- 탭 2개 구성
  - 캘린더
  - 그래프

주요 상태:

- 현재 보고 있는 연월
- 선택 날짜
- 해당 월 날짜 상태 요약
- 선택 날짜 입력 폼 상태

### 3.2 캘린더 탭

역할:

- 월 단위 날짜 탐색
- 날짜 상태 시각화
- 선택 날짜 데이터 입력
- 특정 날짜 상세 진입

주요 UI 블록:

- 연월 헤더
- 월력 그리드
- 날짜 상태 마커
- 선택 날짜 입력 폼
- 선택 날짜 액션

상태 표현 기본 규칙:

- `NONE`: 중립색
- `MORNING_ONLY`: 아침 강조색
- `EVENING_ONLY`: 저녁 강조색
- `BOTH`: 완료색

추가 요구:

- 색상만으로 상태를 구분하지 않는다.
- 날짜 셀의 `contentDescription` 또는 별도 라벨로 상태를 함께 제공한다.

### 3.3 일별 상세 화면

역할:

- 선택 날짜의 아침 혈압 확인/수정/삭제
- 선택 날짜의 저녁 혈압 확인/수정/삭제
- 선택 날짜의 체중 확인/수정/삭제
- 날짜 메모 확인/수정/삭제

주요 UI 블록:

- 날짜 헤더
- 아침 혈압 카드
- 저녁 혈압 카드
- 체중 카드
- 메모 카드

### 3.4 그래프 탭

역할:

- 최근 7일 기본 그래프 표시
- 최근 7일/최근 30일 전환
- 혈압 2라인 그래프 표시
- 체중 그래프 표시

### 3.5 알림 설정 화면

역할:

- 아침 알림 시간 설정
- 저녁 알림 시간 설정
- 재알림 여부 설정
- 재알림 횟수 설정

## 4. 권장 데이터 모델

### 4.1 DailyHealthRecord

날짜별 건강 기록 엔티티

- `dateIso: String`
- `morningSystolic: Int?`
- `morningDiastolic: Int?`
- `morningMeasuredAtEpochMs: Long?`
- `eveningSystolic: Int?`
- `eveningDiastolic: Int?`
- `eveningMeasuredAtEpochMs: Long?`
- `weightKg: Double?`
- `weightMeasuredAtEpochMs: Long?`
- `createdAtEpochMs: Long`
- `updatedAtEpochMs: Long`

제약:

- 날짜당 1건
- 아침 혈압은 수축기/이완기가 함께 존재하거나 함께 비어 있어야 한다.
- 저녁 혈압도 같은 규칙을 따른다.
- 체중은 nullable
- 수정 시 기존 측정 시간은 유지한다.

### 4.2 DailyNote

- `dateIso: String`
- `note: String`
- `createdAtEpochMs: Long`
- `updatedAtEpochMs: Long`

제약:

- 날짜당 1건

### 4.3 NotificationSettings

- `id: Int`
- `morningEnabled: Boolean`
- `morningTime: String`
- `eveningEnabled: Boolean`
- `eveningTime: String`
- `repeatEnabled: Boolean`
- `repeatCount: Int`
- `updatedAtEpochMs: Long`

### 4.4 CalendarDayStatus

화면 렌더링용 조회 모델

- `dateIso: String`
- `hasMorningRecord: Boolean`
- `hasEveningRecord: Boolean`
- `hasWeight: Boolean`
- `hasNote: Boolean`
- `status: DayRecordStatus`

`DayRecordStatus`는 아래 4가지 값으로 구성한다.

- `NONE`
- `MORNING_ONLY`
- `EVENING_ONLY`
- `BOTH`

## 5. 저장 정책

- 선택 날짜에 기록이 없으면 신규 혈압/체중 저장 허용
- 선택 날짜에 기록이 있으면 update 경로 사용
- 메모는 과거/오늘/미래 모두 upsert 허용
- 저장 시 측정 시간은 기기 현재 시각으로 자동 기록
- 수정 시에는 기존 측정 시간을 유지한다

## 6. 검증 정책

- 혈압: 50~200
- 체중: 0~100kg
- 체중 저장값: 소수점 둘째 자리 반올림
- 비정상 값은 저장 차단
- UI 검증과 도메인 검증을 모두 둔다
- 그래프 기간은 최근 7일 또는 최근 30일의 이동 구간 기준으로 조회한다

## 7. 조회 정책

필수 조회:

- 월 단위 날짜 상태 조회
- 특정 날짜 상세 조회
- 그래프용 기간 조회
- 오늘 아침/저녁 기록 존재 여부 조회
- 알림 설정 조회

권장 DAO 예시:

- `observeMonthStatuses(yearMonth)`
- `observeDailyRecord(dateIso)`
- `upsertDailyRecord(record)`
- `clearMorningRecord(dateIso)`
- `clearEveningRecord(dateIso)`
- `clearWeight(dateIso)`
- `observeDailyNote(dateIso)`
- `upsertDailyNote(note)`
- `deleteDailyNote(dateIso)`
- `observeGraphRange(fromDateIso, toDateIso)`
- `observeNotificationSettings()`
- `upsertNotificationSettings(settings)`

## 8. 상태 관리 제안

- `CalendarViewModel`
- `DayDetailViewModel`
- `GraphViewModel`
- `SettingsViewModel`

공통 서비스:

- 기록 저장/수정/삭제 서비스
- 날짜 상태 계산 서비스
- 그래프 변환 서비스
- 알림 스케줄링 서비스

## 9. 현재 코드 기준 반드시 바뀌는 부분

- `BpRecord` 스키마
- `BpDao` 쿼리 구조
- `BpViewModel` 책임
- `BloodPressureScreen` 단일 화면 구조
- `TimeUtils`의 AM/PM 슬롯 전제
- `MainActivity`의 직접 객체 생성 구조
- 알림 처리 구조 전반

## 10. 설계 리스크

- 기존 today-slot 구조에서 날짜 단위 레코드 구조로의 마이그레이션이 필요하다.
- 메모는 미래 날짜까지 허용되므로 상세 화면 저장 정책이 혈압/체중과 다르다.
- 캘린더 4상태 색상은 실제 브랜드 톤에 맞게 최종 디자인 치환이 필요하다.
