package com.google.hamahang.core.mermaid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull

object MermaidRenderer {
    /**
     * Renders Mermaid diagram code into an Android Bitmap using an offscreen WebView on the UI thread.
     * Bypasses bidi characters, initializes mermaid, renders via the SVG compiler, and captures a bitmap.
     * Times out and returns null if offline or unable to compile.
     */
    suspend fun renderToBitmap(context: Context, mermaidCode: String, isDark: Boolean): Bitmap? = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<Bitmap?>()
        val webView = WebView(context)
        
        // Clean bidi characters to avoid mermaid parsing syntax errors
        val cleanCode = mermaidCode.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
        val mermaidTheme = if (isDark) "dark" else "default"

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
                <script>
                    mermaid.initialize({
                        startOnLoad: false,
                        theme: '$mermaidTheme',
                        securityLevel: 'loose'
                    });
                    
                    window.onload = function() {
                        try {
                            var container = document.getElementById('mermaid-container');
                            var cleanCodeText = `${cleanCode.replace("`", "\\`").replace("$", "\\$")}`;
                            mermaid.render('mermaid-svg', cleanCodeText, function(svgCode) {
                                container.innerHTML = svgCode;
                                setTimeout(function() {
                                    var width = container.scrollWidth || 500;
                                    var height = container.scrollHeight || 300;
                                    if (window.AndroidInterface) {
                                        window.AndroidInterface.onRenderComplete(width, height);
                                    }
                                }, 100);
                            });
                        } catch (err) {
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onError(err.message);
                            }
                        }
                    };
                </script>
                <style>
                    body {
                        background-color: transparent;
                        margin: 0;
                        padding: 16px;
                        overflow: hidden;
                    }
                    #mermaid-container {
                        display: inline-block;
                        width: auto;
                        height: auto;
                    }
                </style>
            </head>
            <body>
                <div id="mermaid-container"></div>
            </body>
            </html>
        """.trimIndent()

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        class WebAppInterface {
            @JavascriptInterface
            fun onRenderComplete(width: Int, height: Int) {
                Handler(Looper.getMainLooper()).post {
                    try {
                        val w = width + 40
                        val h = height + 40
                        webView.measure(
                            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
                        )
                        webView.layout(0, 0, w, h)
                        
                        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        webView.draw(canvas)
                        deferred.complete(bitmap)
                    } catch (e: Exception) {
                        deferred.complete(null)
                    } finally {
                        webView.destroy()
                    }
                }
            }

            @JavascriptInterface
            fun onError(error: String) {
                Handler(Looper.getMainLooper()).post {
                    deferred.complete(null)
                    webView.destroy()
                }
            }
        }

        webView.addJavascriptInterface(WebAppInterface(), "AndroidInterface")

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                deferred.complete(null)
                webView.destroy()
            }
        }

        webView.loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)

        // Set a timeout of 8 seconds to prevent hanging if offline or if CDN is unreachable
        val result = withTimeoutOrNull(8000) {
            deferred.await()
        }
        
        if (result == null) {
            webView.destroy()
        }
        result
    }
}
