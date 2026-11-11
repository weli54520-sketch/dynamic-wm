package com.wm.dynamic.service

import android.Manifest
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.telecom.TelecomManager
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap

/** يشغّل الرابط حق الإشعار (يفتح التطبيق أو يضغط زر) حتى لو احنا بالخلفية */
fun PendingIntent.fire(): Boolean = try {
    if (Build.VERSION.SDK_INT >= 34) {
        val o = ActivityOptions.makeBasic()
        @Suppress("DEPRECATION")
        o.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
        send(null, 0, null, null, null, null, o.toBundle())
    } else send()
    true
} catch (_: Exception) { false }

fun openApp(ctx: Context, pkg: String?) {
    if (pkg == null) return
    try {
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {}
}

object Apps {
    private val labels = HashMap<String, String>()
    private val icons = LruCache<String, Bitmap>(40)

    fun label(ctx: Context, pkg: String): String = labels.getOrPut(pkg) {
        try {
            val pm = ctx.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) { pkg }
    }

    fun icon(ctx: Context, pkg: String): Bitmap? {
        icons.get(pkg)?.let { return it }
        return try {
            ctx.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).also { icons.put(pkg, it) }
        } catch (_: Exception) { null }
    }
}

/** الرد وإنهاء المكالمة */
object Calls {
    fun hasPermission(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION", "MissingPermission")
    fun answer(ctx: Context, pi: PendingIntent?) {
        if (hasPermission(ctx)) {
            try { ctx.getSystemService(TelecomManager::class.java).acceptRingingCall(); return } catch (_: Exception) {}
        }
        pi?.fire()
    }

    @Suppress("DEPRECATION", "MissingPermission")
    fun end(ctx: Context, pi: PendingIntent?) {
        if (pi != null && pi.fire()) return
        if (hasPermission(ctx)) {
            try { ctx.getSystemService(TelecomManager::class.java).endCall() } catch (_: Exception) {}
        }
    }
}
