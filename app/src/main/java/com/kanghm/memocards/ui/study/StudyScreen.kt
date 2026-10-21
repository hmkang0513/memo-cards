package com.kanghm.memocards.ui.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kanghm.memocards.data.CardWithProgress
import com.kanghm.memocards.data.FrontSide

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(deckId: Long, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as android.app.Application
    val viewModel: StudyViewModel = viewModel(
        key = "study-$deckId",
        factory = viewModelFactory { initializer { StudyViewModel(app, deckId) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.notice) {
        state.notice?.let {
            snackbar.showSnackbar(it)
            viewModel.noticeShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.deckName) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로") }
                },
                actions = {
                    if (!state.loading && !state.emptyDeck) {
                        TextButton(onClick = viewModel::toggleFrontSide) {
                            Text(if (state.frontSide == FrontSide.TERM) "단어→뜻" else "뜻→단어")
                        }
                    }
                    if (!state.loading && !state.finished) {
                        Text("남은 ${state.remaining}장", modifier = Modifier.padding(end = 16.dp))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val card = state.current
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.emptyDeck -> CenterMessage("이 덱에는 카드가 없어요.")
                state.finished || card == null -> CenterMessage(
                    "지금 복습할 카드가 없어요.\n오늘 ${state.answered}장 봤어요.",
                    action = { OutlinedButton(onClick = onBack) { Text("홈으로") } },
                )
                else -> CardContent(
                    card = card,
                    flipped = state.flipped,
                    frontSide = state.frontSide,
                    onFlip = viewModel::flip,
                    onAnswer = viewModel::answer,
                )
            }
        }
    }
}

@Composable
private fun CenterMessage(text: String, action: (@Composable () -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        action?.let {
            Box(Modifier.padding(top = 16.dp)) { it() }
        }
    }
}

@Composable
private fun CardContent(
    card: CardWithProgress,
    flipped: Boolean,
    frontSide: FrontSide,
    onFlip: () -> Unit,
    onAnswer: (Boolean) -> Unit,
) {
    val c = card.card
    // 앞면 선택에 따라 단어/뜻 중 먼저 보이는 쪽이 달라진다. 읽기(후리가나)는 단어와 함께 보인다.
    val termFirst = frontSide == FrontSide.TERM
    val shown = if (termFirst) c.front else c.back
    val hidden = if (termFirst) c.back else c.front

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(Modifier.weight(1f).fillMaxWidth().clickable(onClick = onFlip)) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(shown, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                if (termFirst && !c.reading.isNullOrBlank()) {
                    Text(c.reading, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (flipped) {
                    Box(Modifier.height(24.dp))
                    Text(hidden, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    if (!termFirst && !c.reading.isNullOrBlank()) {
                        Text(c.reading, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    if (!c.extra.isNullOrBlank()) {
                        Box(Modifier.height(16.dp))
                        Text(
                            c.extra,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    Box(Modifier.height(24.dp))
                    Text("탭해서 뒤집기", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(
            "알고있음 ${card.progress.knownCount}회 · 모름 ${card.progress.unknownCount}회",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onAnswer(false) },
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("모름") }
            Button(
                onClick = { onAnswer(true) },
                modifier = Modifier.weight(1f).height(56.dp),
            ) { Text("알고있음") }
        }
    }
}
