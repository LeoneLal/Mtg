package fr.leonelal.mtgduel.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.collectAsState
import fr.leonelal.mtgduel.MtgDuelApp
import fr.leonelal.mtgduel.ui.screens.DeckFormScreen
import fr.leonelal.mtgduel.ui.screens.DecksScreen
import fr.leonelal.mtgduel.ui.screens.GameFormScreen
import fr.leonelal.mtgduel.ui.screens.GamesScreen
import fr.leonelal.mtgduel.ui.screens.SettingsScreen
import fr.leonelal.mtgduel.ui.screens.StatsScreen

object Routes {
    const val GAMES = "games"
    const val DECKS = "decks"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val GAME_FORM = "game_form"
    const val DECK_FORM = "deck_form"

    fun gameForm(id: String? = null) = if (id == null) "$GAME_FORM?id=" else "$GAME_FORM?id=$id"
    fun deckForm(id: String? = null) = if (id == null) "$DECK_FORM?id=" else "$DECK_FORM?id=$id"
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.GAMES, "Parties", Icons.Filled.History),
    TabItem(Routes.DECKS, "Decks", Icons.Filled.Style),
    TabItem(Routes.STATS, "Stats", Icons.Filled.Insights),
    TabItem(Routes.SETTINGS, "Réglages", Icons.Filled.Settings)
)

@Composable
fun AppRoot(
    viewModel: AppViewModel = viewModel(
        factory = AppViewModel.factory(LocalContext.current.applicationContext as MtgDuelApp)
    )
) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            modifier = Modifier.testTag("onglet-${tab.route}"),
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.GAMES
        ) {
            composable(Routes.GAMES) {
                GamesScreen(
                    state = state,
                    contentPadding = innerPadding,
                    onNewGame = { navController.navigate(Routes.gameForm()) },
                    onOpenGame = { navController.navigate(Routes.gameForm(it)) },
                    onOpenDecks = { navController.navigate(Routes.DECKS) }
                )
            }
            composable(Routes.DECKS) {
                DecksScreen(
                    state = state,
                    contentPadding = innerPadding,
                    onNewDeck = { navController.navigate(Routes.deckForm()) },
                    onOpenDeck = { navController.navigate(Routes.deckForm(it)) }
                )
            }
            composable(Routes.STATS) {
                StatsScreen(state = state, contentPadding = innerPadding)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    state = state,
                    viewModel = viewModel,
                    contentPadding = innerPadding
                )
            }
            composable(
                route = "${Routes.GAME_FORM}?id={id}",
                arguments = listOf(navArgument("id") {
                    type = NavType.StringType
                    defaultValue = ""
                })
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty().ifEmpty { null }
                GameFormScreen(
                    state = state,
                    viewModel = viewModel,
                    gameId = id,
                    onDone = { navController.popBackStack() },
                    onManageDecks = { navController.navigate(Routes.DECKS) }
                )
            }
            composable(
                route = "${Routes.DECK_FORM}?id={id}",
                arguments = listOf(navArgument("id") {
                    type = NavType.StringType
                    defaultValue = ""
                })
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty().ifEmpty { null }
                DeckFormScreen(
                    state = state,
                    viewModel = viewModel,
                    deckId = id,
                    onDone = { navController.popBackStack() }
                )
            }
        }
    }
}
