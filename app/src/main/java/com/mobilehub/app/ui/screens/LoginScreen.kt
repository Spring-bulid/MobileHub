package com.mobilehub.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilehub.app.core.GitHubApi
import com.mobilehub.app.core.TokenStore
import com.mobilehub.app.ui.GhColors
import com.mobilehub.app.ui.Octicons
import com.mobilehub.app.ui.toast
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 登录页：只需一枚 GitHub Personal Access Token。
 */
@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    // GitHub 不开放用 API 创建 PAT，只能跳转网页；预填 scopes 与描述，创建后复制回来即可
    fun openTokenPage() {
        val url = "https://github.com/settings/tokens/new" +
            "?scopes=repo,notifications,user,delete_repo,workflow,read:org" +
            "&description=MobileHub"
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure { error = "未找到可用浏览器，请手动访问 github.com/settings/tokens" }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(96.dp))
            Icon(
                imageVector = Octicons.Org,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(text = "MobileHub", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "用一枚 Token 掌控整个 GitHub",
                fontSize = 14.sp,
                color = GhColors.gray,
            )
            Spacer(Modifier.height(40.dp))

            Card(modifier = Modifier.fillMaxWidth(), insideMargin = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextField(
                        value = token,
                        onValueChange = { token = it.trim() },
                        label = "Personal Access Token",
                        useLabelAsPlaceholder = true,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    TextButton(text = "粘贴", onClick = {
                        val t = clipboard.getText()?.text?.trim().orEmpty()
                        if (t.isNotBlank()) {
                            token = t
                        } else {
                            toast(context, "剪贴板为空，请先复制 Token")
                        }
                    })
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "支持 classic 与 fine-grained token，建议勾选 repo、notifications、user 权限。Token 将由本机 C 保险库加密保存。",
                    fontSize = 12.sp,
                    color = GhColors.gray,
                )
                if (error.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = error, fontSize = 13.sp, color = GhColors.closed)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (token.isBlank() || loading) return@Button
                        loading = true
                        error = ""
                        scope.launch {
                            GitHubApi.token = token
                            val me = GitHubApi.fetchMe()
                            loading = false
                            if (me == null) {
                                GitHubApi.token = ""
                                error = "Token 无效或网络不可用，请检查后重试"
                            } else {
                                TokenStore.save(context, token)
                                onLoggedIn()
                            }
                        }
                    },
                    enabled = !loading && token.isNotBlank(),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (loading) {
                        CircularProgressIndicator(size = 20.dp)
                    } else {
                        Text(text = "登录", color = MiuixTheme.colorScheme.onPrimary)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { openTokenPage() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Octicons.Link,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(text = "没有 Token？去 GitHub 创建")
                }
            }

            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "创建页已预勾 repo / notifications / user 等权限", fontSize = 12.sp, color = GhColors.gray)
                Text(text = "生成后复制 Token，回到本页点击粘贴即可登录", fontSize = 12.sp, color = GhColors.gray)
            }
        }
    }
}
