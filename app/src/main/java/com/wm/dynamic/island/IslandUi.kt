package com.wm.dynamic.island

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Rect
import android.graphics.Shader
import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wm.dynamic.core.Accents
import com.wm.dynamic.core.P
import com.wm.dynamic.core.Prefs
import com.wm.dynamic.core.Saver
import com.wm.dynamic.core.isAr
import com.wm.dynamic.core.t
import com.wm.dynamic.ui.Plex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

/** يخلي الأجزاء تتحدث لما تتغير الإعدادات */
private val PrefRev = compositionLocalOf { 0 }

/** توفير البطارية شغال: الموجة تثبت، الغلاف ما يدور، والتحديثات أقل */
private val LocalSaver = compositionLocalOf { false }

/** نفس اللون بس أغمق */
private fun Color.dim(f: Float) = Color(red * f, green * f, blue * f, alpha)

private data class Face(val mode: Int, val key: String)

private val Gray = Color(0xFFA1A1AA)

private fun ts(size: Float, weight: FontWeight = FontWeight.Normal, color: Color = Color.White) =
    TextStyle(fontFamily = Plex, fontSize = size.sp, fontWeight = weight, color = color)

/** وش تبي الجزيرة من النوافذ: ارتفاع نافذة الرسم (px) ومكان اللمس (px) */
data class IslandReq(val drawH: Int, val touch: Rect?, val watchOutside: Boolean)

/** أرقام النابض حسب نمط الحركة */
private fun springOf(motion: String): Pair<Float, Float> = when (motion) {
    "smooth" -> 1f to 210f
    "fast" -> 0.84f to 650f
    else -> 0.72f to 280f // iOS: ارتداد خفيف
}

