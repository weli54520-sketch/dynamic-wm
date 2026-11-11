package com.wm.dynamic.service

import android.app.KeyguardManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Person
import android.content.ComponentName
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.wm.dynamic.core.P
import com.wm.dynamic.core.Prefs
import com.wm.dynamic.core.t
import com.wm.dynamic.island.Act
import com.wm.dynamic.island.Controls
import com.wm.dynamic.island.Hub
import com.wm.dynamic.island.Kind
import com.wm.dynamic.island.Pri
import java.util.concurrent.Executors

/**
 * يقرأ الإشعارات (الموسيقى، المكالمات، المؤقتات، التحميل، الخرائط، الرسائل)
 * ويحولها لأنشطة في الجزيرة. ما يرسل أي شي برا الجوال.
 */
class NotifListener : NotificationListenerService() {
    private val main = Handler(Looper.getMainLooper())
    private val bg = Executors.newSingleThreadExecutor()

    private var msm: MediaSessionManager? = null
    private val controllers = HashMap<MediaSession.Token, Pair<MediaController, MediaController.Callback>>()
    /** لكل تطبيق موسيقى: متى توقف، وغلافه المصغر ولونه */
    private class ArtCache(var id: Int = -1, var bmp: Bitmap? = null, var color: Int = 0)
    private val pausedSince = HashMap<String, Long>()
    private val arts = HashMap<String, ArtCache>()
    /** مفاتيح الموسيقى المعروضة الحين (media:<الحزمة>) */
    private val mediaKeys = HashSet<String>()

    /** آخر محتوى انعرض لكل إشعار (بدون الأرقام) عشان التحديثات ما تفتح الجزيرة كل ثانية */
    private val shown = HashMap<String, String>()

