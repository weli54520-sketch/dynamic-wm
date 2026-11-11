package com.wm.dynamic.ui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wm.dynamic.core.Backdrops
import com.wm.dynamic.core.P
import com.wm.dynamic.core.Prefs
import com.wm.dynamic.core.isAr
import com.wm.dynamic.core.t
import com.wm.dynamic.service.OverlayService
import com.wm.dynamic.update.Updater

class MainActivity : ComponentActivity() {
    /** يزيد كل ما رجعت للتطبيق، عشان نعيد فحص الأذونات */
    var resumeTick by mutableIntStateOf(0)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        enableEdgeToEdge()
        setContent { AppRoot(this) }
    }

    fun bump() { resumeTick++ }

    override fun onResume() {
        super.onResume()
        resumeTick++
        OverlayService.sync(this)
    }

    /** خيار "خلفية جوالك": نظهر الخلفية ونضببها (أندرويد 12+) */
    fun applyBackdrop() {
        val wall = P.bg.v == Backdrops.WALLPAPER
        if (wall) window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                window.setBackgroundBlurRadius(if (wall && windowManager.isCrossWindowBlurEnabled) 90 else 0)
            } catch (_: Exception) {}
        }
    }
}

private data class Tab(val icon: ImageVector, val ar: String, val en: String)

private val tabs = listOf(
    Tab(Icons.Rounded.Home, "الرئيسية", "Home"),
    Tab(Icons.Rounded.Apps, "الأنشطة", "Activities"),
    Tab(Icons.Rounded.Palette, "الشكل", "Look"),
    Tab(Icons.Rounded.Settings, "المزيد", "More"),
)

@Composable
fun AppRoot(act: MainActivity) {
    prefRev()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var page by rememberSaveable { mutableStateOf("") }
    BackHandler(enabled = page.isNotEmpty()) { page = "" }

    LaunchedEffect(P.bg.v) { act.applyBackdrop() }
    LaunchedEffect(Unit) { if (P.autoUpdate.v) Updater.check(quiet = true) }

    val dir = if (isAr) LayoutDirection.Rtl else LayoutDirection.Ltr
    MaterialTheme(colorScheme = darkColorScheme(primary = accentColor(), background = Color.Transparent, surface = Color(0xFF1C1C22))) {
        CompositionLocalProvider(
            LocalLayoutDirection provides dir,
            LocalTextStyle provides ts(15f),
        ) {
            Box(Modifier.fillMaxSize()) {
                Backdrop()
                BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                    val wide = maxWidth >= 600.dp
                    if (page == "apps") {
                        AppsPage(onBack = { page = "" })
                    } else if (wide) {
                        Row(Modifier.fillMaxSize()) {
                            SideNav(tab) { tab = it }
                            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                                Box(Modifier.widthIn(max = 720.dp)) { Pages(tab, act, wide) { page = it } }
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            Pages(tab, act, wide) { page = it }
                            BottomNav(tab, Modifier.align(Alignment.BottomCenter)) { tab = it }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Pages(tab: Int, act: MainActivity, wide: Boolean, open: (String) -> Unit) {
    AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tabs") { i ->
        when (i) {
            0 -> HomeScreen(act, wide)
            1 -> ActivitiesScreen(wide, open)
            2 -> LookScreen(wide)
            else -> MoreScreen(wide)
        }
    }
}

@Composable
private fun BottomNav(sel: Int, modifier: Modifier, onSel: (Int) -> Unit) {
    Row(
        modifier.padding(bottom = 14.dp).glass(RoundedCornerShape(50)).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { i, tb ->
            val on = i == sel
            Row(
                Modifier.clip(RoundedCornerShape(50))
                    .background(if (on) Color.White else Color.Transparent)
                    .clickable { onSel(i) }
                    .padding(horizontal = if (on) 16.dp else 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(tb.icon, null, tint = if (on) Color(0xFF111114) else Soft, modifier = Modifier.size(22.dp))
                if (on) {
                    Spacer(Modifier.width(6.dp))
                    Text(t(tb.ar, tb.en), style = ts(13.5f, FontWeight.Bold, Color(0xFF111114)))
                }
            }
        }
    }
}

@Composable
private fun SideNav(sel: Int, onSel: (Int) -> Unit) {
    Box(Modifier.fillMaxHeight().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.glass(RoundedCornerShape(40.dp)).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            tabs.forEachIndexed { i, tb ->
                val on = i == sel
                Column(
                    Modifier.width(76.dp).clip(RoundedCornerShape(30.dp))
                        .background(if (on) Color.White else Color.Transparent)
                        .clickable { onSel(i) }.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(tb.icon, null, tint = if (on) Color(0xFF111114) else Soft, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(t(tb.ar, tb.en), style = ts(11.5f, FontWeight.Bold, if (on) Color(0xFF111114) else Soft))
                }
            }
        }
    }
}
