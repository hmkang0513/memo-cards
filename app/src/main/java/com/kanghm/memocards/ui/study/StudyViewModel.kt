package com.kanghm.memocards.ui.study

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kanghm.memocards.MemoApp
import com.kanghm.memocards.data.CardWithProgress
import com.kanghm.memocards.data.FrontSide
import com.kanghm.memocards.domain.ReviewMode
import com.kanghm.memocards.domain.Scheduler
import com.kanghm.memocards.domain.StudyQueue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StudyUiState(
    val loading: Boolean = true,
    val deckName: String = "",
    val current: CardWithProgress? = null,
    val remaining: Int = 0,
    /** 이번 라운드를 시작할 때 큐에 있던 카드 수. 진행 막대의 분모. */
    val total: Int = 0,
    /** 응답 횟수(모름으로 다시 나온 카드는 여러 번 센다). 카드 전환 애니메이션의 키로도 쓴다. */
    val answered: Int = 0,
    /** 이번 라운드에서 알고있음으로 넘긴 카드 수. 카드마다 한 번만 센다. */
    val sessionKnown: Int = 0,
    val sessionUnknown: Int = 0,
    /** 마지막 응답. 카드가 넘어가는 방향을 정한다. */
    val lastAnswerKnown: Boolean = true,
    val frontSide: FrontSide = FrontSide.TERM,
    /** 한 번 보여줄 안내(스낵바용). */
    val notice: String? = null,
    /** 큐가 비어서 더 볼 카드가 없는 상태. */
    val finished: Boolean = false,
    val emptyDeck: Boolean = false,
)

class StudyViewModel(app: Application, private val deckId: Long) : AndroidViewModel(app) {
    private val repository = (app as MemoApp).repository
    private val settingsStore = (app as MemoApp).settings
    private val settings = settingsStore.state.value

    private val queue = ArrayDeque<CardWithProgress>()
    private val _state = MutableStateFlow(StudyUiState(frontSide = settings.frontSide))
    val state: StateFlow<StudyUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { start() }
    }

    private suspend fun start() {
        val deck = repository.getDeck(deckId)
        var cards = repository.getCards(deckId)
        var notice: String? = null
        var built = buildQueue(cards)

        // 모름만 반복: 전부 알고있음인 채로 열면 처음부터 다시 시작한다.
        if (built.isEmpty() && settings.mode == ReviewMode.UNKNOWN_ONLY && StudyQueue.isRoundComplete(cards)) {
            repository.resetRound(deckId)
            cards = repository.getCards(deckId)
            built = buildQueue(cards)
            notice = "전부 알고 있어서 처음부터 다시 시작해요"
        }
        queue.clear()
        queue.addAll(built)
        _state.value = _state.value.copy(
            loading = false,
            deckName = deck?.name.orEmpty(),
            emptyDeck = cards.isEmpty(),
            total = built.size,
            notice = notice,
        ).withHead()
    }

    private fun buildQueue(cards: List<CardWithProgress>) =
        StudyQueue.build(cards, settings.mode, settings.order, System.currentTimeMillis())

    /** 단어→뜻 / 뜻→단어를 바로 바꾼다. 다음에 열 때도 유지되도록 설정에도 저장한다. */
    fun toggleFrontSide() {
        val next = if (_state.value.frontSide == FrontSide.TERM) FrontSide.MEANING else FrontSide.TERM
        settingsStore.update { it.copy(frontSide = next) }
        _state.value = _state.value.copy(frontSide = next)
    }

    fun noticeShown() {
        _state.value = _state.value.copy(notice = null)
    }

    fun answer(known: Boolean) {
        val card = queue.removeFirstOrNull() ?: return
        val updated = Scheduler.review(card.progress, known, settings.mode, System.currentTimeMillis())
        // 모름은 같은 세션 끝에 다시 넣어, 알고있음으로 답할 때까지 반복한다.
        if (!known) queue.addLast(card.copy(progress = updated))
        val s = _state.value
        _state.value = s.copy(
            answered = s.answered + 1,
            sessionKnown = s.sessionKnown + if (known) 1 else 0,
            sessionUnknown = s.sessionUnknown + if (known) 0 else 1,
            lastAnswerKnown = known,
        ).withHead()
        viewModelScope.launch {
            repository.saveProgress(updated)
            repository.recordAnswer(known)
        }
    }

    /**
     * 처음부터 다시: 라운드 표시를 초기화하고 덱의 모든 카드로 새 큐를 만든다.
     * SM-2 모드에서도 복습 시기와 상관없이 전부 다시 보며, 응답은 평소처럼 진행 기록에 반영된다.
     */
    fun restart() {
        viewModelScope.launch {
            repository.resetRound(deckId)
            val all = StudyQueue.build(
                repository.getCards(deckId),
                ReviewMode.UNKNOWN_ONLY,
                settings.order,
                System.currentTimeMillis(),
            )
            queue.clear()
            queue.addAll(all)
            _state.value = _state.value.copy(
                total = all.size,
                answered = 0,
                sessionKnown = 0,
                sessionUnknown = 0,
                lastAnswerKnown = true,
            ).withHead()
        }
    }

    private fun StudyUiState.withHead() = copy(
        current = queue.firstOrNull(),
        remaining = queue.size,
        finished = queue.isEmpty() && !loading,
    )
}
