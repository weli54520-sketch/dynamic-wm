package com.wm.dynamic.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * حالة توفير البطارية في الجوال.
 * [on] = توفير الطاقة مشغّل أو البطارية ضعيفة. الجزيرة تخفف حركاتها وقتها (لو [P.autoSaver] مشغّل).
 */
object Saver {
    private val _on = MutableStateFlow(false)
    val on: StateFlow<Boolean> = _on

    @Volatile private var powerSave = false
    @Volatile private var lowBattery = false

    fun setPowerSave(v: Boolean) { powerSave = v; publish() }
    fun setLow(v: Boolean) { lowBattery = v; publish() }

    private fun publish() { _on.value = powerSave || lowBattery }
}
