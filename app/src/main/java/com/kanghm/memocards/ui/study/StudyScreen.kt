package com.kanghm.memocards.ui.study

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kanghm.memocards.data.CardWithProgress
import com.kanghm.memocards.data.FrontSide
import kotlinx.coroutines.delay

/** 카드 전환 애니메이션의 단위. 응답할 때마다 seq가 달라져, 같은 카드가 다시 나와도 새로 넘어간다. */
private data class Slot(val card: CardWithProgress, val seq: Int)

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
                state.finished || card == null -> FinishedView(
                    state = state,
                    onRestart = viewModel::restart,
                    onHome = onBack,
                )
                else -> Column(Modifier.fillMaxSize()) {
                    val progress by animateFloatAsState(
                        targetValue = if (state.total == 0) 0f else state.sessionKnown.toFloat() / state.total,
                        animationSpec = tween(300),
                        label = "progress",
                    )
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    val known = state.lastAnswerKnown
                    AnimatedContent(
                        targetState = Slot(card, state.answered),
                        contentKey = { it.seq },
                        transitionSpec = {
                            // 알고있음은 왼쪽으로 넘기고, 모름은 오른쪽으로 보낸다.
                            val dir = if (known) -1 else 1
                            (slideInHorizontally(tween(280)) { -it * dir } + fadeIn(tween(280))) togetherWith
                                (slideOutHorizontally(tween(280)) { it * dir } + fadeOut(tween(200)))
                        },
                        label = "card",
                    ) { slot ->
                        CardContent(
                            card = slot.card,
                            seq = slot.seq,
                            frontSide = state.frontSide,
                            onAnswer = viewModel::answer,
                        )
                    }
                }
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

/** 한 바퀴를 마쳤거나 복습할 카드가 없을 때. 처음부터 다시 시작할 수 있다. */
@Composable
private fun FinishedView(state: StudyUiState, onRestart: () -> Unit, onHome: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val doneSomething = state.answered > 0

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = shown,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(),
        ) {
            Text(if (doneSomething) "🎉" else "🌙", fontSize = 88.sp)
        }
        Box(Modifier.height(16.dp))
        AnimatedVisibility(visible = shown, enter = fadeIn(tween(500, delayMillis = 200))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (doneSomething) "한 바퀴 끝!" else "지금 복습할 카드가 없어요",
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (doneSomething) {
                    Box(Modifier.height(8.dp))
                    Text(
                        "알고있음 ${state.sessionKnown}장 · 모름 ${state.sessionUnknown}번",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Box(Modifier.height(32.dp))
        AnimatedVisibility(visible = shown, enter = fadeIn(tween(500, delayMillis = 400))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRestart) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  처음부터 다시", modifier = Modifier.padding(start = 4.dp))
                }
                OutlinedButton(onClick = onHome) { Text("홈으로") }
            }
        }
    }
}

@Composable
private fun CardContent(
    card: CardWithProgress,
    seq: Int,
    frontSide: FrontSide,
    onAnswer: (Boolean) -> Unit,
) {
    // 카드가 바뀌거나 앞면 방향을 바꾸면 다시 앞면에서 시작한다.
    var flipped by remember(seq, frontSide) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(400),
        label = "flip",
    )

    // 응답을 누르면 색·아이콘·진동으로 먼저 보여주고, 잠깐 뒤에 실제 응답을 넘겨 카드가 넘어가게 한다.
    var result by remember(seq) { mutableStateOf<Boolean?>(null) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(result) {
        result?.let {
            delay(ANSWER_FEEDBACK_MS)
            onAnswer(it)
        }
    }
    val submit: (Boolean) -> Unit = { known ->
        if (result == null) {
            result = known
            haptic.performHapticFeedback(if (known) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress)
        }
    }
    val cardColor by animateColorAsState(
        targetValue = when (result) {
            true -> MaterialTheme.colorScheme.primaryContainer
            false -> MaterialTheme.colorScheme.errorContainer
            null -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(150),
        label = "cardColor",
    )

    val c = card.card
    // 앞면 선택에 따라 단어/뜻 중 먼저 보이는 쪽이 달라진다. 읽기(후리가나)는 단어와 함께 보인다.
    val termFirst = frontSide == FrontSide.TERM
    val shown = if (termFirst) c.front else c.back
    val hidden = if (termFirst) c.back else c.front

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
        Card(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clickable { flipped = !flipped },
            colors = CardDefaults.cardColors(containerColor = cardColor),
        ) {
            if (rotation <= 90f) {
                // 앞면
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(shown, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                    if (termFirst && !c.reading.isNullOrBlank()) {
                        Text(c.reading, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Box(Modifier.height(24.dp))
                    Text("탭해서 뒤집기", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // 뒷면: 180도 돌아간 상태라 글자가 뒤집혀 보이지 않게 한 번 더 돌려준다.
                Column(
                    Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }.padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        shown,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Box(Modifier.height(16.dp))
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
                }
            }
        }
        // 응답 결과 아이콘: 카드 위에 튀어나온다. 카드와 달리 회전하지 않는다.
        androidx.compose.animation.AnimatedVisibility(
            visible = result != null,
            modifier = Modifier.align(Alignment.Center),
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(tween(100)),
        ) {
            Text(if (result == true) "😆" else "😢", fontSize = 88.sp)
        }
        }
        Text(
            "알고있음 ${card.progress.knownCount}회 · 모름 ${card.progress.unknownCount}회",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnswerButton(
                text = "모름",
                selected = result == false,
                onClick = { submit(false) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            )
            AnswerButton(
                text = "알고있음",
                selected = result == true,
                onClick = { submit(true) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            )
        }
    }
}

/** 누르는 동안 눌리고, 선택되면 살짝 커지는 응답 버튼. */
@Composable
private fun AnswerButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ButtonColors,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.92f
            selected -> 1.08f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "buttonScale",
    )
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        colors = colors,
        interactionSource = interaction,
    ) { Text(text) }
}

private const val ANSWER_FEEDBACK_MS = 220L
