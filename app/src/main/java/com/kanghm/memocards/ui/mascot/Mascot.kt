package com.kanghm.memocards.ui.mascot

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kanghm.memocards.domain.MascotStage

/** 마스코트 표정. 학습 상황에 따라 바뀐다. */
enum class MascotMood {
    /** 기본: 눈을 깜빡이며 가볍게 숨 쉰다. */
    IDLE,

    /** 알고있음: 웃는 눈(^ ^)과 통통 튄다. */
    HAPPY,

    /** 모름: 눈물을 글썽이며 풀이 죽는다. */
    SAD,

    /** 한 바퀴 완료 / 목표 달성: 크게 웃으며 신나게 뛴다. */
    CHEER,

    /** 복습할 카드 없음: 눈을 감고 졸고 있다. */
    SLEEPY,
}

private val Body = Color(0xFFFFE1EA)
private val BodyShade = Color(0xFFFFC6D6)
private val EarInner = Color(0xFFFFA7C0)
private val Ink = Color(0xFF4A2A36)
private val Cheek = Color(0xFFFF8FAF)
private val Tear = Color(0xFF8FD3FF)

/**
 * 직접 그린 토끼 마스코트. 이모지와 달리 표정과 움직임을 상황에 맞게 바꿀 수 있다.
 * [stage]가 올라가면 소품(리본·별·왕관)이 하나씩 늘어난다.
 */
@Composable
fun Mascot(
    mood: MascotMood,
    modifier: Modifier = Modifier,
    stage: MascotStage = MascotStage.BABY,
    size: Dp = 120.dp,
) {
    val t = rememberInfiniteTransition(label = "mascot")

    // 숨쉬기: 몸이 아주 살짝 커졌다 작아진다.
    val breath by t.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    // 통통 튀기: HAPPY/CHEER에서만 쓴다. 0(바닥) → 1(최고점)
    val hopPeriod = if (mood == MascotMood.CHEER) 520 else 640
    val hop by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(hopPeriod, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hop",
    )
    // 깜빡임: 대부분 0(뜬 눈), 짧게 1(감은 눈)
    val blink by t.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = InfiniteRepeatableSpec(
            keyframes {
                durationMillis = 3200
                0f at 0
                0f at 2900
                1f at 3000
                0f at 3100
            },
        ),
        label = "blink",
    )
    // 귀 흔들림
    val ear by t.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ear",
    )

    val jumping = mood == MascotMood.HAPPY || mood == MascotMood.CHEER
    val lift = if (jumping) hop * (if (mood == MascotMood.CHEER) 0.14f else 0.08f) else 0f
    val sag = if (mood == MascotMood.SAD) 0.04f else 0f

    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // 위로 뛰는 만큼 올리고, 풀이 죽으면 내려앉는다.
        translate(top = -lift * h + sag * h) {
            if (stage == MascotStage.EGG) {
                // 알 단계: 아직 부화 전이라 토끼 대신 알이 얼굴을 하고 있다.
                rotate(degrees = ear * 0.8f, pivot = Offset(w / 2, h * 0.95f)) {
                    drawEgg(w, h, breath)
                    drawFace(w, h, mood, blink)
                }
            } else {
                drawEars(w, h, if (mood == MascotMood.SAD) 14f else ear, droop = mood == MascotMood.SAD)
                drawBody(w, h, breath)
                drawFace(w, h, mood, blink)
                drawAccessories(w, h, stage)
            }
        }
    }
}

private fun DrawScope.drawEars(w: Float, h: Float, swing: Float, droop: Boolean) {
    val earW = w * 0.20f
    val earH = h * 0.46f
    fun ear(cx: Float, dir: Float) {
        // 귀 뿌리를 중심으로 회전. 풀이 죽으면 양옆으로 늘어진다.
        val pivot = Offset(cx, h * 0.42f)
        val angle = if (droop) dir * 38f else dir * 8f + swing * dir * -1f
        rotate(degrees = angle, pivot = pivot) {
            drawOval(Body, Offset(cx - earW / 2, h * 0.42f - earH), Size(earW, earH))
            drawOval(EarInner, Offset(cx - earW * 0.32f, h * 0.42f - earH * 0.84f), Size(earW * 0.64f, earH * 0.66f))
        }
    }
    ear(w * 0.36f, -1f)
    ear(w * 0.64f, 1f)
}

