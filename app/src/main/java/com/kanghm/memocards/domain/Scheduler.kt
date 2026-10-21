package com.kanghm.memocards.domain

import com.kanghm.memocards.data.ProgressEntity
import kotlin.math.ceil
import kotlin.math.max

/** 복습 방식. */
enum class ReviewMode {
    /** SM-2 간격 반복: 복습 시기가 된 카드만 나온다. */
    SM2,

    /** 모름만 반복: 알고있음은 넘기고, 전부 알면 처음부터 다시 시작한다. */
    UNKNOWN_ONLY,
}

/** 카드 노출 순서. */
enum class CardOrder { SEQUENTIAL, RANDOM, UNKNOWN_FIRST }

object Scheduler {
    const val DAY_MS = 24L * 60 * 60 * 1000
    const val INITIAL_EASE = 2.5
    const val MIN_EASE = 1.3
    private const val LAPSE_EASE_PENALTY = 0.2
    private const val FIRST_INTERVAL_DAYS = 1
    private const val SECOND_INTERVAL_DAYS = 6

    /** 카드 한 장에 대한 응답(알고있음/모름)을 반영한 새 진행 상태를 돌려준다. */
    fun review(p: ProgressEntity, known: Boolean, mode: ReviewMode, now: Long): ProgressEntity {
        val counted = p.copy(
            knownCount = p.knownCount + if (known) 1 else 0,
            unknownCount = p.unknownCount + if (known) 0 else 1,
            lastKnown = known,
            lastReviewedAt = now,
        )
        return when (mode) {
            ReviewMode.UNKNOWN_ONLY -> counted.copy(roundKnown = known)
            ReviewMode.SM2 -> if (known) scheduleKnown(counted, now) else scheduleUnknown(counted, now)
        }
    }

    /** 알고있음: 1일 → 6일 → 직전 간격 × EF (올림). EF는 유지(SM-2 grade 4와 같다). */
    private fun scheduleKnown(p: ProgressEntity, now: Long): ProgressEntity {
        val repetitions = p.repetitions + 1
        val interval = when (repetitions) {
            1 -> FIRST_INTERVAL_DAYS
            2 -> SECOND_INTERVAL_DAYS
            else -> ceil(p.intervalDays * p.ease).toInt()
        }
        return p.copy(
            repetitions = repetitions,
            intervalDays = interval,
            dueAt = now + interval * DAY_MS,
            roundKnown = true,
        )
    }

    /** 모름: 반복 횟수 초기화, 간격 1일, EF 감소(최소 1.3). 오늘 안에 다시 나오도록 dueAt은 지금으로 둔다. */
    private fun scheduleUnknown(p: ProgressEntity, now: Long): ProgressEntity = p.copy(
        repetitions = 0,
        intervalDays = FIRST_INTERVAL_DAYS,
        ease = max(MIN_EASE, p.ease - LAPSE_EASE_PENALTY),
        dueAt = now,
        roundKnown = false,
    )
}
