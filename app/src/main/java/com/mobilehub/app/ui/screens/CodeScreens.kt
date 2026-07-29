package com.mobilehub.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilehub.app.Nav
import com.mobilehub.app.Screen
import com.mobilehub.app.core.GhContent
import com.mobilehub.app.core.GitHubApi
import com.mobilehub.app.ui.EmptyBox
import com.mobilehub.app.ui.GhColors
import com.mobilehub.app.ui.LoadingBox
import com.mobilehub.app.ui.Octicons
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 代码目录浏览：GitHubApi.contents，目录在前文件在后，点击进入子目录或文件查看器。
 */
@Composable
fun CodeBrowserScreen(nav: Nav, owner: String, repo: String, path: String, ref: String) {
    var entries by remember { mutableStateOf(listOf<GhContent>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        entries = GitHubApi.contents(owner, repo, path, ref)
        loading = false
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = if (path.isBlank()) repo else path.substringAfterLast('/'),
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
                    text = "$owner/$repo" + if (path.isBlank()) "" else "/$path",
                    fontSize = 12.sp,
                    color = GhColors.gray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            if (loading) {
                item { LoadingBox() }
            } else if (entries.isEmpty()) {
                item { EmptyBox("空目录或加载失败") }
            } else {
                items(entries.size) { i ->
                    val e = entries[i]
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
                        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        onClick = {
                            if (e.type == "dir") {
                                nav.push(Screen.CodeBrowser(owner, repo, e.path, ref))
                            } else {
                                nav.push(Screen.FileViewer(owner, repo, e.path, ref))
                            }
                        },
                        showIndication = true,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (e.type == "dir") Octicons.Folder else Octicons.File,
                                contentDescription = null,
                                tint = if (e.type == "dir") GhColors.link else GhColors.gray,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = e.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                            )
                            if (e.type != "dir") {
                                Text(text = sizeText(e.size), fontSize = 11.sp, color = GhColors.gray)
                            } else {
                                Icon(
                                    imageVector = Octicons.ChevronRight,
                                    contentDescription = null,
                                    tint = GhColors.gray,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 文件查看器：raw 内容等宽字体显示，横向可滚动。
 */
@Composable
fun FileViewerScreen(nav: Nav, owner: String, repo: String, path: String, ref: String) {
    var content by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        content = GitHubApi.fileRaw(owner, repo, path, ref)
        loading = false
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = path.substringAfterLast('/'),
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
                    text = path,
                    fontSize = 12.sp,
                    color = GhColors.gray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            if (loading) {
                item { LoadingBox() }
                return@LazyColumn
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    insideMargin = PaddingValues(14.dp),
                ) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        Text(
                            text = content.ifBlank { "(空文件)" },
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun sizeText(bytes: Long): String = when {
    bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576f)
    bytes >= 1_024 -> String.format("%.1f KB", bytes / 1_024f)
    else -> "$bytes B"
}
