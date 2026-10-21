package com.kanghm.memocards.ui.study

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kanghm.memocards.data.CardWithProgress
import com.kanghm.memocards.data.FrontSide
import com.kanghm.memocards.domain.MascotStage
import com.kanghm.memocards.ui.mascot.Mascot
import com.kanghm.memocards.ui.mascot.MascotMood
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(deckId: Long, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as android.app.Application
    val viewModel: StudyViewModel = viewModel(
        key = "study-$deckId",
        factory = viewModelFactory { initializer { StudyViewModel(app, deckId) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val stage = MascotStage.of(stats.totalKnown)
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.notice) {
        state.notice?.let {
            snackbar.showSnackbar(it)
            viewModel.noticeShown()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
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
                    stage = stage,
                    onRestart = viewModel::restart,
                    onHome = onBack,
                )
                else -> StudyBody(state = state, card = card, stage = stage, onAnswer = viewModel::answer)
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

private val CHEERS = listOf("좋아!", "대단해!", "척척이네!", "맞았다!", "역시!")

/** 암기 중 화면: 진행 막대, 반응하는 마스코트, 겹쳐진 카드, 응답 버튼. */
@Composable
private fun StudyBody(state: StudyUiState, card: CardWithProgress, stage: MascotStage, onAnswer: (Boolean) -> Unit) {
    val progress by animateFloatAsState(
        targetValue = if (state.total == 0) 0f else state.sessionKnown.toFloat() / state.total,
        animationSpec = tween(300),
        label = "progress",
    )

    // 응답할 때마다 마스코트가 잠깐 반응하고 다시 평소 표정으로 돌아온다.
    var mood by remember { mutableStateOf(MascotMood.IDLE) }
    var bubble by remember { mutableStateOf("오른쪽으로 밀면 알고있음, 왼쪽으로 밀면 모름이야") }
    LaunchedEffect(state.answered) {
        if (state.answered == 0) return@LaunchedEffect
        if (state.lastAnswerKnown) {
            val streak = state.combo >= 3
            mood = if (streak) MascotMood.CHEER else MascotMood.HAPPY
            bubble = if (streak) "${state.combo}연속! 🔥" else CHEERS[state.answered % CHEERS.size]
        } else {
            mood = MascotMood.SAD
            bubble = "괜찮아, 다시 보면 돼!"
        }
        delay(1300)
        mood = MascotMood.IDLE
    }

    Column(Modifier.fillMaxSize()) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Mascot(mood = mood, stage = stage, size = 76.dp)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(
                    bubble,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        val width = LocalConfiguration.current.screenWidthDp.dp
        val widthPx = with(LocalDensity.current) { width.toPx() }
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        // 응답마다 새 SwipeState를 만들어, 다음 카드는 항상 중앙에서 시작한다.
        val swipe = remember(state.answered) { SwipeState(widthPx, scope, haptic, onAnswer) }

        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(bottom = 18.dp)) {
                // 뒤에 겹쳐 보이는 다음 카드들
                state.upcoming.asReversed().forEachIndexed { index, next ->
                    BackCard(next, depth = state.upcoming.size - index)
                }
                SwipeCard(card = card, seq = state.answered, frontSide = state.frontSide, swipe = swipe)
            }
            Text(
                "알고있음 ${card.progress.knownCount}회 · 모름 ${card.progress.unknownCount}회",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnswerButton(
                    text = "← 모름",
                    selected = swipe.result == false,
                    onClick = { swipe.fling(false) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                )
                AnswerButton(
                    text = "알고있음 →",
                    selected = swipe.result == true,
                    onClick = { swipe.fling(true) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                )
            }
        }
    }
}

/**
 * 한 장의 스와이프 상태. 버튼과 드래그가 같은 동작(fling)을 쓰도록 위로 끌어올렸다.
 * [fling]은 카드를 화면 밖으로 날린 뒤에 응답을 넘긴다. 중복 호출은 무시한다.
 */
@Stable
private class SwipeState(
    val widthPx: Float,
    private val scope: CoroutineScope,
    private val haptic: HapticFeedback,
    private val onAnswer: (Boolean) -> Unit,
) {
    val offsetX = Animatable(0f)
    var result by mutableStateOf<Boolean?>(null)
        private set

    val threshold get() = widthPx * 0.28f

    /** -1(모름 쪽 끝) ~ 1(알고있음 쪽 끝). 색·도장·표정의 세기를 정한다. */
    val progress: Float
        get() = when (result) {
            true -> 1f
            false -> -1f
            null -> (offsetX.value / threshold).coerceIn(-1f, 1f)
        }

    fun fling(known: Boolean) {
        if (result != null) return
        result = known
        haptic.performHapticFeedback(if (known) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress)
        scope.launch {
            offsetX.animateTo(if (known) widthPx * 1.3f else -widthPx * 1.3f, tween(260))
            onAnswer(known)
        }
    }

    fun drag(dx: Float) {
        if (result == null) scope.launch { offsetX.snapTo(offsetX.value + dx) }
    }

    fun release() {
        if (result != null) return
        if (abs(offsetX.value) > threshold) fling(offsetX.value > 0) else cancel()
    }

    fun cancel() {
        if (result == null) scope.launch { offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
    }
}

/** 카드마다 정해진 작은 기울기(±2도 안팎). 같은 카드는 항상 같은 각도라서 포스트잇처럼 보인다. */
private fun tilt(uid: String): Float = ((uid.hashCode() and 0x7fffffff) % 7 - 3) * 0.7f

@Composable
private fun BackCard(card: CardWithProgress, depth: Int) {
    Card(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val s = 1f - 0.05f * depth
                scaleX = s
                scaleY = s
                translationY = 14.dp.toPx() * depth
                rotationZ = tilt(card.card.uid) * depth
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {}
}

@Composable
private fun SwipeCard(card: CardWithProgress, seq: Int, frontSide: FrontSide, swipe: SwipeState) {
    var flipped by remember(seq, frontSide) { mutableStateOf(false) }
    val flip by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(400),
        label = "flip",
    )
    // 새 카드는 뒤 카드 크기(0.95)에서 커지며 앞으로 나온다.
    val enter = remember { Animatable(0.95f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(200)) }

    val p = swipe.progress
    val scheme = MaterialTheme.colorScheme
    val base = scheme.surfaceContainerHighest
    val cardColor = if (p >= 0f) lerp(base, scheme.secondaryContainer, p) else lerp(base, scheme.errorContainer, -p)
    val strength = abs(p)

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = swipe.offsetX.value
                // 밀수록 기울어지고, 평소에는 포스트잇처럼 살짝 기울어 있다.
                rotationZ = tilt(card.card.uid) + swipe.offsetX.value / swipe.widthPx * 18f
                scaleX = enter.value
                scaleY = enter.value
            }
            .pointerInput(seq) {
                detectDragGestures(
                    onDragEnd = { swipe.release() },
                    onDragCancel = { swipe.cancel() },
                ) { change, drag ->
                    change.consume()
                    swipe.drag(drag.x)
                }
            },
    ) {
        Card(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = flip
                    cameraDistance = 12f * density
                }
                .clickable { flipped = !flipped },
            colors = CardDefaults.cardColors(containerColor = cardColor),
        ) {
            CardFace(card, frontSide, showBack = flip > 90f)
        }

        // 마스킹 테이프
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 0.dp)
                .graphicsLayer { translationY = -8.dp.toPx() }
                .rotate(-3f)
                .size(width = 72.dp, height = 20.dp)
                .background(scheme.tertiary.copy(alpha = 0.55f), RoundedCornerShape(3.dp)),
        )

        // 밀 때 나타나는 도장
        Stamp("알고있음!", scheme.secondary, rotation = -12f, alpha = (p).coerceAtLeast(0f), modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
        Stamp("모름", scheme.error, rotation = 12f, alpha = (-p).coerceAtLeast(0f), modifier = Modifier.align(Alignment.TopEnd).padding(20.dp))

        // 가운데 반응 이모지
        Text(
            if (p >= 0f) "😆" else "😢",
            fontSize = 88.sp,
            modifier = Modifier.align(Alignment.Center).graphicsLayer {
                alpha = strength
                val s = 0.6f + 0.4f * strength
                scaleX = s
                scaleY = s
            },
        )
    }
}

@Composable
private fun Stamp(text: String, color: Color, rotation: Float, alpha: Float, modifier: Modifier = Modifier) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.headlineSmall,
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .rotate(rotation)
            .border(3.dp, color, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 2.dp),
    )
}