/**
 * الجزيرة كلها.
 * النافذة ما يتغير مقاسها أبداً وقت الحركة (هذا سبب الاهتزاز القديم).
 * الشكل يترسم بالقلم (Canvas) بأرقام متحركة بنابض واحد: العرض، الطول، المكان، الزوايا.
 * المحتوى فوقه بمقاس الشكل نفسه، ويطلع بعد ما يتمدد الشكل.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun IslandView(st: IslandState, fullH: Int, onReq: (IslandReq) -> Unit) {
    val rev by Prefs.rev.collectAsState()
    val all by Hub.list.collectAsState()
    val saverRaw by Saver.on.collectAsState()
    val saver = saverRaw && P.autoSaver.v
    val g = st.geo
    val dens = LocalDensity.current
    val d = dens.density
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // ملء الشاشة (فيديو، ألعاب): نخفيها، إلا المكالمة الواردة
    val acts = if (st.fullscreen && P.hideFullscreen.v) all.filter { it.kind == Kind.CALL_IN } else all

    // نحفظ آخر نسخة من كل نشاط عشان حركة الرجوع تلقى شي ترسمه
    val latest = remember { HashMap<String, Act>() }
    all.forEach { latest[it.key] = it }

    val off = rev < 0 || !st.screenOn || !P.enabled.v || (g.landscape && P.hideLandscape.v)
    // الأحداث المؤقتة والمكالمات تغطي على أي شي. غير كذا: اللي اختاره المستخدم، أو الأهم
    val first = if (off) null else acts.firstOrNull()
    val picked = if (off) null else acts.firstOrNull { it.key == st.selected }
    val top = if (first != null && (first.until > 0 || first.priority >= Pri.CALL_ON)) first else picked ?: first
    val maxActs = P.maxActs.v.coerceIn(1, 3)
    val others = if (off || top == null) emptyList()
    else acts.filter { it.key != top.key && it.kind != Kind.PREVIEW }.take(maxActs - 1)
    val picks = if (off) emptyList() else acts.filter { it.until == 0L && it.kind != Kind.PREVIEW }
    val keepIdle = P.idle.v == "pill"
    val appear = P.appear.v
    val black = P.style.v != "glass"

    val autoExp = top != null && (top.autoExpand || top.kind == Kind.NOTIF || top.kind == Kind.GREETING || top.kind == Kind.CALL_IN)
    LaunchedEffect(top?.key) {
        if (st.keepOpen) st.keepOpen = false else st.expanded = autoExp
    }
    val expanded = st.expanded && top != null

    // ترجع صغيرة لحالها بعد شوي لو ما لمستها
    LaunchedEffect(expanded, top?.key, st.touchTick) {
        if (expanded && P.autoCollapse.v && top?.kind != Kind.CALL_IN && top?.kind != Kind.NOTIF) {
            delay(7000); st.expanded = false
        }
    }

    val face = Face(if (top == null) 0 else if (expanded) 2 else 1, top?.key ?: "")
    val hidden = face.mode == 0 && !keepIdle

    // ───── الأهداف (وين لازم يوصل الشكل) ─────
    val H = g.compactH()
    val seed = g.seed()
    var content by remember { mutableStateOf(IntSize.Zero) }
    val cx = g.centerX() * d
    val tW: Float
    val tH: Float
    val tTop: Float
    when {
        face.mode != 0 || keepIdle -> { tW = content.width.toFloat(); tH = content.height.toFloat(); tTop = g.top() * d }
        appear == "drop" -> { tW = g.compactW() * d; tH = H * d; tTop = -(H + 12f) * d }
        appear == "fade" -> { tW = g.compactW() * d; tH = H * d; tTop = g.top() * d }
        else -> { tW = seed * d; tH = seed * d; tTop = (g.top() + (H - seed) / 2f) * d }
    }
    val tCorner = if (face.mode == 2) 40f * d else minOf(tW, tH) / 2f

    // ───── نابض واحد للعرض والطول والمكان والزوايا ─────
    // توفير البطارية: حركة أسرع وأقصر
    val (damp, stiff) = springOf(if (saver) "fast" else P.motion.v)
    val aw = remember { Animatable(tW) }
    val ah = remember { Animatable(tH) }
    val aTop = remember { Animatable(tTop) }
    val aCorner = remember { Animatable(tCorner) }
    LaunchedEffect(tW, tH, tTop, tCorner, damp, stiff) {
        if (tW <= 0f || tH <= 0f) return@LaunchedEffect
        val grow = tW * tH >= aw.value * ah.value
        // الكبر بارتداد، والرجوع أهدأ شوي (مثل iOS)
        val sp = spring<Float>(if (grow) damp else minOf(1f, damp + 0.14f), stiff, 0.5f)
        launch { aw.animateTo(tW, sp) }
        launch { ah.animateTo(tH, sp) }
        launch { aTop.animateTo(tTop, sp) }
        launch { aCorner.animateTo(tCorner, sp) }
    }
    val moving = aw.isRunning || ah.isRunning || aTop.isRunning

    // الظهور: تبان على طول (تطلع من الكاميرا أو من فوق)، والتلاشي بشفافية
    val alpha = remember { Animatable(if (hidden) 0f else 1f) }
    LaunchedEffect(hidden, appear, off) {
        when {
            off -> alpha.snapTo(0f)
            !hidden -> if (appear == "fade") alpha.animateTo(1f, tween(220)) else alpha.snapTo(1f)
            appear == "fade" -> alpha.animateTo(0f, tween(240))
            else -> {
                // ننتظر الشكل يرجع للكاميرا، وبعدين يختفي
                delay(48)
                snapshotFlow { aw.isRunning || ah.isRunning || aTop.isRunning }.first { !it }
                alpha.snapTo(0f)
            }
        }
    }

    // ───── الدوائر الجانبية (على اليسار) ─────
    // نشاط ثاني = دائرة يسار الجزيرة. نشاط ثالث = دائرة فوقها مايلة شوي.
    // ضغطة على الدوائر المكدسة = تنفرد جنب بعض، وتختار وحدة فتلتحم مع الجزيرة.
    val B = H * d
    val bGap = 7f * d
    val showBubbles = !expanded && top != null
    val slotA = rememberSlot(others.getOrNull(0), showBubbles, damp, stiff)
    val slotB = rememberSlot(others.getOrNull(1), showBubbles, damp, stiff)
    val bTop = g.top() * d
    var fan by remember { mutableStateOf(false) }
    val canFan = showBubbles && others.size >= 2
    LaunchedEffect(canFan) { if (!canFan) fan = false }
    // ترجع مكدسة لحالها لو ما اخترت شي
    LaunchedEffect(fan, st.touchTick) { if (fan) { delay(4000); fan = false } }
    val aFan = remember { Animatable(0f) }
    LaunchedEffect(fan, damp, stiff) { aFan.animateTo(if (fan) 1f else 0f, spring(damp, stiff * 0.9f, 0.002f)) }
    val bubbleSc = 0.84f
    /** مركز الدائرة الأولى */
    fun xA(sep: Float): Float = cx - aw.value / 2f + B / 2f - (bGap + B) * sep
    /** بُعد الدائرة الثانية عن الأولى: مكدسة (متداخلة) أو منفردة (جنبها) */
    fun offB(f: Float): Float = B * 0.62f + (bGap + B * 0.38f) * f
    fun xB(sep: Float, f: Float): Float = cx - aw.value / 2f + B / 2f - (bGap + B + offB(f)) * sep
    /** الثانية نازلة شوي (مايلة) وهي مكدسة */
    fun yB(sep: Float, f: Float): Float = bTop + B * 0.14f * (1f - f) * sep
    fun scB(f: Float): Float = bubbleSc + (1f - bubbleSc) * f

    // ───── النوافذ: وين اللمس ─────
    val pad = 4f * d
    val touch: Rect? = if (hidden || off || alpha.value <= 0f && hidden) null else {
        val r = Rect(
            (cx - tW / 2f - pad).roundToInt(), (tTop - pad).roundToInt(),
            (cx + tW / 2f + pad).roundToInt(), (tTop + tH + pad).roundToInt(),
        )
        if (!expanded && slotA.act != null) {
            var left = cx - tW / 2f - bGap - B - pad
            if (slotB.act != null) {
                left -= offB(if (fan) 1f else 0f)
                r.bottom = maxOf(r.bottom, (bTop + B * 1.16f + pad).roundToInt())
            }
            r.left = left.roundToInt()
        }
        r
    }
    val busy = moving || slotA.sep.isRunning || slotB.sep.isRunning || aFan.isRunning || alpha.isRunning
    val drawH = when {
        alpha.value <= 0f && hidden && !busy -> 1
        busy || expanded -> fullH
        else -> ((g.top() + H + 10f) * d).roundToInt().coerceAtMost(fullH)
    }
    SideEffect { onReq(IslandReq(drawH, touch, expanded)) }

    // ───── تأثير الزئبق (يشتغل بس وقت الانفصال والالتحام) ─────
    val goo = remember(d) { gooEffect(d) }
    val gooOn = black && P.goo.v && goo != null && !saver &&
        (slotA.sep.isRunning || slotB.sep.isRunning || aFan.isRunning)

    // ───── اللون واللمعة ─────
    val glowTarget = if (P.glow.v && top != null && top.kind != Kind.PREVIEW) tint(top).copy(alpha = 0.7f) else Color.Transparent
    val glow by animateColorAsState(glowTarget, tween(300), label = "glow")
    // الكبسولة المكبرة بلون غلاف الأغنية
    val tintTarget = when {
        !expanded || top == null || top.kind != Kind.MEDIA || !P.mediaTintBg.v -> Color.Transparent
        top.bg != 0 -> Color(top.bg)
        top.accent != 0 -> Color(top.accent).copy(alpha = 0.32f)
        else -> Color.Transparent
    }
    val bgTint by animateColorAsState(tintTarget, tween(450), label = "tint")

    // ───── اللمس ─────
    val cur by rememberUpdatedState(top)
    val isExp by rememberUpdatedState(expanded)
    val pickList by rememberUpdatedState(picks)
    val othersNow by rememberUpdatedState(others)
    val showNow by rememberUpdatedState(showBubbles)
    fun buzz() { if (P.haptics.v) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    /** ضغطة على دائرة: لو مكدسة تنفرد، وإلا تلتحم مع الجزيرة وتصير هي المعروضة */
    fun tapBubble(s: Slot, a: Act) {
        st.touchTick++
        if (othersNow.size >= 2 && !fan) { buzz(); fan = true; return }
        buzz(); fan = false
        scope.launch {
            s.sep.animateTo(0f, spring(minOf(1f, damp + 0.14f), stiff, 0.002f))
            st.keepOpen = false; st.selected = a.key
            // لو بقت نفس الدائرة (مثلاً فيه مكالمة تغطي) نرجعها
            delay(60)
            if (showNow && othersNow.any { it.key == s.act?.key } && s.sep.value < 0.99f && !s.sep.isRunning) {
                s.sep.animateTo(1f, spring(damp, stiff * 0.8f, 0.002f))
            }
        }
    }
    fun switchBy(dir: Int) {
        val list = pickList
        val a = cur ?: return
        if (list.size < 2) return
        val i = list.indexOfFirst { it.key == a.key }.coerceAtLeast(0)
        val n = list.size
        buzz(); st.keepOpen = isExp; st.selected = list[(i + dir + n) % n].key
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr,
        LocalDensity provides Density(d, dens.fontScale.coerceAtMost(1.1f)),
        PrefRev provides rev,
        LocalSaver provides saver,
    ) {
        Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha.value }) {
            // 1) الشكل (أسود أو زجاج) بالقلم
            Canvas(Modifier.fillMaxSize().graphicsLayer { renderEffect = if (gooOn) goo else null }) {
                val brush = if (black) Brush.verticalGradient(listOf(Color.Black, Color.Black))
                else Brush.verticalGradient(listOf(Color(0xF22A2C34), Color(0xF7111216)))
                val w = aw.value.coerceAtLeast(0f); val h = ah.value.coerceAtLeast(0f)
                val c = aCorner.value.coerceIn(0f, minOf(w, h) / 2f)
                drawRoundRect(brush, Offset(cx - w / 2f, aTop.value), Size(w, h), CornerRadius(c, c))
                val f = aFan.value
                if (slotA.act != null && slotA.sep.value > 0.001f) {
                    drawCircle(brush, B / 2f, Offset(xA(slotA.sep.value), bTop + B / 2f))
                }
                if (slotB.act != null && slotB.sep.value > 0.001f) {
                    val sb = slotB.sep.value
                    drawCircle(brush, B / 2f * scB(f), Offset(xB(sb, f), yB(sb, f) + B / 2f))
                }
            }

            // 2) المحتوى بمقاس الشكل بالضبط
            Box(
                Modifier
                    .offset { IntOffset((cx - aw.value / 2f).roundToInt(), aTop.value.roundToInt()) }
                    .layout { m, _ ->
                        val w = aw.value.roundToInt().coerceAtLeast(0)
                        val h = ah.value.roundToInt().coerceAtLeast(0)
                        val p = m.measure(Constraints.fixed(w, h))
                        layout(w, h) { p.place(0, 0) }
                    }
                    .clip(RoundedCornerShape(aCorner.value.coerceAtLeast(0f)))
                    .background(Brush.verticalGradient(listOf(bgTint, bgTint.dim(0.62f))))
                    .then(
                        if (glow.alpha > 0.02f) Modifier.border(1.dp, Brush.verticalGradient(listOf(glow, glow.copy(alpha = glow.alpha * 0.3f))), RoundedCornerShape(aCorner.value.coerceAtLeast(0f)))
                        else if (!black) Modifier.border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.06f))), RoundedCornerShape(aCorner.value.coerceAtLeast(0f)))
                        else Modifier
                    )
                    // أي لمسة = نأجل التصغير التلقائي
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) { awaitPointerEvent(PointerEventPass.Initial); st.touchTick++ }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                val a = cur ?: return@detectTapGestures
                                if (a.kind == Kind.PREVIEW) return@detectTapGestures
                                when {
                                    isExp -> { a.controls?.open?.invoke(); st.expanded = false }
                                    P.tapAction.v == "open" -> { buzz(); a.controls?.open?.invoke() }
                                    else -> { buzz(); st.expanded = true }
                                }
                            },
                            onLongPress = {
                                val a = cur ?: return@detectTapGestures
                                if (a.kind == Kind.PREVIEW) return@detectTapGestures
                                buzz()
                                if (!isExp && P.tapAction.v == "open") st.expanded = true
                                else { a.controls?.open?.invoke(); st.expanded = false }
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        var dx = 0f; var dy = 0f
                        detectDragGestures(
                            onDragStart = { dx = 0f; dy = 0f },
                            onDragEnd = {
                                val a = cur ?: return@detectDragGestures
                                if (abs(dx) > abs(dy)) {
                                    // يمين/يسار = التطبيق اللي بعده
                                    if (abs(dx) > 50f) switchBy(if (dx < 0) 1 else -1)
                                } else if (dy < -40f) {
                                    if (isExp) st.expanded = false else Hub.dismiss(a.key)
                                } else if (dy > 40f && !isExp && a.kind != Kind.PREVIEW) {
                                    buzz(); st.expanded = true
                                }
                            },
                        ) { change, drag -> dx += drag.x; dy += drag.y; change.consume() }
                    },
            ) {
                // المحتوى بمقاسه الطبيعي (يتقص بالشكل لين يكبر)
                Box(Modifier.wrapContentSize(Alignment.TopCenter, unbounded = true).onSizeChanged { content = it }) {
                    AnimatedContent(
                        targetState = face,
                        transitionSpec = {
                            // الشكل يتمدد أول، والمحتوى يبان بعده
                            (fadeIn(tween(200, delayMillis = 90)) + scaleIn(tween(260, delayMillis = 60), initialScale = 0.9f))
                                .togetherWith(fadeOut(tween(80)) + scaleOut(tween(120), targetScale = 0.94f))
                                .using(SizeTransform(clip = false) { _, _ -> snap() })
                        },
                        contentAlignment = Alignment.TopCenter,
                        label = "face",
                    ) { f ->
                        val a = latest[f.key]
                        when {
                            f.mode == 0 || a == null -> IdleBox(g, keepIdle, appear)
                            f.mode == 1 -> Compact(a, g)
                            else -> Expanded(a, g, st, pickList)
                        }
                    }
                }
            }

            // 3) محتوى الدوائر الجانبية (الأولى، وبعدها الثانية فوقها)
            listOf(slotA, slotB).forEach { s ->
                val a = s.act
                if (a != null && s.sep.value > 0.001f) {
                    val second = s === slotB
                    Box(
                        Modifier
                            .offset {
                                val sep = s.sep.value
                                val f = aFan.value
                                val x = if (second) xB(sep, f) else xA(sep)
                                val y = if (second) yB(sep, f) else bTop
                                IntOffset((x - B / 2f).roundToInt(), y.roundToInt())
                            }
                            .size((B / d).dp)
                            .graphicsLayer {
                                val sc = if (second) scB(aFan.value) else 1f
                                scaleX = sc; scaleY = sc
                                this.alpha = ((s.sep.value - 0.4f) / 0.6f).coerceIn(0f, 1f)
                            }
                            .clip(CircleShape)
                            .pointerInput(a.key) {
                                detectTapGestures(
                                    onTap = { tapBubble(s, a) },
                                    onLongPress = { buzz(); a.controls?.open?.invoke() },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) { Lead(a, (B / d * 0.62f).dp) }
                }
            }
        }
    }
}

