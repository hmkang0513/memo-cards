package com.kanghm.memocards.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kanghm.memocards.data.Stats
import com.kanghm.memocards.domain.DAILY_GOAL
import com.kanghm.memocards.domain.MascotStage
import com.kanghm.memocards.ui.mascot.Mascot
import com.kanghm.memocards.ui.mascot.MascotMood

/** 홈 맨 위 카드: 마스코트의 한마디, 연속 학습일, 오늘 목표 게이지, 성장 단계. */
@Composable
fun HomeHeader(stats: Stats, modifier: Modifier = Modifier) {
    val stage = MascotStage.of(stats.totalKnown)
    val goalReached = stats.todayAnswered >= DAILY_GOAL
    val mood = if (goalReached) MascotMood.CHEER else MascotMood.IDLE

    val progress by animateFloatAsState(
        targetValue = (stats.todayAnswered.toFloat() / DAILY_GOAL).coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "goal",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Mascot(mood = mood, stage = stage, size = 112.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(speech(stats, stage, goalReached), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "🔥 ${stats.streak}일 연속 · ${stage.title} 단계",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(MaterialTheme.shapes.small),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface,
                )
                Text(
                    "오늘 ${minOf(stats.todayAnswered, DAILY_GOAL)} / $DAILY_GOAL" + nextStageText(stats.totalKnown),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

private fun speech(stats: Stats, stage: MascotStage, goalReached: Boolean): String = when {
    stats.totalKnown == 0 -> "같이 외워볼까? 알고있음을 누르면 내가 자라!"
    goalReached -> "오늘 목표 달성! 최고야 🎉"
    stats.todayAnswered == 0 && stats.streak > 0 -> "오늘도 시작해볼까? ${stats.streak}일째 이어가는 중!"
    stats.todayAnswered == 0 -> "오늘 첫 카드를 넘겨볼까?"
    stage == MascotStage.KING -> "역시 암기왕! 조금만 더!"
    else -> "좋아, ${DAILY_GOAL - stats.todayAnswered}번만 더!"
}

private fun nextStageText(totalKnown: Int): String {
    val remaining = MascotStage.remainingToNext(totalKnown) ?: return " · 최고 단계!"
    val next = MascotStage.entries.first { it.minKnown > totalKnown }
    return " · ${next.title}까지 $remaining"
}
