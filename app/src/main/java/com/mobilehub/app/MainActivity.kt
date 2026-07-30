package com.mobilehub.app

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.mobilehub.app.core.AppPrefs
import com.mobilehub.app.core.GitHubApi
import com.mobilehub.app.core.TokenStore
import com.mobilehub.app.ui.screens.ActionsScreen
import com.mobilehub.app.ui.screens.CodeBrowserScreen
import com.mobilehub.app.ui.screens.FileViewerScreen
import com.mobilehub.app.ui.screens.IssueDetailScreen
import com.mobilehub.app.ui.screens.LoginScreen
import com.mobilehub.app.ui.screens.MainScreen
import com.mobilehub.app.ui.screens.NewIssueScreen
import com.mobilehub.app.ui.screens.RepoDetailScreen
import com.mobilehub.app.ui.screens.SettingsScreen
import com.mobilehub.app.ui.screens.UserProfileScreen
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/** 页面路由 */
sealed interface Screen {
    data object Login : Screen
    data object Main : Screen
    data class RepoDetail(val owner: String, val name: String) : Screen
    data class IssueDetail(val owner: String, val repo: String, val number: Int, val isPr: Boolean) : Screen
    data class NewIssue(val owner: String, val repo: String) : Screen
    data class UserProfile(val login: String) : Screen
    data class CodeBrowser(val owner: String, val repo: String, val path: String, val ref: String) : Screen
    data class FileViewer(val owner: String, val repo: String, val path: String, val ref: String) : Screen
    data class Actions(val owner: String, val repo: String) : Screen
    data object Settings : Screen
}

/** 简单回退栈导航 */
class Nav(private val stack: androidx.compose.runtime.snapshots.SnapshotStateList<Screen>) {
    val current: Screen get() = stack.last()
    val canPop: Boolean get() = stack.size > 1
    fun push(s: Screen) = stack.add(s)
    fun pop() { if (canPop) stack.removeAt(stack.lastIndex) }
    fun reset(s: Screen) { stack.clear(); stack.add(s) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // WebView 嵌在外部滚动容器里时，默认只光栅化自身视口附近区域，
        // 远端区块滚到时才画会白闪；必须在创建任何 WebView 之前开启整页绘制
        WebView.enableSlowWholeDocumentDraw()
        enableEdgeToEdge()

        val saved = TokenStore.load(this)
        if (saved != null) GitHubApi.token = saved
        AppPrefs.load(this)

        setContent {
            val useDark = when (AppPrefs.themeMode.intValue) {
                1 -> false  // 强制浅色
                2 -> true   // 强制深色
                else -> isSystemInDarkTheme()  // 跟随系统
            }
            val colors = if (useDark) darkColorScheme() else lightColorScheme()
            MiuixTheme(colors = colors) {
                App(startLoggedIn = saved != null)
            }
        }
    }
}

@Composable
fun App(startLoggedIn: Boolean) {
    val stack = remember {
        mutableStateListOf<Screen>(if (startLoggedIn) Screen.Main else Screen.Login)
    }
    val nav = remember { Nav(stack) }

    BackHandler(enabled = nav.canPop) { nav.pop() }

    when (val s = nav.current) {
        is Screen.Login -> LoginScreen(onLoggedIn = { nav.reset(Screen.Main) })
        is Screen.Main -> MainScreen(nav)
        is Screen.RepoDetail -> RepoDetailScreen(nav, s.owner, s.name)
        is Screen.IssueDetail -> IssueDetailScreen(nav, s.owner, s.repo, s.number, s.isPr)
        is Screen.NewIssue -> NewIssueScreen(nav, s.owner, s.repo)
        is Screen.UserProfile -> UserProfileScreen(nav, s.login)
        is Screen.CodeBrowser -> CodeBrowserScreen(nav, s.owner, s.repo, s.path, s.ref)
        is Screen.FileViewer -> FileViewerScreen(nav, s.owner, s.repo, s.path, s.ref)
        is Screen.Actions -> ActionsScreen(nav, s.owner, s.repo)
        is Screen.Settings -> SettingsScreen(nav)
    }
}
