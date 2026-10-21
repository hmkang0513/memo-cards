package com.kanghm.memocards.ui.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kanghm.memocards.MemoApp
import com.kanghm.memocards.data.CardWithProgress

private enum class CardSort(val label: String) {
    POSITION("순서"),
    UNKNOWN_DESC("모름 많은 순"),
    KNOWN_DESC("알고있음 많은 순"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardListScreen(deckId: Long, onBack: () -> Unit) {
    val repository = (LocalContext.current.applicationContext as MemoApp).repository
    val viewModel: CardListViewModel = viewModel(
        key = "cards-$deckId",
        factory = viewModelFactory { initializer { CardListViewModel(repository, deckId) } },
    )
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val deckName by viewModel.deckName.collectAsStateWithLifecycle()

    var sort by remember { mutableStateOf(CardSort.POSITION) }
    var unknownOnly by remember { mutableStateOf(false) }

    val shown = cards
        .filter { !unknownOnly || (it.progress.unknownCount > 0 && !it.progress.lastKnown) }
        .let { list ->
            when (sort) {
                CardSort.POSITION -> list.sortedBy { it.card.position }
                CardSort.UNKNOWN_DESC -> list.sortedByDescending { it.progress.unknownCount }
                CardSort.KNOWN_DESC -> list.sortedByDescending { it.progress.knownCount }
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deckName) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CardSort.entries.forEach { option ->
                    FilterChip(selected = sort == option, onClick = { sort = option }, label = { Text(option.label) })
                }
            }
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = unknownOnly, onClick = { unknownOnly = !unknownOnly }, label = { Text("지금 모르는 카드만") })
                Text(
                    "  ${shown.size} / ${cards.size}장",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(shown, key = { it.card.uid }) { CardRow(it) }
            }
        }
    }
}

@Composable
private fun CardRow(item: CardWithProgress) {
    val c = item.card
    val p = item.progress
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(c.front, style = MaterialTheme.typography.titleMedium)
                if (!c.reading.isNullOrBlank()) {
                    Text(c.reading, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Text(c.back, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("알고있음 ${p.knownCount}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "모름 ${p.unknownCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (p.unknownCount > 0 && !p.lastKnown) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