    private val mediaTick = Runnable { updateMedia() }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list -> sessionsChanged(list) }

    override fun onListenerConnected() {
        instance = this
        Prefs.init(this)
        setupMedia()
        try {
            activeNotifications?.forEach { handle(it, initial = true) }
        } catch (_: Exception) {}
    }

    override fun onListenerDisconnected() {
        instance = null
        teardownMedia()
        Hub.removeIf { it.key.startsWith("n:") || it.key.startsWith("nb:") || it.key.startsWith("media") }
        try { requestRebind(ComponentName(this, NotifListener::class.java)) } catch (_: Exception) {}
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try { handle(sbn, initial = false) } catch (_: Exception) {}
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        try {
            Hub.remove("n:" + sbn.key)
            Hub.remove("nb:" + sbn.key)
            shown.remove(sbn.key)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        teardownMedia()
        bg.shutdown()
        if (instance === this) instance = null
        super.onDestroy()
    }

    // ───────────── الإشعارات ─────────────

    private fun importance(sbn: StatusBarNotification): Int = try {
        val r = Ranking()
        if (currentRanking.getRanking(sbn.key, r)) r.importance else 3
    } catch (_: Exception) { 3 }

    private fun pendingExtra(ex: Bundle, key: String): PendingIntent? =
        if (Build.VERSION.SDK_INT >= 33) ex.getParcelable(key, PendingIntent::class.java)
        else @Suppress("DEPRECATION") ex.getParcelable<PendingIntent>(key)

    private fun actionLike(n: Notification, words: List<String>): PendingIntent? =
        n.actions?.firstOrNull { a -> val tt = a.title?.toString() ?: ""; words.any { tt.contains(it, ignoreCase = true) } }?.actionIntent

    private fun largeIcon(n: Notification): Bitmap? = try {
        n.getLargeIcon()?.loadDrawable(this)?.toBitmap(128, 128)
    } catch (_: Exception) { null }

    private fun handle(sbn: StatusBarNotification, initial: Boolean) {
        val pkg = sbn.packageName
        if (pkg == packageName) return
        val n = sbn.notification ?: return
        val ex = n.extras ?: Bundle()
        val template = ex.getString(Notification.EXTRA_TEMPLATE) ?: ""
        // الموسيقى نقرأها من جلسة الوسائط مباشرة
        if (template.contains("MediaStyle") || ex.containsKey(Notification.EXTRA_MEDIA_SESSION)) return

        val key = "n:" + sbn.key
        val title = (ex.getCharSequence(Notification.EXTRA_TITLE) ?: ex.getCharSequence(Notification.EXTRA_TITLE_BIG))
            ?.toString()?.trim().orEmpty()
        val text = (ex.getCharSequence(Notification.EXTRA_TEXT) ?: ex.getCharSequence(Notification.EXTRA_BIG_TEXT))
            ?.toString()?.trim().orEmpty()
        val app = Apps.label(this, pkg)
        val flags = n.flags
        val ongoing = flags and Notification.FLAG_ONGOING_EVENT != 0 || flags and Notification.FLAG_FOREGROUND_SERVICE != 0
        val blocked = pkg in P.blockedApps.v
        val color = if (n.color != Notification.COLOR_DEFAULT && n.color != 0) n.color or 0xFF000000.toInt() else 0
        val open: () -> Unit = { if (n.contentIntent?.fire() != true) openApp(this, pkg) }
        val chrono = ex.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)
        val countDown = ex.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN)

        // 1) المكالمات
        val isCall = n.category == Notification.CATEGORY_CALL || template.endsWith("CallStyle")
        if (isCall) {
            val answer = pendingExtra(ex, "android.answerIntent") ?: actionLike(n, ANSWER)
            val decline = pendingExtra(ex, "android.declineIntent") ?: actionLike(n, DECLINE)
            val hangUp = pendingExtra(ex, "android.hangUpIntent") ?: actionLike(n, HANGUP)
            val incoming = answer != null && !chrono
            val person: Person? = if (Build.VERSION.SDK_INT >= 33) ex.getParcelable("android.callPerson", Person::class.java)
            else @Suppress("DEPRECATION") ex.getParcelable<Person>("android.callPerson")
            val name = person?.name?.toString()?.takeIf { it.isNotBlank() } ?: title
            val ctl = Controls(
                answer = { Calls.answer(this, answer) },
                decline = { Calls.end(this, decline ?: hangUp) },
                hangUp = { Calls.end(this, hangUp ?: decline) },
                open = open,
            )
            Hub.remove("nb:" + sbn.key)
            Hub.put(Act(key, if (incoming) Kind.CALL_IN else Kind.CALL_ON, if (incoming) Pri.CALL_IN else Pri.CALL_ON,
                title = name, text = text, app = app, pkg = pkg, icon = largeIcon(n),
                chrono = if (!incoming && chrono) n.`when` else 0L, controls = ctl, autoExpand = incoming))
            return
        }

        // 2) الخرائط
        if (n.category == "navigation" && ongoing) {
            Hub.put(Act(key, Kind.NAV, Pri.NAV, title = title, text = text, app = app, pkg = pkg,
                icon = Apps.icon(this, pkg), accent = color, controls = Controls(open = open)))
            return
        }

        // 3) المؤقت والساعة
        val isClock = pkg in CLOCKS
        if ((chrono && ongoing) || (isClock && ongoing)) {
            Hub.put(Act(key, Kind.TIMER, Pri.TIMER, title = title.ifEmpty { app }, text = text, app = app, pkg = pkg,
                chrono = if (chrono) n.`when` else 0L, countDown = countDown, controls = Controls(open = open)))
            return
        }

        // 4) التحميل والتقدم
        val max = ex.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val prog = ex.getInt(Notification.EXTRA_PROGRESS, 0)
        val indet = ex.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)
        if ((max > 0 && prog < max) || (indet && ongoing)) {
            if (!blocked) {
                Hub.put(Act(key, Kind.PROGRESS, Pri.PROGRESS, title = title.ifEmpty { app }, text = text, app = app, pkg = pkg,
                    icon = Apps.icon(this, pkg), accent = color, indeterminate = indet || max <= 0,
                    progress = if (max > 0) prog.toFloat() / max else -1f, controls = Controls(open = open)))
            }
            return
        }
        Hub.remove(key)

        // 5) التطبيقات اللي تشتغل بالخلفية
        if (ongoing) {
            if (!blocked && pkg !in SYSTEM && importance(sbn) > 1 && (title.isNotEmpty() || text.isNotEmpty())) {
                Hub.put(Act(key, Kind.ONGOING, Pri.ONGOING, title = title.ifEmpty { app }, text = text, app = app, pkg = pkg,
                    icon = Apps.icon(this, pkg), accent = color, controls = Controls(open = open)))
            }
            return
        }

        // 6) الإشعارات العادية (تطلع شوي وتختفي)
        if (initial || blocked || !P.notif.v) return
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (title.isEmpty() && text.isEmpty()) return
        if (importance(sbn) < 3) return // الإشعارات الصامتة
        if (P.notifSkipOpen.v && pkg == Hub.foregroundPkg) return
        // نفس الإشعار تحدّث (مثل عداد التسجيل أو نسبة): ما نفتح الجزيرة مرة ثانية
        val sig = (title + "\u0001" + text).replace(DIGITS, "")
        val prev = shown[sbn.key]
        if (prev != null && (prev == sig || flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)) return
        if (chrono) return // فيه عداد يمشي = مو رسالة
        shown[sbn.key] = sig

        val locked = try { getSystemService(KeyguardManager::class.java).isKeyguardLocked } catch (_: Exception) { false }
        val hide = locked && P.notifHideLocked.v
        Hub.flash(
            Act("nb:" + sbn.key, Kind.NOTIF, Pri.NOTIF,
                title = if (hide) app else title, text = if (hide) t("إشعار جديد", "New notification") else text,
                app = if (hide) "" else app, pkg = pkg,
                icon = (if (hide) null else largeIcon(n)) ?: Apps.icon(this, pkg),
                accent = color, controls = Controls(open = open)),
            P.notifSec.v.coerceIn(2, 15) * 1000L,
        )
    }

    // ───────────── الموسيقى ─────────────

    private fun setupMedia() {
        try {
            val m = getSystemService(MediaSessionManager::class.java)
            msm = m
            val comp = ComponentName(this, NotifListener::class.java)
            m.addOnActiveSessionsChangedListener(sessionsListener, comp, main)
            sessionsChanged(m.getActiveSessions(comp))
        } catch (_: Exception) {}
    }

    private fun teardownMedia() {
        try { msm?.removeOnActiveSessionsChangedListener(sessionsListener) } catch (_: Exception) {}
        controllers.values.forEach { (c, cb) -> try { c.unregisterCallback(cb) } catch (_: Exception) {} }
        controllers.clear()
        main.removeCallbacks(mediaTick)
    }

    private fun sessionsChanged(list: List<MediaController>?) {
        try { sessionsChangedInner(list) } catch (_: Exception) {}
    }

    private fun sessionsChangedInner(list: List<MediaController>?) {
        val now = list.orEmpty()
        val tokens = now.map { it.sessionToken }.toSet()
        controllers.keys.filter { it !in tokens }.forEach { tk ->
            controllers.remove(tk)?.let { (c, cb) -> try { c.unregisterCallback(cb) } catch (_: Exception) {} }
        }
        now.forEach { c ->
            if (c.sessionToken !in controllers) {
                val cb = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) = updateMedia()
                    override fun onMetadataChanged(metadata: MediaMetadata?) = updateMedia()
                    override fun onSessionDestroyed() {
                        controllers.remove(c.sessionToken)?.let { (cc, cb2) -> try { cc.unregisterCallback(cb2) } catch (_: Exception) {} }
                        updateMedia()
                    }
                }
                try { c.registerCallback(cb, main); controllers[c.sessionToken] = c to cb } catch (_: Exception) {}
            }
        }
        updateMedia()
    }

    private fun isPaused(c: MediaController): Boolean = c.playbackState?.state == PlaybackState.STATE_PAUSED

    private fun isPlaying(c: MediaController): Boolean {
        val s = c.playbackState?.state
        return s == PlaybackState.STATE_PLAYING || s == PlaybackState.STATE_BUFFERING
    }

    private fun updateMedia() {
        try { updateMediaInner() } catch (_: Exception) {}
    }

    private fun updateMediaInner() {
        main.removeCallbacks(mediaTick)
        val list = controllers.values.map { it.first }.filter { it.metadata != null && (isPlaying(it) || isPaused(it)) }
        // تطبيق واحد ممكن يكون عنده أكثر من جلسة: ناخذ الشغالة أول
        val byPkg = list.groupBy { it.packageName }.mapValues { (_, cs) -> cs.firstOrNull { isPlaying(it) } ?: cs.first() }
        val nowT = SystemClock.elapsedRealtime()
        val keep = P.mediaKeep.v.coerceIn(0, 300) * 1000L
        var nextCheck = Long.MAX_VALUE
        val alive = HashSet<String>()
        byPkg.forEach { (pkg, c) ->
            val key = "media:$pkg"
            val playing = isPlaying(c)
            // الإيقاف المؤقت: تبقى شوي وبعدها تختفي
            if (playing) pausedSince.remove(pkg) else {
                val since = pausedSince.getOrPut(pkg) { nowT }
                val left = keep - (nowT - since)
                if (left <= 0) return@forEach
                nextCheck = minOf(nextCheck, left + 300)
            }
            val md = c.metadata ?: return@forEach
            alive += key
            Hub.put(mediaAct(key, pkg, c, md, playing))
        }
        pausedSince.keys.retainAll(byPkg.keys)
        arts.keys.retainAll(byPkg.keys)
        (mediaKeys - alive).forEach { Hub.remove(it) }
        mediaKeys.clear(); mediaKeys += alive
        if (nextCheck != Long.MAX_VALUE) main.postDelayed(mediaTick, nextCheck)
    }

    private fun mediaAct(key: String, pkg: String, c: MediaController, md: MediaMetadata, playing: Boolean): Act {
        val title = md.getString(MediaMetadata.METADATA_KEY_TITLE) ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: ""
        val artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: ""
        val art = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val cache = prepareArt(pkg, art)

        val st = c.playbackState
        val tc = c.transportControls
        val acts = st?.actions ?: 0L
        val canSeek = acts and PlaybackState.ACTION_SEEK_TO != 0L
        val ctl = Controls(
            playPause = { if (isPlaying(c)) tc.pause() else tc.play() },
            next = { tc.skipToNext() },
            prev = { tc.skipToPrevious() },
            seek = if (canSeek) ({ p: Long -> tc.seekTo(p) }) else null,
            open = { if (c.sessionActivity?.fire() != true) openApp(this, pkg) },
        )
        return Act(
            key, Kind.MEDIA, if (playing) Pri.MEDIA else Pri.MEDIA_PAUSED,
            title = title, text = artist, app = Apps.label(this, pkg), pkg = pkg,
            icon = cache.bmp ?: Apps.icon(this, pkg), accent = if (cache.bmp != null) cache.color else 0,
            playing = playing, position = st?.position ?: 0L,
            duration = md.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0L),
            posAt = st?.lastPositionUpdateTime ?: 0L,
            speed = (st?.playbackSpeed ?: 1f).let { if (it <= 0f) 1f else it },
            controls = ctl,
        )
    }

    /** نصغّر صورة الغلاف ونطلع لونها (مرة وحدة لكل صورة، ولكل تطبيق لحاله) */
    private fun prepareArt(pkg: String, art: Bitmap?): ArtCache {
        val cache = arts.getOrPut(pkg) { ArtCache() }
        if (art == null) { cache.id = -1; cache.bmp = null; cache.color = 0; return cache }
        val id = System.identityHashCode(art) * 31 + art.generationId
        if (id == cache.id) return cache
        cache.id = id
        cache.bmp = try {
            val w = art.width.coerceAtLeast(1); val h = art.height.coerceAtLeast(1)
            val s = 160f / maxOf(w, h)
            if (s < 1f) Bitmap.createScaledBitmap(art, (w * s).toInt().coerceAtLeast(1), (h * s).toInt().coerceAtLeast(1), true)
            else art.copy(art.config ?: Bitmap.Config.ARGB_8888, false)
        } catch (_: Exception) { null }
        cache.color = 0
        val src = cache.bmp ?: return cache
        bg.execute {
            val col = try {
                val p = Palette.from(src).maximumColorCount(16).generate()
                p.getVibrantColor(p.getLightVibrantColor(p.getDominantColor(0)))
            } catch (_: Exception) { 0 }
            main.post { if (cache.id == id) { cache.color = col; updateMedia() } }
        }
        return cache
    }

    companion object {
        @Volatile var instance: NotifListener? = null
        private val ANSWER = listOf("answer", "accept", "رد", "إجابة", "اجابة", "قبول")
        private val DECLINE = listOf("decline", "reject", "رفض")
        private val HANGUP = listOf("hang up", "end call", "إنهاء", "انهاء")
        private val CLOCKS = setOf(
            "com.sec.android.app.clockpackage", "com.google.android.deskclock", "com.android.deskclock",
        )
        private val DIGITS = Regex("[0-9\u0660-\u0669:.,٫%]")
        private val SYSTEM = setOf("android", "com.android.systemui", "com.android.providers.downloads.ui")
    }
}
