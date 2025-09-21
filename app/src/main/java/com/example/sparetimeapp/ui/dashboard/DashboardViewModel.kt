package com.example.sparetimeapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sparetimeapp.data.*
import kotlinx.coroutines.flow.*

class DashboardViewModel(private val repo: RulesRepo) : ViewModel() {

    data class Totals(val minutes: Int, val accesses: Int, val blockedCount: Int)

    // Eine UI-Zeile inkl. Limits und echtem Status
    data class AppRow(
        val pkg: String,
        val stats: TodayStats,
        val limitMinutes: Int?,     // <- für Progress
        val limitAccesses: Int?,
        val countingMode: String,
        val allowanceUntil: Long,
        val isBlocked: Boolean,
        val overLimit: Boolean
    )

    private fun blockedNow(
        rule: Rule,
        stats: TodayStats,
        allowanceUntil: Long,
        now: Long
    ): Pair<Boolean, Boolean> {
        val over = repo.isOverLimit(rule, stats)
        val timed = repo.isBlockedNow(stats, now)
        val allowanceExpired = rule.countingMode == "allowance" && allowanceUntil <= now
        return (timed || over || allowanceExpired) to over
    }

    private fun <T> combineList(flows: List<Flow<T>>): Flow<List<T>> = when (flows.size) {
        0 -> flowOf(emptyList())
        1 -> flows[0].map { listOf(it) }
        else -> flows.drop(1).fold(flows.first().map { listOf(it) }) { acc, f ->
            acc.combine(f) { list, v -> list + v }
        }
    }

    private val pkgs = repo.packagesFlow().distinctUntilChanged()

    // Totals
    private val totalsMinutesAccesses: Flow<Pair<Int, Int>> =
        pkgs.flatMapLatest { list ->
            if (list.isEmpty()) flowOf(0 to 0)
            else combineList(list.map { repo.todayStatsFlow(it) })
                .map { s -> s.sumOf { it.minutesUsed } to s.sumOf { it.accessesUsed } }
        }

    private val blockedCountFlow: Flow<Int> =
        pkgs.flatMapLatest { list ->
            if (list.isEmpty()) flowOf(0)
            else {
                val perPkg = list.map { p ->
                    combine(
                        repo.ruleFlow(p),
                        repo.todayStatsFlow(p),
                        repo.allowanceUntilFlow(p)
                    ) { rule, stats, allow ->
                        val (blocked, _) = blockedNow(rule, stats, allow, System.currentTimeMillis())
                        blocked
                    }
                }
                combineList(perPkg).map { flags -> flags.count { it } }
            }
        }

    val totals: StateFlow<Totals> =
        combine(totalsMinutesAccesses, blockedCountFlow) { (min, acc), blocked ->
            Totals(min, acc, blocked)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Totals(0, 0, 0))

    // Top 5 Apps mit echtem Status + Limits
    val topByMinutes: StateFlow<List<AppRow>> =
        pkgs.flatMapLatest { list ->
            if (list.isEmpty()) flowOf(emptyList())
            else {
                val perPkg: List<Flow<AppRow>> = list.map { p ->
                    combine(
                        repo.ruleFlow(p),
                        repo.todayStatsFlow(p),
                        repo.allowanceUntilFlow(p)
                    ) { rule, stats, allow ->
                        val now = System.currentTimeMillis()
                        val (blocked, over) = blockedNow(rule, stats, allow, now)
                        AppRow(
                            pkg = p,
                            stats = stats,
                            limitMinutes = rule.minutesLimit,
                            limitAccesses = rule.accessLimit,
                            countingMode = rule.countingMode,
                            allowanceUntil = allow,
                            isBlocked = blocked,
                            overLimit = over
                        )
                    }
                }
                combineList(perPkg).map { rows ->
                    rows.sortedByDescending { it.stats.minutesUsed }.take(5)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Optional: aktuell blockierte Apps
    val blockedApps: StateFlow<List<AppRow>> =
        pkgs.flatMapLatest { list ->
            if (list.isEmpty()) flowOf(emptyList())
            else {
                val perPkg = list.map { p ->
                    combine(
                        repo.ruleFlow(p),
                        repo.todayStatsFlow(p),
                        repo.allowanceUntilFlow(p)
                    ) { rule, stats, allow ->
                        val now = System.currentTimeMillis()
                        val (blocked, over) = blockedNow(rule, stats, allow, now)
                        AppRow(
                            pkg = p,
                            stats = stats,
                            limitMinutes = rule.minutesLimit,
                            limitAccesses = rule.accessLimit,
                            countingMode = rule.countingMode,
                            allowanceUntil = allow,
                            isBlocked = blocked,
                            overLimit = over
                        )
                    }
                }
                combineList(perPkg).map { rows -> rows.filter { it.isBlocked } }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
