package com.wm.dynamic.core

import android.content.Context
import com.wm.dynamic.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * سجل الأعطال: أي خطأ يوقف التطبيق ينحفظ هنا (داخل الجوال بس)،
 * ويطلع في قسم "المزيد" عشان تنسخه وترسله.
 */
object CrashLog {
    private const val NAME = "crash.txt"
    private var file: File? = null

    fun install(ctx: Context) {
        file = File(ctx.filesDir, NAME)
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { th, e ->
            try {
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val txt = "Dynamic WM ${BuildConfig.VERSION_NAME} · $time · ${th.name}\n" + e.stackTraceToString()
                file?.writeText(txt.take(12000))
            } catch (_: Throwable) {}
            prev?.uncaughtException(th, e)
        }
    }

    fun read(): String? = try { file?.takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() } } catch (_: Exception) { null }

    fun clear() { try { file?.delete() } catch (_: Exception) {} }
}
