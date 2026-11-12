package com.wm.dynamic.island

import android.graphics.Bitmap

/** نوع النشاط اللي تعرضه الجزيرة */
enum class Kind {
    CALL_IN, CALL_ON, MEDIA, TIMER, NAV, PROGRESS, ONGOING, NOTIF,
    CHARGE, UNPLUG, BATTERY_LOW, BT, HEADSET, RINGER, TORCH, DND, UNLOCK, VOLUME, GREETING, PREVIEW
}

/** أزرار التحكم (اللي ما يدعمها النشاط تكون null) */
class Controls(
    val answer: (() -> Unit)? = null,
    val decline: (() -> Unit)? = null,
    val hangUp: (() -> Unit)? = null,
    val playPause: (() -> Unit)? = null,
    val next: (() -> Unit)? = null,
    val prev: (() -> Unit)? = null,
    val seek: ((Long) -> Unit)? = null,
    val open: (() -> Unit)? = null,
    /** تغيير المستوى (مثل شدة الكشاف) */
    val setLevel: ((Int) -> Unit)? = null,
    /** إطفاء (مثل الكشاف) */
    val off: (() -> Unit)? = null,
    /** أزرار الإشعار نفسه (إيقاف المؤقت، إلغاء التحميل ...): الاسم والضغطة */
    val actions: List<Pair<String, () -> Unit>> = emptyList(),
)

/**
 * نشاط واحد في الجزيرة (مكالمة، أغنية، مؤقت ...).
 * [priority] الأعلى يطلع أول. [until] وقت الاختفاء (uptime) للأحداث المؤقتة، 0 = دائم.
 */
data class Act(
    val key: String,
    val kind: Kind,
    val priority: Int,
    val title: String = "",
    val text: String = "",
    val app: String = "",
    val pkg: String? = null,
    val icon: Bitmap? = null,
    val accent: Int = 0,
    /** لون غامق من غلاف الأغنية (خلفية الكبسولة المكبرة) */
    val bg: Int = 0,
    val progress: Float = -1f,
    val indeterminate: Boolean = false,
    /** وقت المؤقت/المكالمة بالمللي (System.currentTimeMillis) */
    val chrono: Long = 0L,
    val countDown: Boolean = false,
    val playing: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    /** elapsedRealtime لما انقرى [position] */
    val posAt: Long = 0L,
    val speed: Float = 1f,
    val level: Int = -1,
    /** أعلى مستوى ممكن لـ [level] (0 = ما يتغير) */
    val levelMax: Int = 0,
    val autoExpand: Boolean = false,
    val until: Long = 0L,
    val stamp: Long = 0L,
    val controls: Controls? = null,
)

object Pri {
    const val CALL_IN = 100
    const val CALL_ON = 95
    const val NOTIF = 90
    const val EVENT = 85
    const val NAV = 80
    const val TIMER = 70
    const val MEDIA = 60
    const val PROGRESS = 50
    const val TORCH = 40
    const val ONGOING = 30
    const val MEDIA_PAUSED = 15
    const val PREVIEW = 5
}
