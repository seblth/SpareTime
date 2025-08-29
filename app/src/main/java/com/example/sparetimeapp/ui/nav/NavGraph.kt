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

object Routes {
    const val Onboarding = "onboarding"
    const val Dashboard  = "dashboard"
    const val Rules      = "rules"
    // Optional: const val DevTools  = "devtools"
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
                onOpenOnboarding = { nav.navigate(Routes.Onboarding) },
                onOpenRules      = { nav.navigate(Routes.Rules) }
                // Optional: onOpenDevTools = { nav.navigate(Routes.DevTools) }
            )
        }

        composable(Routes.Rules) {
            RulesScreen(
                repo = repo,
                onBack = { nav.popBackStack(Routes.Dashboard, false) } // 👈 zurück zum Dashboard
            )
        }

        // Optionaler Test-Screen später:
        // composable(Routes.DevTools) { DevToolsScreen(repo = repo) }
    }
}
