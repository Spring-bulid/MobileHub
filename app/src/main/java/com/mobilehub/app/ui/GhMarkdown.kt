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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs

/**
 * 渲染 GitHub 官方输出的 HTML（README / 议题正文 / 评论），
 * 套用 github-markdown-css，视觉与 github.com 完全一致。
 *
 * fillContainer = false：高度由页内 JS 上报，自适应内容，适合短正文嵌列表；
 * fillContainer = true：填满容器并由 WebView 自己滚动，适合长 README，
 * 避免超高 WebView 嵌外部列表引发的光栅化白闪。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GhMarkdown(html: String, modifier: Modifier = Modifier, contentPadding: Int = 16, fillContainer: Boolean = false) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    var contentHeight by remember { mutableFloatStateOf(0f) }

    // 透明背景 + 硬件加速的 WebView 在滚动合成时会闪，改用与卡片一致的不透明背景
    val bgArgb = MiuixTheme.colorScheme.surface.toArgb()
    val bgHex = String.format("#%06X", 0xFFFFFF and bgArgb)

    val css = remember(dark) {
        val file = if (dark) "github-markdown-dark.css" else "github-markdown-light.css"
        context.assets.open(file).bufferedReader().use { it.readText() }
    }
    val page = remember(html, css, contentPadding, bgHex, fillContainer) {
        buildPage(css, html, contentPadding, bgHex, reportHeight = !fillContainer)
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                isVerticalScrollBarEnabled = fillContainer
                isHorizontalScrollBarEnabled = false
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onHeight(h: Float) {
                        // 1px 以内的变化直接忽略，避免取整误差引发循环抖动
                        if (h > 0f && abs(h - contentHeight) > 1f) contentHeight = h
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
            web.setBackgroundColor(bgArgb)
            if (web.tag != page.hashCode()) {
                web.tag = page.hashCode()
                web.loadDataWithBaseURL("https://github.com/render", page, "text/html", "utf-8", null)
            }
        },
        modifier = if (fillContainer) {
            modifier.fillMaxWidth()
        } else {
            modifier
                .fillMaxWidth()
                .height(if (contentHeight > 0f) contentHeight.dp else 1.dp)
                // 首次高度未就绪前隐藏，避免加载过程白屏闪烁
                .alpha(if (contentHeight > 0f) 1f else 0f)
        },
    )
}

private fun buildPage(css: String, html: String, contentPadding: Int, bgHex: String, reportHeight: Boolean): String {
    val script = if (reportHeight) """
<script>
  var last = 0;
  function report() {
    // 只量文章自身高度；scrollHeight 会受视口影响，与宿主设高互相触发导致闪烁
    var h = Math.ceil(document.getElementById('ghmd').getBoundingClientRect().height);
    if (h > 0 && Math.abs(h - last) > 1) {
      last = h;
      MobileHub.onHeight(h);
    }
  }
  window.addEventListener('load', report);
  new ResizeObserver(report).observe(document.getElementById('ghmd'));
  for (const img of document.images) {
    img.addEventListener('load', report);
    img.addEventListener('error', report);
  }
</script>
""" else ""
    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
<style>$css</style>
<style>
  html, body { margin: 0; padding: 0; background: $bgHex; }
  .markdown-body {
    background: $bgHex;
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
<article class="markdown-body" id="ghmd">
$html
</article>
$script
</body>
</html>
"""
}
