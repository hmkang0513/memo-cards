package com.kanghm.memocards.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanghm.memocards.MemoApp
import com.kanghm.memocards.data.FrontSide
import com.kanghm.memocards.domain.CardOrder
import com.kanghm.memocards.domain.ReviewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val store = (LocalContext.current.applicationContext as MemoApp).settings
    val settings by store.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Section("복습 방식")
            Option("SM-2 간격 반복", "복습할 때가 된 카드만 나옵니다", settings.mode == ReviewMode.SM2) {
                store.update { it.copy(mode = ReviewMode.SM2) }
            }
            Option("모름만 반복", "알고있음은 넘기고, 전부 알면 처음부터 다시", settings.mode == ReviewMode.UNKNOWN_ONLY) {
                store.update { it.copy(mode = ReviewMode.UNKNOWN_ONLY) }
            }

            Section("카드 순서")
            Option("순서대로", null, settings.order == CardOrder.SEQUENTIAL) { store.update { it.copy(order = CardOrder.SEQUENTIAL) } }
            Option("랜덤", null, settings.order == CardOrder.RANDOM) { store.update { it.copy(order = CardOrder.RANDOM) } }
            Option("모름 먼저", "최근에 모른 카드를 앞에 둡니다", settings.order == CardOrder.UNKNOWN_FIRST) {
                store.update { it.copy(order = CardOrder.UNKNOWN_FIRST) }
            }

            Section("카드 앞면")
            Option("단어 먼저", "단어를 보고 뜻을 떠올립니다", settings.frontSide == FrontSide.TERM) {
                store.update { it.copy(frontSide = FrontSide.TERM) }
            }
            Option("뜻 먼저", "뜻을 보고 단어를 떠올립니다", settings.frontSide == FrontSide.MEANING) {
                store.update { it.copy(frontSide = FrontSide.MEANING) }
            }

            Text(
                "설정은 다음에 암기 화면을 열 때부터 적용됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
}

@Composable
private fun Option(title: String, description: String?, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onSelect).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
