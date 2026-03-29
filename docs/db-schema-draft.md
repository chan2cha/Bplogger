# Bplogger DB 스키마 초안

## 1. 목적

최신 정책 기준으로 Room DB 재설계 방향을 정리한다.
이 문서는 `세트` 모델 대신 `날짜 단위 건강 기록` 모델을 기준으로 작성한다.

## 2. 권장 테이블 구성

### 2.1 `daily_health_records`

날짜 단위 혈압/체중 기록 테이블

필드:

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

- 기록이 없는 날짜에만 신규 생성하고, 기록이 있는 날짜는 update 로 처리하는 정책은 DB 제약보다는 도메인 서비스에서 처리하는 편이 안전하다.
- 수정 시 측정 시간은 기존 값을 유지해야 하므로 부분 update 시 시간 필드를 덮어쓰지 않도록 주의해야 한다.

### 2.2 `daily_notes`

날짜 단위 메모 테이블

필드:

- `date_iso TEXT PRIMARY KEY`
- `note TEXT NOT NULL`
- `created_at_epoch_ms INTEGER NOT NULL`
- `updated_at_epoch_ms INTEGER NOT NULL`

제약:

- 날짜당 메모 1건

### 2.3 `notification_settings`

알림 설정 테이블

필드:

- `id INTEGER PRIMARY KEY`
- `morning_enabled INTEGER NOT NULL`
- `morning_time TEXT NOT NULL`
- `evening_enabled INTEGER NOT NULL`
- `evening_time TEXT NOT NULL`
- `repeat_enabled INTEGER NOT NULL`
- `repeat_count INTEGER NOT NULL`
- `updated_at_epoch_ms INTEGER NOT NULL`

비고:

- 단일 row 설정이면 `id = 1` 고정 방식으로 단순화할 수 있다.

## 3. Room 엔티티 예시 구조

### 3.1 DailyHealthRecordEntity

```kotlin
@Entity(tableName = "daily_health_records")
data class DailyHealthRecordEntity(
    @PrimaryKey val dateIso: String,
    val morningSystolic: Int? = null,
    val morningDiastolic: Int? = null,
    val morningMeasuredAtEpochMs: Long? = null,
    val eveningSystolic: Int? = null,
    val eveningDiastolic: Int? = null,
    val eveningMeasuredAtEpochMs: Long? = null,
    val weightKg: Double? = null,
    val weightMeasuredAtEpochMs: Long? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
```

### 3.2 DailyNoteEntity

```kotlin
@Entity(tableName = "daily_notes")
data class DailyNoteEntity(
    @PrimaryKey val dateIso: String,
    val note: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
```

### 3.3 NotificationSettingsEntity

```kotlin
@Entity(tableName = "notification_settings")
data class NotificationSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val morningEnabled: Boolean,
    val morningTime: String,
    val eveningEnabled: Boolean,
    val eveningTime: String,
    val repeatEnabled: Boolean,
    val repeatCount: Int,
    val updatedAtEpochMs: Long
)
```

## 4. 조회 모델 초안

### 4.1 CalendarDayStatus

```kotlin
data class CalendarDayStatus(
    val dateIso: String,
    val hasMorningRecord: Boolean,
    val hasEveningRecord: Boolean,
    val hasWeight: Boolean,
    val hasNote: Boolean,
    val status: DayRecordStatus
)
```

### 4.2 GraphPoint

```kotlin
data class GraphPoint(
    val dateIso: String,
    val morningSystolic: Int?,
    val morningDiastolic: Int?,
    val eveningSystolic: Int?,
    val eveningDiastolic: Int?,
    val weightKg: Double?
)
```

## 5. DAO 초안

```kotlin
@Dao
interface HealthRecordDao {
    @Query("SELECT * FROM daily_health_records WHERE date_iso = :dateIso")
    fun observeDailyRecord(dateIso: String): Flow<DailyHealthRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyRecord(record: DailyHealthRecordEntity)

    @Query("UPDATE daily_health_records SET morning_systolic = NULL, morning_diastolic = NULL, morning_measured_at_epoch_ms = NULL, updated_at_epoch_ms = :updatedAt WHERE date_iso = :dateIso")
    suspend fun clearMorningRecord(dateIso: String, updatedAt: Long)

    @Query("UPDATE daily_health_records SET evening_systolic = NULL, evening_diastolic = NULL, evening_measured_at_epoch_ms = NULL, updated_at_epoch_ms = :updatedAt WHERE date_iso = :dateIso")
    suspend fun clearEveningRecord(dateIso: String, updatedAt: Long)

    @Query("UPDATE daily_health_records SET weight_kg = NULL, weight_measured_at_epoch_ms = NULL, updated_at_epoch_ms = :updatedAt WHERE date_iso = :dateIso")
    suspend fun clearWeight(dateIso: String, updatedAt: Long)

    @Query("SELECT * FROM daily_notes WHERE date_iso = :dateIso")
    fun observeDailyNote(dateIso: String): Flow<DailyNoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyNote(note: DailyNoteEntity)

    @Query("DELETE FROM daily_notes WHERE date_iso = :dateIso")
    suspend fun deleteDailyNote(dateIso: String)

    @Query("SELECT * FROM notification_settings WHERE id = 1")
    fun observeNotificationSettings(): Flow<NotificationSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotificationSettings(settings: NotificationSettingsEntity)
}
```

## 6. 구현 시 주의점

- 선택한 날짜에 row가 없으면 신규 생성할 수 있어야 한다.
- 선택한 날짜에 row가 있으면 update 로 처리해야 한다.
- 수정 시 기존 측정 시간은 유지해야 하므로 부분 수정 메서드에서 시간 필드를 재계산하지 않아야 한다.
- 메모는 미래 날짜도 허용되므로 별도 정책으로 처리해야 한다.
- 그래프 집계 시 하루 안에 아침/저녁 값이 일부만 존재할 수 있음을 고려해야 한다.
- 기존 `bp_records`에서 새 구조로 이전할 경우 아침/저녁 슬롯을 같은 날짜 row로 병합하는 마이그레이션이 필요하다.
