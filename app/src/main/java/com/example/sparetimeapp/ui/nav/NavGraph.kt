package com.example.sparetimeapp.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.sparetimeapp.ui.onboarding.OnboardingScreen
import com.example.sparetimeapp.ui.dashboard.DashboardScreen
import com.example.sparetimeapp.ui.rules.RulesScreen
import com.example.sparetimeapp.data.RulesRepo
import com.example.sparetimeapp.ui.devtools.DevToolsScreen


object Routes {
    const val Onboarding = "onboarding"
    const val Dashboard  = "dashboard"
    const val Rules      = "rules"
    const val DevTools  = "devtools"
}

@Composable
fun NavGraph(
    repo: RulesRepo,
    startDestination: String = Routes.Onboarding
) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = startDestination) {

        composable(Routes.Onboarding) {
            OnboardingScreen(
                onAllGranted = {
                    nav.navigate(Routes.Dashboard) {
                        popUpTo(Routes.Onboarding) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Dashboard) {
            DashboardScreen(
                repo = repo,
                onOpenOnboarding = { nav.navigate(Routes.Onboarding) },
                onOpenRules      = { nav.navigate(Routes.Rules) },
                onOpenDevTools   = { nav.navigate(Routes.DevTools) }
            )
        }

        composable(Routes.Rules) {
            RulesScreen(
                repo = repo,
                onBackToDashboard = { nav.popBackStack(Routes.Dashboard, false) }
            )
        }

        composable(Routes.DevTools) {
            DevToolsScreen(
                repo = repo,
                onBack = { nav.popBackStack() }
            )
        }
    }
}
