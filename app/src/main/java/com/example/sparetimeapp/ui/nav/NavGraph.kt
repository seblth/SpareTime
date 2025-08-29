package com.example.sparetimeapp.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.sparetimeapp.ui.onboarding.OnboardingScreen
import com.example.sparetimeapp.ui.home.DashboardScreen

@Composable
fun NavGraph(startDestination: String = "onboarding") {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(
                onAllGranted = {
                    nav.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard") {
            DashboardScreen(nav)
        }
    }
}
