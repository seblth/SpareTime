package com.example.sparetimeapp.data.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Einfacher globaler Logger:
 * - Schreibt in Logcat (Log.d)
 * - Spiegelt die letzten ~200 Zeilen in eine StateFlow-Liste für die DevTools-UI
 */
object DebugLog {
    private const val MAX = 200
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    fun lines(): StateFlow<List<String>> = _lines

    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
        val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val line = "[$ts][$tag] $msg"
        val cur = _lines.value
        _lines.value = (listOf(line) + cur).take(MAX)
    }
}