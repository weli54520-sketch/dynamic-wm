package com.wm.dynamic.island

import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.wm.dynamic.core.Accents
import com.wm.dynamic.core.MediaVol
import com.wm.dynamic.core.P
import com.wm.dynamic.core.Saver
import com.wm.dynamic.core.t

/**
 * أحداث الجوال: الشحن، البلوتوث، السماعات، الصامت، الكشاف، عدم الإزعاج، فتح القفل، الصوت.
 * كلها تجي من النظام (بدون فحص متكرر) عشان ما تستهلك بطارية.
 */
class SystemEvents(private val ctx: Context, private val onScreen: (Boolean) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private var started = false
    private val torchOn = HashSet<String>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            // الأحداث الثابتة (sticky) توصل أول ما نسجّل، نتجاهلها
            if (isInitialStickyBroadcast) return
            try { handle(i) } catch (_: Exception) {}
        }
    }

    private val torchCb = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (enabled) torchOn.add(cameraId) else torchOn.remove(cameraId)
            val id = torchOn.firstOrNull()
            if (id != null) Hub.put(torchAct(id, -1)) else Hub.remove("ev_torch")
        }

        override fun onTorchStrengthLevelChanged(cameraId: String, newStrengthLevel: Int) {
            if (cameraId in torchOn) Hub.put(torchAct(cameraId, newStrengthLevel))
        }
    }

    /** نشاط الكشاف: مع شدة الضوء لو الجوال يسمح (أندرويد 13+) وزر إطفاء */
    private fun torchAct(id: String, known: Int): Act {
        val cm = ctx.getSystemService(CameraManager::class.java)
        var max = 1
        var lv = known
        if (Build.VERSION.SDK_INT >= 33) try {
            max = cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
            if (lv < 1) lv = cm.getTorchStrengthLevel(id)
        } catch (_: Exception) {}
        val canDim = Build.VERSION.SDK_INT >= 33 && max > 1
        return Act(
            "ev_torch", Kind.TORCH, Pri.TORCH, title = t("الكشاف", "Flashlight"),
            text = if (canDim && lv > 0) "${lv * 100 / max}%" else t("مشغّل", "On"),
            accent = Accents.YELLOW, level = lv, levelMax = if (canDim) max else 0,
            controls = Controls(
                setLevel = if (canDim) ({ v: Int ->
                    try { if (Build.VERSION.SDK_INT >= 33) cm.turnOnTorchWithStrengthLevel(id, v.coerceIn(1, max)) } catch (_: Exception) {}
                }) else null,
                off = { try { cm.setTorchMode(id, false) } catch (_: Exception) {} },
            ),
        )
    }

    /** نقرأ حالة توفير الطاقة والبطارية أول ما نبدأ */
    private fun readSaver() {
        try { Saver.setPowerSave(ctx.getSystemService(PowerManager::class.java).isPowerSaveMode) } catch (_: Exception) {}
        val lv = battery()
        Saver.setLow(lv in 0..15)
    }

    fun start() {
        if (started) return
        started = true
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(VOLUME_CHANGED)
        }
        try { ContextCompat.registerReceiver(ctx, receiver, f, ContextCompat.RECEIVER_EXPORTED) } catch (_: Exception) {}
        try { ctx.getSystemService(CameraManager::class.java).registerTorchCallback(torchCb, main) } catch (_: Exception) {}
        readSaver()
        try {
            val am = ctx.getSystemService(AudioManager::class.java)
            MediaVol.set(am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1))
        } catch (_: Exception) {}
    }

    fun stop() {
        if (!started) return
        started = false
        try { ctx.unregisterReceiver(receiver) } catch (_: Exception) {}
        try { ctx.getSystemService(CameraManager::class.java).unregisterTorchCallback(torchCb) } catch (_: Exception) {}
        Hub.remove("ev_torch")
    }

    private fun battery(): Int = try {
        ctx.getSystemService(BatteryManager::class.java).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    } catch (_: Exception) { -1 }

    private fun handle(i: Intent) {
        when (i.action) {
            Intent.ACTION_SCREEN_ON -> onScreen(true)
            Intent.ACTION_SCREEN_OFF -> onScreen(false)
            Intent.ACTION_USER_PRESENT -> {
                val greet = P.greetText.v.trim()
                if (P.greetOn.v && greet.isNotEmpty()) {
                    Hub.flash(Act("ev_greet", Kind.GREETING, Pri.EVENT, title = greet, accent = Accents.current), 2600)
                } else {
                    Hub.flash(Act("ev_unlock", Kind.UNLOCK, Pri.EVENT, title = t("تم فتح القفل", "Unlocked")), 1200)
                }
            }
            Intent.ACTION_POWER_CONNECTED -> main.postDelayed({
                val lv = battery()
                Hub.remove("ev_unplug")
                Hub.flash(Act("ev_charge", Kind.CHARGE, Pri.EVENT, title = t("جاري الشحن", "Charging"),
                    text = if (lv >= 0) "$lv%" else "", level = lv, accent = Accents.GREEN), 3500)
            }, 400)
            Intent.ACTION_POWER_DISCONNECTED -> {
                val lv = battery()
                Hub.remove("ev_charge")
                Hub.flash(Act("ev_unplug", Kind.UNPLUG, Pri.EVENT, title = t("تم فصل الشاحن", "Unplugged"),
                    text = if (lv >= 0) "$lv%" else "", level = lv, accent = 0xFFFFFFFF.toInt()), 2200)
            }
            PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> readSaver()
            Intent.ACTION_BATTERY_OKAY -> Saver.setLow(false)
            Intent.ACTION_BATTERY_LOW -> {
                Saver.setLow(true)
                val lv = battery()
                Hub.flash(Act("ev_low", Kind.BATTERY_LOW, Pri.EVENT, title = t("البطارية ضعيفة", "Low battery"),
                    text = if (lv >= 0) "$lv%" else "", level = lv, accent = Accents.RED), 4500)
            }
            BluetoothDevice.ACTION_ACL_CONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val on = i.action == BluetoothDevice.ACTION_ACL_CONNECTED
                val dev: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33)
                    i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                else @Suppress("DEPRECATION") i.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val name = try { dev?.name } catch (_: SecurityException) { null } ?: t("جهاز بلوتوث", "Bluetooth device")
                Hub.flash(Act("ev_bt", Kind.BT, Pri.EVENT, title = name,
                    text = if (on) t("متصل", "Connected") else t("انفصل", "Disconnected"),
                    accent = if (on) Accents.BLUE else 0xFF8E8E93.toInt()), 3000)
            }
            AudioManager.ACTION_HEADSET_PLUG -> {
                val on = i.getIntExtra("state", 0) == 1
                Hub.flash(Act("ev_headset", Kind.HEADSET, Pri.EVENT, title = t("السماعة", "Headphones"),
                    text = if (on) t("متصلة", "Connected") else t("انفصلت", "Disconnected"),
                    accent = Accents.BLUE), 2200)
            }
            AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                val mode = i.getIntExtra(AudioManager.EXTRA_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL)
                val (txt, col) = when (mode) {
                    AudioManager.RINGER_MODE_SILENT -> t("صامت", "Silent") to Accents.RED
                    AudioManager.RINGER_MODE_VIBRATE -> t("اهتزاز", "Vibrate") to Accents.ORANGE
                    else -> t("رنين", "Ring") to Accents.GREEN
                }
                Hub.flash(Act("ev_ringer", Kind.RINGER, Pri.EVENT, title = t("وضع الصوت", "Sound mode"),
                    text = txt, level = mode, accent = col), 2000)
            }
            NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                val nm = ctx.getSystemService(NotificationManager::class.java)
                val on = nm.currentInterruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL
                Hub.flash(Act("ev_dnd", Kind.DND, Pri.EVENT, title = t("عدم الإزعاج", "Do not disturb"),
                    text = if (on) t("مشغّل", "On") else t("مطفي", "Off"),
                    level = if (on) 1 else 0, accent = Accents.PURPLE), 2200)
            }
            VOLUME_CHANGED -> {
                val stream = i.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                if (stream != AudioManager.STREAM_MUSIC && stream != AudioManager.STREAM_RING) return
                val am = ctx.getSystemService(AudioManager::class.java)
                val v = i.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", am.getStreamVolume(stream))
                val max = am.getStreamMaxVolume(stream).coerceAtLeast(1)
                if (stream == AudioManager.STREAM_MUSIC) MediaVol.set(v.toFloat() / max)
                Hub.flash(Act("ev_volume", Kind.VOLUME, Pri.EVENT + 1,
                    title = if (stream == AudioManager.STREAM_MUSIC) t("الوسائط", "Media") else t("الرنين", "Ringer"),
                    level = v * 100 / max, accent = Accents.current), 1600)
            }
        }
    }

    companion object {
        const val VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
    }
}
