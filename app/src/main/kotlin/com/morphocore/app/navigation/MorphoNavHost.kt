package com.morphocore.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.morphocore.feature.browse.ui.BrowseScreen
import com.morphocore.feature.movements.ui.MovementsScreen

@Composable
fun MorphoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Browse,
        modifier = modifier
    ) {
        composable<Browse> {
            BrowseScreen(
                onDisciplineSelected = { id -> navController.navigate(Movements(id)) },
                onSettingsClick = { navController.navigate(Settings) }
            )
        }
        composable<Movements> { backStackEntry ->
            val dest: Movements = backStackEntry.toRoute()
            MovementsScreen(
                disciplineId = dest.disciplineId,
                onMovementSelected = { id -> navController.navigate(Detail(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<Detail> {
            // Sprint 4
        }
        composable<Settings> {
            // Sprint 4
        }
    }
}
