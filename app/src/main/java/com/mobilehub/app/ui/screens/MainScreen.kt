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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilehub.app.Nav
import com.mobilehub.app.Screen
import com.mobilehub.app.core.AppPrefs
import com.mobilehub.app.core.GhIssue
import com.mobilehub.app.core.GhNotification
import com.mobilehub.app.core.GhRepo
import com.mobilehub.app.core.GhUser
import com.mobilehub.app.core.GitHubApi
import com.mobilehub.app.core.TokenStore
import com.mobilehub.app.ui.Avatar
import com.mobilehub.app.ui.EmptyBox
import com.mobilehub.app.ui.GhColors
import com.mobilehub.app.ui.IssueRow
import com.mobilehub.app.ui.LiquidNavBar
import com.mobilehub.app.ui.LoadingBox
import com.mobilehub.app.ui.NotificationRow
import com.mobilehub.app.ui.Octicons
import com.mobilehub.app.ui.RepoRow
import com.mobilehub.app.ui.SegmentTabs
import com.mobilehub.app.ui.UserRow
import com.mobilehub.app.ui.countText
import com.mobilehub.app.ui.toast
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 主界面：底部导航（主页 / 通知 / 探索 / 我的），仿 GitHub Mobile 信息架构。
 */
@Composable
fun MainScreen(nav: Nav) {
    var tab by remember { mutableIntStateOf(0) }
    val scrollBehavior = MiuixScrollBehavior()
    val liquid = AppPrefs.liquidBar.value

    val title = when (tab) {
        0 -> "主页"
        1 -> "通知"
        2 -> "探索"
        else -> "我的"
    }

    // 液态玻璃需要把内容录制成背景层，背景色一并画入，避免透明像素
    val bgColor = MiuixTheme.colorScheme.background
    val backdropDraw = remember(bgColor) {
        val block: androidx.compose.ui.graphics.drawscope.ContentDrawScope.() -> Unit = {
            drawRect(bgColor)
            drawContent()
        }
        block
    }
    val backdrop = rememberLayerBackdrop(onDraw = backdropDraw)

    Scaffold(
        topBar = {
            TopAppBar(
                title = title,
                largeTitle = title,
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            if (!liquid) {
                NavigationBar {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = Octicons.Home, label = "主页")
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = Octicons.Bell, label = "通知")
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = Octicons.Search, label = "探索")
                    NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = Octicons.Person, label = "我的")
                }
            }
        },
    ) { padding ->
        val contentModifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        // 悬浮模式下 Scaffold 没有底栏，手动为内容留出悬浮栏的空间
        val effectivePadding = if (liquid) {
            PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = navInset + 88.dp,
            )
        } else {
            padding
        }

        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .then(if (liquid) Modifier.layerBackdrop(backdrop) else Modifier),
            ) {
                // 按切换方向轻滑动 + 淡入淡出，避免硬切
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(tween(300)) { it / 8 * dir } + fadeIn(tween(300))) togetherWith
                            (slideOutHorizontally(tween(300)) { -it / 8 * dir } + fadeOut(tween(150)))
                    },
                    label = "tabContent",
                ) { t ->
                    when (t) {
                        0 -> HomeTab(nav, effectivePadding, contentModifier)
                        1 -> NotificationsTab(nav, effectivePadding, contentModifier)
                        2 -> ExploreTab(nav, effectivePadding, contentModifier)
                        else -> ProfileTab(nav, effectivePadding, contentModifier)
                    }
                }
            }
            if (liquid) {
                LiquidNavBar(
                    items = listOf(
                        Octicons.Home to "主页",
                        Octicons.Bell to "通知",
                        Octicons.Search to "探索",
                        Octicons.Person to "我的",
                    ),
                    selected = { tab },
                    onSelect = { tab = it },
                    backdrop = backdrop,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = navInset + 12.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 主页
// ---------------------------------------------------------------------------

@Composable
private fun HomeTab(nav: Nav, padding: PaddingValues, modifier: Modifier = Modifier) {
    var loading by remember { mutableStateOf(true) }
    var repos by remember { mutableStateOf(listOf<GhRepo>()) }
    var issues by remember { mutableStateOf(listOf<GhIssue>()) }
    var section by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        loading = true
        repos = GitHubApi.myRepos()
        issues = GitHubApi.myIssues()
        loading = false
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 12.dp),
    ) {
        item {
            SegmentTabs(listOf("我的仓库", "我的议题"), section) { section = it }
        }
        if (loading) {
            item { LoadingBox() }
        } else if (section == 0) {
            if (repos.isEmpty()) item { EmptyBox("暂无仓库") }
            items(repos.size) { i ->
                RepoRow(repos[i]) { nav.push(Screen.RepoDetail(repos[i].owner, repos[i].name)) }
            }
        } else {
            if (issues.isEmpty()) item { EmptyBox("没有分配给你的开放议题") }
            items(issues.size) { i ->
                val issue = issues[i]
                IssueRow(issue, showRepo = true) {
                    val parts = issue.repoFullName.split("/")
                    if (parts.size == 2) {
                        nav.push(Screen.IssueDetail(parts[0], parts[1], issue.number, issue.isPullRequest))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 通知
// ---------------------------------------------------------------------------

@Composable
private fun NotificationsTab(nav: Nav, padding: PaddingValues, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var showAll by remember { mutableIntStateOf(0) }
    var list by remember { mutableStateOf(listOf<GhNotification>()) }

    suspend fun reload() {
        loading = true
        list = GitHubApi.notifications(all = showAll == 1)
        loading = false
    }

    LaunchedEffect(showAll) { reload() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 12.dp),
    ) {
        item {
            SegmentTabs(listOf("未读", "全部"), showAll) { showAll = it }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(text = "全部标为已读", onClick = {
                    scope.launch {
                        val ok = GitHubApi.markAllRead()
                        if (ok) {
                            toast(context, "已全部标为已读")
                            reload()
                        } else {
                            toast(context, "操作失败，请检查网络")
                        }
                    }
                })
            }
        }
        if (loading) {
            item { LoadingBox() }
        } else {
            if (list.isEmpty()) item { EmptyBox("没有通知，一切安好") }
            items(list.size) { i ->
                val n = list[i]
                NotificationRow(n) {
                    scope.launch {
                        GitHubApi.markThreadRead(n.id)
                        reload()
                    }
                    // subjectUrl 形如 https://api.github.com/repos/{o}/{r}/issues/{n}
                    val seg = n.subjectUrl.substringAfter("/repos/", "").split("/")
                    if (seg.size >= 4) {
                        val number = seg[3].toIntOrNull()
                        when {
                            number != null && seg[2] == "issues" ->
                                nav.push(Screen.IssueDetail(seg[0], seg[1], number, false))
                            number != null && seg[2] == "pulls" ->
                                nav.push(Screen.IssueDetail(seg[0], seg[1], number, true))
                            else -> nav.push(Screen.RepoDetail(seg[0], seg[1]))
                        }
                    } else if (n.repoFullName.contains("/")) {
                        val p = n.repoFullName.split("/")
                        nav.push(Screen.RepoDetail(p[0], p[1]))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 探索（搜索 + 热门）
// ---------------------------------------------------------------------------

@Composable
private fun ExploreTab(nav: Nav, padding: PaddingValues, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var mode by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var repos by remember { mutableStateOf(listOf<GhRepo>()) }
    var users by remember { mutableStateOf(listOf<GhUser>()) }
    var searched by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repos = GitHubApi.trending()
        loading = false
    }

    fun doSearch() {
        if (query.isBlank()) return
        scope.launch {
            loading = true
            searched = true
            if (mode == 0) repos = GitHubApi.searchRepos(query) else users = GitHubApi.searchUsers(query)
            loading = false
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    label = "搜索仓库或开发者",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { doSearch() }) {
                    Icon(
                        imageVector = Octicons.Search,
                        contentDescription = "搜索",
                        tint = MiuixTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        item {
            SegmentTabs(listOf("仓库", "开发者"), mode) {
                mode = it
                if (searched) doSearch()
            }
        }
        if (!searched) {
            item { SmallTitle(text = "热门仓库") }
        }
        if (loading) {
            item { LoadingBox() }
        } else if (mode == 0) {
            if (repos.isEmpty()) item { EmptyBox("没有找到仓库") }
            items(repos.size) { i ->
                RepoRow(repos[i]) { nav.push(Screen.RepoDetail(repos[i].owner, repos[i].name)) }
            }
        } else {
            if (users.isEmpty()) item { EmptyBox("没有找到开发者") }
            items(users.size) { i ->
                UserRow(users[i]) { nav.push(Screen.UserProfile(users[i].login)) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 我的
// ---------------------------------------------------------------------------

@Composable
private fun ProfileTab(nav: Nav, padding: PaddingValues, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var me by remember { mutableStateOf(GitHubApi.me) }
    var starred by remember { mutableStateOf(listOf<GhRepo>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (me == null) me = GitHubApi.fetchMe()
        starred = GitHubApi.starredRepos()
        loading = false
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 12.dp),
    ) {
        item {
            val user = me
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                insideMargin = PaddingValues(16.dp),
            ) {
                if (user == null) {
                    Text(text = "加载中...", color = GhColors.gray)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(user.avatarUrl, size = 56.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(text = user.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(text = user.login, fontSize = 13.sp, color = GhColors.gray)
                        }
                        IconButton(onClick = { nav.push(Screen.UserProfile(user.login)) }) {
                            Icon(
                                imageVector = Octicons.ChevronRight,
                                contentDescription = null,
                                tint = GhColors.gray,
                            )
                        }
                    }
                    if (user.bio.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(text = user.bio, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(text = "${countText(user.followers)} 关注者", fontSize = 12.sp, color = GhColors.gray)
                        Text(text = "${countText(user.following)} 正在关注", fontSize = 12.sp, color = GhColors.gray)
                        Text(text = "${countText(user.publicRepos)} 仓库", fontSize = 12.sp, color = GhColors.gray)
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = { nav.push(Screen.NewIssue("", "")) }, modifier = Modifier.weight(1f)) {
                    Icon(imageVector = Octicons.Plus, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(text = "新建仓库")
                }
                Button(
                    onClick = {
                        TokenStore.clear(context)
                        GitHubApi.token = ""
                        GitHubApi.me = null
                        nav.reset(com.mobilehub.app.Screen.Login)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(imageVector = Octicons.SignOut, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(text = "退出登录")
                }
            }
        }
        item { SmallTitle(text = "已加星标") }
        if (loading) {
            item { LoadingBox() }
        } else {
            if (starred.isEmpty()) item { EmptyBox("还没有加星标的仓库") }
            items(starred.size) { i ->
                RepoRow(starred[i]) { nav.push(Screen.RepoDetail(starred[i].owner, starred[i].name)) }
            }
        }
        item { SmallTitle(text = "更多") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                insideMargin = PaddingValues(0.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { nav.push(com.mobilehub.app.Screen.Settings) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Icon(
                        imageVector = Octicons.Gear,
                        contentDescription = null,
                        tint = GhColors.gray,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(text = "设置", fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Octicons.ChevronRight,
                        contentDescription = null,
                        tint = GhColors.gray,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
