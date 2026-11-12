package com.wm.dynamic.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.wm.dynamic.core.CrashLog
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wm.dynamic.BuildConfig
import com.wm.dynamic.core.Accents
import com.wm.dynamic.core.Backdrops
import com.wm.dynamic.core.P
import com.wm.dynamic.core.Perms
import com.wm.dynamic.core.t
import com.wm.dynamic.island.Act
import com.wm.dynamic.island.Hub
import com.wm.dynamic.island.Kind
import com.wm.dynamic.island.Pri
import com.wm.dynamic.service.OverlayService
import com.wm.dynamic.update.UpdateState
import com.wm.dynamic.update.Updater
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val cGreen = Color(Accents.GREEN)
private val cRed = Color(Accents.RED)
private val cOrange = Color(Accents.ORANGE)
private val cBlue = Color(Accents.BLUE)
private val cPurple = Color(Accents.PURPLE)
private val cYellow = Color(Accents.YELLOW)
private val cPink = Color(0xFFFF375F)
private val cCyan = Color(0xFF64D2FF)
private val cGray = Color(0xFF8E8E93)

@Composable
private fun Screen(wide: Boolean, title: String, sub: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = if (wide) 28.dp else 112.dp),
    ) {
        Text(title, style = ts(30f, FontWeight.Bold), modifier = Modifier.padding(start = 6.dp))
        Text(sub, style = ts(14f, color = Soft), modifier = Modifier.padding(start = 6.dp, bottom = 6.dp))
        content()
    }
}

@Composable
private fun Sub(content: @Composable () -> Unit) {
    Box(Modifier.padding(start = 54.dp)) { content() }
}

