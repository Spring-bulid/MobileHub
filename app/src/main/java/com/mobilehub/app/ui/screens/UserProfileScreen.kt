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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilehub.app.Nav
import com.mobilehub.app.Screen
import com.mobilehub.app.core.GhRepo
import com.mobilehub.app.core.GhUser
import com.mobilehub.app.core.GitHubApi
import com.mobilehub.app.ui.Avatar
import com.mobilehub.app.ui.EmptyBox
import com.mobilehub.app.ui.GhColors
import com.mobilehub.app.ui.LoadingBox
import com.mobilehub.app.ui.Octicons
import com.mobilehub.app.ui.RepoRow
import com.mobilehub.app.ui.countText
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
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 用户资料页：头像 / 简介 / 关注按钮 / 其仓库列表。
 */
@Composable
fun UserProfileScreen(nav: Nav, login: String) {
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<GhUser?>(null) }
    var repos by remember { mutableStateOf(listOf<GhRepo>()) }
    var following by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    val isMe = login == (GitHubApi.me?.login ?: "")

    LaunchedEffect(Unit) {
        loading = true
        user = GitHubApi.fetchUser(login)
        if (!isMe) following = GitHubApi.isFollowing(login)
        repos = GitHubApi.userRepos(login)
        loading = false
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = login,
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
            if (loading) {
                item { LoadingBox() }
                return@LazyColumn
            }
            val u = user
            if (u == null) {
                item { EmptyBox("用户加载失败") }
                return@LazyColumn
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    insideMargin = PaddingValues(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(u.avatarUrl, size = 64.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (u.name.isNotBlank()) u.name else u.login,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(text = u.login, fontSize = 13.sp, color = GhColors.gray)
                        }
                    }
                    if (u.bio.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(text = u.bio, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        InfoChip(Octicons.Person, "${countText(u.followers)} 关注者 · ${countText(u.following)} 关注中")
                        InfoChip(Octicons.Repo, "${u.publicRepos} 仓库")
                    }
                    if (u.company.isNotBlank() || u.location.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (u.company.isNotBlank()) InfoChip(Octicons.Org, u.company)
                            if (u.location.isNotBlank()) InfoChip(Octicons.Location, u.location)
                        }
                    }
                    if (u.blog.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        InfoChip(Octicons.Link, u.blog, GhColors.link)
                    }
                    if (!isMe) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    if (GitHubApi.setFollow(login, !following)) following = !following
                                }
                            },
                            colors = if (following) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColorsPrimary(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = if (following) "取消关注" else "关注",
                                color = if (following) MiuixTheme.colorScheme.onSurface else Color.White,
                            )
                        }
                    }
                }
            }

            item { SmallTitle(text = "仓库 (${repos.size})") }
            if (repos.isEmpty()) {
                item { EmptyBox("暂无公开仓库") }
            }
            items(repos.size) { i ->
                RepoRow(repos[i]) { nav.push(Screen.RepoDetail(repos[i].owner, repos[i].name)) }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String, tint: Color = GhColors.gray) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(text = text, fontSize = 12.sp, color = tint)
    }
}
