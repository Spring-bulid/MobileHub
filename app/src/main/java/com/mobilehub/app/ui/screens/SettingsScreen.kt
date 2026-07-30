package com.mobilehub.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilehub.app.Nav
import com.mobilehub.app.core.AppPrefs
import com.mobilehub.app.ui.GhColors
import com.mobilehub.app.ui.Octicons
import com.mobilehub.app.ui.SegmentTabs
import com.mobilehub.app.ui.toast
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 设置页：外观偏好，子页面化组织。
 * 第一层为主设置列表，点击条目进入子页面。
 */
@Composable
fun SettingsScreen(nav: Nav) {
    // 用 Int 表示当前子页面：0=主列表, 1=外观, 2=底部栏
    var subPage by remember { mutableIntStateOf(0) }

    // 进入子页从右侧滑入，返回主列表向右滑出
    AnimatedContent(
        targetState = subPage,
        transitionSpec = {
            val dir = if (targetState > initialState) 1 else -1
            (slideInHorizontally(tween(300)) { it / 4 * dir } + fadeIn(tween(300))) togetherWith
                (slideOutHorizontally(tween(300)) { -it / 4 * dir } + fadeOut(tween(180)))
        },
        label = "settingsSubPage",
    ) { page ->
        when (page) {
            0 -> SettingsMain(nav, onNavigate = { subPage = it })
            1 -> AppearancePage { subPage = 0 }
            2 -> BottomBarPage { subPage = 0 }
        }
    }
}

// ---------------------------------------------------------------------------
// 主设置列表
// ---------------------------------------------------------------------------

@Composable
private fun SettingsMain(nav: Nav, onNavigate: (Int) -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "设置",
                navigationIcon = {
                    IconButton(onClick = { nav.pop() }) {
                        Icon(
                            imageVector = Octicons.Back,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            item { SmallTitle(text = "偏好设置") }

            // 外观
            item {
                SettingsRow(
                    icon = Octicons.Sun,
                    title = "外观",
                    subtitle = when (AppPrefs.themeMode.intValue) {
                        1 -> "浅色模式"
                        2 -> "深色模式"
                        else -> "跟随系统"
                    },
                    onClick = { onNavigate(1) },
                )
            }

            // 底部栏样式
            item {
                SettingsRow(
                    icon = Octicons.Layout,
                    title = "底部栏样式",
                    subtitle = if (AppPrefs.liquidBar.value) "液态玻璃悬浮" else "经典导航栏",
                    onClick = { onNavigate(2) },
                )
            }

            item { SmallTitle(text = "关于") }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                    insideMargin = PaddingValues(14.dp),
                ) {
                    Text(text = "MobileHub v1.0.0", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "用一枚 Token 掌控整个 GitHub。\n开源协议：MIT",
                        fontSize = 12.sp,
                        color = GhColors.gray,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 外观子页面
// ---------------------------------------------------------------------------

@Composable
private fun AppearancePage(onBack: () -> Unit) {
    val context = LocalContext.current
    val themeLabels = listOf("跟随系统", "浅色模式", "深色模式")
    val themeIcons = listOf(Octicons.Sync, Octicons.Sun, Octicons.Moon)

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "外观",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Octicons.Back,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            item { SmallTitle(text = "主题模式") }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                    insideMargin = PaddingValues(4.dp),
                ) {
                    themeLabels.forEachIndexed { index, label ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AppPrefs.setThemeMode(context, index)
                                    toast(context, "已切换至「$label」")
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                        ) {
                            Icon(
                                imageVector = themeIcons[index],
                                contentDescription = null,
                                tint = if (AppPrefs.themeMode.intValue == index)
                                    MiuixTheme.colorScheme.primary else GhColors.gray,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = label,
                                fontSize = 15.sp,
                                fontWeight = if (AppPrefs.themeMode.intValue == index)
                                    FontWeight.SemiBold else FontWeight.Normal,
                                color = if (AppPrefs.themeMode.intValue == index)
                                    MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (AppPrefs.themeMode.intValue == index) {
                                Icon(
                                    imageVector = Octicons.Check,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    insideMargin = PaddingValues(14.dp),
                ) {
                    Text(
                        text = "主题更改即时生效，下次启动时自动恢复。",
                        fontSize = 12.sp,
                        color = GhColors.gray,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 底部栏样式子页面
// ---------------------------------------------------------------------------

@Composable
private fun BottomBarPage(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "底部栏样式",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Octicons.Back,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            item { SmallTitle(text = "底部栏样式") }
            item {
                SegmentTabs(
                    listOf("经典导航栏", "液态玻璃悬浮"),
                    if (AppPrefs.liquidBar.value) 1 else 0,
                ) { AppPrefs.setLiquidBar(context, it == 1) }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    insideMargin = PaddingValues(14.dp),
                ) {
                    Text(
                        text = "液态玻璃悬浮栏基于 Kyant0/AndroidLiquidGlass。",
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Android 13+ 有完整折射效果，Android 12 仅模糊，更低版本退化为半透明悬浮栏。拇指块支持左右拖拽换页。",
                        fontSize = 12.sp,
                        color = GhColors.gray,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 通用设置行
// ---------------------------------------------------------------------------

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        insideMargin = PaddingValues(0.dp),
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GhColors.gray,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(text = subtitle, fontSize = 12.sp, color = GhColors.gray)
            }
            Icon(
                imageVector = Octicons.ChevronRight,
                contentDescription = null,
                tint = GhColors.gray,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
