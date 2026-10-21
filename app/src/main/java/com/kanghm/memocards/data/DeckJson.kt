package com.kanghm.memocards.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 카드 JSON 스키마 v1. Claude가 볼트에서 만들어 주는 파일이자, 앱이 내보내는 백업 형식이기도 하다. */
@Serializable
data class DeckFile(
    val version: Int = 1,
    val decks: List<DeckJson>,
)

@Serializable
data class DeckJson(
    val name: String,
    val source: String? = null,
    val cards: List<CardJson>,
)

@Serializable
data class CardJson(
    val uid: String,
    val front: String,
    val back: String,
    val reading: String? = null,
    val extra: String? = null,
    /** true면 방향을 바꿔 보지 않고 항상 front가 앞면. 코드 문제 카드에 쓴다. */
    val oneWay: Boolean = false,
    /** 백업 내보내기에만 채워진다. 카드 가져오기에서는 없어도 된다. */
    val progress: ProgressJson? = null,
)

@Serializable
data class ProgressJson(
    val knownCount: Int = 0,
    val unknownCount: Int = 0,
    val ease: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val dueAt: Long = 0,
    val lastReviewedAt: Long? = null,
    val lastKnown: Boolean = false,
    val roundKnown: Boolean = false,
)

val deckJsonFormat = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    encodeDefaults = true
}
