package com.kanghm.memocards.ui.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kanghm.memocards.MemoApp
import com.kanghm.memocards.data.DeckSummary
import com.kanghm.memocards.data.Stats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = (app as MemoApp).repository
    private val resolver = app.contentResolver

    /** 화면이 다시 켜질 때 "복습할 개수" 기준 시각을 새로 잡기 위한 트리거. */
    private val clock = MutableStateFlow(System.currentTimeMillis())

    @OptIn(ExperimentalCoroutinesApi::class)
    val decks: StateFlow<List<DeckSummary>> = clock
        .flatMapLatest { repository.observeSummaries(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<Stats> = repository.observeStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Stats())

    /** 한 번 보여주고 비우는 안내 메시지(스낵바용). */
    val message = MutableStateFlow<String?>(null)

    fun refreshClock() {
        clock.value = System.currentTimeMillis()
    }

    fun import(uri: Uri) = viewModelScope.launch {
        message.value = runCatching {
            val text = withContext(Dispatchers.IO) {
                resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("파일을 열 수 없습니다")
            }
            val r = repository.importJson(text)
            "덱 ${r.decks}개 가져옴 (새 카드 ${r.added}, 갱신 ${r.updated})"
        }.getOrElse { "가져오기 실패: ${it.message}" }
    }

    fun export(uri: Uri) = viewModelScope.launch {
        message.value = runCatching {
            val text = repository.exportJson()
            withContext(Dispatchers.IO) {
                resolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(text) }
                    ?: error("파일을 쓸 수 없습니다")
            }
            "백업을 저장했습니다"
        }.getOrElse { "내보내기 실패: ${it.message}" }
    }

    fun deleteDeck(id: Long) = viewModelScope.launch { repository.deleteDeck(id) }

    fun messageShown() {
        message.value = null
    }
}
