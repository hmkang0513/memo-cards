package com.kanghm.memocards.domain

import com.kanghm.memocards.data.CardWithProgress
import kotlin.random.Random

/** 암기 세션에 쓸 카드 큐를 만든다. */
object StudyQueue {
    /**
     * [mode]에 맞는 카드를 [order]대로 정렬해 돌려준다.
     * UNKNOWN_ONLY에서 아직 안 넘긴 카드가 없으면(전부 알고있음) 빈 리스트가 나오므로,
     * 호출한 쪽에서 [isRoundComplete]로 확인해 라운드를 초기화한다.
     */
    fun build(
        cards: List<CardWithProgress>,
        mode: ReviewMode,
        order: CardOrder,
        now: Long,
        random: Random = Random.Default,
    ): List<CardWithProgress> {
        val candidates = when (mode) {
            ReviewMode.SM2 -> cards.filter { it.progress.dueAt <= now }
            ReviewMode.UNKNOWN_ONLY -> cards.filterNot { it.progress.roundKnown }
        }
        return when (order) {
            CardOrder.SEQUENTIAL -> candidates.sortedBy { it.card.position }
            CardOrder.RANDOM -> candidates.shuffled(random)
            CardOrder.UNKNOWN_FIRST -> candidates.sortedWith(
                compareByDescending<CardWithProgress> { it.progress.unknownCount > 0 && !it.progress.lastKnown }
                    .thenByDescending { it.progress.unknownCount }
                    .thenBy { it.card.position },
            )
        }
    }

    /** '모름만 반복' 모드에서 모든 카드를 알고있음으로 넘겼는지. */
    fun isRoundComplete(cards: List<CardWithProgress>): Boolean =
        cards.isNotEmpty() && cards.all { it.progress.roundKnown }
}
