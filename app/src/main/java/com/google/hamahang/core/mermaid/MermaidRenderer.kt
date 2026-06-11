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
                <script src="file:///android_asset/mermaid/mermaid.min.js"></script>
                <script>
                    mermaid.initialize({
                        startOnLoad: true,
                        theme: '$mermaidTheme',
                        securityLevel: 'loose',
                        fontFamily: 'Vazirmatn, sans-serif'
                    });
                    
                    window.onload = function() {
                        var container = document.getElementById('mermaid-container');
                        
                        // Check if it's already rendered
                        var svgs = document.getElementsByTagName('svg');
                        if (svgs.length > 0) {
                            reportComplete(svgs[0], container);
                            return;
                        }

                        // Otherwise wait for it
                        var observer = new MutationObserver(function(mutations) {
                            var svgs = document.getElementsByTagName('svg');
                            if (svgs.length > 0) {
                                observer.disconnect();
                                // Wait a tiny bit for the browser to layout the SVG
                                setTimeout(function() {
                                    reportComplete(svgs[0], container);
                                }, 100);
                            }
                        });
                        
                        observer.observe(document.body, { childList: true, subtree: true });
                        
                        // Fallback timeout in case mermaid fails silently
                        setTimeout(function() {
                            var svgs = document.getElementsByTagName('svg');
                            if (svgs.length === 0) {
                                observer.disconnect();
                                if (window.AndroidInterface) {
                                    window.AndroidInterface.onError("Timeout: No SVG element found after rendering.");
                                }
                            }
                        }, 5000);
                    };

                    function reportComplete(svg, container) {
                        try {
                            var rect = svg.getBoundingClientRect();
                            var width = rect.width || container.scrollWidth || 500;
                            var height = rect.height || container.scrollHeight || 300;
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onRenderComplete(Math.ceil(width), Math.ceil(height));
                            }
                        } catch (err) {
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onError("JS Error in reportComplete: " + err.message);
                            }
                        }
                    }
                </script>
                <style>
                    @font-face {
                        font-family: 'Vazirmatn';
                        src: url('file:///android_asset/fonts/vazirmatn_regular.ttf');
                    }
                    body {
                        background-color: transparent;
                        margin: 0;
                        padding: 16px;
                        overflow: hidden;
                        display: inline-block;
                        font-family: 'Vazirmatn', sans-serif;
                        direction: ltr;
                    }
                    #mermaid-container {
                        display: inline-block;
                    }
                </style>
            </head>
            <body>
                <div id="mermaid-container">
                    <pre class="mermaid">$cleanCode</pre>
                </div>
            </body>
            </html>
        """.trimIndent()

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        class WebAppInterface {
            @JavascriptInterface
            fun onRenderComplete(width: Int, height: Int) {
                android.util.Log.d("MermaidRenderer", "onRenderComplete: width=$width, height=$height")
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
                        android.util.Log.e("MermaidRenderer", "Error drawing bitmap: ${e.message}")
                        deferred.complete(null)
                    }
                }
            }

            @JavascriptInterface
            fun onError(error: String) {
                android.util.Log.e("MermaidRenderer", "onError from JS: $error")
                Handler(Looper.getMainLooper()).post {
                    deferred.complete(null)
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
                android.util.Log.e("MermaidRenderer", "WebView Error: ${error?.description}")
                deferred.complete(null)
            }
        }

        android.util.Log.d("MermaidRenderer", "Loading HTML into WebView...")
        
        // Give the WebView an initial size so Mermaid has a viewport to calculate SVG layouts
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY)
        )
        webView.layout(0, 0, 2000, 2000)

        // Attach to Window so requestAnimationFrame and JS timers tick correctly!
        var activity: android.app.Activity? = null
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) {
                activity = ctx
                break
            }
            ctx = ctx.baseContext
        }
        
        val decorView = activity?.window?.decorView as? android.view.ViewGroup
        if (decorView != null) {
            val params = android.widget.FrameLayout.LayoutParams(2000, 2000)
            // Hide it way off screen
            params.leftMargin = -10000
            decorView.addView(webView, params)
        }

        try {
            webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)

            // Set a timeout of 8 seconds to prevent hanging if offline or if CDN is unreachable
            val result = withTimeoutOrNull(8000) {
                deferred.await()
            }
            
            if (result == null) {
                android.util.Log.e("MermaidRenderer", "renderToBitmap timed out or failed.")
            } else {
                android.util.Log.d("MermaidRenderer", "renderToBitmap success.")
            }
            
            result
        } finally {
            if (decorView != null) {
                decorView.removeView(webView)
            }
            webView.destroy()
        }
    }
}