/** خانة دائرة جانبية: تحفظ آخر نشاط عشان تقدر تتحرك وهي راجعة */
private class Slot(val sep: Animatable<Float, AnimationVector1D>) { var act by mutableStateOf<Act?>(null) }

@Composable
private fun rememberSlot(a: Act?, show: Boolean, damp: Float, stiff: Float): Slot {
    val slot = remember { Slot(Animatable(0f)) }
    if (a != null) slot.act = a
    val want = a != null && show
    LaunchedEffect(want, a?.key) {
        slot.sep.animateTo(if (want) 1f else 0f, spring(if (want) damp else minOf(1f, damp + 0.14f), stiff * 0.8f, 0.002f))
        if (!want && a == null) slot.act = null
    }
    return slot
}

/** تأثير الزئبق: تضبيب + حد للشفافية = الأشكال القريبة تلتحم */
private fun gooEffect(d: Float): RenderEffect? {
    if (Build.VERSION.SDK_INT < 31) return null
    return try {
        val r = 6f * d
        val m = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 28f, -3400f,
        ))
        android.graphics.RenderEffect.createChainEffect(
            android.graphics.RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(m)),
            android.graphics.RenderEffect.createBlurEffect(r, r, Shader.TileMode.DECAL),
        ).asComposeRenderEffect()
    } catch (_: Throwable) { null }
}

