package com.example.sparetimeapp.data

import com.example.sparetimeapp.util.todayKey
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val KEY_RULE_PKGS = stringSetPreferencesKey("rule_pkgs")

private const val DS_NAME = "settings"
val Context.dataStore by preferencesDataStore(DS_NAME)

// Schlüssel-Helfer (pro App & Tag)
private fun keyRuleMin(pkg: String) = intPreferencesKey("rule_min_$pkg")
private fun keyRuleAcc(pkg: String) = intPreferencesKey("rule_acc_$pkg")
private fun keyRuleNotif(pkg: String) = booleanPreferencesKey("rule_notif_$pkg")

private fun keyUsedMin(pkg: String, day: String) = intPreferencesKey("used_min_${pkg}_$day")
private fun keyUsedAcc(pkg: String, day: String) = intPreferencesKey("used_acc_${pkg}_$day")
private fun keyBlockedUntil(pkg: String, day: String) = longPreferencesKey("blocked_until_${pkg}_$day")
private fun keyRuleCountingMode(pkg: String) = stringPreferencesKey("rule_mode_$pkg")          // "foreground" | "allowance"
private fun keyRuleAllowanceMin(pkg: String) = intPreferencesKey("rule_allow_min_$pkg")        // Minuten pro Zugriff (z.B. 5)

private fun keyAllowanceUntil(pkg: String, day: String) = longPreferencesKey("allow_until_${pkg}_$day") // Heutiger Allowance-Status

data class Rule(
    val pkg: String,
    val minutesLimit: Int?,      // null = kein Minutenlimit
    val accessLimit: Int?,       // null = kein Accesslimit
    val notifications: Boolean,  // Notifs erlaubt?
    val countingMode: String = "foreground", // "foreground" (MVP) oder "allowance"
    val allowanceMinutes: Int? = null        // z.B. 5 (= 5 Minuten pro Zugriff)
)

class SettingsStore(private val ctx: Context) {

    // ✅ Regeln & heutige Nutzung eines Pakets löschen
    suspend fun deletePackage(pkg: String) {
        val day = todayKey()
        ctx.dataStore.edit { p ->
            p.remove(keyRuleMin(pkg))
            p.remove(keyRuleAcc(pkg))
            p.remove(keyRuleNotif(pkg))

            p.remove(keyUsedMin(pkg, day))
            p.remove(keyUsedAcc(pkg, day))
            p.remove(keyBlockedUntil(pkg, day))

            val cur = p[KEY_RULE_PKGS] ?: emptySet()
            p[KEY_RULE_PKGS] = cur - pkg
        }
    }

    // (Optional) Nur heutige Nutzung eines Pakets löschen – z. B. für Reset-Button im Dev-Tools
    suspend fun clearTodayForPackage(pkg: String) {
        val day = todayKey()
        ctx.dataStore.edit { p ->
            p.remove(keyUsedMin(pkg, day))
            p.remove(keyUsedAcc(pkg, day))
            p.remove(keyBlockedUntil(pkg, day))
        }
    }

    // ----- RULES -----
    fun ruleFlow(pkg: String): Flow<Rule> =
        ctx.dataStore.data.map { p ->
            Rule(
                pkg = pkg,
                minutesLimit   = p[keyRuleMin(pkg)],
                accessLimit    = p[keyRuleAcc(pkg)],
                notifications  = p[keyRuleNotif(pkg)] ?: true,
                countingMode   = p[keyRuleCountingMode(pkg)] ?: "foreground",
                allowanceMinutes = p[keyRuleAllowanceMin(pkg)]
            )
        }


    suspend fun setRule(
        pkg: String,
        minutesLimit: Int?,
        accessLimit: Int?,
        notifications: Boolean,
        // --- NEW (optional für später; kann null bleiben) ---
        countingMode: String = "foreground",
        allowanceMinutes: Int? = null
    ) {
        ctx.dataStore.edit { p ->
            if (minutesLimit != null) p[keyRuleMin(pkg)] = minutesLimit else p.remove(keyRuleMin(pkg))
            if (accessLimit != null)  p[keyRuleAcc(pkg)] = accessLimit else p.remove(keyRuleAcc(pkg))
            p[keyRuleNotif(pkg)] = notifications
            // --- NEW ---
            p[keyRuleCountingMode(pkg)] = countingMode
            if (allowanceMinutes != null) p[keyRuleAllowanceMin(pkg)] = allowanceMinutes else p.remove(keyRuleAllowanceMin(pkg))
        }
        // Index pflegen (wie gehabt)
        addPackageToIndex(pkg)
    }

    // --- NEW: Allowance-Status (heute) ---
    fun allowanceUntilFlow(pkg: String, day: String): Flow<Long> =
        ctx.dataStore.data.map { it[keyAllowanceUntil(pkg, day)] ?: 0L }

    suspend fun setAllowanceUntil(pkg: String, day: String, millis: Long) {
        ctx.dataStore.edit { it[keyAllowanceUntil(pkg, day)] = millis }
    }
    suspend fun clearAllowance(pkg: String, day: String) {
        ctx.dataStore.edit { it.remove(keyAllowanceUntil(pkg, day)) }
    }


    // ----- TODAY USAGE -----
    fun usedMinutesFlow(pkg: String, day: String): Flow<Int> =
        ctx.dataStore.data.map { it[keyUsedMin(pkg, day)] ?: 0 }

    fun usedAccessesFlow(pkg: String, day: String): Flow<Int> =
        ctx.dataStore.data.map { it[keyUsedAcc(pkg, day)] ?: 0 }

    suspend fun incUsedMinute(pkg: String, day: String) {
        ctx.dataStore.edit { p -> p[keyUsedMin(pkg, day)] = (p[keyUsedMin(pkg, day)] ?: 0) + 1 }
    }
    suspend fun incAccess(pkg: String, day: String) {
        ctx.dataStore.edit { p -> p[keyUsedAcc(pkg, day)] = (p[keyUsedAcc(pkg, day)] ?: 0) + 1 }
    }

    // ----- BLOCK -----
    fun blockedUntilFlow(pkg: String, day: String): Flow<Long> =
        ctx.dataStore.data.map { it[keyBlockedUntil(pkg, day)] ?: 0L }

    suspend fun setBlockedUntil(pkg: String, day: String, millis: Long) {
        ctx.dataStore.edit { p -> p[keyBlockedUntil(pkg, day)] = millis }
    }
    suspend fun clearBlocked(pkg: String, day: String) {
        ctx.dataStore.edit { p -> p.remove(keyBlockedUntil(pkg, day)) }
    }

    // ----- RESET DAY -----
    suspend fun resetDay(day: String) {
        // Optional: Wenn du alle Packages kennst, iteriere und entferne used_min/_acc/_blocked_until Keys dieses Tages.
        // Für MVP reicht später ein „neuer dayKey“; alte Werte bleiben, stören aber nicht.
    }

    // --- Index lesen
    fun packagesFlow(): Flow<Set<String>> =
        ctx.dataStore.data.map { it[KEY_RULE_PKGS] ?: emptySet() }

    // --- Index aktualisieren
    private suspend fun addPackageToIndex(pkg: String) {
        ctx.dataStore.edit { p ->
            val current = p[KEY_RULE_PKGS] ?: emptySet()
            p[KEY_RULE_PKGS] = current + pkg
        }
    }
    private suspend fun removePackageFromIndex(pkg: String) {
        ctx.dataStore.edit { p ->
            val current = p[KEY_RULE_PKGS] ?: emptySet()
            p[KEY_RULE_PKGS] = current - pkg
        }
    }
}
