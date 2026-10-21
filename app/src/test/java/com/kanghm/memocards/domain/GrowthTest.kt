package com.kanghm.memocards.domain

import com.kanghm.memocards.data.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GrowthTest {
    private val today = LocalDate.of(2026, 10, 21)

    @Test
    fun `오늘까지 이어진 날을 센다`() {
        val days = setOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(4))
        assertEquals(3, Streak.count(days, today))
    }

    @Test
    fun `오늘 아직 안 했어도 어제까지 이어졌으면 연속이 유지된다`() {
        val days = setOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, Streak.count(days, today))
    }

    @Test
    fun `어제도 안 했으면 연속이 끊긴다`() {
        assertEquals(0, Streak.count(setOf(today.minusDays(2)), today))
    }

    @Test
    fun `누적 알고있음에 따라 성장 단계가 정해진다`() {
        assertEquals(MascotStage.EGG, MascotStage.of(0))
        assertEquals(MascotStage.BABY, MascotStage.of(10))
        assertEquals(MascotStage.KID, MascotStage.of(60))
        assertEquals(MascotStage.KING, MascotStage.of(1000))
        assertEquals(10, MascotStage.remainingToNext(0))
        assertNull(MascotStage.remainingToNext(400))
    }

    @Test
    fun `스티커는 통계로 달성 여부가 계산된다`() {
        val stats = Stats(streak = 3, todayAnswered = 5, totalKnown = 12)
        val achieved = ALL_STICKERS.filter { it.achieved(stats) }.map { it.id }
        assertTrue(achieved.containsAll(listOf("first", "k10", "s3")))
        assertTrue("goal" !in achieved && "k50" !in achieved && "s7" !in achieved)
    }
}
