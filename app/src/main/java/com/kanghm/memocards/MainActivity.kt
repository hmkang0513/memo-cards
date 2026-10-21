package com.kanghm.memocards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.kanghm.memocards.ui.cards.CardListScreen
import com.kanghm.memocards.ui.home.HomeScreen
import com.kanghm.memocards.ui.settings.SettingsScreen
import com.kanghm.memocards.ui.stickers.StickerBookScreen
import com.kanghm.memocards.ui.study.StudyScreen
import com.kanghm.memocards.ui.theme.MemoTheme
import com.kanghm.memocards.ui.theme.paperBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemoTheme {
                Box(Modifier.fillMaxSize().paperBackground()) { MemoNavHost() }
            }
        }
    }
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
        composable("stickers") { StickerBookScreen(onBack = { nav.popBackStack() }) }
        composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
        composable("home") {
            HomeScreen(
                onStudy = { nav.navigate("study/$it") },
                onCardList = { nav.navigate("cards/$it") },
                onSettings = { nav.navigate("settings") },
                onStickers = { nav.navigate("stickers") },
            )
        }
    }
}
