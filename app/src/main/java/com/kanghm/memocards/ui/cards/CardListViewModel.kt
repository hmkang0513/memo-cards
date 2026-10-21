package com.kanghm.memocards.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kanghm.memocards.data.CardWithProgress
import com.kanghm.memocards.data.DeckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CardListViewModel(repository: DeckRepository, deckId: Long) : ViewModel() {
    val cards: StateFlow<List<CardWithProgress>> = repository.observeCards(deckId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _deckName = MutableStateFlow("")
    val deckName: StateFlow<String> = _deckName.asStateFlow()

    init {
        viewModelScope.launch { _deckName.value = repository.getDeck(deckId)?.name.orEmpty() }
    }
}
