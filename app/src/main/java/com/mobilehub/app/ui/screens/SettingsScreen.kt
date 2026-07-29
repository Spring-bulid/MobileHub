package com.mobilehub.app.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilehub.app.Nav
import com.mobilehub.app.core.AppPrefs
import com.mobilehub.app.ui.GhColors
import com.mobilehub.app.ui.Octicons
import com.mobilehub.app.ui.SegmentTabs
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 设置页：外观等应用偏好集中在这里改动。
 */
@Composable
fun SettingsScreen(nav: Nav) {
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
