package com.kanghm.memocards.data

import androidx.room.withTransaction
import com.kanghm.memocards.domain.Streak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/** 가져오기 결과 요약. */
data class ImportResult(val decks: Int, val added: Int, val updated: Int)

class DeckRepository(private val db: AppDatabase) {
    private val deckDao = db.deckDao()
    private val cardDao = db.cardDao()
    private val statsDao = db.statsDao()

    fun observeSummaries(now: Long): Flow<List<DeckSummary>> = deckDao.observeSummaries(now)

    fun observeCards(deckId: Long): Flow<List<CardWithProgress>> = cardDao.observeByDeck(deckId)

    suspend fun getDeck(deckId: Long): DeckEntity? = deckDao.getById(deckId)

    suspend fun getCards(deckId: Long): List<CardWithProgress> = cardDao.getByDeck(deckId)

    suspend fun deleteDeck(deckId: Long) = deckDao.delete(deckId)

    suspend fun saveProgress(progress: ProgressEntity) = cardDao.updateProgress(progress)

    suspend fun resetRound(deckId: Long) = cardDao.resetRound(deckId)

    /** 오늘 기록에 응답 1회를 더한다. */
    suspend fun recordAnswer(known: Boolean, today: LocalDate = LocalDate.now()) {
        val date = today.toString()
        statsDao.ensureDay(DailyStatEntity(date))
        statsDao.addAnswer(date, if (known) 1 else 0)
    }

    /** 연속 학습일, 오늘 진행량, 누적 알고있음 수. 응답할 때마다 새 값이 나온다. */
    fun observeStats(today: LocalDate = LocalDate.now()): Flow<Stats> =
        combine(statsDao.observeDays(), statsDao.observeTotalKnown()) { days, totalKnown ->
            val studied = days.filter { it.answered > 0 }.map { LocalDate.parse(it.date) }.toSet()
            val todayRow = days.firstOrNull { it.date == today.toString() }
            Stats(
                streak = Streak.count(studied, today),
                todayAnswered = todayRow?.answered ?: 0,
                todayKnown = todayRow?.known ?: 0,
                totalKnown = totalKnown,
                daysStudied = studied.size,
            )
        }

    /**
     * JSON 텍스트를 가져온다. uid가 이미 있으면 내용(앞면/뒷면 등)만 갱신하고 진행 기록은 그대로 둔다.
     * 파일에 progress가 들어 있는 경우(백업 복원)에는 새로 들어오는 카드에 한해 그 값을 사용한다.
     */
    suspend fun importJson(text: String): ImportResult {
        val file = deckJsonFormat.decodeFromString(DeckFile.serializer(), text)
        var added = 0
        var updated = 0
        db.withTransaction {
            file.decks.forEach { deckJson ->
                val deckId = deckDao.findByName(deckJson.name)?.id
                    ?: deckDao.insert(DeckEntity(name = deckJson.name, source = deckJson.source))
                deckJson.cards.forEachIndexed { index, c ->
                    val entity = CardEntity(c.uid, deckId, c.front, c.back, c.reading, c.extra, index, c.oneWay)
                    if (cardDao.insertIfAbsent(entity) == -1L) {
                        cardDao.update(entity)
                        updated++
                    } else {
                        added++
                    }
                    cardDao.insertProgressIfAbsent(c.progress?.toEntity(c.uid) ?: ProgressEntity(cardUid = c.uid))
                }
            }
        }
        return ImportResult(decks = file.decks.size, added = added, updated = updated)
    }

    /** 모든 덱과 진행 기록을 JSON 텍스트로 내보낸다(백업). */
    suspend fun exportJson(): String {
        val decks = deckDaoAll().map { deck ->
            DeckJson(
                name = deck.name,
                source = deck.source,
                cards = cardDao.getByDeck(deck.id).map { cp ->
                    CardJson(
                        uid = cp.card.uid,
                        front = cp.card.front,
                        back = cp.card.back,
                        reading = cp.card.reading,
                        extra = cp.card.extra,
                        oneWay = cp.card.oneWay,
                        progress = cp.progress.toJson(),
                    )
                },
            )
        }
        return deckJsonFormat.encodeToString(DeckFile.serializer(), DeckFile(decks = decks))
    }

    private suspend fun deckDaoAll(): List<DeckEntity> = deckDao.all()
}

private fun ProgressJson.toEntity(uid: String) = ProgressEntity(
    cardUid = uid,
    knownCount = knownCount,
    unknownCount = unknownCount,
    ease = ease,
    intervalDays = intervalDays,
    repetitions = repetitions,
    dueAt = dueAt,
    lastReviewedAt = lastReviewedAt,
    lastKnown = lastKnown,
    roundKnown = roundKnown,
)

private fun ProgressEntity.toJson() = ProgressJson(
    knownCount = knownCount,
    unknownCount = unknownCount,
    ease = ease,
    intervalDays = intervalDays,
    repetitions = repetitions,
    dueAt = dueAt,
    lastReviewedAt = lastReviewedAt,
    lastKnown = lastKnown,
    roundKnown = roundKnown,
)