@Composable
private fun IdleBox(g: Geo, pill: Boolean, appear: String) {
    val H = g.compactH()
    when {
        pill -> Box(Modifier.size(max(H, g.gap() + H * 0.9f).dp, H.dp))
        appear == "camera" -> { val s = g.seed(); Box(Modifier.size(s.dp, s.dp)) }
        else -> Box(Modifier.size(g.compactW().dp, H.dp))
    }
}

/** صورة الغلاف: ساكنة أو تدور مثل الأسطوانة */
@Composable
private fun Art(bmp: Bitmap, size: Dp, corner: Dp, playing: Boolean) {
    PrefRev.current
    val spin = P.mediaArtSpin.v && !LocalSaver.current
    val rot = remember { Animatable(0f) }
    LaunchedEffect(spin && playing) {
        if (spin && playing) while (true) {
            rot.snapTo(rot.value % 360f)
            rot.animateTo(rot.value + 360f, tween(9000, easing = LinearEasing))
        } else if (!spin) rot.snapTo(0f)
    }
    Image(
        bmp.asImageBitmap(), null, contentScale = ContentScale.Crop,
        modifier = Modifier.size(size).graphicsLayer { rotationZ = rot.value }
            .clip(if (spin) CircleShape else RoundedCornerShape(corner)),
    )
}

