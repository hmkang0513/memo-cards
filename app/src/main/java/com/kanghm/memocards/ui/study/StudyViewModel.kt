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
    val flipped: Boolean = false,
    val remaining: Int = 0,
    val answered: Int = 0,
    val frontSide: FrontSide = FrontSide.TERM,
    /** 라운드가 새로 시작됐을 때 등 한 번 보여줄 안내. */
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

        // 모름만 반복: 전부 알고있음이면 처음부터 다시 시작한다.
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
            notice = notice,
        ).withHead()
    }

    private fun buildQueue(cards: List<CardWithProgress>) =
        StudyQueue.build(cards, settings.mode, settings.order, System.currentTimeMillis())

    fun flip() {
        _state.value = _state.value.let { it.copy(flipped = !it.flipped) }
    }

    /** 단어→뜻 / 뜻→단어를 바로 바꾼다. 다음에 열 때도 유지되도록 설정에도 저장한다. */
    fun toggleFrontSide() {
        val next = if (_state.value.frontSide == FrontSide.TERM) FrontSide.MEANING else FrontSide.TERM
        settingsStore.update { it.copy(frontSide = next) }
        _state.value = _state.value.copy(frontSide = next, flipped = false)
    }

    fun noticeShown() {
        _state.value = _state.value.copy(notice = null)
    }

    fun answer(known: Boolean) {
        val card = queue.removeFirstOrNull() ?: return
        val updated = Scheduler.review(card.progress, known, settings.mode, System.currentTimeMillis())
        // 모름은 같은 세션 끝에 다시 넣어, 알고있음으로 답할 때까지 반복한다.
        if (!known) queue.addLast(card.copy(progress = updated))
        _state.value = _state.value.copy(answered = _state.value.answered + 1, flipped = false).withHead()
        viewModelScope.launch {
            repository.saveProgress(updated)
            if (queue.isEmpty() && settings.mode == ReviewMode.UNKNOWN_ONLY) restartRound()
        }
    }

    /** 모름만 반복에서 마지막 카드까지 알고있음으로 넘기면 라운드를 초기화하고 다시 시작한다. */
    private suspend fun restartRound() {
        repository.resetRound(deckId)
        val built = buildQueue(repository.getCards(deckId))
        queue.addAll(built)
        _state.value = _state.value.copy(notice = "한 바퀴 끝! 처음부터 다시 시작해요").withHead()
    }

    private fun StudyUiState.withHead() = copy(
        current = queue.firstOrNull(),
        remaining = queue.size,
        finished = queue.isEmpty() && !loading,
    )
}
