package com.example.sparetimeapp.focus

import android.content.Context
import com.example.sparetimeapp.data.RulesRepo
import com.example.sparetimeapp.data.TodayStats
import com.example.sparetimeapp.data.util.DebugLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class SessionController(
    private val appContext: Context,
    private val repo: RulesRepo,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private var minuteJob: Job? = null
    private var allowanceWatchJob: Job? = null

    // Aktive App
    @Volatile private var currentPkg: String? = null

    // --- Variante B: Nur beobachtete Pakete (mit Regeln) zählen ---
    private val observedPkgs = ConcurrentHashMap.newKeySet<String>()

    init {
        // Laufend aktualisieren, welche Pakete Regeln/Index haben
        scope.launch {
            repo.packagesFlow().collectLatest { pkgs ->
                observedPkgs.clear()
                observedPkgs.addAll(pkgs)
                DebugLog.d("M3", "observedPkgs updated: ${pkgs.size} pkgs")
            }
        }
    }

    fun onAppOpened(pkg: String) {
        // HARD FILTER: ignorieren, wenn nicht beobachtet -> ABER TICKER STOPPEN!
        if (!observedPkgs.contains(pkg)) {
            DebugLog.d("M3", "unobserved switch → stop ticker, ignore $pkg")
            setActiveApp(null)   // ⬅️ wichtig: Minute-Job beenden
            return
        }
        scope.launch {
            val rule0 = repo.ruleFlow(pkg).first()
            val stats = repo.todayStatsFlow(pkg).first()

            val blocked = repo.isBlockedNow(stats)
            val over    = repo.isOverLimit(rule0, stats)
            DebugLog.d(
                "M3",
                "onAppOpened pkg=$pkg blocked=$blocked over=$over min=${stats.minutesUsed}/${rule0.minutesLimit} acc=${stats.accessesUsed}/${rule0.accessLimit}"
            )

            // Zugriff nur, wenn nicht blockiert/over und echter Wechsel
            if (!blocked && !over && currentPkg != pkg) {
                repo.incAccess(pkg)
                DebugLog.d("M3", "Access +1 for $pkg (currentPkg=$currentPkg)")
            }

            if (blocked || over) {
                if (over && !blocked) {
                    repo.blockUntilMidnight(pkg)
                    DebugLog.d("M3", "Block set (over limit) for $pkg")
                }
                setActiveApp(null)
                DebugLog.d("M3", "Show overlay for $pkg")
                OverlayBridge.show(appContext, pkg)
                return@launch
            }

            when (rule0.countingMode.lowercase()) {
                "allowance" -> {
                    val until = repo.allowanceUntilFlow(pkg).first()
                    if (until <= System.currentTimeMillis()) {
                        DebugLog.d("M3", "No active allowance for $pkg — blocking")
                        setActiveApp(null)
                        OverlayBridge.show(appContext, pkg)
                        return@launch
                    } else {
                        DebugLog.d("M3", "Allowance active (until=$until) for $pkg; no foreground ticker")
                        setActiveApp(null)
                    }
                }
                else -> {
                    DebugLog.d("M3", "Start minute ticker for $pkg")
                    setActiveApp(pkg)
                }
            }
        }
    }

    fun onAppGone() {
        DebugLog.d("M3", "onAppGone; stop minute ticker")
        setActiveApp(null)
        stopAllowanceWatcher()
    }

    private val switchMutex = Mutex()
    private fun setActiveApp(pkg: String?) {
        scope.launch {
            switchMutex.withLock {
                val old = currentPkg
                currentPkg = pkg
                minuteJob?.cancel()
                DebugLog.d("M3", "switch activeApp: $old → $pkg")
                if (pkg == null) return@withLock
                minuteJob = scope.launch {
                    while (isActive) {
                        delay(10_000L)
                        repo.incUsedMinute(pkg)
                        DebugLog.d("M3", "+1 minute for $pkg")
                        val rule = repo.ruleFlow(pkg).first()
                        val stats = repo.todayStatsFlow(pkg).first()
                        if (repo.isOverLimit(rule, stats) && !repo.isBlockedNow(stats)) {
                            repo.blockUntilMidnight(pkg)
                            DebugLog.d("M3", "Minute tick triggered block for $pkg")
                            OverlayBridge.show(appContext, pkg)
                        }
                    }
                }
            }
        }
    }

    private fun stopAllowanceWatcher() {
        allowanceWatchJob?.cancel()
        allowanceWatchJob = null
    }
}