package com.wm.dynamic.island

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.WindowManager
import com.wm.dynamic.core.P
import com.wm.dynamic.core.ScreenPrefs

/**
 * مقاسات الشاشة الحالية ومكان الكاميرا (بالـ dp).
 * [screen] = inner (الشاشة الداخلية للفولد) أو outer (الخارجية / جوال عادي).
 */
data class Geo(
    val wDp: Float = 400f,
    val hDp: Float = 800f,
    val camCx: Float = 200f,
    val camCy: Float = 14f,
    val camW: Float = 0f,
    val camH: Float = 0f,
    val hasCam: Boolean = false,
    val landscape: Boolean = false,
    val screen: String = "outer",
    val density: Float = 3f,
) {
    val prefs: ScreenPrefs get() = P.screen(screen)

    /** ارتفاع الجزيرة المصغرة */
    fun compactH(): Float {
        val base = if (hasCam && prefs.autoCam.v) (camH + 10f).coerceIn(30f, 44f) else 34f
        return base * prefs.scale.v
    }

    /** عرض فتحة الكاميرا اللي نخليها فاضية بالنص */
    fun gap(): Float = if (hasCam && prefs.autoCam.v) (camW + 6f).coerceIn(14f, 40f) else 18f

    fun compactW(): Float = gap() + 2 * (compactH() * 1.55f + prefs.extraW.v / 2f)

    fun expandedW(): Float = (wDp - 20f).coerceAtMost(440f)

    /** مركز الجزيرة أفقياً (من يسار الشاشة) */
    fun centerX(): Float = (if (hasCam && prefs.autoCam.v) camCx else wDp / 2f) + prefs.offX.v

    /** أعلى الجزيرة */
    fun top(): Float {
        val base = if (hasCam && prefs.autoCam.v) camCy - compactH() / 2f else 6f
        return (base + prefs.offY.v).coerceAtLeast(0f)
    }

    companion object {
        fun read(ctx: Context): Geo {
            val res = ctx.resources
            val dm = res.displayMetrics
            val d = dm.density
            val cfg = res.configuration
            var wPx = dm.widthPixels
            var hPx = dm.heightPixels
            try {
                val b = ctx.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
                wPx = b.width(); hPx = b.height()
            } catch (_: Exception) {}
            val screen = if (cfg.smallestScreenWidthDp >= 600) "inner" else "outer"
            val land = wPx > hPx
            var g = Geo(
                wDp = wPx / d, hDp = hPx / d, camCx = wPx / d / 2f, camCy = 14f,
                landscape = land, screen = screen, density = d,
            )
            try {
                val disp = ctx.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
                val r = disp?.cutout?.boundingRectTop
                if (r != null && !r.isEmpty) {
                    g = g.copy(
                        camCx = r.exactCenterX() / d, camCy = r.exactCenterY() / d,
                        camW = r.width() / d, camH = r.height() / d, hasCam = true,
                    )
                }
            } catch (_: Exception) {}
            return g
        }
    }
}