// ───────────── الألوان والأيقونات ─────────────

private fun tint(a: Act): Color = Color(
    when {
        a.accent != 0 && !(a.kind == Kind.MEDIA && !P.mediaArtColor.v) -> a.accent
        a.kind == Kind.CALL_IN || a.kind == Kind.CALL_ON -> Accents.GREEN
        a.kind == Kind.TIMER -> Accents.ORANGE
        else -> Accents.current
    }
)

private fun glyph(a: Act): ImageVector = when (a.kind) {
    Kind.CALL_IN -> Icons.Rounded.Call
    Kind.CALL_ON -> Icons.Rounded.PhoneInTalk
    Kind.MEDIA -> Icons.Rounded.MusicNote
    Kind.TIMER -> Icons.Rounded.Timer
    Kind.NAV -> Icons.Rounded.Navigation
    Kind.PROGRESS -> Icons.Rounded.Download
    Kind.ONGOING -> Icons.Rounded.Sync
    Kind.NOTIF -> Icons.Rounded.Notifications
    Kind.CHARGE -> Icons.Rounded.BatteryChargingFull
    Kind.UNPLUG -> Icons.Rounded.BatteryFull
    Kind.BATTERY_LOW -> Icons.Rounded.BatteryAlert
    Kind.BT -> Icons.Rounded.Bluetooth
    Kind.HEADSET -> Icons.Rounded.Headphones
    Kind.RINGER -> when (a.level) { 0 -> Icons.Rounded.NotificationsOff; 1 -> Icons.Rounded.Vibration; else -> Icons.Rounded.NotificationsActive }
    Kind.TORCH -> Icons.Rounded.FlashlightOn
    Kind.DND -> Icons.Rounded.DoNotDisturbOn
    Kind.UNLOCK -> Icons.Rounded.LockOpen
    Kind.VOLUME -> Icons.AutoMirrored.Rounded.VolumeUp
    Kind.GREETING -> Icons.Rounded.WavingHand
    Kind.PREVIEW -> Icons.Rounded.AutoAwesome
}

// ───────────── الوقت ─────────────

@Composable
private fun rememberNow(active: Boolean, period: Long = 1000L): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active, period) {
        now = System.currentTimeMillis()
        while (active) {
            delay(period - System.currentTimeMillis() % period)
            now = System.currentTimeMillis()
        }
    }
    return now
}

