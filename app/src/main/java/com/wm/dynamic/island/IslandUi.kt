package com.wm.dynamic.island

import android.os.SystemClock
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wm.dynamic.core.Accents
import com.wm.dynamic.core.P
import com.wm.dynamic.core.Prefs
import com.wm.dynamic.core.isAr
import com.wm.dynamic.core.t
import com.wm.dynamic.ui.Plex
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.random.Random

/** يخلي الأجزاء تتحدث لما تتغير الإعدادات */
private val PrefRev = compositionLocalOf { 0 }

private data class Face(val mode: Int, val key: String)

private val Gray = Color(0xFFA1A1AA)

private fun ts(size: Float, weight: FontWeight = FontWeight.Normal, color: Color = Color.White) =
    TextStyle(fontFamily = Plex, fontSize = size.sp, fontWeight = weight, color = color)

@Composable
fun IslandView(st: IslandState, onWindow: (w: Int, h: Int, touchable: Boolean) -> Unit) {
    val rev by Prefs.rev.collectAsState()
    val acts by Hub.list.collectAsState()
    val g = st.geo
    val dens = LocalDensity.current

    // نحفظ آخر نسخة من كل نشاط عشان حركة الرجوع تلقى شي ترسمه
    val latest = remember { HashMap<String, Act>() }
    acts.forEach { latest[it.key] = it }

    val off = rev < 0 || !st.screenOn || !P.enabled.v || (g.landscape && P.hideLandscape.v)
    val top = if (off) null else acts.firstOrNull()
    val second = if (off || !P.secondBubble.v) null else acts.getOrNull(1)?.takeIf { it.kind != Kind.PREVIEW }
    val keepIdle = P.idle.v == "pill"

    val autoExp = top != null && (top.autoExpand || top.kind == Kind.NOTIF || top.kind == Kind.GREETING || top.kind == Kind.CALL_IN)
    LaunchedEffect(top?.key) { st.expanded = autoExp }
    val expanded = st.expanded && top != null

    // ترجع صغيرة لحالها بعد شوي لو ما لمستها
    LaunchedEffect(expanded, top?.key, st.touchTick) {
        if (expanded && P.autoCollapse.v && top?.kind != Kind.CALL_IN && top?.kind != Kind.NOTIF) {
            delay(6000); st.expanded = false
        }
    }

    val face = Face(if (top == null) 0 else if (expanded) 2 else 1, top?.key ?: "")
    val tr = updateTransition(face, label = "island")

    // الظهور والاختفاء: تطلع من الكاميرا وترجع تدخل فيها
    val alpha = remember { Animatable(0f) }
    val wantVisible = !off && (top != null || keepIdle)
    LaunchedEffect(wantVisible, off) {
        when {
            off -> alpha.snapTo(0f)
            wantVisible -> alpha.animateTo(1f, tween(70))
            else -> { delay(330); alpha.animateTo(0f, tween(160)) }
        }
    }
    val visible by remember { derivedStateOf { alpha.value > 0.01f } }

    // النافذة: كبيرة وقت الحركة (عشان ما تتعدل كل إطار = سلاسة)، وبمقاس الجزيرة بالضبط وقت السكون
    val H = g.compactH()
    var measured by remember { mutableStateOf(IntSize.Zero) }
    val running = tr.isRunning || alpha.isRunning
    val bigW = with(dens) { (max(g.expandedW(), g.compactW() + 2 * (H + 6f)) + 8f).dp.roundToPx() }
    val bigH = with(dens) { 320.dp.roundToPx() }
    SideEffect {
        when {
            !visible && !wantVisible -> onWindow(1, 1, false)
            running || measured.width <= 1 -> onWindow(bigW, bigH, true)
            else -> onWindow(measured.width, measured.height, true)
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr,
        LocalDensity provides Density(dens.density, dens.fontScale.coerceAtMost(1.1f)),
        PrefRev provides rev,
    ) {
        Box(
            // لمسة على الفراغ اللي حول الجزيرة = تصغير
            Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { st.expanded = false } },
            contentAlignment = Alignment.TopCenter,
        ) {
            Row(
                Modifier.onSizeChanged { measured = it }.graphicsLayer { this.alpha = alpha.value },
                verticalAlignment = Alignment.Top,
            ) {
                val bubble = second != null && !expanded && top != null
                if (bubble) Spacer(Modifier.width(H.dp + 6.dp))
                IslandBody(st, tr, top, expanded, latest, g)
                if (bubble && second != null) {
                    Spacer(Modifier.width(6.dp))
                    Bubble(second, g)
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun IslandBody(st: IslandState, tr: Transition<Face>, top: Act?, expanded: Boolean, latest: Map<String, Act>, g: Geo) {
    PrefRev.current
    val H = g.compactH()
    val glass = P.style.v == "glass"
    // نابض مثل iOS: سريع مع ارتداد خفيف
    val stiff = when (P.speed.v) { 0 -> 320f; 2 -> 1100f; else -> 620f }
    val damp = 0.78f
    val corner by tr.animateDp(transitionSpec = { spring(damp, stiff) }, label = "corner") { f ->
        if (f.mode == 2) 38.dp else (H / 2f).dp
    }
    val glowTarget = if (P.glow.v && top != null && top.kind != Kind.PREVIEW) tint(top).copy(alpha = 0.7f) else Color.Transparent
    val glow by animateColorAsState(glowTarget, tween(300), label = "glow")
    // خلفية تاخذ لون الغلاف أو الفيديو وهي مكبرة
    val tintTarget = if (expanded && top != null && top.kind == Kind.MEDIA && P.mediaTintBg.v && top.accent != 0)
        Color(top.accent).copy(alpha = 0.32f) else Color.Transparent
    val bgTint by animateColorAsState(tintTarget, tween(450), label = "tint")
    val shape = RoundedCornerShape(corner)
    val haptic = LocalHapticFeedback.current

    // ضغطة: تنضغط شوي مثل الزر
    var pressed by remember { mutableStateOf(false) }
    val press by animateFloatAsState(if (pressed) 0.95f else 1f, spring(0.6f, 900f), label = "press")

    val cur by rememberUpdatedState(top)
    val isExp by rememberUpdatedState(expanded)
    fun buzz() { if (P.haptics.v) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }

    tr.AnimatedContent(
        modifier = Modifier
            .graphicsLayer { scaleX = press; scaleY = press }
            .islandBg(shape, glass, glow, bgTint)
            // أي لمسة = نأجل التصغير التلقائي
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) { awaitPointerEvent(PointerEventPass.Initial); st.touchTick++ }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (cur != null) pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = {
                        val a = cur ?: return@detectTapGestures
                        if (a.kind == Kind.PREVIEW) return@detectTapGestures
                        if (isExp) {
                            a.controls?.open?.invoke(); st.expanded = false
                        } else { buzz(); st.expanded = true }
                    },
                    onLongPress = {
                        val a = cur ?: return@detectTapGestures
                        buzz(); a.controls?.open?.invoke(); st.expanded = false
                    },
                )
            }
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        val a = cur
                        if (a != null && total < -40f) {
                            if (isExp) st.expanded = false else Hub.dismiss(a.key)
                        } else if (a != null && total > 40f && !isExp && a.kind != Kind.PREVIEW) {
                            buzz(); st.expanded = true
                        }
                    },
                ) { change, dy -> total += dy; change.consume() }
            },
        transitionSpec = {
            // الشكل يكبر أول، والمحتوى يبان بعده بلحظة
            (fadeIn(tween(200, delayMillis = 100)) + scaleIn(tween(280, delayMillis = 80), initialScale = 0.88f))
                .togetherWith(fadeOut(tween(70)) + scaleOut(tween(110), targetScale = 0.92f))
                .using(SizeTransform(clip = true) { _, _ -> spring<IntSize>(damp, stiff) })
        },
        contentAlignment = Alignment.TopCenter,
    ) { f ->
        val a = latest[f.key]
        when {
            f.mode == 0 || a == null -> Idle(g)
            f.mode == 1 -> Compact(a, g)
            else -> Expanded(a, g, st)
        }
    }
}

