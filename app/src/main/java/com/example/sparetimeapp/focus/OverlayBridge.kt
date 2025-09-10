package com.example.sparetimeapp.focus

import android.content.Context
import android.content.Intent
import com.example.sparetimeapp.overlay.OverlayService

object OverlayBridge {
    fun show(ctx: Context, pkg: String) {
        val i = Intent(ctx, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_BLOCK
            putExtra(OverlayService.EXTRA_PKG, pkg)
        }
        ctx.startService(i)
    }
}
