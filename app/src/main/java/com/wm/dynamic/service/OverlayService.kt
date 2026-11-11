package com.wm.dynamic.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.IBinder
import android.provider.Settings
import android.view.Display
import android.view.WindowManager
import androidx.core.app.ServiceCompat
import com.wm.dynamic.R
import com.wm.dynamic.core.P
import com.wm.dynamic.core.Prefs
import com.wm.dynamic.core.t
import com.wm.dynamic.island.IslandHost
import com.wm.dynamic.ui.MainActivity

/**
 * الطريقة البديلة: الجزيرة كنافذة "الظهور فوق التطبيقات".
 * تحتاج إشعار ثابت صغير عشان أندرويد ما يوقفها.
 */
class OverlayService : Service() {
    private var host: IslandHost? = null
    private var winCtx: Context? = null

    private val cfgCb = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) { host?.onConfigChanged() }
        @Deprecated("Deprecated in Java")
        override fun onLowMemory() {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Prefs.init(this)
        // أندرويد ممكن يرفض التشغيل بالخلفية: بدل ما يطيح التطبيق نوقف الخدمة بهدوء
        val fg = try { goForeground(); true } catch (_: Exception) { false }
        if (!fg || !shouldRun(this)) { stopSelf(); return START_NOT_STICKY }
        if (host == null) {
            try {
                val disp = getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
                    ?: throw IllegalStateException("no display")
                val wc = createWindowContext(disp, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
                wc.registerComponentCallbacks(cfgCb)
                winCtx = wc
                host = IslandHost(wc, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY).also { it.start() }
            } catch (_: Exception) { stopSelf() }
        }
        return START_NOT_STICKY
    }

    private fun goForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, t("الجزيرة", "Island"), NotificationManager.IMPORTANCE_MIN).apply { setShowBadge(false) }
        )
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_app_mono)
            .setContentTitle("Dynamic WM")
            .setContentText(t("الجزيرة شغالة", "Island is running"))
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(this, 7, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    }

    override fun onDestroy() {
        host?.stop(); host = null
        try { winCtx?.unregisterComponentCallbacks(cfgCb) } catch (_: Exception) {}
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "island"

        fun shouldRun(ctx: Context): Boolean =
            P.enabled.v && P.mode.v == "overlay" && Settings.canDrawOverlays(ctx) &&
                IslandAccessibility.instance?.hosting != true

        /** يشغّل أو يوقف الخدمة حسب الإعدادات */
        fun sync(ctx: Context) {
            if (shouldRun(ctx)) {
                try { ctx.startForegroundService(Intent(ctx, OverlayService::class.java)) } catch (_: Exception) {}
            } else stop(ctx)
        }

        fun stop(ctx: Context) {
            try { ctx.stopService(Intent(ctx, OverlayService::class.java)) } catch (_: Exception) {}
        }
    }
}

/** بعد تشغيل الجوال أو تحديث التطبيق */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Prefs.init(context)
        OverlayService.sync(context)
    }
}
