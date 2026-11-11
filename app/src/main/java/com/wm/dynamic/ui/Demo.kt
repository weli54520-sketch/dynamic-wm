package com.wm.dynamic.ui

import android.content.Context
import android.os.BatteryManager
import android.os.SystemClock
import com.wm.dynamic.core.Accents
import com.wm.dynamic.core.t
import com.wm.dynamic.island.Act
import com.wm.dynamic.island.Controls
import com.wm.dynamic.island.Hub
import com.wm.dynamic.island.Kind
import com.wm.dynamic.island.Pri

/** أحداث تجريبية عشان تشوف شكل الجزيرة بدون ما تنتظر شي حقيقي */
object Demo {
    private const val PRI = Pri.CALL_IN + 5

    fun music() {
        var playing = true
        fun act(): Act = Act(
            "demo_media", Kind.MEDIA, PRI, title = t("أغنية تجريبية", "Demo song"), text = "Dynamic WM",
            app = t("تجربة", "Demo"), accent = Accents.current, playing = playing,
            position = 42_000, duration = 214_000, posAt = SystemClock.elapsedRealtime(),
            controls = Controls(
                playPause = { playing = !playing; Hub.flash(act(), 15_000) },
                next = {}, prev = {}, seek = {},
            ),
        )
        Hub.flash(act(), 15_000)
    }

    fun call() {
        val name = t("متصل تجريبي", "Demo caller")
        Hub.flash(Act(
            "demo_call", Kind.CALL_IN, PRI, title = name, app = t("تجربة", "Demo"), autoExpand = true,
            controls = Controls(
                answer = {
                    Hub.flash(Act("demo_call", Kind.CALL_ON, PRI, title = name, chrono = System.currentTimeMillis(),
                        controls = Controls(hangUp = { Hub.remove("demo_call") })), 15_000)
                },
                decline = { Hub.remove("demo_call") },
            ),
        ), 15_000)
    }

    fun timer() {
        Hub.flash(Act("demo_timer", Kind.TIMER, PRI, title = t("مؤقت الشاي", "Tea timer"),
            chrono = System.currentTimeMillis() + 5 * 60_000, countDown = true), 12_000)
    }

    fun charge(ctx: Context) {
        val lv = try {
            ctx.getSystemService(BatteryManager::class.java).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (_: Exception) { 80 }
        Hub.flash(Act("demo_charge", Kind.CHARGE, PRI, title = t("جاري الشحن", "Charging"), text = "$lv%",
            level = lv, accent = Accents.GREEN), 4_000)
    }

    fun download() {
        Hub.flash(Act("demo_dl", Kind.PROGRESS, PRI, title = t("تحميل ملف", "Downloading"), text = "video.mp4",
            app = t("تجربة", "Demo"), progress = 0.62f), 8_000)
    }

    fun notif() {
        Hub.flash(Act("demo_notif", Kind.NOTIF, PRI, title = "Dynamic WM",
            text = t("هذا إشعار تجريبي، كذا بتطلع رسايلك.", "This is a demo notification."),
            app = t("تجربة", "Demo")), 5_000)
    }

    fun bluetooth() {
        Hub.flash(Act("demo_bt", Kind.BT, PRI, title = "Galaxy Buds", text = t("متصل", "Connected"),
            accent = Accents.BLUE), 3_000)
    }
}
