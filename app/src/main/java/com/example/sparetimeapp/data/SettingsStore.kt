package com.example.sparetimeapp.data

import com.example.sparetimeapp.util.todayKey
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
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

data class Rule(
    val pkg: String,
    val minutesLimit: Int?,     // null = kein Minutenlimit
    val accessLimit: Int?,      // null = kein Accesslimit
    val notifications: Boolean  // true = Notifs erlauben
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
                minutesLimit = p[keyRuleMin(pkg)],
                accessLimit = p[keyRuleAcc(pkg)],
                notifications = p[keyRuleNotif(pkg)] ?: true
            )
        }

    suspend fun setRule(
        pkg: String,
        minutesLimit: Int?,
        accessLimit: Int?,
        notifications: Boolean
    ) {
        ctx.dataStore.edit { p ->
            if (minutesLimit != null) p[keyRuleMin(pkg)] = minutesLimit else p.remove(keyRuleMin(pkg))
            if (accessLimit != null)  p[keyRuleAcc(pkg)] = accessLimit else p.remove(keyRuleAcc(pkg))
            p[keyRuleNotif(pkg)] = notifications
        }
        // Wenn wenigstens irgend ein Limit existiert oder Notifications gesetzt sind → im Index behalten
        if (minutesLimit != null || accessLimit != null || notifications) {
            addPackageToIndex(pkg)
        } else {
            // Falls du „leere” Regeln entfernen willst:
            removePackageFromIndex(pkg)
        }
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
