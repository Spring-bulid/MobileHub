package com.mobilehub.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 渲染 GitHub 官方输出的 HTML（README / 议题正文 / 评论），
 * 套用 github-markdown-css，视觉与 github.com 完全一致。
 * 高度由页面内 JS 上报，自适应内容。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GhMarkdown(html: String, modifier: Modifier = Modifier, contentPadding: Int = 16) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    var contentHeight by remember { mutableFloatStateOf(48f) }

    val css = remember(dark) {
        val file = if (dark) "github-markdown-dark.css" else "github-markdown-light.css"
        context.assets.open(file).bufferedReader().use { it.readText() }
    }
    val page = remember(html, css, contentPadding) { buildPage(css, html, contentPadding) }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onHeight(h: Float) {
                        if (h > 0f) contentHeight = h
                    }
                }, "MobileHub")
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val url = request.url ?: return false
                        // 锚点跳转留在页面内，其余链接交给系统浏览器
                        if (url.fragment != null && url.host == "github.com" && request.isRedirect.not() &&
                            url.toString().substringBefore('#') == "https://github.com/render"
                        ) return false
                        runCatching { view.context.startActivity(Intent(Intent.ACTION_VIEW, url)) }
                        return true
                    }
                }
            }
        },
        update = { web ->
            if (web.tag != page.hashCode()) {
                web.tag = page.hashCode()
                web.loadDataWithBaseURL("https://github.com/render", page, "text/html", "utf-8", null)
            }
        },
        modifier = modifier.fillMaxWidth().height(contentHeight.dp),
    )
}

private fun buildPage(css: String, html: String, contentPadding: Int): String = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
<style>$css</style>
<style>
  html, body { margin: 0; padding: 0; background: transparent; }
  .markdown-body {
    background: transparent;
    padding: ${contentPadding}px;
    font-size: 15px;
    overflow-wrap: break-word;
  }
  .markdown-body img { max-width: 100%; height: auto; }
  .markdown-body pre { overflow-x: auto; }
  .markdown-body table { display: block; overflow-x: auto; }
</style>
</head>
<body>
<article class="markdown-body">
$html
</article>
<script>
  function report() {
    MobileHub.onHeight(document.documentElement.scrollHeight);
  }
  window.addEventListener('load', report);
  new ResizeObserver(report).observe(document.body);
  for (const img of document.images) {
    img.addEventListener('load', report);
    img.addEventListener('error', report);
  }
</script>
</body>
</html>
"""
