package com.wm.dynamic.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.wm.dynamic.core.P
import com.wm.dynamic.core.t
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class AppItem(val pkg: String, val label: String, val icon: ImageBitmap?)

private fun loadApps(ctx: Context): List<AppItem> {
    val pm = ctx.packageManager
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(i, 0)
        .distinctBy { it.activityInfo.packageName }
        .filter { it.activityInfo.packageName != ctx.packageName }
        .map { ri ->
            val icon = try { ri.loadIcon(pm).toBitmap(72, 72).asImageBitmap() } catch (_: Exception) { null }
            AppItem(ri.activityInfo.packageName, ri.loadLabel(pm).toString(), icon)
        }
        .sortedBy { it.label.lowercase() }
}

/** اختيار التطبيقات اللي تطلع في الجزيرة */
@Composable
fun AppsPage(onBack: () -> Unit) {
    val ctx = LocalContext.current
    prefRev()
    var apps by remember { mutableStateOf<List<AppItem>?>(null) }
    var q by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { loadApps(ctx) } }
    val blocked = P.blockedApps.v
    val acc = accentColor()

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = White)
            }
            Spacer(Modifier.width(8.dp))
            Text(t("اختيار التطبيقات", "Choose apps"), style = ts(24f, FontWeight.Bold))
        }
        Text(
            t("المطفي ما تطلع إشعاراته ولا نشاطاته في الجزيرة.", "Apps turned off won't show in the island."),
            style = ts(13f, color = Soft), modifier = Modifier.padding(start = 8.dp, bottom = 10.dp),
        )
        OutlinedTextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Faint) },
            placeholder = { Text(t("بحث", "Search"), style = ts(14f, color = Faint)) },
            textStyle = ts(15f), shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = acc,
                focusedBorderColor = acc, unfocusedBorderColor = Faint,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        val list = apps
        if (list == null) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = acc)
            }
        } else {
            val shown = if (q.isBlank()) list else list.filter { it.label.contains(q.trim(), ignoreCase = true) }
            LazyColumn(Modifier.fillMaxWidth().weight(1f).glass().padding(horizontal = 14.dp)) {
                items(shown, key = { it.pkg }) { app ->
                    val on = app.pkg !in blocked
                    val toggle: (Boolean) -> Unit = { v -> P.blockedApps.v = if (v) blocked - app.pkg else blocked + app.pkg }
                    Row(
                        Modifier.fillMaxWidth().clickable { toggle(!on) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val ic = app.icon
                        if (ic != null) Image(ic, null, Modifier.size(40.dp))
                        else Box(Modifier.size(40.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(app.label, style = ts(15f, FontWeight.Medium), modifier = Modifier.weight(1f), maxLines = 1)
                        Toggle(on) { toggle(it) }
                    }
                }
            }
        }
    }
}