private fun DrawScope.drawBody(w: Float, h: Float, breath: Float) {
    // 통통한 둥근 몸. 숨쉴 때 바닥 기준으로 크기가 변한다.
    scale(breath, breath, pivot = Offset(w / 2, h * 0.9f)) {
        drawOval(BodyShade, Offset(w * 0.12f, h * 0.36f), Size(w * 0.76f, h * 0.60f))
        drawOval(Body, Offset(w * 0.12f, h * 0.34f), Size(w * 0.76f, h * 0.58f))
        // 발
        drawOval(Body, Offset(w * 0.24f, h * 0.86f), Size(w * 0.22f, h * 0.11f))
        drawOval(Body, Offset(w * 0.54f, h * 0.86f), Size(w * 0.22f, h * 0.11f))
    }
}

private fun DrawScope.drawFace(w: Float, h: Float, mood: MascotMood, blink: Float) {
    val eyeY = h * 0.60f
    val leftX = w * 0.36f
    val rightX = w * 0.64f
    val stroke = Stroke(width = w * 0.032f, cap = StrokeCap.Round)

    // 볼터치
    val cheekAlpha = if (mood == MascotMood.SAD) 0.35f else 0.8f
    drawOval(Cheek.copy(alpha = cheekAlpha), Offset(w * 0.20f, h * 0.65f), Size(w * 0.13f, h * 0.075f))
    drawOval(Cheek.copy(alpha = cheekAlpha), Offset(w * 0.67f, h * 0.65f), Size(w * 0.13f, h * 0.075f))

    when (mood) {
        MascotMood.HAPPY, MascotMood.CHEER -> {
            // 웃는 눈 ^ ^
            happyEye(leftX, eyeY, w, stroke)
            happyEye(rightX, eyeY, w, stroke)
        }
        MascotMood.SLEEPY -> {
            // 감은 눈 ᵕ ᵕ
            closedEye(leftX, eyeY, w, stroke)
            closedEye(rightX, eyeY, w, stroke)
        }
        MascotMood.SAD -> {
            roundEye(leftX, eyeY, w, 0.2f)
            roundEye(rightX, eyeY, w, 0.2f)
            // 눈물
            drawOval(Tear, Offset(leftX - w * 0.01f, eyeY + h * 0.045f), Size(w * 0.03f, h * 0.06f))
            drawOval(Tear, Offset(rightX - w * 0.02f, eyeY + h * 0.045f), Size(w * 0.03f, h * 0.06f))
        }
        MascotMood.IDLE -> {
            roundEye(leftX, eyeY, w, blink)
            roundEye(rightX, eyeY, w, blink)
        }
    }

    // 코와 입
    val mouthY = h * 0.69f
    drawOval(Cheek, Offset(w * 0.485f, h * 0.645f), Size(w * 0.03f, h * 0.02f))
    val mouth = Path()
    when (mood) {
        MascotMood.CHEER -> {
            // 크게 벌린 입
            mouth.moveTo(w * 0.43f, mouthY - h * 0.005f)
            mouth.quadraticTo(w * 0.5f, mouthY + h * 0.11f, w * 0.57f, mouthY - h * 0.005f)
            mouth.close()
            drawPath(mouth, Ink)
            drawOval(Cheek, Offset(w * 0.465f, mouthY + h * 0.04f), Size(w * 0.07f, h * 0.035f))
        }
        MascotMood.HAPPY -> {
            mouth.moveTo(w * 0.45f, mouthY)
            mouth.quadraticTo(w * 0.5f, mouthY + h * 0.06f, w * 0.55f, mouthY)
            drawPath(mouth, Ink, style = stroke)
        }
        MascotMood.SAD -> {
            mouth.moveTo(w * 0.46f, mouthY + h * 0.03f)
            mouth.quadraticTo(w * 0.5f, mouthY - h * 0.02f, w * 0.54f, mouthY + h * 0.03f)
            drawPath(mouth, Ink, style = stroke)
        }
        MascotMood.SLEEPY -> {
            drawOval(Ink.copy(alpha = 0.7f), Offset(w * 0.48f, mouthY), Size(w * 0.04f, h * 0.035f))
        }
        MascotMood.IDLE -> {
            mouth.moveTo(w * 0.46f, mouthY)
            mouth.quadraticTo(w * 0.48f, mouthY + h * 0.035f, w * 0.5f, mouthY)
            mouth.quadraticTo(w * 0.52f, mouthY + h * 0.035f, w * 0.54f, mouthY)
            drawPath(mouth, Ink, style = stroke)
        }
    }
}

/** 동그란 눈. [closed]가 1에 가까우면 가늘게 감는다. */
private fun DrawScope.roundEye(cx: Float, cy: Float, w: Float, closed: Float) {
    val r = w * 0.045f
    val ry = r * (1f - 0.9f * closed.coerceIn(0f, 1f))
    drawOval(Ink, Offset(cx - r, cy - ry), Size(r * 2, ry * 2))
    if (closed < 0.5f) {
        drawCircle(Color.White, r * 0.35f, Offset(cx + r * 0.3f, cy - ry * 0.35f))
    }
}

