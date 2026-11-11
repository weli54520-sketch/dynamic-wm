package com.wm.dynamic.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * كل إعدادات التطبيق في مكان واحد.
 * أي تغيير يزيد [rev]، فالجزيرة وشاشة الإعدادات تتحدث لحالها.
 */
object Prefs {
    lateinit var sp: SharedPreferences
        private set
    val rev = MutableStateFlow(0)

    // لازم نحتفظ بالمستمع، وإلا أندرويد يحذفه
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> rev.value = rev.value + 1 }

    fun init(ctx: Context) {
        if (::sp.isInitialized) return
        sp = ctx.getSharedPreferences("dwm", Context.MODE_PRIVATE)
        sp.registerOnSharedPreferenceChangeListener(listener)
    }
}

class BoolPref(val key: String, val def: Boolean) {
    var v: Boolean
        get() = Prefs.sp.getBoolean(key, def)
        set(x) { Prefs.sp.edit().putBoolean(key, x).apply() }
}

class IntPref(val key: String, val def: Int) {
    var v: Int
        get() = Prefs.sp.getInt(key, def)
        set(x) { Prefs.sp.edit().putInt(key, x).apply() }
}

class FloatPref(val key: String, val def: Float) {
    var v: Float
        get() = Prefs.sp.getFloat(key, def)
        set(x) { Prefs.sp.edit().putFloat(key, x).apply() }
}

class StrPref(val key: String, val def: String) {
    var v: String
        get() = Prefs.sp.getString(key, def) ?: def
        set(x) { Prefs.sp.edit().putString(key, x).apply() }
}

class SetPref(val key: String) {
    var v: Set<String>
        get() = Prefs.sp.getStringSet(key, emptySet())?.toSet() ?: emptySet()
        set(x) { Prefs.sp.edit().putStringSet(key, x).apply() }
}

/** إعدادات لكل شاشة (الداخلية / الخارجية في الفولد) */
class ScreenPrefs(screen: String) {
    val scale = FloatPref("scale_$screen", 1f)
    val offX = FloatPref("offx_$screen", 0f)      // dp
    val offY = FloatPref("offy_$screen", 0f)      // dp
    val extraW = FloatPref("extraw_$screen", 0f)  // dp زيادة عرض الحالة المصغرة
    val autoCam = BoolPref("autocam_$screen", true)

    fun reset() {
        scale.v = 1f; offX.v = 0f; offY.v = 0f; extraW.v = 0f; autoCam.v = true
    }
}

object P {
    val enabled = BoolPref("enabled", true)
    val lang = StrPref("lang", "ar")
    /** acc = إمكانية الوصول (مفضّل) ، overlay = الظهور فوق التطبيقات */
    val mode = StrPref("mode", "acc")

    // الأنشطة
    val media = BoolPref("f_media", true)
    val mediaWave = BoolPref("f_media_wave", true)
    val mediaArtColor = BoolPref("f_media_art", true)
    val mediaHideOpen = BoolPref("f_media_hide_open", true)
    /** كم ثانية تبقى الموسيقى بعد الإيقاف المؤقت (0 = تختفي فوراً) */
    val mediaKeep = IntPref("f_media_keep", 5)
    val mediaArtSpin = BoolPref("f_media_spin", false)
    val mediaTintBg = BoolPref("f_media_tint", true)
    val call = BoolPref("f_call", true)
    val callControls = BoolPref("f_call_ctrl", true)
    val timer = BoolPref("f_timer", true)
    val nav = BoolPref("f_nav", true)
    val progress = BoolPref("f_progress", true)
    val ongoing = BoolPref("f_ongoing", true)

    // الإشعارات
    val notif = BoolPref("f_notif", true)
    val notifSec = IntPref("notif_sec", 4)
    val notifHideLocked = BoolPref("notif_hide_locked", true)
    val notifSkipOpen = BoolPref("notif_skip_open", true)
    /** التطبيقات المستبعدة (كل شي يظهر ما عدا هذي) */
    val blockedApps = SetPref("blocked_apps")

    // أحداث النظام
    val evCharge = BoolPref("e_charge", true)
    val evBattery = BoolPref("e_battery", true)
    val evBt = BoolPref("e_bt", true)
    val evHeadset = BoolPref("e_headset", true)
    val evRinger = BoolPref("e_ringer", true)
    val evTorch = BoolPref("e_torch", true)
    val evDnd = BoolPref("e_dnd", true)
    val evUnlock = BoolPref("e_unlock", false)
    val evVolume = BoolPref("e_volume", false)

    // الشكل
    /** glass = زجاج سائل ، black = أسود يندمج مع الكاميرا */
    val style = StrPref("style", "black")
    val accent = IntPref("accent", 0)
    val glow = BoolPref("glow", true)
    /** 0 بطيئة ، 1 عادية ، 2 سريعة */
    val speed = IntPref("speed", 1)
    val haptics = BoolPref("haptics", true)
    /** نمط الحركة: ios = نابض بارتداد خفيف ، smooth = ناعم بدون ارتداد ، fast = سريع */
    val motion = StrPref("motion", "ios")
    /** طريقة الظهور: camera = تتمدد من الكاميرا ، drop = تنزل من فوق ، fade = تتلاشى */
    val appear = StrPref("appear", "camera")
    /** كم نشاط يبان بنفس الوقت (1 - 3) */
    val maxActs = IntPref("max_acts", 2)
    /** الضغطة الوحدة: expand = تكبر ، open = تفتح التطبيق */
    val tapAction = StrPref("tap_action", "expand")
    /** الدائرة الثانية تنفصل وتلتحم مثل الزئبق */
    val goo = BoolPref("goo", true)
    /** hidden = مخفية ، pill = حبة صغيرة حول الكاميرا */
    val idle = StrPref("idle", "hidden")
    val secondBubble = BoolPref("second_bubble", true)
    val hideLandscape = BoolPref("hide_land", true)
    val autoCollapse = BoolPref("auto_collapse", true)
    val bg = IntPref("bg", 0)

    // لمستك
    val greetOn = BoolPref("greet_on", false)
    val greetText = StrPref("greet_text", "")

    // التحديث
    val autoUpdate = BoolPref("auto_update", true)

    val inner = ScreenPrefs("inner")
    val outer = ScreenPrefs("outer")
    fun screen(key: String) = if (key == "outer") outer else inner
}

/** النص بلغة المستخدم */
fun t(ar: String, en: String): String = if (P.lang.v == "en") en else ar
val isAr: Boolean get() = P.lang.v != "en"