private fun Modifier.islandBg(shape: Shape, glass: Boolean, glow: Color, tint: Color): Modifier {
    var m = this.clip(shape)
    m = if (glass) m.background(Brush.verticalGradient(listOf(Color(0xF22A2C34), Color(0xF7111216))))
    else m.background(Color.Black)
    if (tint.alpha > 0.01f) m = m.background(Brush.verticalGradient(listOf(Color.Transparent, tint)))
    m = when {
        glow.alpha > 0.02f -> m.border(1.dp, Brush.verticalGradient(listOf(glow, glow.copy(alpha = glow.alpha * 0.3f))), shape)
        glass -> m.border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.06f))), shape)
        else -> m
    }
    return m
}

/** صورة الغلاف: ساكنة أو تدور مثل الأسطوانة */
@Composable
private fun Art(bmp: Bitmap, size: Dp, corner: Dp, playing: Boolean) {
    PrefRev.current
    val spin = P.mediaArtSpin.v
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
private fun Idle(g: Geo) {
    val H = g.compactH()
    Box(Modifier.size(max(H, g.gap() + H * 0.9f).dp, H.dp))
}

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
        Kind.NAV, Kind.ONGOING, Kind.NOTIF, Kind.GREETING -> txt(a.text.ifEmpty { a.title }, Color.White)
        else -> txt(a.text, c)
    }
}

@Composable
private fun Wave(color: Color, playing: Boolean, h: Dp) {
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

@Composable
private fun Bubble(a: Act, g: Geo) {
    val H = g.compactH()
    val glass = P.style.v == "glass"
    Box(
        Modifier.size(H.dp).islandBg(CircleShape, glass, Color.Transparent)
            .pointerInput(a.key) { detectTapGestures(onTap = { a.controls?.open?.invoke() }) },
        contentAlignment = Alignment.Center,
    ) { Lead(a, (H * 0.6f).dp) }
}

// ───────────── الجزيرة المكبرة ─────────────

@Composable
private fun Expanded(a: Act, g: Geo, st: IslandState) {
    PrefRev.current
    CompositionLocalProvider(LocalLayoutDirection provides if (isAr) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Column(Modifier.width(g.expandedW().dp).padding(horizontal = 18.dp, vertical = 16.dp)) {
            when (a.kind) {
                Kind.MEDIA -> MediaFull(a, st)
                Kind.CALL_IN, Kind.CALL_ON -> CallFull(a)
                Kind.TIMER -> TimerFull(a)
                else -> GenericFull(a)
            }
        }
    }
}

@Composable
private fun AppLine(a: Act) {
    if (a.app.isNotEmpty()) Text(a.app, style = ts(11.5f, FontWeight.Medium, Gray), maxLines = 1)
}

@Composable
private fun MediaFull(a: Act, st: IslandState) {
    val c = tint(a)
    rememberNow(a.playing, 500L)
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
