package com.kanghm.memocards.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// 파스텔 핑크(기본) + 민트(알고있음) + 피치(포인트) + 크림 배경.
// 글자 대비가 너무 낮지 않도록 primary는 파스텔보다 한 단계 진한 핑크를 쓴다.
private val LightColors = lightColorScheme(
    primary = Color(0xFFD6577F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDCE6),
    onPrimaryContainer = Color(0xFF5C1130),
    secondary = Color(0xFF2E8F78),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3F4E8),
    onSecondaryContainer = Color(0xFF00382C),
    tertiary = Color(0xFFC77A2E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE5CC),
    onTertiaryContainer = Color(0xFF4A2800),
    background = Color.Transparent, // 종이 배경(paperBackground)이 비치도록 투명
    onBackground = Color(0xFF3B2B30),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF3B2B30),
    surfaceVariant = Color(0xFFF8E3E8),
    onSurfaceVariant = Color(0xFF6E5860),
    surfaceContainerHighest = Color(0xFFFFEBF0),
    error = Color(0xFFD64550),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF5C0A12),
    outline = Color(0xFFB89AA4),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB1C8),
    onPrimary = Color(0xFF5E1133),
    primaryContainer = Color(0xFF7C2A4A),
    onPrimaryContainer = Color(0xFFFFDCE6),
    secondary = Color(0xFF8DD8C0),
    onSecondary = Color(0xFF00382C),
    secondaryContainer = Color(0xFF1F4F42),
    onSecondaryContainer = Color(0xFFD3F4E8),
    tertiary = Color(0xFFFFB77C),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF6A4318),
    onTertiaryContainer = Color(0xFFFFE5CC),
    background = Color.Transparent,
    onBackground = Color(0xFFF3DFE5),
    surface = Color(0xFF201519),
    onSurface = Color(0xFFF3DFE5),
    surfaceVariant = Color(0xFF4A3840),
    onSurfaceVariant = Color(0xFFD9C2CA),
    surfaceContainerHighest = Color(0xFF3A2A31),
    error = Color(0xFFFFB3B0),
    onError = Color(0xFF68000F),
    errorContainer = Color(0xFF93000F),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF9E8791),
)

private val CuteShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/** 앱 전체 테마. 기기 배경화면 색(동적 색상)은 쓰지 않고 고정 팔레트로 귀여운 느낌을 유지한다. */
@Composable
fun MemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = CuteShapes,
        typography = MemoTypography,
        content = content,
    )
}
