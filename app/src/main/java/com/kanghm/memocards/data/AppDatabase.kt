package com.kanghm.memocards.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DeckEntity::class, CardEntity::class, ProgressEntity::class, DailyStatEntity::class],
    version = 3,
    exportSchema = true,
    // 1 → 2: daily_stats 테이블 추가. 2 → 3: cards.oneWay 컬럼 추가(기본 0). 기존 덱·카드·진행 기록은 그대로 유지된다.
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun statsDao(): StatsDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "memo-cards.db")
                .build()
                .also { instance = it }
        }
    }
}
