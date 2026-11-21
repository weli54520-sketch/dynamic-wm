package com.wm.dynamic.island

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.wm.dynamic.core.P
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * المكان اللي تتجمع فيه كل الأنشطة.
 * الخدمات تضيف وتحذف، والجزيرة تعرض الأهم.
 */
object Hub {
    private val main = Handler(Looper.getMainLooper())
    private val acts = LinkedHashMap<String, Act>()
    private val _list = MutableStateFlow<List<Act>>(emptyList())
    /** الأنشطة مرتبة: الأهم أول */
    val list: StateFlow<List<Act>> = _list

    /** التطبيق المفتوح الحين (من خدمة إمكانية الوصول) */
    @Volatile var foregroundPkg: String? = null

    private val expire = Runnable { try { publish() } catch (_: Exception) {} }

    /** أنشطة سحبها المستخدم لفوق: تختفي لين يتغير عنوانها */
    private val snoozed = HashMap<String, String>()

    private fun onMain(block: () -> Unit) {
        val safe = { try { block() } catch (_: Exception) {} }
        if (Looper.myLooper() == Looper.getMainLooper()) safe() else main.post(safe)
    }

    fun put(a: Act) = onMain {
        val old = acts[a.key]
        if (snoozed[a.key] != null && snoozed[a.key] != a.title) snoozed.remove(a.key)
        // نخلي وقت الظهور الأول عشان الترتيب ما يتنطط مع كل تحديث
        acts[a.key] = a.copy(stamp = old?.stamp ?: SystemClock.uptimeMillis())
        publish()
    }

    /** حدث مؤقت يظهر [ms] ثم يختفي */
    fun flash(a: Act, ms: Long) = onMain {
        acts.remove(a.key)
        acts[a.key] = a.copy(until = SystemClock.uptimeMillis() + ms, stamp = SystemClock.uptimeMillis())
        publish()
    }

    fun remove(key: String) = onMain { if (acts.remove(key) != null) publish() }

    fun removeIf(pred: (Act) -> Boolean) = onMain {
        if (acts.values.removeAll { pred(it) }) publish()
    }

    fun get(key: String): Act? = acts[key]

    /** المستخدم سحب الجزيرة لفوق: المؤقت ينحذف، والدائم يختفي لين يتغير */
    fun dismiss(key: String) = onMain {
        val a = acts[key] ?: return@onMain
        if (a.until > 0) acts.remove(key) else snoozed[key] = a.title
        publish()
    }

    fun refresh() = onMain { publish() }

    private fun allowed(a: Act): Boolean = when (a.kind) {
        Kind.CALL_IN, Kind.CALL_ON -> P.call.v
        Kind.MEDIA -> P.media.v && !(P.mediaHideOpen.v && a.pkg != null && a.pkg == foregroundPkg)
        Kind.TIMER -> P.timer.v
        Kind.NAV -> P.nav.v
        Kind.PROGRESS -> P.progress.v
        Kind.ONGOING -> P.ongoing.v
        Kind.NOTIF -> P.notif.v
        Kind.CHARGE, Kind.UNPLUG -> P.evCharge.v
        Kind.BATTERY_LOW -> P.evBattery.v
        Kind.BT -> P.evBt.v
        Kind.HEADSET -> P.evHeadset.v
        Kind.RINGER -> P.evRinger.v
        Kind.TORCH -> P.evTorch.v
        Kind.DND -> P.evDnd.v
        Kind.UNLOCK -> P.evUnlock.v
        Kind.VOLUME -> P.evVolume.v
        Kind.RECORD -> P.record.v
        Kind.MISSED -> P.missed.v
        Kind.ALARM -> P.alarm.v
        Kind.ASSIST -> P.assist.v
        Kind.PRIVACY -> P.privacy.v
        Kind.GREETING, Kind.PREVIEW, Kind.CONTROLS, Kind.PET -> true
    }

    private fun publish() {
        val now = SystemClock.uptimeMillis()
        acts.values.removeAll { it.until in 1..now }
        // تطبيق يشغّل فيديو أو أغنية: إشعار "قيد التشغيل" حقه ما له داعي (دائرة وحدة لكل تطبيق)
        val playing = acts.values.filter { it.kind == Kind.MEDIA }.mapNotNullTo(HashSet()) { it.pkg }
        _list.value = if (!P.enabled.v) emptyList() else acts.values
            .filter { allowed(it) && snoozed[it.key] != it.title }
            .filterNot { it.kind == Kind.ONGOING && it.pkg != null && it.pkg in playing }
            .sortedWith(compareByDescending<Act> { it.priority }.thenByDescending { it.stamp })
        main.removeCallbacks(expire)
        val next = acts.values.filter { it.until > 0 }.minOfOrNull { it.until }
        if (next != null) main.postAtTime(expire, next + 5)
    }
}
