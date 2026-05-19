package com.morphocore.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.morphocore.feature.browse.ui.BrowseScreen
import com.morphocore.feature.detail.ui.DetailScreen
import com.morphocore.feature.movements.ui.MovementsScreen
import com.morphocore.feature.settings.ui.SettingsScreen

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
                onMovementSelected = { id -> navController.navigate(Detail(id)) },
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
        composable<Detail> { backStackEntry ->
            val dest: Detail = backStackEntry.toRoute()
            DetailScreen(
                movementId = dest.movementId,
                onBack = { navController.popBackStack() },
                onNavigateToMovement = { id -> navController.navigate(Detail(id)) }
            )
        }
        composable<Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