fun fmtTime(ms: Long): String {
    val s = max(0L, ms) / 1000
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

private fun chronoText(a: Act, now: Long): String {
    if (a.chrono <= 0L) return a.text
    return fmtTime(if (a.countDown) a.chrono - now else now - a.chrono)
}

private fun mediaPos(a: Act): Long {
    var p = a.position
    if (a.playing && a.posAt > 0) p += ((SystemClock.elapsedRealtime() - a.posAt) * a.speed).toLong()
    return if (a.duration > 0) p.coerceIn(0L, a.duration) else max(0L, p)
}

// ───────────── الحالات ─────────────

@Composable
private fun Compact(a: Act, g: Geo) {
    PrefRev.current
    val H = g.compactH()
    val side = (g.compactW() - g.gap()) / 2f
    Row(Modifier.size(g.compactW().dp, H.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(side.dp).padding(start = (H * 0.2f).dp), contentAlignment = Alignment.CenterStart) {
            Lead(a, (H * 0.62f).dp)
        }
        Spacer(Modifier.width(g.gap().dp))
        Box(Modifier.width(side.dp).padding(end = (H * 0.24f).dp), contentAlignment = Alignment.CenterEnd) {
            Trail(a, H, side - H * 0.3f)
        }
    }
}

@Composable
private fun Lead(a: Act, size: Dp) {
    val icon = a.icon
    val photoKinds = a.kind == Kind.MEDIA || a.kind == Kind.NOTIF || a.kind == Kind.PROGRESS || a.kind == Kind.ONGOING
    if (icon != null && a.kind == Kind.MEDIA) {
        Art(icon, size, size * 0.28f, a.playing)
    } else if (icon != null && photoKinds) {
        Image(
            bitmap = icon.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(CircleShape),
        )
    } else {
        Icon(glyph(a), null, tint = tint(a), modifier = Modifier.size(size))
    }
}

@Composable
private fun Trail(a: Act, H: Float, maxW: Float) {
    PrefRev.current
    val c = tint(a)
    val textSize = H * 0.37f
    val txt: @Composable (String, Color) -> Unit = { s, col ->
        Text(s, style = ts(textSize, FontWeight.Bold, col), maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = maxW.coerceAtLeast(10f).dp))
    }
    when (a.kind) {
        Kind.MEDIA -> Wave(c, a.playing && P.mediaWave.v, (H * 0.5f).dp)
        Kind.CALL_ON, Kind.TIMER -> {
            val now = rememberNow(a.chrono > 0)
            txt(chronoText(a, now), c)
        }
        Kind.CALL_IN -> txt(a.title.substringBefore(' '), Color.White)
        Kind.PROGRESS -> if (a.indeterminate || a.progress < 0) Ring(-1f, c, (H * 0.55f).dp) else Ring(a.progress, c, (H * 0.55f).dp)
        Kind.CHARGE, Kind.UNPLUG, Kind.BATTERY_LOW -> txt(a.text, c)
        Kind.VOLUME -> LevelBar(a.level / 100f, c, Modifier.width((maxW * 0.8f).coerceAtMost(48f).dp).height((H * 0.18f).dp))
        Kind.UNLOCK -> Unit
        Kind.PREVIEW -> txt("WM", c)
        // التطبيقات الشغالة بالخلفية: نقطة بلونها بدل نص طويل يتقص
        Kind.ONGOING -> Box(Modifier.size((H * 0.24f).dp).clip(CircleShape).background(c))
        Kind.NAV, Kind.NOTIF, Kind.GREETING -> txt(a.text.ifEmpty { a.title }, Color.White)
        else -> txt(a.text, c)
    }
}

@Composable
private fun Wave(color: Color, live: Boolean, h: Dp) {
    // وقت توفير البطارية الموجة تثبت
    val playing = live && !LocalSaver.current
    val bars = 4
    val targets = remember { mutableStateListOf(0.4f, 0.75f, 0.5f, 0.85f) }
    LaunchedEffect(playing) {
        if (playing) while (true) {
            for (i in 0 until bars) targets[i] = 0.25f + Random.nextFloat() * 0.75f
            delay(170)
        } else for (i in 0 until bars) targets[i] = 0.3f
    }
    Row(Modifier.height(h), horizontalArrangement = Arrangement.spacedBy(h * 0.14f), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until bars) {
            val v by animateFloatAsState(targets[i], tween(170), label = "bar")
            Box(Modifier.width(h * 0.16f).fillMaxHeight(v).clip(RoundedCornerShape(50)).background(color))
        }
    }
}

@Composable
private fun Ring(p: Float, color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val sw = this.size.minDimension * 0.16f
        val r = Size(this.size.width - sw, this.size.height - sw)
        val tl = Offset(sw / 2, sw / 2)
        drawArc(color.copy(alpha = 0.25f), 0f, 360f, false, tl, r, style = Stroke(sw))
        val sweep = if (p < 0f) 90f else 360f * p.coerceIn(0f, 1f)
        drawArc(color, -90f, sweep, false, tl, r, style = Stroke(sw, cap = StrokeCap.Round))
    }
}

@Composable
private fun LevelBar(p: Float, color: Color, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.18f))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(p.coerceIn(0f, 1f)).clip(RoundedCornerShape(50)).background(color))
    }
}

// ───────────── الجزيرة المكبرة ─────────────

@Composable
private fun Expanded(a: Act, g: Geo, st: IslandState, picks: List<Act>) {
    PrefRev.current
    CompositionLocalProvider(LocalLayoutDirection provides if (isAr) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Column(Modifier.width(g.expandedW().dp).padding(horizontal = 18.dp, vertical = 16.dp)) {
            when (a.kind) {
                Kind.MEDIA -> MediaFull(a, st)
                Kind.CALL_IN, Kind.CALL_ON -> CallFull(a)
                Kind.TIMER -> TimerFull(a)
                Kind.TORCH -> TorchFull(a, st)
                else -> GenericFull(a)
            }
            // أزرار التطبيق نفسه (إيقاف المؤقت، إلغاء التحميل، إنهاء التسجيل ...)
            val btns = a.controls?.actions.orEmpty()
            if (btns.isNotEmpty() && a.kind != Kind.MEDIA && a.kind != Kind.CALL_IN && a.kind != Kind.CALL_ON) ActionRow(btns, st)
            // أكثر من تطبيق شغال: صف أيقونات تختار منه
            if (picks.size > 1 && picks.any { it.key == a.key }) Switcher(a, picks, st)
        }
    }
}

