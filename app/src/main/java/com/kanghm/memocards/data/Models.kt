package com.kanghm.memocards.data

import androidx.room.Embedded

/** 홈 화면용 덱 요약. */
data class DeckSummary(
    val id: Long,
    val name: String,
    val total: Int,
    val due: Int,
    val unknown: Int,
)

/** 카드와 진행 상태를 합친 조회 결과. */
data class CardWithProgress(
    @Embedded val card: CardEntity,
    @Embedded(prefix = "p_") val progress: ProgressEntity,
)
