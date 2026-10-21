package com.kanghm.memocards.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kanghm.memocards.R

/** 손글씨 느낌의 제목용 글꼴 (Gaegu Bold, SIL OFL 1.1). 본문은 읽기 편하도록 기본 글꼴을 그대로 쓴다. */
val Handwriting = FontFamily(Font(R.font.gaegu_bold, FontWeight.Bold))

private val Base = Typography()

/**
 * 큰 글자(제목·카드 앞뒷면)만 손글씨로 바꾼다. 글꼴 자체가 작아 보여서 크기를 한 단계씩 키웠다.
 */
val MemoTypography = Typography(
    headlineLarge = Base.headlineLarge.copy(fontFamily = Handwriting, fontWeight = FontWeight.Bold, fontSize = Base.headlineLarge.fontSize * 1.15f),
    headlineMedium = Base.headlineMedium.copy(fontFamily = Handwriting, fontWeight = FontWeight.Bold, fontSize = Base.headlineMedium.fontSize * 1.15f),
    headlineSmall = Base.headlineSmall.copy(fontFamily = Handwriting, fontWeight = FontWeight.Bold, fontSize = Base.headlineSmall.fontSize * 1.15f),
    titleLarge = Base.titleLarge.copy(fontFamily = Handwriting, fontWeight = FontWeight.Bold, fontSize = Base.titleLarge.fontSize * 1.15f),
    titleMedium = Base.titleMedium.copy(fontFamily = Handwriting, fontWeight = FontWeight.Bold, fontSize = Base.titleMedium.fontSize * 1.15f),
)
