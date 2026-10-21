package com.kanghm.memocards.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Query("SELECT * FROM decks WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): DeckEntity?

    @Insert
    suspend fun insert(deck: DeckEntity): Long

    @Query(
        """
        SELECT d.id AS id, d.name AS name,
               COUNT(c.uid) AS total,
               COALESCE(SUM(CASE WHEN p.dueAt <= :now THEN 1 ELSE 0 END), 0) AS due,
               COALESCE(SUM(CASE WHEN p.unknownCount > 0 AND p.lastKnown = 0 THEN 1 ELSE 0 END), 0) AS unknown
        FROM decks d
        LEFT JOIN cards c ON c.deckId = d.id
        LEFT JOIN card_progress p ON p.cardUid = c.uid
        GROUP BY d.id
        ORDER BY d.name
        """,
    )
    fun observeSummaries(now: Long): Flow<List<DeckSummary>>

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun getById(id: Long): DeckEntity?

    @Query("SELECT * FROM decks ORDER BY name")
    suspend fun all(): List<DeckEntity>

    @Query("DELETE FROM decks WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface CardDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(card: CardEntity): Long

    @Update
    suspend fun update(card: CardEntity)

    @Query("SELECT * FROM cards WHERE uid = :uid")
    suspend fun getByUid(uid: String): CardEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProgressIfAbsent(progress: ProgressEntity): Long

    @Update
    suspend fun updateProgress(progress: ProgressEntity)

    @Query("SELECT * FROM card_progress WHERE cardUid = :uid")
    suspend fun getProgress(uid: String): ProgressEntity?

    @Query(CARDS_WITH_PROGRESS + " WHERE c.deckId = :deckId ORDER BY c.position")
    fun observeByDeck(deckId: Long): Flow<List<CardWithProgress>>

    @Query(CARDS_WITH_PROGRESS + " WHERE c.deckId = :deckId ORDER BY c.position")
    suspend fun getByDeck(deckId: Long): List<CardWithProgress>

    @Query("SELECT uid FROM cards WHERE deckId = :deckId")
    suspend fun uidsOfDeck(deckId: Long): List<String>

    @Query("UPDATE card_progress SET roundKnown = 0 WHERE cardUid IN (SELECT uid FROM cards WHERE deckId = :deckId)")
    suspend fun resetRound(deckId: Long)

    @Query("SELECT * FROM card_progress")
    suspend fun allProgress(): List<ProgressEntity>

    companion object {
        const val CARDS_WITH_PROGRESS = """
            SELECT c.uid, c.deckId, c.front, c.back, c.reading, c.extra, c.position, c.oneWay,
                   p.cardUid AS p_cardUid, p.knownCount AS p_knownCount, p.unknownCount AS p_unknownCount,
                   p.ease AS p_ease, p.intervalDays AS p_intervalDays, p.repetitions AS p_repetitions,
                   p.dueAt AS p_dueAt, p.lastReviewedAt AS p_lastReviewedAt, p.lastKnown AS p_lastKnown,
                   p.roundKnown AS p_roundKnown
            FROM cards c JOIN card_progress p ON p.cardUid = c.uid
        """
    }
}
