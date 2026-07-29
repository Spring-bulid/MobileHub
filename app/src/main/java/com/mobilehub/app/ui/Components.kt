package com.mobilehub.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mobilehub.app.core.GhIssue
import com.mobilehub.app.core.GhNotification
import com.mobilehub.app.core.GhRepo
import com.mobilehub.app.core.GhUser
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.Duration
import java.time.Instant

// GitHub 品牌色
object GhColors {
    val open = Color(0xFF1F883D)
    val closed = Color(0xFFCF222E)
    val merged = Color(0xFF8250DF)
    val link = Color(0xFF0969DA)
    val yellow = Color(0xFFEAC54F)
    val gray = Color(0xFF656D76)

    fun language(name: String): Color = when (name) {
        "Kotlin" -> Color(0xFFA97BFF)
        "Java" -> Color(0xFFB07219)
        "Rust" -> Color(0xFFDEA584)
        "C" -> Color(0xFF555555)
        "C++" -> Color(0xFFF34B7D)
        "Python" -> Color(0xFF3572A5)
        "JavaScript" -> Color(0xFFF1E05A)
        "TypeScript" -> Color(0xFF3178C6)
        "Go" -> Color(0xFF00ADD8)
        "Swift" -> Color(0xFFF05138)
        "Dart" -> Color(0xFF00B4AB)
        "Shell" -> Color(0xFF89E051)
        "HTML" -> Color(0xFFE34C26)
        "CSS" -> Color(0xFF563D7C)
        "Ruby" -> Color(0xFF701516)
        "PHP" -> Color(0xFF4F5D95)
        else -> Color(0xFF858585)
    }
}

fun relativeTime(iso: String): String {
    if (iso.isBlank()) return ""
    return runCatching {
        val then = Instant.parse(iso)
        val d = Duration.between(then, Instant.now())
        when {
            d.toMinutes() < 1 -> "刚刚"
            d.toMinutes() < 60 -> "${d.toMinutes()} 分钟前"
            d.toHours() < 24 -> "${d.toHours()} 小时前"
            d.toDays() < 30 -> "${d.toDays()} 天前"
            d.toDays() < 365 -> "${d.toDays() / 30} 个月前"
            else -> "${d.toDays() / 365} 年前"
        }
    }.getOrDefault("")
}

fun countText(n: Int): String = when {
    n >= 1_000_000 -> String.format("%.1fm", n / 1_000_000f)
    n >= 1_000 -> String.format("%.1fk", n / 1_000f)
    else -> n.toString()
}

@Composable
fun Avatar(url: String, size: Dp = 40.dp, rounded: Boolean = true) {
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .clip(if (rounded) CircleShape else RoundedCornerShape(6.dp))
            .background(Color(0x11000000)),
    )
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyBox(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        Text(text = text, color = GhColors.gray, fontSize = 14.sp)
    }
}

@Composable
fun StatChip(icon: ImageVector, value: String, tint: Color = GhColors.gray) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text = value, fontSize = 12.sp, color = GhColors.gray)
    }
}

@Composable
fun LabelChip(name: String, hex: String) {
    val color = runCatching { Color(("FF$hex").toLong(16)) }.getOrDefault(GhColors.merged)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text = name, fontSize = 11.sp, color = color)
    }
}

/** 仓库列表条目，仿 GitHub Mobile 卡片 */
@Composable
fun RepoRow(repo: GhRepo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        insideMargin = androidx.compose.foundation.layout.PaddingValues(14.dp),
        onClick = onClick,
        showIndication = true,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(repo.ownerAvatar, size = 22.dp, rounded = false)
            Spacer(Modifier.width(8.dp))
            Text(
                text = repo.owner,
                fontSize = 13.sp,
                color = GhColors.gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (repo.isPrivate) {
                Spacer(Modifier.width(6.dp))
                LabelChip("Private", "CF222E")
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = repo.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (repo.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = repo.description,
                fontSize = 13.sp,
                color = GhColors.gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatChip(Octicons.Star, countText(repo.stars), GhColors.yellow)
            StatChip(Octicons.Fork, countText(repo.forks))
            if (repo.language.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(GhColors.language(repo.language)),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(text = repo.language, fontSize = 12.sp, color = GhColors.gray)
                }
            }
        }
    }
}

/** issue / PR 列表条目 */
@Composable
fun IssueRow(issue: GhIssue, showRepo: Boolean = false, onClick: () -> Unit) {
    val (icon, tint) = when {
        issue.isPullRequest && issue.merged -> Octicons.Merge to GhColors.merged
        issue.isPullRequest && issue.state == "open" -> Octicons.PullRequest to GhColors.open
        issue.isPullRequest -> Octicons.PullRequest to GhColors.closed
        issue.state == "open" -> Octicons.IssueOpen to GhColors.open
        else -> Octicons.Check to GhColors.merged
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        insideMargin = androidx.compose.foundation.layout.PaddingValues(14.dp),
        onClick = onClick,
        showIndication = true,
    ) {
        Row {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                if (showRepo && issue.repoFullName.isNotBlank()) {
                    Text(text = issue.repoFullName, fontSize = 12.sp, color = GhColors.gray)
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = issue.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "#${issue.number}", fontSize = 12.sp, color = GhColors.gray)
                    Text(text = relativeTime(issue.createdAt), fontSize = 12.sp, color = GhColors.gray)
                    if (issue.comments > 0) StatChip(Octicons.Comment, issue.comments.toString())
                }
                if (issue.labels.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        issue.labels.take(3).forEach { (n, c) -> LabelChip(n, c) }
                    }
                }
            }
        }
    }
}

/** 通知条目 */
@Composable
fun NotificationRow(n: GhNotification, onClick: () -> Unit) {
    val icon = when (n.type) {
        "Issue" -> Octicons.IssueOpen
        "PullRequest" -> Octicons.PullRequest
        "Release" -> Octicons.Star
        "Discussion" -> Octicons.Comment
        else -> Octicons.Bell
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        insideMargin = androidx.compose.foundation.layout.PaddingValues(14.dp),
        onClick = onClick,
        showIndication = true,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (n.unread) GhColors.link else GhColors.gray,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(text = n.repoFullName, fontSize = 12.sp, color = GhColors.gray)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = n.title,
                    fontSize = 15.sp,
                    fontWeight = if (n.unread) FontWeight.SemiBold else FontWeight.Normal,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(text = "${n.reason} · ${relativeTime(n.updatedAt)}", fontSize = 12.sp, color = GhColors.gray)
            }
            if (n.unread) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(GhColors.link))
            }
        }
    }
}

/** 用户条目 */
@Composable
fun UserRow(user: GhUser, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        insideMargin = androidx.compose.foundation.layout.PaddingValues(14.dp),
        onClick = onClick,
        showIndication = true,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(user.avatarUrl, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = user.login,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                if (user.bio.isNotBlank()) {
                    Text(
                        text = user.bio,
                        fontSize = 12.sp,
                        color = GhColors.gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** 简易分段选项卡 (仿 GitHub Mobile 顶部过滤) */
@Composable
fun SegmentTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer),
    ) {
        tabs.forEachIndexed { i, t ->
            val active = i == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) MiuixTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = t,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) MiuixTheme.colorScheme.onSurface else GhColors.gray,
                )
            }
        }
    }
}
