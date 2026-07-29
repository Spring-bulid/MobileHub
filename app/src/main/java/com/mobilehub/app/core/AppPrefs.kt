package com.mobilehub.app.core

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * 轻量应用偏好：底部栏样式等 UI 设置。
 * 状态用 Compose State 暴露，改动即时生效并持久化。
 */
object AppPrefs {

    private const val FILE = "app_prefs"
    private const val KEY_LIQUID_BAR = "liquid_bar"

    /** true = Apple 风格悬浮液态玻璃栏，false = 经典导航栏 */
    val liquidBar = mutableStateOf(false)

    fun load(context: Context) {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        liquidBar.value = sp.getBoolean(KEY_LIQUID_BAR, false)
    }

    fun setLiquidBar(context: Context, enabled: Boolean) {
        liquidBar.value = enabled
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_LIQUID_BAR, enabled).apply()
    }
}
