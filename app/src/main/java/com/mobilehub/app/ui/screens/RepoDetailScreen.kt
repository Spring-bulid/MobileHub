package com.mobilehub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilehub.app.Nav
import com.mobilehub.app.Screen
import com.mobilehub.app.core.GhCommit
import com.mobilehub.app.core.GhIssue
import com.mobilehub.app.core.GhRepo
import com.mobilehub.app.core.GitHubApi
import com.mobilehub.app.ui.Avatar
import com.mobilehub.app.ui.EmptyBox
import com.mobilehub.app.ui.GhColors
import com.mobilehub.app.ui.GhMarkdown
import com.mobilehub.app.ui.IssueRow
import com.mobilehub.app.ui.LoadingBox
import com.mobilehub.app.ui.Octicons
import com.mobilehub.app.ui.SegmentTabs
import com.mobilehub.app.ui.StatChip
import com.mobilehub.app.ui.countText
import com.mobilehub.app.ui.relativeTime
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 仓库详情：README / 议题 / 合并请求 / 提交 / 代码浏览 + star/watch/fork。
 */
@Composable
fun RepoDetailScreen(nav: Nav, owner: String, name: String) {
    val scope = rememberCoroutineScope()
    var repo by remember { mutableStateOf<GhRepo?>(null) }
    var readme by remember { mutableStateOf("") }
    var issues by remember { mutableStateOf(listOf<GhIssue>()) }
    var pulls by remember { mutableStateOf(listOf<GhIssue>()) }
    var commits by remember { mutableStateOf(listOf<GhCommit>()) }
    var starred by remember { mutableStateOf(false) }
    var watching by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    var tabLoading by remember { mutableStateOf(false) }
    var issueState by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        repo = GitHubApi.repo(owner, name)
        starred = GitHubApi.isStarred(owner, name)
        watching = GitHubApi.isWatching(owner, name)
        readme = GitHubApi.readmeHtml(owner, name)
    }

    LaunchedEffect(tab, issueState) {
        when (tab) {
            1 -> {
                tabLoading = true
                issues = GitHubApi.repoIssues(owner, name, if (issueState == 0) "open" else "closed")
                tabLoading = false
            }
            2 -> {
                tabLoading = true
                pulls = GitHubApi.repoPulls(owner, name, if (issueState == 0) "open" else "closed")
                tabLoading = false
            }
            3 -> {
                tabLoading = true
                commits = GitHubApi.commits(owner, name)
                tabLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = name,
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
        floatingActionButton = {
            if (tab == 1) {
                FloatingActionButton(onClick = { nav.push(Screen.NewIssue(owner, name)) }) {
                    Icon(imageVector = Octicons.Plus, contentDescription = "新建议题", tint = androidx.compose.ui.graphics.Color.White)
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            // 边缘拉伸回弹的 RenderEffect 会让硬件加速的 WebView 闪烁，关掉
            overscrollEffect = null,
        ) {
            item {
                RepoHeader(
                    repo = repo,
                    owner = owner,
                    name = name,
                    starred = starred,
                    watching = watching,
                    onOwnerClick = { nav.push(Screen.UserProfile(owner)) },
                    onStar = {
                        scope.launch {
                            if (GitHubApi.setStar(owner, name, !starred)) starred = !starred
                        }
                    },
                    onWatch = {
                        scope.launch {
                            if (GitHubApi.setWatch(owner, name, !watching)) watching = !watching
                        }
                    },
                    onFork = { scope.launch { GitHubApi.forkRepo(owner, name) } },
                )
            }
            item {
                SegmentTabs(listOf("自述", "议题", "合并", "提交", "代码"), tab) { tab = it }
            }
            when (tab) {
                0 -> item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                        insideMargin = PaddingValues(2.dp),
                    ) {
                        if (readme.isBlank()) {
                            Text(
                                text = "README 加载中或不存在...",
                                fontSize = 13.sp,
                                color = GhColors.gray,
                                modifier = Modifier.padding(14.dp),
                            )
                        } else {
                            GhMarkdown(readme)
                        }
                    }
                }

                1, 2 -> {
                    item { SegmentTabs(listOf("开放", "已关闭"), issueState) { issueState = it } }
                    if (tabLoading) {
                        item { LoadingBox() }
                    } else {
                        val list = if (tab == 1) issues else pulls
                        if (list.isEmpty()) item { EmptyBox(if (tab == 1) "没有议题" else "没有合并请求") }
                        items(list.size) { i ->
                            IssueRow(list[i]) {
                                nav.push(Screen.IssueDetail(owner, name, list[i].number, list[i].isPullRequest))
                            }
                        }
                    }
                }

                3 -> {
                    if (tabLoading) {
                        item { LoadingBox() }
                    } else {
                        if (commits.isEmpty()) item { EmptyBox("没有提交记录") }
                        items(commits.size) { i ->
                            val c = commits[i]
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                                insideMargin = PaddingValues(14.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Avatar(c.avatarUrl, size = 28.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = c.message,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2,
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = "${c.author} · ${relativeTime(c.date)}",
                                            fontSize = 12.sp,
                                            color = GhColors.gray,
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = c.sha,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = GhColors.link,
                                    )
                                }
                            }
                        }
                    }
                }

                4 -> item {
                    LaunchedEffect(Unit) {
                        nav.push(Screen.CodeBrowser(owner, name, "", repo?.defaultBranch ?: ""))
                        tab = 0
                    }
                }
            }
        }
    }
}

