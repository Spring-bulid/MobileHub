package com.mobilehub.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilehub.app.Nav
import com.mobilehub.app.core.GhRun
import com.mobilehub.app.core.GitHubApi
import com.mobilehub.app.ui.EmptyBox
import com.mobilehub.app.ui.GhColors
import com.mobilehub.app.ui.LoadingBox
import com.mobilehub.app.ui.Octicons
import com.mobilehub.app.ui.relativeTime
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 编译进度页：展示仓库的 GitHub Actions 运行列表，
 * 有排队/进行中的运行时每 5 秒自动刷新。
 */
@Composable
fun ActionsScreen(nav: Nav, owner: String, name: String) {
    var runs by remember { mutableStateOf(listOf<GhRun>()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            runs = GitHubApi.workflowRuns(owner, name)
            loaded = true
            // 全部跑完就放慢刷新
            val active = runs.any { it.status == "queued" || it.status == "in_progress" }
            delay(if (active) 5_000 else 20_000)
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "编译进度",
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
            item {
                Text(
                    text = "$owner/$name",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = GhColors.gray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (!loaded) {
                item { LoadingBox() }
            } else if (runs.isEmpty()) {
                item { EmptyBox("还没有运行记录，刚触发的编译可能要几秒才出现") }
            } else {
                items(runs.size) { i -> RunRow(runs[i]) }
            }
        }
    }
}

/** 单条运行记录：状态点 + 标题 + 分支/编号/时间 */
@Composable
private fun RunRow(run: GhRun) {
    val (color, label) = when {
        run.status == "queued" -> GhColors.gray to "排队中"
        run.status == "in_progress" -> GhColors.yellow to "编译中"
        run.conclusion == "success" -> GhColors.open to "成功"
        run.conclusion == "cancelled" -> GhColors.gray to "已取消"
        run.conclusion.isNotBlank() -> GhColors.closed to "失败"
        else -> GhColors.gray to run.status
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        insideMargin = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = run.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${run.workflowName} #${run.runNumber} · ${run.branch} · ${relativeTime(run.createdAt)}",
                    fontSize = 12.sp,
                    color = GhColors.gray,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color)
        }
    }
}
