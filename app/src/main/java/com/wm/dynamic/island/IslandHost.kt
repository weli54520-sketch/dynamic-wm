package com.wm.dynamic.island

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
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

/** يلقط اللمسة برا الجزيرة عشان نصغّرها */
@SuppressLint("ViewConstructor")
private class TouchFrame(ctx: Context, val onOutside: () -> Unit) : FrameLayout(ctx) {
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_OUTSIDE) {
            onOutside(); return false
        }
        return super.dispatchTouchEvent(ev)
    }
}

/**
 * يرسم الجزيرة في نافذة صغيرة فوق كل شي.
 * [type] = نافذة إمكانية الوصول (فوق شريط الحالة) أو الظهور فوق التطبيقات.
 */
class IslandHost(private val ctx: Context, private val type: Int) {
    private val wm = ctx.getSystemService(WindowManager::class.java)
    private val owner = OverlayOwner()
    private val scope = MainScope()
    val state = IslandState()
    private var root: TouchFrame? = null
    private var touchable = false
    private val events = SystemEvents(ctx) { on -> state.screenOn = on; if (!on) state.expanded = false }

    fun start() {
        if (root != null) return
        state.geo = Geo.read(ctx)
        owner.start()
        val frame = TouchFrame(ctx) { state.expanded = false }
        val compose = ComposeView(ctx).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent { IslandView(state) { t -> setTouchable(t) } }
        }
        frame.addView(compose)
        frame.setViewTreeLifecycleOwner(owner)
        frame.setViewTreeSavedStateRegistryOwner(owner)
        frame.setViewTreeViewModelStoreOwner(owner)
        try {
            wm.addView(frame, params())
            root = frame
        } catch (e: Exception) {
            owner.stop()
            return
        }
        events.start()
        // أي تغيير في الحجم أو المكان من الإعدادات = نحرك النافذة
        scope.launch { Prefs.rev.collect { relayout() } }
    }

    fun stop() {
        events.stop()
        scope.cancel()
        root?.let { try { wm.removeViewImmediate(it) } catch (_: Exception) {} }
        root = null
        owner.stop()
    }

    /** تغيّرت الشاشة (فتح/قفل الفولد أو تدوير) */
    fun onConfigChanged() {
        state.geo = Geo.read(ctx)
        state.expanded = false
        relayout()
    }

    private fun setTouchable(t: Boolean) {
        if (touchable == t) return
        touchable = t
        relayout()
    }

    private fun relayout() {
        val r = root ?: return
        try { wm.updateViewLayout(r, params()) } catch (_: Exception) {}
    }

    private fun params(): WindowManager.LayoutParams {
        val g = state.geo
        val d = g.density
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        if (!touchable) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type, flags, PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = ((g.centerX() - g.wDp / 2f) * d).roundToInt()
            y = (g.top() * d).roundToInt()
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            title = "DynamicWM"
            setFitInsetsTypes(0)
        }
    }
}
