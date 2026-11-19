package com.wm.dynamic.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.wm.dynamic.core.P
import com.wm.dynamic.core.Prefs
import com.wm.dynamic.island.Hub
import com.wm.dynamic.island.IslandHost

/**
 * الطريقة المفضلة: الجزيرة تنرسم فوق شريط الحالة وتستقبل اللمس.
 * ما نقرأ محتوى الشاشة، بس اسم التطبيق المفتوح.
 */
class IslandAccessibility : AccessibilityService() {
    private var host: IslandHost? = null

    private val onPref = SharedPreferences.OnSharedPreferenceChangeListener { _, k ->
        if (k == P.mode.key || k == P.enabled.key) sync()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Prefs.init(this)
        Prefs.sp.registerOnSharedPreferenceChangeListener(onPref)
        sync()
    }

    private fun sync() {
        val want = P.mode.v == "acc"
        if (want && host == null) {
            OverlayService.stop(this)
            host = IslandHost(this, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY).also { it.start() }
        } else if (!want && host != null) {
            host?.stop(); host = null
            OverlayService.sync(this)
        }
    }

    val hosting: Boolean get() = host != null

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e == null || e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        // الكيبورد والنوافذ الصغيرة ما تعتبر "تطبيق مفتوح"
        if (!e.isFullScreen) return
        val p = e.packageName?.toString() ?: return
        if (p == "com.android.systemui" || p == packageName) return
        // نوافذ النظام (عد تنازلي حق مسجل الشاشة، الكيبورد ...) مو تطبيق فتحته:
        // لو حسبناها، الجزيرة تحسب إنك طلعت من التطبيق وترجع تطلع وأنت داخله
        if (!isRealApp(p)) return
        if (Hub.foregroundPkg != p) {
            Hub.foregroundPkg = p
            Hub.refresh()
        }
    }

    private val realApps = HashMap<String, Boolean>()
    private var home: String? = null

    /** تطبيق حقيقي = له أيقونة في قائمة التطبيقات، أو الشاشة الرئيسية */
    private fun isRealApp(p: String): Boolean = realApps.getOrPut(p) {
        if (OVERLAYS.any { p.contains(it, ignoreCase = true) }) return@getOrPut false
        if (home == null) home = try {
            packageManager.resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY,
            )?.activityInfo?.packageName
        } catch (_: Exception) { null }
        p == home || try { packageManager.getLaunchIntentForPackage(p) != null } catch (_: Exception) { true }
    }

    override fun onInterrupt() {}

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        host?.onConfigChanged()
    }

    override fun onDestroy() {
        try { Prefs.sp.unregisterOnSharedPreferenceChangeListener(onPref) } catch (_: Exception) {}
        host?.stop(); host = null
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile var instance: IslandAccessibility? = null
        private val OVERLAYS = listOf("screenrecord", "smartcapture", "inputmethod", "honeyboard", "keyboard")

        /** يفتح لوحة الإشعارات (السحب لتحت من الكاميرا) */
        fun openNotifications(): Boolean = try {
            instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) == true
        } catch (_: Exception) { false }
    }
}
