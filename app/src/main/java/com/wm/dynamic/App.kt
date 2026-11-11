package com.wm.dynamic

import android.app.Application
import android.content.SharedPreferences
import com.wm.dynamic.core.Prefs
import com.wm.dynamic.island.Hub

class App : Application() {
    // أي إعداد يتغير = الجزيرة تعيد الترتيب (تشغيل/إطفاء نوع نشاط)
    private val onPref = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> Hub.refresh() }

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        Prefs.sp.registerOnSharedPreferenceChangeListener(onPref)
    }
}
