package com.example.sparetimeapp.data

import android.content.Context
import com.example.sparetimeapp.data.util.DebugLog
import com.example.sparetimeapp.util.midnightMillis
import com.example.sparetimeapp.util.todayKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

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

    suspend fun setRule(
        pkg: String,
        minutesLimit: Int?,
        accessLimit: Int?,
        notifications: Boolean,
        countingMode: String = "foreground",
        allowanceMinutes: Int? = null
    ) {
        store.setRule(
            pkg = pkg,
            minutesLimit = minutesLimit,
            accessLimit = accessLimit,
            notifications = notifications,
            countingMode = countingMode,
            allowanceMinutes = allowanceMinutes
        )
        DebugLog.d("RULES", "SetRule: $pkg min=$minutesLimit acc=$accessLimit")
    }

    // Komfort: Regel setzen UND sofort neu bewerten
    suspend fun setRuleAndReevaluate(
        pkg: String,
        minutesLimit: Int?,
        accessLimit: Int?,
        notifications: Boolean,
        countingMode: String = "foreground",
        allowanceMinutes: Int? = null
    ) {
        setRule(pkg, minutesLimit, accessLimit, notifications, countingMode, allowanceMinutes)
        reevaluateBlockFor(pkg)
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

    // Re-eval: passt Blockstatus an die (neue) Regel + heutigen Stand an
    suspend fun reevaluateBlockFor(pkg: String) {
        val rule  = ruleFlow(pkg).first()
        val stats = todayStatsFlow(pkg).first()

        val over    = isOverLimit(rule, stats)
        val blocked = isBlockedNow(stats)

        when {
            over && !blocked -> blockUntilMidnight(pkg) // jetzt über Limit → sperren
            !over && blocked -> clearBlock(pkg)         // nicht mehr über Limit → entsperren
            else -> { /* keine Änderung */ }
        }
    }

    fun packagesFlow(): Flow<List<String>> =
        store.packagesFlow().map { it.sorted() }

    suspend fun deletePackage(pkg: String){
        store.deletePackage(pkg)
        DebugLog.d("RULES", "DeleteRule: $pkg")
    }
    suspend fun clearTodayForPackage(pkg: String) = store.clearTodayForPackage(pkg)

    // --- NEW: Allowance (heute) ---
    fun allowanceUntilFlow(pkg: String): Flow<Long> =
        store.allowanceUntilFlow(pkg, todayKey())

    suspend fun startAllowance(pkg: String, minutes: Int) {
        val end = System.currentTimeMillis() + minutes * 60_000L
        store.setAllowanceUntil(pkg, todayKey(), end)
    }

    suspend fun clearAllowance(pkg: String) {
        store.clearAllowance(pkg, todayKey())
    }

    // Top-level Totals für heute
    fun totalsTodayFlow(): Flow<Totals> = packagesFlow().flatMapLatest { pkgs ->
        if (pkgs.isEmpty()) flowOf(Totals(0,0,0))
        else combine(pkgs.map { p -> todayStatsFlow(p) }) { arr ->
            val now = System.currentTimeMillis()
            Totals(
                minutes = arr.sumOf { it.minutesUsed },
                accesses = arr.sumOf { it.accessesUsed },
                blockedCount = arr.count { it.blockedUntil > now }
            )
        }
    }

    data class Totals(val minutes: Int, val accesses: Int, val blockedCount: Int)

    // Per-App Liste (heute), sortiert nach Minuten
    fun perAppTodayFlow(): Flow<List<AppToday>> = packagesFlow().flatMapLatest { pkgs ->
        if (pkgs.isEmpty()) flowOf(emptyList())
        else combine(pkgs.map { p -> todayStatsFlow(p).map { AppToday(p, it) } }) { arr ->
            arr.sortedByDescending { it.stats.minutesUsed }
        }
    }

    data class AppToday(val pkg: String, val stats: TodayStats)

    /** Setzt für ein Paket die heutigen Nutzungswerte zurück. */
//    suspend fun resetToday(pkg: String) {
//        store.updateStats(pkg) { it.copy(minutesUsed = 0, accessesUsed = 0, blockedUntil = 0L) }
//        store.setAllowanceUntil(pkg, 0L)
//    }
//
//    /** Setzt alle heutigen Werte für alle beobachteten Pakete zurück. */
//    suspend fun resetAllToday() {
//        val pkgs = packagesFlow().firstOrNull().orEmpty()
//        for (p in pkgs) resetToday(p)
//        store.markTodayAsCurrent()
//    }
//
//    /** Prüft, ob „heute“ schon aktiv ist. Wenn nicht → Tages-Reset. */
//    suspend fun ensureTodayFresh() {
//        val last = store.getLastDayKey()
//        val today = store.todayKey()
//        if (last != today) {
//            val pkgs = packagesFlow().firstOrNull().orEmpty()
//            for (p in pkgs) resetToday(p)
//            store.setLastDayKey(today)
//        }
//    }

        // Optional helper
    private fun now() = System.currentTimeMillis()

}