@Composable
private fun RepoHeader(
    repo: GhRepo?,
    owner: String,
    name: String,
    starred: Boolean,
    watching: Boolean,
    onOwnerClick: () -> Unit,
    onStar: () -> Unit,
    onWatch: () -> Unit,
    onFork: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        insideMargin = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(repo?.ownerAvatar ?: "", size = 32.dp, rounded = false)
            Spacer(Modifier.width(10.dp))
            Text(
                text = owner,
                fontSize = 14.sp,
                color = GhColors.link,
                modifier = Modifier.androidClickable(onOwnerClick),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(text = name, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        val desc = repo?.description ?: ""
        if (desc.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(text = desc, fontSize = 14.sp, color = GhColors.gray)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            StatChip(Octicons.Star, countText(repo?.stars ?: 0), GhColors.yellow)
            StatChip(Octicons.Fork, countText(repo?.forks ?: 0))
            StatChip(Octicons.IssueOpen, countText(repo?.openIssues ?: 0), GhColors.open)
            val lang = repo?.language ?: ""
            if (lang.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(10.dp).clip(CircleShape).background(GhColors.language(lang)),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(text = lang, fontSize = 12.sp, color = GhColors.gray)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onStar,
                colors = if (starred) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors(),
            ) {
                Icon(
                    imageVector = if (starred) Octicons.StarFill else Octicons.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (starred) androidx.compose.ui.graphics.Color.White else GhColors.yellow,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (starred) "已加星" else "加星",
                    color = if (starred) androidx.compose.ui.graphics.Color.White else MiuixTheme.colorScheme.onSurface,
                )
            }
            Button(onClick = onWatch) {
                Icon(imageVector = Octicons.Eye, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(text = if (watching) "取消关注" else "关注")
            }
            Button(onClick = onFork) {
                Icon(imageVector = Octicons.Fork, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(text = "复刻")
            }
        }
    }
}

/** 轻量 Markdown 渲染：标题加粗、代码块等宽、其余原样 */
@Composable
fun MarkdownLite(text: String) {
    Column {
        var inCode = false
        text.lineSequence().take(400).forEach { line ->
            when {
                line.trimStart().startsWith("```") -> inCode = !inCode
                inCode -> Text(
                    text = line,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                line.startsWith("#") -> Text(
                    text = line.trimStart('#', ' '),
                    fontSize = (20 - 2 * line.takeWhile { it == '#' }.length.coerceAtMost(4)).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                else -> Text(
                    text = line.replace(Regex("[*_`>]"), ""),
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

fun Modifier.androidClickable(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)