/**
 * 글 길이에 맞춰 글자 크기를 낮춘다. 줄바꿈이 들어 있으면 코드로 보고 고정폭 글꼴로 왼쪽 정렬한다.
 * (``` 코드 블록을 제대로 해석하는 건 아니고, 우선 코드가 깨져 보이지 않게 하는 정도다.)
 */
@Composable
private fun faceStyle(text: String, large: Boolean): TextStyle {
    if ('\n' in text) {
        return TextStyle(fontFamily = FontFamily.Monospace, fontSize = if (large) 18.sp else 16.sp, lineHeight = 26.sp)
    }
    val t = MaterialTheme.typography
    val len = text.length
    return if (large) {
        when {
            len <= 14 -> t.headlineLarge
            len <= 40 -> t.headlineMedium
            else -> t.headlineSmall
        }
    } else {
        if (len <= 40) t.headlineSmall else t.titleLarge
    }
}

private fun alignFor(text: String): TextAlign = if ('\n' in text) TextAlign.Start else TextAlign.Center

/** 카드 앞면/뒷면 내용. 뒷면은 Y축 180도 돌아간 상태라 글자가 뒤집히지 않게 한 번 더 돌려준다. */
@Composable
private fun CardFace(card: CardWithProgress, frontSide: FrontSide, showBack: Boolean) {
    val c = card.card
    // 앞면 선택에 따라 단어/뜻 중 먼저 보이는 쪽이 달라진다. 읽기(후리가나)는 단어와 함께 보인다.
    // oneWay 카드(코드 문제 등)는 방향 설정과 상관없이 항상 front가 앞면이다.
    val termFirst = frontSide == FrontSide.TERM || c.oneWay
    val shown = if (termFirst) c.front else c.back
    val hidden = if (termFirst) c.back else c.front

    if (!showBack) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(shown, style = faceStyle(shown, large = true), textAlign = alignFor(shown))
            if (termFirst && !c.reading.isNullOrBlank()) {
                Text(c.reading, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Box(Modifier.height(24.dp))
            Text("탭해서 뒤집기", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Column(
            Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                shown,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = alignFor(shown),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
            Box(Modifier.height(16.dp))
            Text(hidden, style = faceStyle(hidden, large = false), textAlign = alignFor(hidden))
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

/** 한 바퀴를 마쳤거나 복습할 카드가 없을 때. 마스코트가 반응하고 도장이 찍힌다. */
@Composable
private fun FinishedView(state: StudyUiState, stage: MascotStage, onRestart: () -> Unit, onHome: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    var stamped by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        shown = true
        delay(700)
        stamped = true
    }
    val doneSomething = state.answered > 0
    val stampScale by animateFloatAsState(
        targetValue = if (stamped) 1f else 2.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "stampScale",
    )
    val stampAlpha by animateFloatAsState(targetValue = if (stamped) 1f else 0f, animationSpec = tween(150), label = "stampAlpha")

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Mascot(mood = if (doneSomething) MascotMood.CHEER else MascotMood.SLEEPY, stage = stage, size = 160.dp)
        Box(Modifier.height(8.dp))
        AnimatedVisibility(visible = shown, enter = fadeIn(tween(500, delayMillis = 200))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (doneSomething) "한 바퀴 끝!" else "지금 복습할 카드가 없어요",
                    style = MaterialTheme.typography.headlineMedium,
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
        if (doneSomething) {
            Box(Modifier.height(20.dp))
            Text(
                "참 잘했어요!",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = stampScale
                        scaleY = stampScale
                        alpha = stampAlpha
                    }
                    .rotate(-10f)
                    .border(4.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                    .padding(horizontal = 18.dp, vertical = 4.dp),
            )
        }
        Box(Modifier.height(28.dp))
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
        modifier = modifier.height(52.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        colors = colors,
        interactionSource = interaction,
    ) { Text(text) }
}
