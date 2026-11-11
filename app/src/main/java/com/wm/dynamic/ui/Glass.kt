package com.wm.dynamic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wm.dynamic.core.Accents
import com.wm.dynamic.core.Backdrops
import com.wm.dynamic.core.BoolPref
import com.wm.dynamic.core.P
import com.wm.dynamic.core.Prefs
import com.wm.dynamic.core.isAr

val White = Color.White
val Soft = Color(0xB8FFFFFF)
val Faint = Color(0x73FFFFFF)

fun ts(size: Float, weight: FontWeight = FontWeight.Normal, color: Color = White) =
    TextStyle(fontFamily = Plex, fontSize = size.sp, fontWeight = weight, color = color, lineHeight = (size * 1.45f).sp)

/** يرجّع رقم يتغير مع أي إعداد، عشان الشاشة تتحدث */
@Composable
fun prefRev(): Int {
    val r by Prefs.rev.collectAsState()
    return r
}

@Composable
fun accentColor(): Color { prefRev(); return Color(Accents.current) }

/** خلفية الإعدادات (تدرّج أو خلفية الجوال المضببة) */
@Composable
fun Backdrop() {
    prefRev()
    val i = P.bg.v
    if (i == Backdrops.WALLPAPER) {
        Box(Modifier.fillMaxSize().background(Color(0x66000000)))
        return
    }
    val c = Backdrops.gradients[i.coerceIn(0, Backdrops.gradients.lastIndex)].map { Color(it) }
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(c)))
    // توهج خفيف يعطي إحساس الزجاج
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(c[1].copy(alpha = 0.55f), Color.Transparent), radius = 900f)))
}

private val cardShape = RoundedCornerShape(28.dp)

fun Modifier.glass(shape: androidx.compose.ui.graphics.Shape = cardShape): Modifier = this
    .clip(shape)
    .background(Brush.verticalGradient(listOf(Color(0x2EFFFFFF), Color(0x14FFFFFF))))
    .border(1.dp, Brush.verticalGradient(listOf(Color(0x59FFFFFF), Color(0x14FFFFFF))), shape)

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().glass().padding(horizontal = 18.dp, vertical = 16.dp), content = content)
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = ts(14f, FontWeight.Bold, Soft), modifier = Modifier.padding(start = 8.dp, top = 22.dp, bottom = 10.dp))
}

@Composable
fun IconBadge(icon: ImageVector, color: Color, size: Int = 40) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.32f).dp)).background(color.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = color, modifier = Modifier.size((size * 0.55f).dp)) }
}

@Composable
fun Toggle(checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val acc = accentColor()
    Switch(
        checked = checked, onCheckedChange = onChange, enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = White, checkedTrackColor = acc, checkedBorderColor = acc,
            uncheckedThumbColor = Color(0xFFD4D4D8), uncheckedTrackColor = Color(0x33FFFFFF), uncheckedBorderColor = Color(0x55FFFFFF),
        ),
    )
}

/** صف فيه أيقونة + عنوان + شرح + مفتاح */
@Composable
fun ToggleRow(icon: ImageVector?, color: Color, title: String, sub: String? = null, pref: BoolPref, enabled: Boolean = true) {
    prefRev()
    val on = pref.v
    Row(
        Modifier.fillMaxWidth().heightIn(min = 58.dp).clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { pref.v = !on }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { IconBadge(icon, if (enabled) color else Faint); Spacer(Modifier.width(14.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = ts(15.5f, FontWeight.Medium, if (enabled) White else Faint))
            if (sub != null) Text(sub, style = ts(12.5f, color = Faint))
        }
        Spacer(Modifier.width(8.dp))
        Toggle(on, enabled) { pref.v = it }
    }
}

@Composable
fun NavRow(icon: ImageVector, color: Color, title: String, sub: String? = null, value: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 58.dp).clip(RoundedCornerShape(16.dp)).clickable { onClick() }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, color); Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = ts(15.5f, FontWeight.Medium))
            if (sub != null) Text(sub, style = ts(12.5f, color = Faint))
        }
        if (value != null) Text(value, style = ts(13.5f, FontWeight.Medium, Soft))
        Icon(
            if (isAr) Icons.AutoMirrored.Rounded.KeyboardArrowLeft else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            null, tint = Faint,
        )
    }
}

@Composable
fun SliderRow(title: String, value: Float, range: ClosedFloatingPointRange<Float>, label: String, onChange: (Float) -> Unit) {
    val acc = accentColor()
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = ts(14.5f, FontWeight.Medium), modifier = Modifier.weight(1f))
            Text(label, style = ts(13.5f, FontWeight.Bold, Soft))
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = White, activeTrackColor = acc, inactiveTrackColor = Color(0x33FFFFFF)),
        )
    }
}

/** أزرار اختيار واحد (مثل: بطيئة / عادية / سريعة) */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Color(0x1FFFFFFF)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, s ->
            val on = i == selected
            Box(
                Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(50))
                    .background(if (on) White else Color.Transparent).clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(s, style = ts(13.5f, FontWeight.Bold, if (on) Color(0xFF111114) else Soft), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun Pill(text: String, filled: Boolean = false, onClick: () -> Unit) {
    val acc = accentColor()
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .then(if (filled) Modifier.background(acc) else Modifier.glass(RoundedCornerShape(50)))
            .clickable { onClick() }.padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = ts(14f, FontWeight.Bold)) }
}

/** صف إذن: نقطة حالة + عنوان + شرح + "تم" أو زر */
@Composable
fun PermRow(title: String, desc: String, ok: Boolean, optional: Boolean = false, button: String, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(10.dp).clip(CircleShape).background(
                Color(if (ok) Accents.GREEN else if (optional) 0xFF8E8E93.toInt() else Accents.RED)
            )
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = ts(15.5f, FontWeight.Bold))
            Text(desc, style = ts(12.5f, color = Faint))
        }
        Spacer(Modifier.width(10.dp))
        if (ok) Text(if (isAr) "تم" else "Done", style = ts(15f, FontWeight.Bold, Color(Accents.GREEN)))
        else Pill(button, filled = !optional, onClick = onOpen)
    }
}
