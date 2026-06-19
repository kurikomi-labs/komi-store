package zed.rainxch.githubstore.utils

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.githubstore.app.desktop.KeyboardNavigation
import zed.rainxch.githubstore.app.desktop.KeyboardNavigationEvent
import zed.rainxch.githubstore.app.navigation.GithubStoreGraph
import zed.rainxch.githubstore.app.navigation.getCurrentScreen

@Composable
fun HandleKeyboardEvents(navController: NavHostController) {
    val currentScreen = navController.currentBackStackEntryAsState().value.getCurrentScreen()

    ObserveAsEvents(KeyboardNavigation.events) { event ->
        when (event) {
            KeyboardNavigationEvent.OnCtrlFClick -> {
                if (currentScreen !is GithubStoreGraph.SearchScreen) {
                    navController.navigate(GithubStoreGraph.SearchScreen()) {
                        popUpTo(GithubStoreGraph.HomeScreen) {
                            saveState = true
                        }

                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }
}
