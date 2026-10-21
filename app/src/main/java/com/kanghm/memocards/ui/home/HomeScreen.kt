package com.kanghm.memocards.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kanghm.memocards.data.DeckSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStudy: (Long) -> Unit,
    onCardList: (Long) -> Unit,
    onSettings: () -> Unit,
    onStickers: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val decks by viewModel.decks.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var deleteTarget by remember { mutableStateOf<DeckSummary?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::import)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::export)
    }

    LaunchedEffect(Unit) { viewModel.refreshClock() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🐰 암기카드", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onStickers) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = "스티커북")
                    }
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) }) {
                        Icon(Icons.Filled.FileUpload, contentDescription = "카드 JSON 가져오기")
                    }
                    IconButton(onClick = { exportLauncher.launch("memo-cards-backup.json") }) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "백업 내보내기")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "설정")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { HomeHeader(stats) }
            if (decks.isEmpty()) {
                item {
                    Text(
                        "아직 카드가 없어요.\n위쪽 가져오기 버튼으로 카드 JSON 파일을 불러오세요.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    )
                }
            }
            items(decks, key = { it.id }) { deck ->
                DeckItem(
                    deck = deck,
                    onClick = { onStudy(deck.id) },
                    onList = { onCardList(deck.id) },
                    onDelete = { deleteTarget = deck },
                )
            }
        }
    }

    deleteTarget?.let { deck ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("덱 삭제") },
            text = { Text("'${deck.name}'과 진행 기록이 모두 삭제됩니다.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDeck(deck.id)
                    deleteTarget = null
                }) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("취소") } },
        )
    }
}

@Composable
private fun DeckItem(deck: DeckSummary, onClick: () -> Unit, onList: () -> Unit, onDelete: () -> Unit) {
    // 덱마다 이모지와 파스텔 색을 돌려가며 입힌다. 덱 id로 정해서 항상 같은 모습이다.
    val scheme = MaterialTheme.colorScheme
    val emoji = DECK_EMOJIS[(deck.id % DECK_EMOJIS.size).toInt()]
    val container = when ((deck.id % 3).toInt()) {
        0 -> scheme.primaryContainer
        1 -> scheme.secondaryContainer
        else -> scheme.tertiaryContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(emoji, fontSize = 36.sp, modifier = Modifier.padding(end = 12.dp))
            Column(Modifier.weight(1f)) {
                Text(deck.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "카드 ${deck.total}장 · 복습 ${deck.due} · 모름 ${deck.unknown}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onList) { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "카드 목록") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "덱 삭제") }
        }
    }
}

private val DECK_EMOJIS = listOf("🐰", "🐻", "🐱", "🐥", "🍓", "🍑", "🌸", "🍀")
