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
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main), // ⬅️ Overlay braucht Main
) {
    private var minuteJob: Job? = null
    private var allowanceWatchJob: Job? = null

    @Volatile private var currentPkg: String? = null

    private val observedPkgs = ConcurrentHashMap.newKeySet<String>()

    init {
        scope.launch {
            repo.packagesFlow().collectLatest { pkgs ->
                observedPkgs.clear()
                observedPkgs.addAll(pkgs)
                DebugLog.d("M3", "observedPkgs updated: ${pkgs.size} pkgs")
            }
        }
    }

    fun onAppOpened(pkg: String) {
        if (!observedPkgs.contains(pkg)) {
            DebugLog.d("M3", "unobserved switch → stop ticker, ignore $pkg")
            setActiveApp(null)
            return
        }
        scope.launch {
            val rule = repo.ruleFlow(pkg).first()
            val stats: TodayStats = repo.todayStatsFlow(pkg).first()

            val blocked = repo.isBlockedNow(stats)
            val over = repo.isOverLimit(rule, stats)
            DebugLog.d("M3", "onAppOpened pkg=$pkg blocked=$blocked over=$over min=${stats.minutesUsed}/${rule.minutesLimit} acc=${stats.accessesUsed}/${rule.accessLimit}")

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
                OverlayBridge.show(appContext, pkg, "Limit erreicht")
                return@launch
            }

            when (rule.countingMode.lowercase()) {
                "allowance" -> {
                    val until = repo.allowanceUntilFlow(pkg).first()
                    if (until <= System.currentTimeMillis()) {
                        DebugLog.d("M3", "No active allowance for $pkg — blocking")
                        setActiveApp(null)
                        OverlayBridge.show(appContext, pkg, "Allowance verbraucht")
                        return@launch
                    } else {
                        DebugLog.d("M3", "Allowance active (until=$until) for $pkg; no foreground ticker")
                        setActiveApp(null)
                    }
                }
                else -> {
                    DebugLog.d("M3", "Start minute ticker for $pkg")
                    setActiveApp(pkg)
                    OverlayBridge.dismiss(appContext)
                }
            }
        }
    }

    fun onAppGone() {
        DebugLog.d("M3", "onAppGone; stop minute ticker")
        setActiveApp(null)
        stopAllowanceWatcher()
        OverlayBridge.dismiss(appContext)
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
                        delay(60_000L) // echte Minute statt 10s für MVP-Demo evtl. kürzer stellen
                        repo.incUsedMinute(pkg)
                        DebugLog.d("M3", "+1 minute for $pkg")
                        val rule = repo.ruleFlow(pkg).first()
                        val stats = repo.todayStatsFlow(pkg).first()
                        if (repo.isOverLimit(rule, stats) && !repo.isBlockedNow(stats)) {
                            repo.blockUntilMidnight(pkg)
                            Notifications.limitReached(appContext, pkg)
                            DebugLog.d("M3", "Minute tick triggered block for $pkg")
                            OverlayBridge.show(appContext, pkg, "Limit erreicht")
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
