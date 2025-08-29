package com.example.sparetimeapp.focus
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo

class BlockerAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // TODO: Implement event handling logic
    }

    override fun onInterrupt() {
        // TODO: Implement interrupt logic
    }
}