// ───────────── الرئيسية ─────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(act: MainActivity, wide: Boolean) {
    val ctx = LocalContext.current
    prefRev()
    val tick = act.resumeTick
    val acc = remember(tick) { Perms.accessibility(ctx) }
    val ovl = remember(tick) { Perms.overlay(ctx) }
    val notif = remember(tick) { Perms.notifications(ctx) }
    val batt = remember(tick) { Perms.battery(ctx) }
    val calls = remember(tick) { Perms.calls(ctx) }
    val bt = remember(tick) { Perms.bluetooth(ctx) }
    val post = remember(tick) { Perms.postNotif(ctx) }
    val modeAcc = P.mode.v == "acc"
    val ready = (if (modeAcc) acc else ovl) && notif
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { act.bump() }

    Screen(wide, "Dynamic WM", t("جزيرتك بلمستك", "Your island, your touch")) {
        Spacer(Modifier.height(8.dp))
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t("تشغيل الجزيرة", "Island on"), style = ts(18f, FontWeight.Bold))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val ok = ready && P.enabled.v
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (ok) cGreen else cOrange))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                !P.enabled.v -> t("مطفية", "Off")
                                ready -> t("شغالة وجاهزة", "Running")
                                else -> t("باقي أذونات تحت", "Finish the permissions below")
                            },
                            style = ts(13f, color = Soft),
                        )
                    }
                }
                Toggle(P.enabled.v) { P.enabled.v = it; OverlayService.sync(ctx) }
            }
        }

        UpdateCard(showIdle = false)

        SectionTitle(t("الأذونات", "Permissions"))
        GlassCard {
            if (modeAcc) {
                PermRow(t("خدمة إمكانية الوصول", "Accessibility service"),
                    t("عشان الجزيرة تطلع فوق شريط الحالة وتقدر تضغط عليها.", "Draws the island over the status bar."),
                    acc, button = t("افتح", "Open")) { Perms.openAccessibility(ctx) }
            } else {
                PermRow(t("الظهور فوق التطبيقات", "Display over apps"),
                    t("عشان ترسم الجزيرة فوق التطبيقات.", "Draws the island over apps."),
                    ovl, button = t("افتح", "Open")) { Perms.openOverlay(ctx) }
            }
            PermRow(t("الوصول للإشعارات", "Notification access"),
                t("عشان تقرأ الموسيقى والمكالمات والمؤقتات والإشعارات.", "Reads music, calls, timers and notifications."),
                notif, button = t("افتح", "Open")) { Perms.openNotifications(ctx) }
            PermRow(t("توفير البطارية", "Battery optimization"),
                t("اختر: غير مقيد، عشان سامسونج ما يوقف الجزيرة.", "Choose Unrestricted so Samsung won't stop it."),
                batt, optional = true, button = t("افتح", "Open")) { Perms.openBattery(ctx) }
            PermRow(t("الرد على المكالمات", "Answer calls"),
                t("اختياري: الرد والإنهاء مباشرة من الجزيرة.", "Optional: answer and end calls from the island."),
                calls, optional = true, button = t("سماح", "Allow")) { ask.launch(Manifest.permission.ANSWER_PHONE_CALLS) }
            PermRow(t("أجهزة البلوتوث", "Bluetooth devices"),
                t("اختياري: يعرض اسم السماعة لما تتصل.", "Optional: shows the device name."),
                bt, optional = true, button = t("سماح", "Allow")) { ask.launch(Manifest.permission.BLUETOOTH_CONNECT) }
            if (!modeAcc) {
                PermRow(t("إشعار التطبيق", "App notification"),
                    t("اختياري: إشعار صغير يخلي الجزيرة شغالة.", "Optional: keeps the island alive."),
                    post, optional = true, button = t("سماح", "Allow")) { ask.launch(Manifest.permission.POST_NOTIFICATIONS) }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                t("إذا كان الخيار رمادي ومقفل: افتح معلومات التطبيق ← النقاط الثلاث فوق ← السماح بالإعدادات المقيدة، وارجع فعّله.",
                    "If the option is greyed out: App info → ⋮ → Allow restricted settings, then enable it."),
                style = ts(12.5f, color = Faint),
            )
            Spacer(Modifier.height(10.dp))
            Pill(t("معلومات التطبيق", "App info")) { Perms.openAppInfo(ctx) }
        }

        SectionTitle(t("تجربة", "Try it"))
        GlassCard {
            val go: (() -> Unit) -> Unit = { f ->
                if (!ready) Toast.makeText(ctx, t("كمّل الأذونات أول", "Finish the permissions first"), Toast.LENGTH_SHORT).show()
                f()
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(t("موسيقى", "Music")) { go { Demo.music() } }
                Pill(t("مكالمة", "Call")) { go { Demo.call() } }
                Pill(t("مؤقت", "Timer")) { go { Demo.timer() } }
                Pill(t("شحن", "Charging")) { go { Demo.charge(ctx) } }
                Pill(t("تحميل", "Download")) { go { Demo.download() } }
                Pill(t("إشعار", "Notification")) { go { Demo.notif() } }
                Pill(t("بلوتوث", "Bluetooth")) { go { Demo.bluetooth() } }
                Pill(t("3 تطبيقات", "3 apps")) { go { Demo.multi() } }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                t("ضغطة: تكبير · ضغطة طويلة: تفتح التطبيق · سحب لفوق: إخفاء · سحب لتحت: تكبير · يمين ويسار: تبديل",
                    "Tap: expand · Long press: open app · Swipe up: hide · Swipe down: expand · Sideways: switch"),
                style = ts(12.5f, color = Faint),
            )
        }
    }
}

// ───────────── الأنشطة ─────────────

