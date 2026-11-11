package com.wm.dynamic.island

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.wm.dynamic.core.Prefs
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** حالة الجزيرة اللي تتغير مع الوقت */
class IslandState {
    var geo by mutableStateOf(Geo())
    var screenOn by mutableStateOf(true)
    var expanded by mutableStateOf(false)
    var touchTick by mutableIntStateOf(0)
    /** النشاط اللي اختاره المستخدم من المكبرة (لو فيه أكثر من تطبيق شغال) */
    var selected by mutableStateOf<String?>(null)
    /** التبديل جا من المستخدم: نخليها مكبرة */
    var keepOpen = false
}

/** يخلي Compose يشتغل داخل نافذة من خدمة (مو من شاشة) */
private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val reg = LifecycleRegistry(this)
    private val ssc = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = reg
    override val savedStateRegistry: SavedStateRegistry get() = ssc.savedStateRegistry
    override val viewModelStore = ViewModelStore()

    fun start() {
        ssc.performRestore(null)
        reg.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        reg.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}

/**
 * نافذة اللمس: شفافة وبمقاس الجزيرة بس. أي لمسة فيها تنرسل لنافذة الرسم.
 * لمسة برا الجزيرة (وهي مكبرة) = تصغير.
 */
@SuppressLint("ViewConstructor")
private class TouchFrame(
    ctx: Context,
    val onOutside: () -> Unit,
    val forward: (MotionEvent) -> Unit,
) : View(ctx) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_OUTSIDE) { onOutside(); return false }
        forward(ev)
        return true
    }
}

/**
 * يرسم الجزيرة فوق كل شي بنافذتين:
 * 1) نافذة رسم بعرض الشاشة وما تستقبل لمس. مقاسها ثابت، فالحركة ناعمة (ما تهتز).
 * 2) نافذة لمس صغيرة فوق الجزيرة بالضبط.
 * [type] = نافذة إمكانية الوصول (فوق شريط الحالة) أو الظهور فوق التطبيقات.
 */
class IslandHost(private val ctx: Context, private val type: Int) {
    private val wm = ctx.getSystemService(WindowManager::class.java)
    private val owner = OverlayOwner()
    private val scope = MainScope()
    val state = IslandState()
    private var drawRoot: FrameLayout? = null
    private var compose: ComposeView? = null
    private var touchRoot: TouchFrame? = null
    private val events = SystemEvents(ctx) { on -> state.screenOn = on; if (!on) state.expanded = false }

    /** نافذة إمكانية الوصول "موثوقة": اللمس يعدي من تحتها، فنخليها ثابتة دايماً */
    private val fixed = type == WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

    private var req = IslandReq(1, null, false)

    fun start() {
        if (drawRoot != null) return
        state.geo = Geo.read(ctx)
        owner.start()
        val cv = ComposeView(ctx).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent { IslandView(state, fullH()) { r -> onReq(r) } }
        }
        val frame = FrameLayout(ctx)
        frame.addView(cv)
        frame.setViewTreeLifecycleOwner(owner)
        frame.setViewTreeSavedStateRegistryOwner(owner)
        frame.setViewTreeViewModelStoreOwner(owner)
        val touch = TouchFrame(ctx, onOutside = { state.expanded = false }, forward = { forward(it) })
        try {
            wm.addView(frame, drawParams())
            drawRoot = frame; compose = cv
            wm.addView(touch, touchParams())
            touchRoot = touch
        } catch (e: Exception) {
            stop()
            return
        }
        events.start()
        // أي تغيير في الحجم أو المكان من الإعدادات = نحرك النوافذ
        scope.launch { Prefs.rev.collect { relayout() } }
    }

    fun stop() {
        events.stop()
        scope.cancel()
        touchRoot?.let { try { wm.removeViewImmediate(it) } catch (_: Exception) {} }
        drawRoot?.let { try { wm.removeViewImmediate(it) } catch (_: Exception) {} }
        touchRoot = null; drawRoot = null; compose = null
        try { owner.stop() } catch (_: Exception) {}
    }

    /** تغيّرت الشاشة (فتح/قفل الفولد أو تدوير) */
    fun onConfigChanged() {
        state.geo = Geo.read(ctx)
        state.expanded = false
        relayout()
    }

    /** أقصى ارتفاع تحتاجه الجزيرة المكبرة */
    private fun fullH(): Int {
        val g = state.geo
        return ((g.top() + MAX_H_DP) * g.density).roundToInt()
    }

    private fun onReq(r: IslandReq) {
        if (r == req) return
        val drawChanged = !fixed && r.drawH != req.drawH && (r.drawH > 1 || req.drawH > 1)
        val touchChanged = r.touch != req.touch || r.watchOutside != req.watchOutside
        req = r
        // ما فيه شي يبان: نخفي نافذة الرسم كلها (أوفر للبطارية)
        drawRoot?.visibility = if (r.drawH <= 1) View.INVISIBLE else View.VISIBLE
        try {
            if (drawChanged) drawRoot?.let { wm.updateViewLayout(it, drawParams()) }
            if (touchChanged) touchRoot?.let { wm.updateViewLayout(it, touchParams()) }
        } catch (_: Exception) {}
    }

    /** نرسل اللمسة لنافذة الرسم بنفس مكانها على الشاشة */
    private fun forward(ev: MotionEvent) {
        val cv = compose ?: return
        val t = touchRoot ?: return
        val a = IntArray(2); val b = IntArray(2)
        t.getLocationOnScreen(a); cv.getLocationOnScreen(b)
        val e = MotionEvent.obtain(ev)
        try {
            e.offsetLocation((a[0] - b[0]).toFloat(), (a[1] - b[1]).toFloat())
            cv.dispatchTouchEvent(e)
        } catch (_: Exception) {
        } finally { e.recycle() }
    }

    private fun relayout() {
        try {
            drawRoot?.let { wm.updateViewLayout(it, drawParams()) }
            touchRoot?.let { wm.updateViewLayout(it, touchParams()) }
        } catch (_: Exception) {}
    }

    private fun base(w: Int, h: Int, flags: Int) = WindowManager.LayoutParams(w, h, type, flags, PixelFormat.TRANSLUCENT).apply {
        gravity = Gravity.TOP or Gravity.LEFT
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        setFitInsetsTypes(0)
    }

    private fun drawParams(): WindowManager.LayoutParams {
        val g = state.geo
        val w = (g.wDp * g.density).roundToInt().coerceAtLeast(1)
        val h = if (fixed) fullH() else req.drawH.coerceIn(1, fullH())
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        return base(w, h, flags).apply { x = 0; y = 0; title = "DynamicWM" }
    }

    private fun touchParams(): WindowManager.LayoutParams {
        val r = req.touch
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (r == null || r.isEmpty) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            return base(1, 1, flags).apply { x = 0; y = 0; title = "DynamicWM-touch" }
        }
        if (req.watchOutside) flags = flags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        return base(r.width(), r.height(), flags).apply { x = r.left; y = r.top; title = "DynamicWM-touch" }
    }

    companion object {
        /** أطول جزيرة مكبرة (مع صف التطبيقات) */
        const val MAX_H_DP = 340f
    }
}
