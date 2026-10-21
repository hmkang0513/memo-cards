package com.kanghm.memocards

import android.app.Application
import com.kanghm.memocards.data.AppDatabase
import com.kanghm.memocards.data.DeckRepository
import com.kanghm.memocards.data.SettingsStore

class MemoApp : Application() {
    val repository: DeckRepository by lazy { DeckRepository(AppDatabase.get(this)) }
    val settings: SettingsStore by lazy { SettingsStore(this) }
}