@Composable
fun ActivitiesScreen(wide: Boolean, open: (String) -> Unit) {
    prefRev()
    Screen(wide, t("الأنشطة", "Activities"), t("اختر وش يطلع في الجزيرة", "Choose what shows in the island")) {
        SectionTitle(t("الأساسية", "Live activities"))
        GlassCard {
            ToggleRow(Icons.Rounded.MusicNote, cPink, t("الموسيقى", "Music"), t("الأغنية والغلاف والتحكم", "Song, artwork and controls"), P.media)
            if (P.media.v) {
                Sub { ToggleRow(null, cPink, t("حركة الموجة", "Wave animation"), pref = P.mediaWave) }
                Sub { ToggleRow(null, cPink, t("لون من غلاف الأغنية", "Color from artwork"), pref = P.mediaArtColor) }
                Sub { ToggleRow(null, cPink, t("الكبسولة المكبرة بلون الغلاف", "Expanded island in artwork color"), t("بدل الأسود", "Instead of black"), P.mediaTintBg) }
                Sub { ToggleRow(null, cPink, t("الغلاف يدور", "Spinning artwork"), t("مثل الأسطوانة وقت التشغيل", "Like a record while playing"), P.mediaArtSpin) }
                Sub { ToggleRow(null, cPink, t("أخفيها لما يكون تطبيق الموسيقى مفتوح", "Hide while the music app is open"), pref = P.mediaHideOpen) }
                Sub {
                    SliderRow(t("تختفي بعد الإيقاف", "Hide after pause"), P.mediaKeep.v.toFloat(), 0f..60f,
                        if (P.mediaKeep.v == 0) t("فوراً", "Instantly") else t("${P.mediaKeep.v} ث", "${P.mediaKeep.v}s")) {
                        P.mediaKeep.v = it.roundToInt()
                    }
                }
            }
            ToggleRow(Icons.Rounded.Call, cGreen, t("المكالمات", "Calls"), t("اسم المتصل ووقت المكالمة", "Caller name and call time"), P.call)
            Sub {
                ToggleRow(null, cGreen, t("أزرار الرد والإنهاء", "Answer / end buttons"),
                    t("طفها إذا تعارضت مع تطبيق الاتصال", "Turn off if it conflicts with your dialer"), P.callControls, enabled = P.call.v)
            }
            ToggleRow(Icons.Rounded.Timer, cOrange, t("المؤقت والساعة", "Timers"), t("المؤقت وساعة الإيقاف والتسجيل", "Timer, stopwatch and recorders"), P.timer)
            ToggleRow(Icons.Rounded.Navigation, cBlue, t("الخرائط", "Navigation"), t("الاتجاهات من تطبيقات الخرائط", "Directions from maps apps"), P.nav)
            ToggleRow(Icons.Rounded.Download, cCyan, t("التحميل والتقدم", "Downloads & progress"), t("أي تحميل أو رفع فيه نسبة", "Any download or upload"), P.progress)
            ToggleRow(Icons.Rounded.Sync, cPurple, t("التطبيقات بالخلفية", "Background apps"), t("أي تطبيق شغال بالخلفية يطلع لحاله", "Apps running in the background"), P.ongoing)
        }

        SectionTitle(t("الإشعارات", "Notifications"))
        GlassCard {
            ToggleRow(Icons.Rounded.Notifications, cPink, t("عرض الإشعارات", "Show notifications"), t("الرسائل تطلع في الجزيرة شوي وتختفي", "Messages pop in the island"), P.notif)
            if (P.notif.v) {
                SliderRow(t("مدة الظهور", "Duration"), P.notifSec.v.toFloat(), 2f..10f, t("${P.notifSec.v} ث", "${P.notifSec.v}s")) {
                    P.notifSec.v = it.roundToInt()
                }
                ToggleRow(Icons.Rounded.Lock, cGray, t("إخفاء المحتوى وقت القفل", "Hide content when locked"), pref = P.notifHideLocked)
                ToggleRow(Icons.Rounded.VisibilityOff, cGray, t("تجاهل إشعارات التطبيق المفتوح", "Skip the open app's notifications"), pref = P.notifSkipOpen)
            }
            val n = P.blockedApps.v.size
            NavRow(Icons.Rounded.Apps, cBlue, t("اختيار التطبيقات", "Choose apps"), t("التطبيقات اللي تطلع في الجزيرة", "Apps allowed in the island"),
                value = if (n == 0) t("الكل", "All") else t("$n مستبعد", "$n off")) { open("apps") }
        }

        SectionTitle(t("أحداث الجوال", "Phone events"))
        GlassCard {
            ToggleRow(Icons.Rounded.BatteryChargingFull, cGreen, t("الشحن", "Charging"), t("لما توصل الشاحن أو تفصله", "Plug in / unplug"), P.evCharge)
            ToggleRow(Icons.Rounded.BatteryAlert, cRed, t("البطارية الضعيفة", "Low battery"), pref = P.evBattery)
            ToggleRow(Icons.Rounded.Bluetooth, cBlue, t("البلوتوث", "Bluetooth"), t("لما تتصل سماعة أو ساعة", "Headphones, watches..."), P.evBt)
            ToggleRow(Icons.Rounded.Headphones, cCyan, t("السماعة السلكية", "Wired headphones"), pref = P.evHeadset)
            ToggleRow(Icons.Rounded.Vibration, cOrange, t("الصامت والاهتزاز", "Silent / vibrate"), pref = P.evRinger)
            ToggleRow(Icons.Rounded.FlashlightOn, cYellow, t("الكشاف", "Flashlight"), t("يبقى ظاهر وهو مشغّل، وكبّره عشان تتحكم بشدته", "Stays while on; expand it to set brightness"), P.evTorch)
            ToggleRow(Icons.Rounded.DoNotDisturbOn, cPurple, t("عدم الإزعاج", "Do not disturb"), pref = P.evDnd)
            ToggleRow(Icons.Rounded.LockOpen, cGray, t("فتح القفل", "Unlock"), pref = P.evUnlock)
            ToggleRow(Icons.AutoMirrored.Rounded.VolumeUp, cBlue, t("مستوى الصوت", "Volume"), t("بدل شريط الصوت العادي", "Shows the volume level"), P.evVolume)
        }
    }
}

