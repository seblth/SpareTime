package com.example.sparetimeapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sparetimeapp.data.RulesRepo
import com.example.sparetimeapp.data.TodayStats
import kotlinx.coroutines.flow.*

class DashboardViewModel(private val repo: RulesRepo) : ViewModel() {

    data class Totals(val minutes: Int, val accesses: Int, val blockedCount: Int)
    data class AppToday(val pkg: String, val stats: TodayStats)

    val totals: StateFlow<Totals> = repo.packagesFlow()
        .flatMapLatest { pkgs ->
            if (pkgs.isEmpty()) flowOf(Totals(0, 0, 0))
            else combine(pkgs.map { repo.todayStatsFlow(it) }) { arr ->
                val now = System.currentTimeMillis()
                val minutes = arr.sumOf { it.minutesUsed }
                val accesses = arr.sumOf { it.accessesUsed }
                val blockedCount = arr.count { it.blockedUntil > now }
                Totals(minutes, accesses, blockedCount)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Totals(0, 0, 0))

    // Top 5 Apps nach Minuten heute
    val topByMinutes: StateFlow<List<AppToday>> = repo.packagesFlow()
        .flatMapLatest { pkgs ->
            if (pkgs.isEmpty()) flowOf(emptyList())
            else combine(pkgs.map { pkg -> repo.todayStatsFlow(pkg).map { AppToday(pkg, it) } }) { arr ->
                arr.sortedByDescending { it.stats.minutesUsed }.take(5)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}