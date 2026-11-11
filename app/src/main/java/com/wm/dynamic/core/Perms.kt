package com.wm.dynamic.core

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.wm.dynamic.service.IslandAccessibility
import com.wm.dynamic.service.NotifListener

/** فحص الأذونات وفتح صفحاتها في الإعدادات */
object Perms {
    fun accessibility(ctx: Context): Boolean {
        if (IslandAccessibility.instance != null) return true
        val list = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val me = ComponentName(ctx, IslandAccessibility::class.java)
        return list.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    fun notifications(ctx: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    fun overlay(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

    fun battery(ctx: Context): Boolean =
        ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

    fun granted(ctx: Context, perm: String): Boolean =
        ctx.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED

    fun calls(ctx: Context) = granted(ctx, Manifest.permission.ANSWER_PHONE_CALLS)
    fun bluetooth(ctx: Context) = granted(ctx, Manifest.permission.BLUETOOTH_CONNECT)
    fun postNotif(ctx: Context) = granted(ctx, Manifest.permission.POST_NOTIFICATIONS)

    /** الجزيرة جاهزة؟ */
    fun ready(ctx: Context): Boolean =
        (if (P.mode.v == "acc") accessibility(ctx) else overlay(ctx)) && notifications(ctx)

    private fun open(ctx: Context, vararg intents: Intent) {
        for (i in intents) {
            try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return } catch (_: Exception) {}
        }
    }

    private fun pkgUri(ctx: Context) = Uri.parse("package:${ctx.packageName}")

    fun openAccessibility(ctx: Context) {
        val comp = ComponentName(ctx, IslandAccessibility::class.java).flattenToString()
        open(ctx,
            Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").putExtra(Intent.EXTRA_COMPONENT_NAME, comp),
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    fun openNotifications(ctx: Context) {
        val comp = ComponentName(ctx, NotifListener::class.java).flattenToString()
        open(ctx,
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, comp),
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun openOverlay(ctx: Context) = open(ctx,
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri(ctx)),
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))

    @android.annotation.SuppressLint("BatteryLife")
    fun openBattery(ctx: Context) = open(ctx,
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkgUri(ctx)),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri(ctx)))

    fun openAppInfo(ctx: Context) = open(ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri(ctx)))
}
