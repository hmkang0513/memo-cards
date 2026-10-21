package com.kanghm.memocards.data

import android.content.Context
import com.kanghm.memocards.domain.CardOrder
import com.kanghm.memocards.domain.ReviewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 카드 앞면에 무엇을 먼저 보여줄지. */
enum class FrontSide { TERM, MEANING }

data class Settings(
    val mode: ReviewMode = ReviewMode.SM2,
    val order: CardOrder = CardOrder.SEQUENTIAL,
    val frontSide: FrontSide = FrontSide.TERM,
)

/** 설정은 값이 몇 개 안 돼서 SharedPreferences에 enum 이름으로 저장한다. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<Settings> = _state.asStateFlow()

    fun update(transform: (Settings) -> Settings) {
        val next = transform(_state.value)
        _state.value = next
        prefs.edit()
            .putString(KEY_MODE, next.mode.name)
            .putString(KEY_ORDER, next.order.name)
            .putString(KEY_FRONT, next.frontSide.name)
            .apply()
    }

    private fun load() = Settings(
        mode = readEnum(KEY_MODE, ReviewMode.SM2),
        order = readEnum(KEY_ORDER, CardOrder.SEQUENTIAL),
        frontSide = readEnum(KEY_FRONT, FrontSide.TERM),
    )

    private inline fun <reified T : Enum<T>> readEnum(key: String, default: T): T =
        prefs.getString(key, null)?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default

    private companion object {
        const val KEY_MODE = "mode"
        const val KEY_ORDER = "order"
        const val KEY_FRONT = "frontSide"
    }
}
