package com.kanghm.memocards.domain

import com.kanghm.memocards.data.Stats
import java.time.LocalDate

/** 하루 목표(응답 횟수). */
const val DAILY_GOAL = 20

object Streak {
    /**
     * [studiedDays] 중 오늘(또는 아직 오늘 안 했다면 어제)부터 거꾸로 하루도 안 빠지고 이어진 날 수.
     * 오늘 아직 공부 전이어도 어제까지의 연속 기록은 끊기지 않은 것으로 본다.
     */
    fun count(studiedDays: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in studiedDays) today else today.minusDays(1)
        var n = 0
        while (day in studiedDays) {
            n++
            day = day.minusDays(1)
        }
        return n
    }
}

/** 마스코트 성장 단계. 누적 알고있음 횟수로 정한다. */
enum class MascotStage(val title: String, val minKnown: Int) {
    EGG("알", 0),
    BABY("아기", 10),
    KID("꼬마", 50),
    STAR("스타", 150),
    KING("왕", 400),
    ;

    companion object {
        fun of(totalKnown: Int): MascotStage = entries.last { totalKnown >= it.minKnown }

        /** 다음 단계까지 남은 알고있음 횟수. 마지막 단계면 null. */
        fun remainingToNext(totalKnown: Int): Int? {
            val next = entries.firstOrNull { it.minKnown > totalKnown } ?: return null
            return next.minKnown - totalKnown
        }
    }
}

/** 스티커북에 모이는 스티커. 따로 저장하지 않고 학습 통계로 달성 여부를 계산한다. */
data class Sticker(val id: String, val emoji: String, val title: String, val condition: String, val achieved: (Stats) -> Boolean)

val ALL_STICKERS: List<Sticker> = listOf(
    Sticker("first", "🌱", "첫 걸음", "알고있음을 1번 누르기") { it.totalKnown >= 1 },
    Sticker("k10", "🍀", "열 번째", "알고있음 누적 10번") { it.totalKnown >= 10 },
    Sticker("k50", "🌸", "쑥쑥", "알고있음 누적 50번") { it.totalKnown >= 50 },
    Sticker("k150", "⭐", "반짝반짝", "알고있음 누적 150번") { it.totalKnown >= 150 },
    Sticker("k400", "👑", "암기왕", "알고있음 누적 400번") { it.totalKnown >= 400 },
    Sticker("goal", "🎯", "오늘 목표 달성", "하루에 $DAILY_GOAL 번 응답하기") { it.todayAnswered >= DAILY_GOAL },
    Sticker("s3", "🔥", "3일 연속", "3일 연속 공부하기") { it.streak >= 3 },
    Sticker("s7", "🌈", "일주일 연속", "7일 연속 공부하기") { it.streak >= 7 },
    Sticker("s14", "🏆", "2주 연속", "14일 연속 공부하기") { it.streak >= 14 },
)
