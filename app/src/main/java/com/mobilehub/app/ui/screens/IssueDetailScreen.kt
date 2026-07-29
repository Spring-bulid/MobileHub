package com.mobilehub.app.ui.screens

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilehub.app.Nav
import com.mobilehub.app.Screen
import com.mobilehub.app.core.GhComment
import com.mobilehub.app.core.GhIssue
import com.mobilehub.app.core.GitHubApi
import com.mobilehub.app.ui.Avatar
import com.mobilehub.app.ui.EmptyBox
import com.mobilehub.app.ui.GhColors
import com.mobilehub.app.ui.GhMarkdown
import com.mobilehub.app.ui.LabelChip
import com.mobilehub.app.ui.LoadingBox
import com.mobilehub.app.ui.Octicons
import com.mobilehub.app.ui.relativeTime
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 议题 / 合并请求 详情：正文 + 评论流 + 操作（评论、关闭/重开、合并）。
 */
@Composable
fun IssueDetailScreen(nav: Nav, owner: String, repo: String, number: Int, isPr: Boolean) {
    val scope = rememberCoroutineScope()
    var issue by remember { mutableStateOf<GhIssue?>(null) }
    var comments by remember { mutableStateOf(listOf<GhComment>()) }
    var loading by remember { mutableStateOf(true) }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh) {
        loading = true
        issue = if (isPr) GitHubApi.pull(owner, repo, number) else GitHubApi.issue(owner, repo, number)
        comments = GitHubApi.comments(owner, repo, number)
        loading = false
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "$owner/$repo #$number",
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
            // 正文/评论内嵌 WebView，边缘拉伸回弹会引发闪烁，关掉
            overscrollEffect = null,
        ) {
            if (loading) {
                item { LoadingBox() }
                return@LazyColumn
            }
            val it0 = issue
            if (it0 == null) {
                item { EmptyBox("加载失败") }
                return@LazyColumn
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    insideMargin = PaddingValues(16.dp),
                ) {
                    // 状态徽章
                    val (label, color) = when {
                        it0.isPullRequest && it0.merged -> "已合并" to GhColors.merged
                        it0.state == "open" -> "开放" to GhColors.open
                        else -> "已关闭" to GhColors.closed
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when {
                                it0.isPullRequest && it0.merged -> Octicons.Merge
                                it0.isPullRequest -> Octicons.PullRequest
                                it0.state == "open" -> Octicons.IssueOpen
                                else -> Octicons.Check
                            },
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
                        if (it0.isPullRequest && it0.headRef.isNotBlank()) {
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "${it0.headRef} -> ${it0.baseRef}",
                                fontSize = 12.sp,
                                color = GhColors.gray,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(text = it0.title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(it0.userAvatar, size = 22.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${it0.userLogin} 创建于 ${relativeTime(it0.createdAt)}",
                            fontSize = 12.sp,
                            color = GhColors.gray,
                            modifier = Modifier.androidClickable { nav.push(Screen.UserProfile(it0.userLogin)) },
                        )
                    }
                    if (it0.labels.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            it0.labels.take(4).forEach { (n, c) -> LabelChip(n, c) }
                        }
                    }
                    if (it0.bodyHtml.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        GhMarkdown(it0.bodyHtml, contentPadding = 0)
                    } else if (it0.body.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        MarkdownLite(it0.body)
                    }
                }
            }

            // 操作区
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (it0.isPullRequest && it0.state == "open" && !it0.merged) {
                        Button(
                            onClick = {
                                if (busy) return@Button
                                busy = true
                                scope.launch {
                                    GitHubApi.mergePull(owner, repo, number)
                                    busy = false
                                    refresh++
                                }
                            },
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(imageVector = Octicons.Merge, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text(text = "合并", color = Color.White)
                        }
                    }
                    if (!it0.merged) {
                        Button(
                            onClick = {
                                if (busy) return@Button
                                busy = true
                                scope.launch {
                                    GitHubApi.setIssueState(owner, repo, number, if (it0.state == "open") "closed" else "open")
                                    busy = false
                                    refresh++
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = if (it0.state == "open") Octicons.Close else Octicons.IssueOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (it0.state == "open") GhColors.closed else GhColors.open,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(text = if (it0.state == "open") "关闭" else "重新开放")
                        }
                    }
                }
            }

            item { SmallTitle(text = "评论 (${comments.size})") }

            if (comments.isEmpty()) {
                item { EmptyBox("还没有评论") }
            }
            items(comments.size) { i ->
                val c = comments[i]
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                    insideMargin = PaddingValues(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(c.userAvatar, size = 24.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = c.userLogin,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.androidClickable { nav.push(Screen.UserProfile(c.userLogin)) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = relativeTime(c.createdAt), fontSize = 12.sp, color = GhColors.gray)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (c.bodyHtml.isNotBlank()) {
                        GhMarkdown(c.bodyHtml, contentPadding = 0)
                    } else {
                        MarkdownLite(c.body)
                    }
                }
            }

            // 评论输入
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    insideMargin = PaddingValues(14.dp),
                ) {
                    TextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = "发表评论",
                        useLabelAsPlaceholder = true,
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (draft.isBlank() || busy) return@Button
                            busy = true
                            scope.launch {
                                val r = GitHubApi.addComment(owner, repo, number, draft)
                                busy = false
                                if (r.ok) {
                                    draft = ""
                                    refresh++
                                }
                            }
                        },
                        enabled = draft.isNotBlank() && !busy,
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(imageVector = Octicons.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text(text = "评论", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * 新建议题；当 owner 为空时复用为「新建仓库」表单。
 */
@Composable
fun NewIssueScreen(nav: Nav, owner: String, repo: String) {
    val scope = rememberCoroutineScope()
    val isRepoMode = owner.isBlank()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = if (isRepoMode) "新建仓库" else "新建议题",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 12.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = if (isRepoMode) "仓库名称" else "标题",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                TextField(
                    value = body,
                    onValueChange = { body = it },
                    label = if (isRepoMode) "仓库描述 (可选)" else "描述 (支持 Markdown)",
                    useLabelAsPlaceholder = true,
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isRepoMode) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.androidClickable { isPrivate = !isPrivate },
                    ) {
                        Icon(
                            imageVector = if (isPrivate) Octicons.Check else Octicons.Close,
                            contentDescription = null,
                            tint = if (isPrivate) GhColors.open else GhColors.gray,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = if (isPrivate) "私有仓库" else "公开仓库 (点击切换)", fontSize = 14.sp)
                    }
                }
                if (error.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = error, fontSize = 13.sp, color = GhColors.closed)
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (title.isBlank() || busy) return@Button
                        busy = true
                        error = ""
                        scope.launch {
                            val r = if (isRepoMode) {
                                GitHubApi.createRepo(title, body, isPrivate)
                            } else {
                                GitHubApi.createIssue(owner, repo, title, body)
                            }
                            busy = false
                            if (r.ok) nav.pop() else error = "操作失败 (HTTP ${r.status})"
                        }
                    },
                    enabled = title.isNotBlank() && !busy,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = if (isRepoMode) "创建仓库" else "提交议题", color = Color.White)
                }
            }
        }
    }
}
