package br.com.tecoff.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class ImageBridge(private val onImageClick: (String) -> Unit) {
    @JavascriptInterface
    fun onImageClick(url: String) {
        Handler(Looper.getMainLooper()).post {
            Log.d("HtmlText", "ImageBridge onImageClick: $url")
            onImageClick(url)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme(),
    onImageClick: ((String) -> Unit)? = null
) {
    val currentOnImageClick by rememberUpdatedState(onImageClick)

    val styledHtml = remember(html, isDark) {
        val textColor = if (isDark) "#F8FAFC" else "#0F172A"
        val linkColor = if (isDark) "#38BDF8" else "#0284C7"

        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    font-size: 15px;
                    line-height: 1.6;
                    color: $textColor;
                    background-color: transparent;
                    margin: 0;
                    padding: 4px;
                    word-wrap: break-word;
                }
                img {
                    max-width: 100%;
                    height: auto;
                    display: block;
                    margin: 10px auto;
                    border-radius: 8px;
                    cursor: pointer;
                    box-shadow: 0 1px 3px rgba(0,0,0,0.12);
                    transition: transform 0.15s ease-in-out;
                }
                img:active {
                    transform: scale(0.97);
                    opacity: 0.85;
                }
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 8px 0;
                }
                th, td {
                    border: 1px solid ${if (isDark) "#334155" else "#E2E8F0"};
                    padding: 6px 10px;
                    text-align: left;
                }
                th {
                    background-color: ${if (isDark) "#0F172A" else "#F1F5F9"};
                }
                a {
                    color: $linkColor;
                    text-decoration: none;
                }
                p {
                    margin-top: 0;
                    margin-bottom: 8px;
                }
                blockquote {
                    margin: 8px 0;
                    padding-left: 12px;
                    border-left: 3px solid $linkColor;
                    color: ${if (isDark) "#94A3B8" else "#64748B"};
                }
            </style>
            <script>
                function attachImageClicks() {
                    var imgs = document.getElementsByTagName("img");
                    for (var i = 0; i < imgs.length; i++) {
                        var img = imgs[i];
                        img.style.cursor = "pointer";
                        img.onclick = function(e) {
                            if (e) {
                                e.preventDefault();
                                e.stopPropagation();
                            }
                            if (window.AndroidBridge && window.AndroidBridge.onImageClick) {
                                window.AndroidBridge.onImageClick(this.src);
                            }
                        };
                    }
                }

                if (document.readyState === "loading") {
                    document.addEventListener("DOMContentLoaded", attachImageClicks);
                } else {
                    attachImageClicks();
                }
                window.addEventListener("load", attachImageClicks);
                setTimeout(attachImageClicks, 100);
                setTimeout(attachImageClicks, 500);
                setTimeout(attachImageClicks, 1200);
            </script>
        </head>
        <body>
            $html
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    loadsImagesAutomatically = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                addJavascriptInterface(
                    ImageBridge { url ->
                        currentOnImageClick?.invoke(url)
                    },
                    "AndroidBridge"
                )

                // Native HitTestResult touch detection for 100% reliable tap handling on images
                var downX = 0f
                var downY = 0f
                setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            downX = event.x
                            downY = event.y
                        }
                        MotionEvent.ACTION_UP -> {
                            val deltaX = Math.abs(event.x - downX)
                            val deltaY = Math.abs(event.y - downY)
                            // Tap detection (minimal movement to prevent triggering during scrolls)
                            if (deltaX < 25 && deltaY < 25) {
                                val hr = hitTestResult
                                if (hr != null) {
                                    val type = hr.type
                                    if (type == WebView.HitTestResult.IMAGE_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                                        val extra = hr.extra
                                        if (!extra.isNullOrBlank()) {
                                            Log.d("HtmlText", "HitTestResult Image Click: $extra")
                                            currentOnImageClick?.invoke(extra)
                                            return@setOnTouchListener true
                                        }
                                    }
                                }
                            }
                        }
                    }
                    false
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("file:///", styledHtml, "text/html", "UTF-8", null)
        }
    )
}
