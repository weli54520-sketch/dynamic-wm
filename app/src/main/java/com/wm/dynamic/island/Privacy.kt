package com.wm.dynamic.island

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import com.wm.dynamic.core.Accents
import com.wm.dynamic.core.t

/**
 * مين يستخدم الكاميرا والمايك (بدون أي إذن، وما نعرف وش التطبيق):
 * - الكاميرا: أندرويد يخبرنا لما كاميرا تصير "مشغولة".
 * - المايك: قائمة التسجيلات الشغالة. لو مصدرها "التعرف على الصوت" = غالباً المساعد الصوتي.
 */
class Privacy(private val ctx: Context) {
    private val main = Handler(Looper.getMainLooper())
    private val busyCams = HashSet<String>()
    private var mic = false
    private var assist = false
    private var started = false

    private val camCb = object : CameraManager.AvailabilityCallback() {
        override fun onCameraUnavailable(cameraId: String) { busyCams.add(cameraId); update() }
        override fun onCameraAvailable(cameraId: String) { busyCams.remove(cameraId); update() }
    }

    private val micCb = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) = read(configs.orEmpty())
    }

    private fun read(configs: List<AudioRecordingConfiguration>) {
        val live = configs.filter { !it.isClientSilenced }
        assist = live.any { it.clientAudioSource == MediaRecorder.AudioSource.VOICE_RECOGNITION }
        mic = live.isNotEmpty()
        update()
    }

    fun start() {
        if (started) return
        started = true
        try { ctx.getSystemService(CameraManager::class.java).registerAvailabilityCallback(camCb, main) } catch (_: Exception) {}
        try {
            val am = ctx.getSystemService(AudioManager::class.java)
            am.registerAudioRecordingCallback(micCb, main)
            read(am.activeRecordingConfigurations)
        } catch (_: Exception) {}
    }

    fun stop() {
        if (!started) return
        started = false
        try { ctx.getSystemService(CameraManager::class.java).unregisterAvailabilityCallback(camCb) } catch (_: Exception) {}
        try { ctx.getSystemService(AudioManager::class.java).unregisterAudioRecordingCallback(micCb) } catch (_: Exception) {}
        busyCams.clear(); mic = false; assist = false
        Hub.remove(KEY_ASSIST); Hub.remove(KEY_PRIV)
    }

    private fun update() {
        if (assist) {
            Hub.put(Act(KEY_ASSIST, Kind.ASSIST, Pri.ASSIST, title = t("المساعد يسمعك", "Assistant is listening"),
                text = t("تكلم الحين", "Go ahead"), accent = Accents.PURPLE))
        } else Hub.remove(KEY_ASSIST)

        // بعض الجوالات تحسب الكشاف "كاميرا مشغولة"
        val cam = busyCams.isNotEmpty() && Hub.get("ev_torch") == null
        // المكالمة نفسها تستخدم المايك: ما نكرر
        val inCall = Hub.list.value.any { it.kind == Kind.CALL_ON || it.kind == Kind.CALL_IN }
        val micOnly = mic && !assist && !inCall
        if (cam || micOnly) {
            val title = when {
                cam && micOnly -> t("الكاميرا والمايك", "Camera & mic")
                cam -> t("الكاميرا", "Camera")
                else -> t("المايك", "Microphone")
            }
            Hub.put(Act(KEY_PRIV, Kind.PRIVACY, Pri.PRIVACY, title = title,
                text = t("تطبيق يستخدمها الحين", "An app is using it now"),
                level = (if (cam) 1 else 0) or (if (micOnly) 2 else 0),
                accent = if (cam) Accents.GREEN else Accents.ORANGE))
        } else Hub.remove(KEY_PRIV)
    }

    companion object {
        const val KEY_ASSIST = "assist"
        const val KEY_PRIV = "privacy"
    }
}
