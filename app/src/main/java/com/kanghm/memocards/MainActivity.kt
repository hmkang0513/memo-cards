package com.kanghm.memocards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.kanghm.memocards.ui.cards.CardListScreen
import com.kanghm.memocards.ui.home.HomeScreen
import com.kanghm.memocards.ui.study.StudyScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemoTheme { MemoNavHost() }
        }
    }
}

@Composable
private fun MemoTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    // minSdk 26이라 동적 색상은 31 이상에서만 쓰고, 나머지는 기본 팔레트로 대체한다.
    val scheme = if (android.os.Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) darkColorScheme() else lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
private fun MemoNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("study/{deckId}", arguments = listOf(navArgument("deckId") { type = NavType.LongType })) {
            StudyScreen(deckId = it.arguments!!.getLong("deckId"), onBack = { nav.popBackStack() })
        }
        composable("cards/{deckId}", arguments = listOf(navArgument("deckId") { type = NavType.LongType })) {
            CardListScreen(deckId = it.arguments!!.getLong("deckId"), onBack = { nav.popBackStack() })
        }
        composable("home") {
            HomeScreen(
                onStudy = { nav.navigate("study/$it") },
                onCardList = { nav.navigate("cards/$it") },
                onSettings = { },
            )
        }
    }
}
