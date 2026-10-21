package com.kanghm.memocards.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** 하루 단위 학습 기록. 연속 학습일과 오늘 목표 게이지에 쓴다. date는 기기 현지 날짜(yyyy-MM-dd). */
@Entity(tableName = "daily_stats")
data class DailyStatEntity(
    @PrimaryKey val date: String,
    val answered: Int = 0,
    val known: Int = 0,
)

@Dao
interface StatsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ensureDay(day: DailyStatEntity): Long

    @Query("UPDATE daily_stats SET answered = answered + 1, known = known + :knownInc WHERE date = :date")
    suspend fun addAnswer(date: String, knownInc: Int)

    @Query("SELECT * FROM daily_stats ORDER BY date DESC")
    fun observeDays(): Flow<List<DailyStatEntity>>

    /** 지금까지 알고있음을 누른 총 횟수. 마스코트 성장과 스티커의 기준이 된다. */
    @Query("SELECT COALESCE(SUM(knownCount), 0) FROM card_progress")
    fun observeTotalKnown(): Flow<Int>
}

/** 홈 화면과 스티커북이 쓰는 학습 통계. */
data class Stats(
    val streak: Int = 0,
    val todayAnswered: Int = 0,
    val todayKnown: Int = 0,
    val totalKnown: Int = 0,
    val daysStudied: Int = 0,
)
