package com.kanghm.memocards.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

/**
 * 도트 노트(점 격자) 종이 배경.
 * 화면의 Scaffold 배경은 투명이라 이 모양이 그대로 비쳐 보인다.
 */
@Composable
fun Modifier.paperBackground(): Modifier {
    val paper = MaterialTheme.colorScheme.surface
    val dot = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    return drawBehind {
        drawRect(paper)
        val step = 22.dp.toPx()
        val r = 1.1.dp.toPx()
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                drawCircle(dot, r, Offset(x, y))
                x += step
            }
            y += step
        }
    }
}
