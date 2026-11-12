package com.wm.dynamic.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** مستوى صوت الوسائط (0 - 1): الموجة تكبر وتصغر معه */
object MediaVol {
    private val _level = MutableStateFlow(0.6f)
    val level: StateFlow<Float> = _level

    fun set(v: Float) { _level.value = v.coerceIn(0f, 1f) }
}
