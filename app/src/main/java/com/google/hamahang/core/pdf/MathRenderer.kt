package com.google.hamahang.core.pdf

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object MathRenderer {
    /**
     * Renders a KaTeX math block into an Android Bitmap using an offscreen WebView on the UI thread.
     */
    suspend fun renderToBitmap(context: Context, mathCode: String): Bitmap? = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<Bitmap?>()
        val webView = WebView(context)
        
        val cleanCode = mathCode.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
        
        // Escape for JS template string
        val jsonSafeCode = cleanCode.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="stylesheet" href="file:///android_asset/katex/katex.min.css">
                <script src="file:///android_asset/katex/katex.min.js"></script>
                <style>
                    body {
                        background-color: transparent;
                        color: #000000;
                        display: inline-block;
                        margin: 0;
                        padding: 16px;
                        overflow: hidden;
                    }
                    #math-container {
                        display: inline-block;
                        font-size: 1.2em;
                        text-align: center;
                    }
                    .katex-display {
                        margin: 0 !important;
                    }
                </style>
            </head>
            <body>
                <div id="math-container"></div>
                <script>
                    function tryRender() {
                        try {
                            if (typeof katex === 'undefined') {
                                setTimeout(tryRender, 50);
                                return;
                            }
                            var container = document.getElementById('math-container');
                            
                            katex.render("$jsonSafeCode", container, {
                                displayMode: true,
                                throwOnError: false
                            });
                            
                            // Wait a tiny bit for the browser to layout the math SVG/HTML
                            setTimeout(function() {
                                reportComplete(container);
                            }, 100);
                            
                        } catch (e) {
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onError("KaTeX Error: " + e.message);
                            }
                        }
                    }

                    function reportComplete(container) {
                        try {
                            var rect = container.getBoundingClientRect();
                            var width = rect.width || container.scrollWidth || 500;
                            var height = rect.height || container.scrollHeight || 100;
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onRenderComplete(Math.ceil(width), Math.ceil(height));
                            }
                        } catch (err) {
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onError("JS Error in reportComplete: " + err.message);
                            }
                        }
                    }
                    
                    window.onload = function() {
                        tryRender();
                        
                        setTimeout(function() {
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onError("Timeout: Rendering took too long.");
                            }
                        }, 5000);
                    };
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        class WebAppInterface {
            @JavascriptInterface
            fun onRenderComplete(width: Int, height: Int) {
                android.util.Log.d("MathRenderer", "onRenderComplete: width=$width, height=$height")
                Handler(Looper.getMainLooper()).post {
                    try {
                        val w = (width + 40).coerceAtMost(2000)
                        val h = (height + 40).coerceAtMost(2000)
                        
                        val fullBitmap = Bitmap.createBitmap(2000, 2000, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(fullBitmap)
                        webView.draw(canvas)
                        
                        val croppedBitmap = Bitmap.createBitmap(fullBitmap, 0, 0, w, h)
                        if (croppedBitmap != fullBitmap) {
                            fullBitmap.recycle()
                        }
                        
                        deferred.complete(croppedBitmap)
                    } catch (e: Exception) {
                        android.util.Log.e("MathRenderer", "Error drawing bitmap: ${e.message}")
                        deferred.complete(null)
                    }
                }
            }

            @JavascriptInterface
            fun onError(error: String) {
                android.util.Log.e("MathRenderer", "onError from JS: $error")
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
                android.util.Log.e("MathRenderer", "WebView Error: ${error?.description}")
                deferred.complete(null)
            }
        }

        webView.measure(
            View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY)
        )
        webView.layout(0, 0, 2000, 2000)

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
            params.leftMargin = -10000
            decorView.addView(webView, params)
        }

        try {
            webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)

            val result = withTimeoutOrNull(8000) {
                deferred.await()
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
