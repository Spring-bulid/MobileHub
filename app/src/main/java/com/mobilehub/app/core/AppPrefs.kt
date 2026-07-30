package com.mobilehub.app.core

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

/**
 * 轻量应用偏好：底部栏样式、主题模式等 UI 设置。
 * 状态用 Compose State 暴露，改动即时生效并持久化。
 */

/** 主题模式：0=跟随系统, 1=浅色, 2=深色 */
enum class ThemeMode(val value: Int) {
    SYSTEM(0), LIGHT(1), DARK(2);

    companion object {
        fun from(value: Int) = entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}

object AppPrefs {

    private const val FILE = "app_prefs"
    private const val KEY_LIQUID_BAR = "liquid_bar"
    private const val KEY_THEME_MODE = "theme_mode"

    /** true = Apple 风格悬浮液态玻璃栏，false = 经典导航栏 */
    val liquidBar = mutableStateOf(false)

    /** 主题模式：0=跟随系统, 1=浅色, 2=深色 */
    val themeMode = mutableIntStateOf(0)

    fun load(context: Context) {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        liquidBar.value = sp.getBoolean(KEY_LIQUID_BAR, false)
        themeMode.intValue = sp.getInt(KEY_THEME_MODE, 0)
    }

    fun setLiquidBar(context: Context, enabled: Boolean) {
        liquidBar.value = enabled
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_LIQUID_BAR, enabled).apply()
    }

    fun setThemeMode(context: Context, mode: Int) {
        themeMode.intValue = mode
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_THEME_MODE, mode).apply()
    }
}
