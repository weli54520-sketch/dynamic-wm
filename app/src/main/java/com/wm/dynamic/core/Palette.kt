package com.wm.dynamic.core

/** ألوان التمييز اللي يختار منها المستخدم */
object Accents {
    val colors = listOf(
        0xFF4C8DFF.toInt(), // أزرق
        0xFF34C759.toInt(), // أخضر
        0xFFFF9F0A.toInt(), // برتقالي
        0xFFFF375F.toInt(), // وردي
        0xFFBF5AF2.toInt(), // بنفسجي
        0xFF64D2FF.toInt(), // سماوي
        0xFFFFD60A.toInt(), // أصفر
        0xFFFFFFFF.toInt(), // أبيض
    )
    val current: Int get() = colors[P.accent.v.coerceIn(0, colors.lastIndex)]

    const val GREEN = 0xFF34C759.toInt()
    const val RED = 0xFFFF453A.toInt()
    const val ORANGE = 0xFFFF9F0A.toInt()
    const val YELLOW = 0xFFFFD60A.toInt()
    const val BLUE = 0xFF0A84FF.toInt()
    const val PURPLE = 0xFFBF5AF2.toInt()
}

/** خلفيات شاشة الإعدادات */
object Backdrops {
    /** رقم خيار "خلفية جوالك المضببة" */
    const val WALLPAPER = 4
    val gradients: List<List<Long>> = listOf(
        listOf(0xFF0B1E4A, 0xFF33397F, 0xFF16424F), // ليل أزرق
        listOf(0xFF1E0B3A, 0xFF5B2A86, 0xFF1A1440), // بنفسجي
        listOf(0xFF042A2B, 0xFF0F5E57, 0xFF0B2236), // زمردي
        listOf(0xFF050506, 0xFF17171C, 0xFF0B0B0E), // أسود
    )
}