@Composable
private fun Switcher(cur: Act, picks: List<Act>, st: IslandState) {
    val haptic = LocalHapticFeedback.current
    Spacer(Modifier.height(12.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
    Spacer(Modifier.height(10.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        picks.take(6).forEach { p ->
            val on = p.key == cur.key
            val ring by animateColorAsState(if (on) tint(p) else Color.Transparent, tween(220), label = "ring")
            val sc by animateFloatAsState(if (on) 1f else 0.86f, spring(0.6f, 700f), label = "chip")
            Box(
                Modifier.size(38.dp).graphicsLayer { scaleX = sc; scaleY = sc; alpha = if (on) 1f else 0.65f }
                    .clip(CircleShape).border(2.dp, ring, CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable {
                        if (!on) {
                            if (P.haptics.v) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            st.keepOpen = true; st.selected = p.key
                        }
                        st.touchTick++
                    },
                contentAlignment = Alignment.Center,
            ) { Lead(p, 28.dp) }
        }
    }
}

/** صف أزرار من التطبيق نفسه */
@Composable
private fun ActionRow(btns: List<Pair<String, () -> Unit>>, st: IslandState) {
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        btns.take(3).forEach { (label, run) ->
            Box(
                Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.13f))
                    .clickable { st.touchTick++; run() },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = ts(13.5f, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp))
            }
        }
    }
}

/** الكشاف: شدة الضوء (لو الجوال يسمح) وزر إطفاء */
@Composable
private fun TorchFull(a: Act, st: IslandState) {
    val c = tint(a)
    val ctl = a.controls
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(c.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.FlashlightOn, null, tint = c, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(a.title, style = ts(15.5f, FontWeight.Bold), maxLines = 1)
            if (a.text.isNotEmpty()) Text(a.text, style = ts(13.5f, color = Gray), maxLines = 1)
        }
        ctl?.off?.let { off ->
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)).clickable { off() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.FlashlightOff, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
        }
    }
    val set = ctl?.setLevel
    if (a.levelMax > 1 && set != null) {
        val max = a.levelMax
        var drag by remember(a.key) { mutableStateOf<Float?>(null) }
        var sent by remember(a.key) { mutableStateOf(-1) }
        val frac = drag ?: ((a.level.coerceIn(1, max) - 1).toFloat() / (max - 1))
        Spacer(Modifier.height(10.dp))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Slider(
                value = frac,
                onValueChange = {
                    drag = it; st.touchTick++
                    val lv = 1 + (it * (max - 1)).roundToInt()
                    if (lv != sent) { sent = lv; set(lv) }
                },
                onValueChangeFinished = { drag = null },
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = c, inactiveTrackColor = Color.White.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth().height(28.dp),
            )
        }
    } else {
        Spacer(Modifier.height(8.dp))
        Text(t("جوالك ما يسمح للتطبيقات تغيّر شدة الكشاف", "Your phone doesn't let apps change flashlight strength"),
            style = ts(12f, color = Gray))
    }
}

@Composable
private fun AppLine(a: Act) {
    if (a.app.isNotEmpty()) Text(a.app, style = ts(11.5f, FontWeight.Medium, Gray), maxLines = 1)
}

