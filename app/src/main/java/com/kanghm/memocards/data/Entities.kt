package com.kanghm.memocards.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 주제(덱). 이름이 같으면 같은 덱으로 본다. */
@Entity(tableName = "decks", indices = [Index(value = ["name"], unique = true)])
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val source: String? = null,
)

/** 카드. uid는 JSON에서 오는 고정 식별자라서 재가져오기 때 진행 기록을 이어 붙이는 키가 된다. */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("deckId")],
)
data class CardEntity(
    @PrimaryKey val uid: String,
    val deckId: Long,
    val front: String,
    val back: String,
    val reading: String? = null,
    val extra: String? = null,
    /** JSON에 적힌 순서. "순서대로" 보기에 사용한다. */
    val position: Int = 0,
)

/** 카드별 학습 진행 상태 (SM-2 값 + 누적 횟수). */
@Entity(
    tableName = "card_progress",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["uid"],
            childColumns = ["cardUid"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ProgressEntity(
    @PrimaryKey val cardUid: String,
    val knownCount: Int = 0,
    val unknownCount: Int = 0,
    val ease: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    /** 다음 복습 시각(epoch ms). 0이면 새 카드라 바로 대상이다. */
    val dueAt: Long = 0,
    val lastReviewedAt: Long? = null,
    /** 가장 최근 응답이 "알고있음"인지. 목록의 모름 표시에 사용한다. */
    val lastKnown: Boolean = false,
    /** '모름만 반복' 모드에서 현재 라운드에 알고있음으로 넘긴 카드인지. */
    val roundKnown: Boolean = false,
)