// ───────────── الشكل ─────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LookScreen(wide: Boolean) {
    prefRev()
    // معاينة: تبان الجزيرة وأنت تعدّل
    // بس وأنت فاتح الشاشة: لو طلعت من التطبيق تختفي
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val show = { Hub.put(Act("preview", Kind.PREVIEW, Pri.PREVIEW, title = "Dynamic WM")) }
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> show()
                Lifecycle.Event.ON_PAUSE -> Hub.remove("preview")
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(obs)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) show()
        onDispose { owner.lifecycle.removeObserver(obs); Hub.remove("preview") }
    }
    val cur = if (LocalConfiguration.current.smallestScreenWidthDp >= 600) "inner" else "outer"
    var scr by remember(cur) { mutableStateOf(cur) }
    val sp = P.screen(scr)

    Screen(wide, t("الشكل", "Look"), t("خلّها على ذوقك", "Make it yours")) {
        SectionTitle(t("الستايل", "Style"))
        GlassCard {
            Segmented(listOf(t("أسود (مثل iOS)", "Black (like iOS)"), t("زجاج سائل", "Liquid glass")), if (P.style.v == "glass") 1 else 0) {
                P.style.v = if (it == 1) "glass" else "black"
            }
            Spacer(Modifier.height(14.dp))
            Text(t("لون التمييز", "Accent color"), style = ts(14.5f, FontWeight.Medium))
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Accents.colors.forEachIndexed { i, c ->
                    val on = P.accent.v == i
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(Color(c))
                            .then(if (on) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                            .clickable { P.accent.v = i },
                        contentAlignment = Alignment.Center,
                    ) { if (on) Icon(Icons.Rounded.Check, null, tint = Color.Black.copy(alpha = 0.7f), modifier = Modifier.size(18.dp)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            ToggleRow(null, Color.White, t("حد مضيء بلون النشاط", "Glowing edge"), pref = P.glow)
        }

        SectionTitle(t("الحجم والمكان", "Size & position"))
        GlassCard {
            Segmented(
                listOf(
                    t("الشاشة الداخلية", "Inner screen") + if (cur == "inner") " •" else "",
                    t("الخارجية", "Cover screen") + if (cur == "outer") " •" else "",
                ),
                if (scr == "inner") 0 else 1,
            ) { scr = if (it == 0) "inner" else "outer" }
            Spacer(Modifier.height(10.dp))
            SliderRow(t("الحجم", "Size"), sp.scale.v, 0.8f..1.4f, "${(sp.scale.v * 100).roundToInt()}%") { sp.scale.v = it }
            SliderRow(t("العرض", "Width"), sp.extraW.v, 0f..80f, "+${sp.extraW.v.roundToInt()}") { sp.extraW.v = it }
            SliderRow(t("يمين ويسار", "Left / right"), sp.offX.v, -150f..150f, "${sp.offX.v.roundToInt()}") { sp.offX.v = it }
            SliderRow(t("فوق وتحت", "Up / down"), sp.offY.v, -30f..60f, "${sp.offY.v.roundToInt()}") { sp.offY.v = it }
            ToggleRow(null, Color.White, t("اكتشاف مكان الكاميرا تلقائياً", "Detect the camera automatically"), pref = sp.autoCam)
            Spacer(Modifier.height(8.dp))
            Text(
                t("التعديل يبان على الجزيرة مباشرة. كل شاشة لها إعدادها، افتح أو سكّر الفولد عشان تشوف الثانية.",
                    "Changes show live. Each screen has its own setting; fold or unfold to see the other one."),
                style = ts(12.5f, color = Faint),
            )
            Spacer(Modifier.height(10.dp))
            Pill(t("رجوع للافتراضي", "Reset")) { sp.reset() }
        }

        SectionTitle(t("الحركة", "Motion"))
        GlassCard {
            val motions = listOf("ios", "smooth", "fast")
            Text(t("نمط الحركة", "Motion style"), style = ts(14.5f, FontWeight.Medium))
            Spacer(Modifier.height(8.dp))
            Segmented(listOf(t("آيفون", "iPhone"), t("ناعم", "Smooth"), t("سريع", "Fast")), motions.indexOf(P.motion.v).coerceAtLeast(0)) {
                P.motion.v = motions[it]
            }
            Spacer(Modifier.height(4.dp))
            Text(t("آيفون: ارتداد خفيف. ناعم: بدون ارتداد. سريع: حركة خاطفة.",
                "iPhone: light bounce. Smooth: no bounce. Fast: snappy."), style = ts(12.5f, color = Faint))
            Spacer(Modifier.height(12.dp))
            val appears = listOf("camera", "drop", "fade")
            Text(t("طريقة الظهور", "How it appears"), style = ts(14.5f, FontWeight.Medium))
            Spacer(Modifier.height(8.dp))
            Segmented(listOf(t("من الكاميرا", "From camera"), t("من فوق", "Drop down"), t("تلاشي", "Fade")), appears.indexOf(P.appear.v).coerceAtLeast(0)) {
                P.appear.v = appears[it]
            }
            Spacer(Modifier.height(6.dp))
            ToggleRow(null, Color.White, t("التحام مثل الزئبق", "Liquid merge"),
                t("الدائرة الثانية تنفصل وتلتحم مع الجزيرة (الستايل الأسود)", "The second circle splits and merges (black style)"), P.goo)
            ToggleRow(null, Color.White, t("اهتزاز خفيف عند اللمس", "Haptic feedback"), pref = P.haptics)
        }

        SectionTitle(t("السلوك", "Behavior"))
        GlassCard {
            Text(t("لما ما فيه شي", "When idle"), style = ts(14.5f, FontWeight.Medium))
            Spacer(Modifier.height(8.dp))
            Segmented(listOf(t("مخفية", "Hidden"), t("حبة حول الكاميرا", "Pill around camera")), if (P.idle.v == "pill") 1 else 0) {
                P.idle.v = if (it == 1) "pill" else "hidden"
            }
            Spacer(Modifier.height(6.dp))
            Spacer(Modifier.height(10.dp))
            Text(t("كم نشاط يبان بنفس الوقت", "Activities shown at once"), style = ts(14.5f, FontWeight.Medium))
            Spacer(Modifier.height(8.dp))
            Segmented(listOf("1", "2", "3"), P.maxActs.v.coerceIn(1, 3) - 1) { P.maxActs.v = it + 1 }
            Spacer(Modifier.height(4.dp))
            Text(t("الزيادة تطلع دوائر على يسار الجزيرة. لو أكثر من وحدة تكون فوق بعض: اضغطها تنفرد، واختر اللي تبي.",
                "Extra ones show as circles on the left. If stacked, tap to fan them out, then pick one."), style = ts(12.5f, color = Faint))
            Spacer(Modifier.height(12.dp))
            Text(t("الضغطة الوحدة على الجزيرة", "Single tap on the island"), style = ts(14.5f, FontWeight.Medium))
            Spacer(Modifier.height(8.dp))
            Segmented(listOf(t("تكبر", "Expand"), t("تفتح التطبيق", "Open app")), if (P.tapAction.v == "open") 1 else 0) {
                P.tapAction.v = if (it == 1) "open" else "expand"
            }
            Spacer(Modifier.height(4.dp))
            Text(t("الضغطة الطويلة تسوي العكس. والسحب يمين ويسار يبدّل بين التطبيقات.",
                "Long press does the other. Swipe sideways to switch apps."), style = ts(12.5f, color = Faint))
            Spacer(Modifier.height(6.dp))
            ToggleRow(null, Color.White, t("إخفاء بالوضع الأفقي", "Hide in landscape"), pref = P.hideLandscape)
            ToggleRow(null, Color.White, t("إخفاء وقت ملء الشاشة", "Hide in full screen"),
                t("الفيديو والألعاب. المكالمة الواردة تطلع برضو", "Videos and games. Incoming calls still show"), P.hideFullscreen)
            ToggleRow(null, Color.White, t("توفير البطارية تلقائي", "Automatic battery saver"),
                t("لما تشغّل توفير الطاقة أو تضعف البطارية: الموجة تثبت والحركات تخف", "With power saving or low battery: still wave, lighter motion"), P.autoSaver)
            ToggleRow(null, Color.White, t("تصغير تلقائي", "Auto collapse"), t("ترجع صغيرة بعد ثواني", "Shrinks back after a few seconds"), P.autoCollapse)
        }

        SectionTitle(t("خلفية التطبيق", "App background"))
        GlassCard {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Backdrops.gradients.forEachIndexed { i, g ->
                    Swatch(P.bg.v == i, false, Modifier.background(Brush.linearGradient(g.map { Color(it) }))) { P.bg.v = i }
                }
                Swatch(P.bg.v == Backdrops.WALLPAPER, true, Modifier.background(Color(0x33FFFFFF))) { P.bg.v = Backdrops.WALLPAPER }
            }
            Spacer(Modifier.height(8.dp))
            Text(t("الأخير: خلفية جوالك مضببة (زجاج حقيقي).", "Last one: your blurred wallpaper (real glass)."), style = ts(12.5f, color = Faint))
        }
    }
}

@Composable
private fun Swatch(on: Boolean, wall: Boolean, fill: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.size(54.dp).clip(shape).then(fill)
            .border(if (on) 3.dp else 1.dp, if (on) Color.White else Color(0x40FFFFFF), shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (on || wall) Icon(if (on) Icons.Rounded.Check else Icons.Rounded.Wallpaper, null,
            tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

// ───────────── المزيد ─────────────

@Composable
fun MoreScreen(wide: Boolean) {
    val ctx = LocalContext.current
    prefRev()
    Screen(wide, t("المزيد", "More"), t("اللغة والتحديثات ولمستك", "Language, updates and your touch")) {
        SectionTitle(t("اللغة", "Language"))
        GlassCard {
            Segmented(listOf("العربية", "English"), if (P.lang.v == "en") 1 else 0) { P.lang.v = if (it == 1) "en" else "ar" }
        }

        SectionTitle(t("طريقة العرض", "Display method"))
        GlassCard {
            Segmented(listOf(t("إمكانية الوصول", "Accessibility"), t("فوق التطبيقات", "Over apps")), if (P.mode.v == "overlay") 1 else 0) {
                P.mode.v = if (it == 1) "overlay" else "acc"
                OverlayService.sync(ctx)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                t("إمكانية الوصول أفضل: تطلع فوق شريط الحالة وبدون إشعار ثابت. فوق التطبيقات: بديل لو ما اشتغلت الأولى.",
                    "Accessibility is best: above the status bar, no sticky notification. Over apps is a fallback."),
                style = ts(12.5f, color = Faint),
            )
        }

        SectionTitle(t("لمستك", "Your touch"))
        GlassCard {
            ToggleRow(Icons.Rounded.WavingHand, cYellow, t("رسالة ترحيب", "Greeting"), t("تطلع في الجزيرة لما تفتح القفل", "Shows when you unlock"), P.greetOn)
            if (P.greetOn.v) {
                var txt by remember { mutableStateOf(P.greetText.v) }
                val acc = accentColor()
                OutlinedTextField(
                    value = txt,
                    onValueChange = { txt = it.take(40); P.greetText.v = txt },
                    placeholder = { Text(t("مثال: هلا والله 👋", "e.g. Welcome back 👋"), style = ts(14f, color = Faint)) },
                    singleLine = true,
                    textStyle = ts(15f),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = acc,
                        focusedBorderColor = acc, unfocusedBorderColor = Faint,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
        }

        SectionTitle(t("التحديثات", "Updates"))
        UpdateCard(showIdle = true)
        Spacer(Modifier.height(8.dp))
        GlassCard {
            ToggleRow(Icons.Rounded.SystemUpdate, cBlue, t("فحص تلقائي", "Check automatically"), t("لما تفتح التطبيق", "When you open the app"), P.autoUpdate)
        }

        SectionTitle(t("نصائح", "Tips"))
        GlassCard {
            listOf(
                t("في سامسونج: الإعدادات ← البطارية ← حدود استخدام الخلفية، وتأكد إن التطبيق مو في التطبيقات النايمة.",
                    "Samsung: Settings → Battery → Background usage limits; keep the app out of sleeping apps."),
                t("إذا ما طلعت الجزيرة: طف خدمة إمكانية الوصول وشغلها مرة ثانية.",
                    "If the island doesn't show: turn the accessibility service off and on."),
                t("كل شاشة في الفولد لها حجم ومكان. اضبطها من قسم الشكل.",
                    "Each Fold screen has its own size and position (Look tab)."),
                t("التطبيق ما يرسل أي شي برا جوالك.", "Nothing leaves your phone."),
            ).forEach { tip ->
                Row(Modifier.padding(vertical = 6.dp)) {
                    Icon(Icons.Rounded.Lightbulb, null, tint = cYellow, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(tip, style = ts(13.5f, color = Soft))
                }
            }
        }

        CrashCard()

        SectionTitle(t("عن التطبيق", "About"))
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.Info, accentColor())
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Dynamic WM ${BuildConfig.VERSION_NAME}", style = ts(15.5f, FontWeight.Bold))
                    Text(t("جزيرة ديناميكية بتصميمك وبدون اشتراك", "Your own dynamic island, no subscription"), style = ts(12.5f, color = Faint))
                }
            }
        }
    }
}

/** آخر خطأ وقّف التطبيق: تنسخه وترسله */
@Composable
private fun CrashCard() {
    var log by remember { mutableStateOf(CrashLog.read()) }
    val text = log ?: return
    val clip = LocalClipboardManager.current
    val ctx = LocalContext.current
    SectionTitle(t("آخر خطأ", "Last crash"))
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.BugReport, Color(0xFFFF453A))
            Spacer(Modifier.width(14.dp))
            Text(t("التطبيق توقف مرة. انسخ الخطأ وأرسله لـ Claude.", "The app crashed once. Copy and send it to Claude."),
                style = ts(13.5f, color = Soft), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Text(text.take(700), style = ts(11f, color = Faint), maxLines = 10)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill(t("نسخ", "Copy"), filled = true) {
                clip.setText(AnnotatedString(text))
                Toast.makeText(ctx, t("انتسخ", "Copied"), Toast.LENGTH_SHORT).show()
            }
            Pill(t("مسح", "Clear")) { CrashLog.clear(); log = null }
        }
    }
}

@Composable
fun UpdateCard(showIdle: Boolean) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val s by Updater.state.collectAsState()
    val active = s is UpdateState.Available || s is UpdateState.Downloading || s is UpdateState.Installing
    if (!showIdle && !active) return
    val acc = accentColor()
    Spacer(Modifier.height(12.dp))
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.SystemUpdate, acc)
            Spacer(Modifier.width(14.dp))
            val st = s
            val (title, sub) = when (st) {
                is UpdateState.Idle -> t("الإصدار الحالي", "Current version") + " ${BuildConfig.VERSION_NAME}" to null
                is UpdateState.Checking -> t("جاري الفحص...", "Checking...") to null
                is UpdateState.UpToDate -> t("عندك آخر إصدار", "You're up to date") to "${BuildConfig.VERSION_NAME}"
                is UpdateState.Available -> t("تحديث جديد", "New update") + " 1.${st.version}" to
                    (if (st.size > 0) "%.1f MB".format(st.size / 1048576f) else null)
                is UpdateState.Downloading -> t("جاري التحميل", "Downloading") + " ${st.percent}%" to null
                is UpdateState.Installing -> t("جاري التثبيت...", "Installing...") to null
                is UpdateState.Error -> t("صار خطأ", "Something went wrong") to st.message
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = ts(15.5f, FontWeight.Bold))
                if (sub != null) Text(sub, style = ts(12.5f, color = Faint))
            }
            when (st) {
                is UpdateState.Available -> Pill(t("تحديث", "Update"), filled = true) {
                    if (!Updater.canInstall(ctx)) {
                        Updater.openInstallPermission(ctx)
                        Toast.makeText(ctx, t("فعّل السماح وارجع اضغط تحديث", "Allow it, then tap Update again"), Toast.LENGTH_LONG).show()
                    } else scope.launch { Updater.downloadAndInstall(ctx, st) }
                }
                is UpdateState.Idle, is UpdateState.UpToDate, is UpdateState.Error ->
                    Pill(t("تحقق", "Check")) { scope.launch { Updater.check() } }
                else -> Unit
            }
        }
        val dl = s
        if (dl is UpdateState.Downloading) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(Color(0x33FFFFFF))) {
                Box(Modifier.fillMaxWidth(dl.percent / 100f).height(6.dp).clip(RoundedCornerShape(50)).background(acc))
            }
        }
    }
}
