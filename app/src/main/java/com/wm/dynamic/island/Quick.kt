package com.wm.dynamic.island

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import com.wm.dynamic.core.t
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * لوحة السطوع والصوت: تطلع بضغطة على الكاميرا (والجزيرة مخفية).
 * السطوع يحتاج إذن "تعديل إعدادات النظام" (المستخدم يعطيه من الإعدادات).
 */
object Quick {
    const val KEY = "quick"
    private const val SHOW_MS = 9000L

    /** آخر مرة لمس فيها المستخدم اللوحة: عشان تنبيه الصوت العادي ما يغطي عليها */
    @Volatile private var lastUse = 0L
    private var lastBump = 0L

    val active: Boolean get() = Hub.get(KEY) != null || SystemClock.uptimeMillis() - lastUse < 1500

    fun show() {
        lastUse = SystemClock.uptimeMillis()
        Hub.flash(Act(KEY, Kind.CONTROLS, Pri.CONTROLS, title = t("التحكم السريع", "Quick controls"), autoExpand = true), SHOW_MS)
    }

    /** المستخدم يحرك شريط: نطوّل بقاء اللوحة (مرة كل ثانية بالكثير) */
    fun bump() {
        val now = SystemClock.uptimeMillis()
        lastUse = now
        if (now - lastBump < 1000) return
        lastBump = now
        Hub.get(KEY)?.let { Hub.flash(it, SHOW_MS) }
    }

    fun hide() = Hub.remove(KEY)

    // ───── السطوع ─────

    fun canWrite(ctx: Context): Boolean = try { Settings.System.canWrite(ctx) } catch (_: Exception) { false }

    fun askWrite(ctx: Context) {
        try {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + ctx.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: Exception) {}
        hide()
    }

    fun autoBrightness(ctx: Context): Boolean = try {
        Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE) ==
            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
    } catch (_: Exception) { false }

    fun setAuto(ctx: Context, on: Boolean) {
        if (!canWrite(ctx)) return
        try {
            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (on) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        } catch (_: Exception) {}
    }

    /** السطوع (0 - 1). العين تحس بالسطوع بشكل غير متساوي، فنستخدم منحنى */
    fun brightness(ctx: Context): Float = try {
        val v = Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        sqrt(((v - 1).coerceIn(0, 254)) / 254f)
    } catch (_: Exception) { 0.5f }

    fun setBrightness(ctx: Context, f: Float) {
        if (!canWrite(ctx)) return
        try {
            val cr = ctx.contentResolver
            // التلقائي يتجاهل القيمة اللي نكتبها، فنحوّله يدوي وقت ما يحرك الشريط
            if (autoBrightness(ctx)) setAuto(ctx, false)
            val v = 1 + (254f * f.coerceIn(0f, 1f).pow(2)).roundToInt()
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, v)
        } catch (_: Exception) {}
    }

    // ───── الصوت ─────

    fun volume(ctx: Context): Float = try {
        val am = ctx.getSystemService(AudioManager::class.java)
        am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    } catch (_: Exception) { 0.5f }

    fun volumeSteps(ctx: Context): Int = try {
        ctx.getSystemService(AudioManager::class.java).getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    } catch (_: Exception) { 15 }

    fun setVolume(ctx: Context, f: Float) {
        try {
            val am = ctx.getSystemService(AudioManager::class.java)
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, (f.coerceIn(0f, 1f) * max).roundToInt(), 0)
        } catch (_: Exception) {}
    }
}