@Composable
private fun MediaFull(a: Act, st: IslandState) {
    val c = tint(a)
    rememberNow(a.playing, if (LocalSaver.current) 1000L else 500L)
    Row(verticalAlignment = Alignment.CenterVertically) {
        val icon = a.icon
        if (icon != null) {
            Art(icon, 58.dp, 15.dp, a.playing)
        } else {
            Box(Modifier.size(58.dp).clip(RoundedCornerShape(15.dp)).background(c.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MusicNote, null, tint = c, modifier = Modifier.size(30.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            AppLine(a)
            Text(a.title.ifEmpty { t("بدون عنوان", "Untitled") }, style = ts(16.5f, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (a.text.isNotEmpty()) Text(a.text, style = ts(13.5f, color = Gray), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Wave(c, a.playing && P.mediaWave.v, 26.dp)
    }
    val ctl = a.controls
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        if (a.duration > 0 && ctl?.seek != null) {
            var drag by remember(a.key) { mutableStateOf<Float?>(null) }
            val pos = mediaPos(a)
            val frac = drag ?: (pos.toFloat() / a.duration).coerceIn(0f, 1f)
            Spacer(Modifier.height(6.dp))
            Slider(
                value = frac,
                onValueChange = { drag = it; st.touchTick++ },
                onValueChangeFinished = { drag?.let { ctl.seek?.invoke((it * a.duration).toLong()) }; drag = null },
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = c, inactiveTrackColor = Color.White.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth().height(26.dp),
            )
            val shown = (frac * a.duration).toLong()
            Row(Modifier.fillMaxWidth()) {
                Text(fmtTime(shown), style = ts(11f, color = Gray))
                Spacer(Modifier.weight(1f))
                Text("-" + fmtTime(a.duration - shown), style = ts(11f, color = Gray))
            }
        } else Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            CtlBtn(Icons.Rounded.SkipPrevious, 30.dp, ctl?.prev)
            CtlBtn(if (a.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, 40.dp, ctl?.playPause)
            CtlBtn(Icons.Rounded.SkipNext, 30.dp, ctl?.next)
        }
    }
}

@Composable
private fun CtlBtn(icon: ImageVector, size: Dp, onClick: (() -> Unit)?) {
    Box(
        Modifier.size(size + 18.dp).clip(CircleShape).clickable(enabled = onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (onClick != null) Color.White else Color.White.copy(alpha = 0.3f), modifier = Modifier.size(size))
    }
}

@Composable
private fun RoundBtn(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(50.dp).clip(CircleShape).background(color).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
}

@Composable
private fun Avatar(a: Act, size: Dp) {
    val icon = a.icon
    if (icon != null) {
        Image(icon.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(CircleShape))
    } else {
        val c = tint(a)
        Box(Modifier.size(size).clip(CircleShape).background(c.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            val letter = a.title.trim().firstOrNull()
            if (letter != null && letter.isLetter()) Text(letter.uppercase(), style = ts(size.value * 0.42f, FontWeight.Bold, c))
            else Icon(Icons.Rounded.Person, null, tint = c, modifier = Modifier.size(size * 0.55f))
        }
    }
}

@Composable
private fun CallFull(a: Act) {
    PrefRev.current
    val incoming = a.kind == Kind.CALL_IN
    val now = rememberNow(!incoming && a.chrono > 0)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(a, 50.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            AppLine(a)
            Text(a.title.ifEmpty { t("مكالمة", "Call") }, style = ts(17f, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (incoming) t("مكالمة واردة", "Incoming call") else chronoText(a, now).ifEmpty { t("مكالمة جارية", "On call") },
                style = ts(13.5f, FontWeight.Medium, if (incoming) Gray else Color(Accents.GREEN)),
            )
        }
        val ctl = a.controls
        if (P.callControls.v && ctl != null) {
            if (incoming) {
                ctl.decline?.let { RoundBtn(Icons.Rounded.CallEnd, Color(Accents.RED), it) }
                Spacer(Modifier.width(12.dp))
                ctl.answer?.let { RoundBtn(Icons.Rounded.Call, Color(Accents.GREEN), it) }
            } else {
                ctl.hangUp?.let { RoundBtn(Icons.Rounded.CallEnd, Color(Accents.RED), it) }
            }
        }
    }
}

@Composable
private fun TimerFull(a: Act) {
    val c = tint(a)
    val now = rememberNow(a.chrono > 0)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(c.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Timer, null, tint = c, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            AppLine(a)
            Text(a.title.ifEmpty { t("المؤقت", "Timer") }, style = ts(14f, FontWeight.Medium, Gray), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(chronoText(a, now), style = ts(32f, FontWeight.Bold, c), maxLines = 1)
    }
}

@Composable
private fun GenericFull(a: Act) {
    val c = tint(a)
    Row(verticalAlignment = Alignment.CenterVertically) {
        val icon = a.icon
        if (icon != null && a.kind != Kind.CALL_IN) {
            Image(icon.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(CircleShape))
        } else {
            Box(Modifier.size(44.dp).clip(CircleShape).background(c.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(glyph(a), null, tint = c, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            AppLine(a)
            if (a.title.isNotEmpty()) Text(a.title, style = ts(15.5f, FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (a.text.isNotEmpty()) Text(a.text, style = ts(13.5f, color = Color(0xFFD4D4D8)), maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        if (a.kind == Kind.PROGRESS && a.progress >= 0f && !a.indeterminate) {
            Spacer(Modifier.width(8.dp))
            Text("${(a.progress * 100).toInt()}%", style = ts(15f, FontWeight.Bold, c))
        }
    }
    val bar = when {
        a.kind == Kind.PROGRESS -> if (a.indeterminate || a.progress < 0f) -1f else a.progress
        a.kind == Kind.VOLUME || a.kind == Kind.CHARGE || a.kind == Kind.UNPLUG || a.kind == Kind.BATTERY_LOW ->
            if (a.level >= 0) a.level / 100f else null
        else -> null
    }
    if (bar != null) {
        Spacer(Modifier.height(12.dp))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            if (bar < 0f) Pulse(c) else LevelBar(bar, c, Modifier.fillMaxWidth().height(6.dp))
        }
    }
}

@Composable
private fun Pulse(c: Color) {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { on = !on; delay(700) } }
    val a by animateFloatAsState(if (on) 0.9f else 0.35f, tween(700), label = "pulse")
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(c.copy(alpha = a)))
}
