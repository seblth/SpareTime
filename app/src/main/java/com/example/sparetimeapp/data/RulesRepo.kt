package com.example.sparetimeapp.data

import android.content.Context
import com.example.sparetimeapp.util.midnightMillis
import com.example.sparetimeapp.util.todayKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class TodayStats(
    val minutesUsed: Int,
    val accessesUsed: Int,
    val blockedUntil: Long
)

class RulesRepo(private val store: SettingsStore, private val appContext: Context) {

    fun ruleFlow(pkg: String): Flow<Rule> = store.ruleFlow(pkg)

    fun todayStatsFlow(pkg: String): Flow<TodayStats> {
        val day = todayKey()
        return combine(
            store.usedMinutesFlow(pkg, day),
            store.usedAccessesFlow(pkg, day),
            store.blockedUntilFlow(pkg, day)
        ) { min, acc, until ->
            TodayStats(min, acc, until)
        }
    }

    suspend fun setRule(pkg: String, minutesLimit: Int?, accessLimit: Int?, notifications: Boolean) {
        store.setRule(pkg, minutesLimit, accessLimit, notifications)
    }

    suspend fun incUsedMinute(pkg: String) = store.incUsedMinute(pkg, todayKey())
    suspend fun incAccess(pkg: String)     = store.incAccess(pkg, todayKey())

    fun isOverLimit(rule: Rule, stats: TodayStats): Boolean {
        val overMin = rule.minutesLimit?.let { stats.minutesUsed >= it } ?: false
        val overAcc = rule.accessLimit?.let { stats.accessesUsed >= it } ?: false
        return overMin || overAcc
    }

    fun isBlockedNow(stats: TodayStats, now: Long = System.currentTimeMillis()): Boolean =
        stats.blockedUntil > now

    suspend fun blockUntilMidnight(pkg: String) {
        store.setBlockedUntil(pkg, todayKey(), midnightMillis())
    }

    suspend fun clearBlock(pkg: String) {
        store.clearBlocked(pkg, todayKey())
    }

    fun packagesFlow(): Flow<List<String>> =
        store.packagesFlow().map { it.sorted() }

    suspend fun deletePackage(pkg: String) = store.deletePackage(pkg)
    suspend fun clearTodayForPackage(pkg: String) = store.clearTodayForPackage(pkg)
}