private fun DrawScope.happyEye(cx: Float, cy: Float, w: Float, stroke: Stroke) {
    val r = w * 0.05f
    val p = Path().apply {
        moveTo(cx - r, cy + r * 0.4f)
        quadraticTo(cx, cy - r * 1.2f, cx + r, cy + r * 0.4f)
    }
    drawPath(p, Ink, style = stroke)
}

private fun DrawScope.closedEye(cx: Float, cy: Float, w: Float, stroke: Stroke) {
    val r = w * 0.05f
    val p = Path().apply {
        moveTo(cx - r, cy)
        quadraticTo(cx, cy + r * 0.9f, cx + r, cy)
    }
    drawPath(p, Ink, style = stroke)
}

/** 성장 단계별 소품: 알(없음) → 아기(없음) → 꼬마(리본) → 스타(별 머리핀) → 왕(왕관). */
private fun DrawScope.drawAccessories(w: Float, h: Float, stage: MascotStage) {
    when (stage) {
        MascotStage.EGG, MascotStage.BABY -> Unit
        MascotStage.KID -> ribbon(w, h, Color(0xFFE5698D))
        MascotStage.STAR -> {
            ribbon(w, h, Color(0xFF7BC8F5))
            star(Offset(w * 0.74f, h * 0.40f), w * 0.07f, Color(0xFFFFD54F))
        }
        MascotStage.KING -> {
            ribbon(w, h, Color(0xFFB388FF))
            crown(w, h)
        }
    }
}

private fun DrawScope.ribbon(w: Float, h: Float, color: Color) {
    val c = Offset(w * 0.5f, h * 0.395f)
    val a = w * 0.09f
    val left = Path().apply {
        moveTo(c.x, c.y); lineTo(c.x - a, c.y - a * 0.7f); lineTo(c.x - a, c.y + a * 0.7f); close()
    }
    val right = Path().apply {
        moveTo(c.x, c.y); lineTo(c.x + a, c.y - a * 0.7f); lineTo(c.x + a, c.y + a * 0.7f); close()
    }
    drawPath(left, color)
    drawPath(right, color)
    drawCircle(color, a * 0.32f, c)
}

private fun DrawScope.star(center: Offset, r: Float, color: Color) {
    val p = Path()
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) r else r * 0.45f
        val ang = Math.toRadians((-90 + i * 36).toDouble())
        val x = center.x + (radius * kotlin.math.cos(ang)).toFloat()
        val y = center.y + (radius * kotlin.math.sin(ang)).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, color)
}

private fun DrawScope.crown(w: Float, h: Float) {
    val gold = Color(0xFFFFD54F)
    val left = w * 0.36f
    val right = w * 0.64f
    val bottom = h * 0.36f
    val top = h * 0.25f
    val p = Path().apply {
        moveTo(left, bottom)
        lineTo(left, top)
        lineTo(w * 0.43f, h * 0.30f)
        lineTo(w * 0.5f, top - h * 0.02f)
        lineTo(w * 0.57f, h * 0.30f)
        lineTo(right, top)
        lineTo(right, bottom)
        close()
    }
    drawPath(p, gold)
    drawCircle(Color(0xFFE5698D), w * 0.014f, Offset(w * 0.5f, bottom - h * 0.03f))
}

private fun DrawScope.drawEgg(w: Float, h: Float, breath: Float) {
    scale(breath, breath, pivot = Offset(w / 2, h * 0.95f)) {
        drawOval(BodyShade, Offset(w * 0.19f, h * 0.15f), Size(w * 0.62f, h * 0.82f))
        drawOval(Color(0xFFFFF4E0), Offset(w * 0.19f, h * 0.13f), Size(w * 0.62f, h * 0.82f))
        // 무늬
        drawCircle(EarInner.copy(alpha = 0.7f), w * 0.04f, Offset(w * 0.34f, h * 0.30f))
        drawCircle(EarInner.copy(alpha = 0.7f), w * 0.03f, Offset(w * 0.62f, h * 0.26f))
        drawCircle(EarInner.copy(alpha = 0.7f), w * 0.035f, Offset(w * 0.70f, h * 0.82f))
        // 금
        val crack = Path().apply {
            moveTo(w * 0.40f, h * 0.20f)
            lineTo(w * 0.46f, h * 0.27f)
            lineTo(w * 0.52f, h * 0.20f)
            lineTo(w * 0.58f, h * 0.27f)
        }
        drawPath(crack, Ink.copy(alpha = 0.5f), style = Stroke(width = w * 0.018f, cap = StrokeCap.Round))
    }
}
