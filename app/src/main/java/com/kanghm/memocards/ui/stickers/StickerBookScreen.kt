package com.kanghm.memocards.ui.stickers

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanghm.memocards.MemoApp
import com.kanghm.memocards.data.Stats
import com.kanghm.memocards.domain.ALL_STICKERS
import com.kanghm.memocards.domain.MascotStage
import com.kanghm.memocards.domain.Sticker
import com.kanghm.memocards.ui.mascot.Mascot
import com.kanghm.memocards.ui.mascot.MascotMood

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerBookScreen(onBack: () -> Unit) {
    val repository = (LocalContext.current.applicationContext as MemoApp).repository
    val stats by remember { repository.observeStats() }.collectAsStateWithLifecycle(Stats())
    val stage = MascotStage.of(stats.totalKnown)
    val achievedCount = ALL_STICKERS.count { it.achieved(stats) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("스티커북", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로") }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Mascot(mood = MascotMood.HAPPY, stage = stage)
                    Text("${stage.title} 단계", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "알고있음 누적 ${stats.totalKnown}번 · 스티커 $achievedCount/${ALL_STICKERS.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(ALL_STICKERS, key = { it.id }) { sticker ->
                StickerCell(sticker, achieved = sticker.achieved(stats))
            }
        }
    }
}

@Composable
private fun StickerCell(sticker: Sticker, achieved: Boolean) {
    // 달성한 스티커는 통통 튀며 붙는다. 못 받은 스티커는 흐리게 자물쇠로 보인다.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(achieved) { shown = true }
    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "sticker",
    )
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (achieved) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box {
                Text(if (achieved) sticker.emoji else "🔒", fontSize = 40.sp, modifier = Modifier.graphicsLayer { alpha = if (achieved) 1f else 0.5f })
            }
            Text(
                if (achieved) sticker.title else "???",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                sticker.condition,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
