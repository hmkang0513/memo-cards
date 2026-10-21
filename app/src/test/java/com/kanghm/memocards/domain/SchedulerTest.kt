package com.kanghm.memocards.domain

import com.kanghm.memocards.data.CardEntity
import com.kanghm.memocards.data.CardWithProgress
import com.kanghm.memocards.data.ProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SchedulerTest {
    private val now = 1_000_000_000L
    private val day = Scheduler.DAY_MS

    private fun progress() = ProgressEntity(cardUid = "a")

    @Test
    fun `알고있음 연속이면 간격이 1일 6일 EF곱으로 늘어난다`() {
        var p = progress()
        p = Scheduler.review(p, true, ReviewMode.SM2, now)
        assertEquals(1, p.intervalDays)
        p = Scheduler.review(p, true, ReviewMode.SM2, now)
        assertEquals(6, p.intervalDays)
        p = Scheduler.review(p, true, ReviewMode.SM2, now)
        assertEquals(15, p.intervalDays) // 6 * 2.5
        assertEquals(now + 15 * day, p.dueAt)
        assertEquals(3, p.knownCount)
    }

    @Test
    fun `모름이면 반복 횟수와 간격이 초기화되고 EF가 내려간다`() {
        var p = progress()
        repeat(3) { p = Scheduler.review(p, true, ReviewMode.SM2, now) }
        p = Scheduler.review(p, false, ReviewMode.SM2, now)
        assertEquals(0, p.repetitions)
        assertEquals(1, p.intervalDays)
        assertEquals(2.3, p.ease, 1e-9)
        assertEquals(now, p.dueAt)
        assertEquals(1, p.unknownCount)
        assertFalse(p.lastKnown)
    }

    @Test
    fun `EF는 1점3 아래로 내려가지 않는다`() {
        var p = progress()
        repeat(20) { p = Scheduler.review(p, false, ReviewMode.SM2, now) }
        assertEquals(Scheduler.MIN_EASE, p.ease, 1e-9)
    }

    @Test
    fun `모름만 반복 모드는 SM2 값을 건드리지 않고 라운드 플래그만 바꾼다`() {
        val p = Scheduler.review(progress(), true, ReviewMode.UNKNOWN_ONLY, now)
        assertTrue(p.roundKnown)
        assertEquals(0, p.intervalDays)
        assertEquals(0L, p.dueAt)
        val q = Scheduler.review(p, false, ReviewMode.UNKNOWN_ONLY, now)
        assertFalse(q.roundKnown)
    }

    private fun card(uid: String, pos: Int, p: ProgressEntity = ProgressEntity(uid)) =
        CardWithProgress(CardEntity(uid, 1, "f$uid", "b$uid", position = pos), p)

    @Test
    fun `SM2 큐는 복습 시기가 된 카드만 담는다`() {
        val cards = listOf(
            card("a", 0, ProgressEntity("a", dueAt = now + day)),
            card("b", 1, ProgressEntity("b", dueAt = now)),
            card("c", 2),
        )
        val q = StudyQueue.build(cards, ReviewMode.SM2, CardOrder.SEQUENTIAL, now)
        assertEquals(listOf("b", "c"), q.map { it.card.uid })
    }

    @Test
    fun `모름만 반복 큐는 넘긴 카드를 제외하고 전부 넘기면 라운드 완료다`() {
        val cards = listOf(
            card("a", 0, ProgressEntity("a", roundKnown = true)),
            card("b", 1),
        )
        val q = StudyQueue.build(cards, ReviewMode.UNKNOWN_ONLY, CardOrder.SEQUENTIAL, now)
        assertEquals(listOf("b"), q.map { it.card.uid })
        assertFalse(StudyQueue.isRoundComplete(cards))
        assertTrue(StudyQueue.isRoundComplete(cards.map { it.copy(progress = it.progress.copy(roundKnown = true)) }))
    }

    @Test
    fun `모름 먼저 순서는 최근에 모른 카드를 앞에 둔다`() {
        val cards = listOf(
            card("a", 0),
            card("b", 1, ProgressEntity("b", unknownCount = 2, lastKnown = false)),
            card("c", 2, ProgressEntity("c", unknownCount = 1, lastKnown = true)),
        )
        val q = StudyQueue.build(cards, ReviewMode.UNKNOWN_ONLY, CardOrder.UNKNOWN_FIRST, now, Random(1))
        assertEquals("b", q.first().card.uid)
    }
}
