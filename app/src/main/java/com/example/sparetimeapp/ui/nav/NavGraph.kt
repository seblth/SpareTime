package com.example.sparetimeapp.ui.nav

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sparetimeapp.data.RulesRepo
import com.example.sparetimeapp.ui.applist.AppListScreen
import com.example.sparetimeapp.ui.dashboard.DashboardScreen
import com.example.sparetimeapp.ui.devtools.DevToolsScreen
import com.example.sparetimeapp.ui.limits.LimitSettingsScreen
import com.example.sparetimeapp.ui.onboarding.OnboardingScreen
import com.example.sparetimeapp.ui.rules.RulesScreen

object Routes {
    const val Onboarding    = "onboarding"
    const val Dashboard     = "dashboard"
    const val Rules         = "rules"
    const val DevTools      = "devtools"
    const val AppList       = "applist"
    const val LimitSettings = "limitSettings/{pkg}"

    // Helper zum Navigieren mit Paketnamen (URL-safe)
    fun limitSettingsFor(pkg: String) = "limitSettings/${Uri.encode(pkg)}"
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
                onOpenAppList  = { nav.navigate(Routes.AppList) },
                onOpenDevTools = { nav.navigate(Routes.DevTools) },
                onOpenOnboarding = { nav.navigate(Routes.Onboarding) },
            )
        }

        // (Optional) Alte Rules-Ansicht bleibt erreichbar
        composable(Routes.Rules) {
            RulesScreen(
                repo = repo,
                onBack = { nav.popBackStack(Routes.Dashboard, false) }
            )
        }

        composable(Routes.DevTools) {
            DevToolsScreen(
                repo = repo,
                onBack = { nav.popBackStack() }
            )
        }

        // Liste installierter Apps → Auswahl für Limit-Setup
        composable(Routes.AppList) {
            val pm = LocalContext.current.packageManager
            AppListScreen(
                pm = pm,
                onAppSelected = { pkg ->
                    nav.navigate(Routes.limitSettingsFor(pkg))
                },
                onBack = { nav.popBackStack() }
            )
        }

        // Limit-Einstellungen für eine App
        composable(
            route = Routes.LimitSettings,
            arguments = listOf(navArgument("pkg") { type = NavType.StringType })
        ) { backStackEntry ->
            val pkg = backStackEntry.arguments?.getString("pkg") ?: return@composable
            LimitSettingsScreen(
                repo = repo,
                pkg = pkg,
                onBack = {
                    // zurück zum Dashboard (oder einfach popBackStack)
                    nav.popBackStack(route = Routes.Dashboard, inclusive = false)
                }
            )
        }
    }
}
