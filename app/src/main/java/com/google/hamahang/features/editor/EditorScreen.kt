package com.google.hamahang.features.editor

import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.google.hamahang.core.bidi.AppLanguage
import com.google.hamahang.core.bidi.AppThemeMode
import com.google.hamahang.core.bidi.Loc
import com.google.hamahang.core.bidi.TextRepairProcessor
import com.google.hamahang.core.pdf.NativePdfExporter
import com.google.hamahang.core.html.HtmlExporter
import com.google.hamahang.theme.CoralEnd
import com.google.hamahang.theme.CoralStart
import com.google.hamahang.theme.RoyalPurple
import java.io.File
import java.io.FileOutputStream
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import com.google.hamahang.core.mermaid.MermaidRenderer
import androidx.compose.ui.platform.LocalUriHandler
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

val PrinterIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Print",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            // M19 8H5c-1.66 0-3 1.34-3 3v6h4v4h12v-4h4v-6c0-1.66-1.34-3-3-3zm-3 11H8v-5h8v5zm3-7c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm-1-9H6v4h12V3z
            moveTo(19f, 8f)
            lineTo(5f, 8f)
            curveTo(3.34f, 8f, 2f, 9.34f, 2f, 11f)
            lineTo(2f, 17f)
            lineTo(6f, 17f)
            lineTo(6f, 21f)
            lineTo(18f, 21f)
            lineTo(18f, 17f)
            lineTo(22f, 17f)
            lineTo(22f, 11f)
            curveTo(22f, 9.34f, 20.66f, 8f, 19f, 8f)
            close()
            moveTo(16f, 19f)
            lineTo(8f, 19f)
            lineTo(8f, 14f)
            lineTo(16f, 14f)
            lineTo(16f, 19f)
            close()
            moveTo(19f, 12f)
            curveTo(18.45f, 12f, 18f, 11.55f, 18f, 11f)
            curveTo(18f, 10.45f, 18.45f, 10f, 19f, 10f)
            curveTo(19.55f, 10f, 20f, 10.45f, 20f, 11f)
            curveTo(20f, 11.55f, 19.55f, 12f, 19f, 12f)
            close()
            moveTo(18f, 3f)
            lineTo(6f, 3f)
            lineTo(6f, 7f)
            lineTo(18f, 7f)
            lineTo(18f, 3f)
            close()
        }
    }.build()

val AlignJustifyIcon: ImageVector
    get() = ImageVector.Builder(
        name = "AlignJustify",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(3f, 21f)
            lineTo(21f, 21f)
            lineTo(21f, 19f)
            lineTo(3f, 19f)
            lineTo(3f, 21f)
            close()
            moveTo(3f, 17f)
            lineTo(21f, 17f)
            lineTo(21f, 15f)
            lineTo(3f, 15f)
            lineTo(3f, 17f)
            close()
            moveTo(3f, 13f)
            lineTo(21f, 13f)
            lineTo(21f, 11f)
            lineTo(3f, 11f)
            lineTo(3f, 13f)
            close()
            moveTo(3f, 9f)
            lineTo(21f, 9f)
            lineTo(21f, 7f)
            lineTo(3f, 7f)
            lineTo(3f, 9f)
            close()
            moveTo(3f, 5f)
            lineTo(21f, 5f)
            lineTo(21f, 3f)
            lineTo(3f, 3f)
            lineTo(3f, 5f)
            close()
        }
    }.build()


@Composable
fun CurveHeader(modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val waveColor = if (isDark) Color(0xFF15172A) else Color(0xFF1B1D36)
    
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(0f, size.height * 0.72f)
            cubicTo(
                size.width * 0.35f, size.height * 1.05f,
                size.width * 0.65f, size.height * 0.45f,
                size.width, size.height * 0.82f
            )
            lineTo(size.width, 0f)
            close()
        }
        drawPath(path = path, color = waveColor)
    }
}

@Composable
fun GradientButton(
    text: String,
    colors: List<Color>,
    onClick: () -> Unit,
    uiFontScale: Float,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.shadow(6.dp, RoundedCornerShape(18.dp))
    ) {
        Box(
            modifier = Modifier
                .background(Brush.horizontalGradient(colors))
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (14.sp * uiFontScale)
            )
        }
    }
}

@Composable
fun FloatingBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    currentLanguage: AppLanguage,
    uiFontScale: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                NavigationItem(Loc.tr("menu_editor", currentLanguage), Icons.Default.Edit, 0),
                NavigationItem(Loc.tr("menu_settings", currentLanguage), Icons.Default.Settings, 1)
            )

            items.forEach { item ->
                val selected = selectedTab == item.index
                val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                val backgroundAlpha = if (selected) 0.12f else 0f
                val backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = backgroundAlpha)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(backgroundColor)
                        .clickable { onTabSelected(item.index) }
                        .padding(horizontal = 22.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        AnimatedVisibility(
                            visible = selected,
                            enter = expandHorizontally() + fadeIn(),
                            exit = shrinkHorizontally() + fadeOut()
                        ) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.label,
                                color = contentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = (13.sp * uiFontScale)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class NavigationItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val index: Int)

@Composable
fun SettingsOptionSelector(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    uiFontScale: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = (14.sp * uiFontScale),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                options.forEachIndexed { index, option ->
                    val selected = selectedIndex == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onOptionSelected(index) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = (13.sp * uiFontScale)
                        )
                    }
                }
            }
        }
    }
}



@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    windowWidthSizeClass: WindowWidthSizeClass,
    sharedText: String? = null,
    
    // hoited states & triggers
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onThemeChange: (AppThemeMode) -> Unit = {},
    currentLanguage: AppLanguage = AppLanguage.FA,
    onLanguageChange: (AppLanguage) -> Unit = {},
    enableNormalization: Boolean = true,
    onNormalizationChange: (Boolean) -> Unit = {},
    uiFontScale: Float = 1.0f,
    onUiFontScaleChange: (Float) -> Unit = {},
    
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var isExportingPdf by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    var navigationTab by remember { mutableStateOf(0) }
    val isImeVisible = WindowInsets.isImeVisible

    var rawText by remember { 
        mutableStateOf(TextFieldValue(sharedText ?: """# معرفی CleanRTL
برنامه CleanRTL برای **اصلاح ترابازبندی متون مخلوط فارسی و انگلیسی** طراحی شده است.

## امکانات کلیدی برنامه:
- پرانتزهای به هم ریخته Kotlin (که در اندروید کاربرد دارند) را اصلاح می‌کند.
- آدرس وب‌سایت‌ها مانند https://google.com را کاملاً راست‌چین و مجزا نگه می‌دارد.
- خروجی با کیفیت بالا به صورت **PDF** و **HTML** تولید می‌کند.

> این یک برنامه کاملاً بومی و متریال ۳ برای سیستم‌عامل اندروید است.

### نمونه کد کاتلین برنامه:
```kotlin
fun repairText(input: String): String {
    return TextRepairProcessor.repairText(input)
}
```""")) 
    }
    
    var correctedText by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) }
    var fontSizeSp by remember { mutableStateOf(16) }
    var isJustified by remember { mutableStateOf(false) }

    val editorScrollState = rememberScrollState()
    val previewScrollState = rememberScrollState()

    LaunchedEffect(editorScrollState.isScrollInProgress, previewScrollState.isScrollInProgress) {
        if (editorScrollState.isScrollInProgress) {
            snapshotFlow { editorScrollState.value }.collect { value ->
                if (editorScrollState.maxValue > 0 && previewScrollState.maxValue > 0) {
                    val progress = value.toFloat() / editorScrollState.maxValue.toFloat()
                    val targetPreviewScroll = (progress * previewScrollState.maxValue).toInt()
                    previewScrollState.scrollTo(targetPreviewScroll)
                }
            }
        } else if (previewScrollState.isScrollInProgress) {
            snapshotFlow { previewScrollState.value }.collect { value ->
                if (previewScrollState.maxValue > 0 && editorScrollState.maxValue > 0) {
                    val progress = value.toFloat() / previewScrollState.maxValue.toFloat()
                    val targetEditorScroll = (progress * editorScrollState.maxValue).toInt()
                    editorScrollState.scrollTo(targetEditorScroll)
                }
            }
        }
    }

    LaunchedEffect(rawText.text, enableNormalization) {
        correctedText = TextRepairProcessor.repairText(rawText.text, enableNormalization)
    }

    fun handleToggleParagraphDirection() {
        val textStr = rawText.text
        val selection = rawText.selection
        if (textStr.isEmpty()) return

        val cursorPosition = selection.start
        
        // Find paragraph boundaries around cursor
        var paraStart = 0
        for (i in (cursorPosition - 1) downTo 0) {
            if (i < textStr.length && textStr[i] == '\n') {
                paraStart = i + 1
                break
            }
        }
        
        var paraEnd = textStr.length
        for (i in cursorPosition until textStr.length) {
            if (i >= 0 && textStr[i] == '\n') {
                paraEnd = i
                break
            }
        }

        if (paraStart > paraEnd) return
        val paragraphText = textStr.substring(paraStart, paraEnd)

        // Remove any existing manual overriding direction marks at start
        val cleanParagraph = paragraphText.removePrefix("\u200E").removePrefix("\u200F")
        
        // Determine whether paragraph is currently RTL or LTR
        val isCurrentRtl = when {
            paragraphText.startsWith("\u200F") -> true
            paragraphText.startsWith("\u200E") -> false
            else -> TextRepairProcessor.isParagraphRtl(paragraphText)
        }

        // Toggle LRM / RLM marks
        val toggledPrefix = if (isCurrentRtl) "\u200E" else "\u200F"
        val newParagraph = toggledPrefix + cleanParagraph

        val newTextStr = textStr.substring(0, paraStart) + newParagraph + textStr.substring(paraEnd)
        
        // Adjust cursor index correctly to avoid shifts or exceptions
        val lengthDiff = newParagraph.length - paragraphText.length
        val newCursor = (cursorPosition + lengthDiff).coerceIn(0, newTextStr.length)
        
        rawText = rawText.copy(
            text = newTextStr,
            selection = TextRange(newCursor)
        )
        
        val directionName = if (isCurrentRtl) "LTR (Left-to-Right)" else "RTL (Right-to-Left)"
        val toastMsg = if (currentLanguage == AppLanguage.FA) {
            "جهت پاراگراف به $directionName تغییر یافت"
        } else {
            "Paragraph direction toggled to $directionName"
        }
        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
    }

    fun handlePrint() {
        val htmlString = HtmlExporter.exportToHtmlString(correctedText, fontSizePx = fontSizeSp, isJustified = isJustified)
        
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                val printAdapter = view.createPrintDocumentAdapter("CleanRTL Document")
                printManager.print("CleanRTL Document", printAdapter, android.print.PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlString, "text/html", "UTF-8", null)
    }

    fun handlePaste() {
        val clip = clipboardManager.getText()
        if (clip != null) {
            rawText = TextFieldValue(clip.text)
            Toast.makeText(context, Loc.tr("toast_pasted", currentLanguage), Toast.LENGTH_SHORT).show()
        }
    }

    fun handleCopy() {
        clipboardManager.setText(AnnotatedString(correctedText))
        Toast.makeText(context, Loc.tr("toast_copied", currentLanguage), Toast.LENGTH_SHORT).show()
    }

    fun handleExportPdf() {
        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                isExportingPdf = true
                
                // 1. Scan for mermaid code blocks
                val paragraphs = correctedText.split("\n")
                val mermaidBlocks = mutableListOf<String>()
                var inMermaid = false
                val currentBlock = StringBuilder()
                
                for (paragraph in paragraphs) {
                    val trimmed = paragraph.trim()
                    val cleanCodeBlockTrim = trimmed.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
                    if (cleanCodeBlockTrim.startsWith("```")) {
                        if (inMermaid) {
                            mermaidBlocks.add(currentBlock.toString())
                            currentBlock.clear()
                            inMermaid = false
                        } else {
                            val rawLang = cleanCodeBlockTrim.substring(3).trim().lowercase()
                            val lang = rawLang.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "")
                            if (lang == "mermaid") {
                                inMermaid = true
                            }
                        }
                    } else if (inMermaid) {
                        if (currentBlock.isNotEmpty()) {
                            currentBlock.append("\n")
                        }
                        currentBlock.append(paragraph)
                    }
                }
                if (inMermaid && currentBlock.isNotEmpty()) {
                    mermaidBlocks.add(currentBlock.toString())
                }

                // 2. Pre-compile all mermaid blocks to bitmaps
                val mermaidBitmaps = mutableMapOf<String, android.graphics.Bitmap>()
                for ((blockIndex, block) in mermaidBlocks.withIndex()) {
                    val bitmap = MermaidRenderer.renderToBitmap(context, block, isDark)
                    if (bitmap != null) {
                        val cleanBlock = block.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
                        mermaidBitmaps[cleanBlock] = bitmap
                        mermaidBitmaps[block] = bitmap
                        // Also store by index as a reliable fallback key
                        mermaidBitmaps["__mermaid_idx_$blockIndex"] = bitmap
                    }
                }
                
                // 3. Pre-compile all math blocks to bitmaps
                val mathBlocks = mutableListOf<String>()
                var inMath = false
                val currentMathBlock = java.lang.StringBuilder()
                for (paragraph in paragraphs) {
                    val trimmed = paragraph.trim()
                    val cleanTrimmed = trimmed
                        .replace(Regex("^[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]+"), "")
                        .replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]+$"), "")
                        .trim()
                    if (cleanTrimmed == "$$") {
                        if (!inMath) {
                            inMath = true
                            currentMathBlock.clear()
                        } else {
                            inMath = false
                            val cleanFormula = currentMathBlock.toString()
                                .replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
                            if (cleanFormula.isNotBlank()) mathBlocks.add(cleanFormula)
                        }
                    } else if (inMath) {
                        if (currentMathBlock.isNotEmpty()) currentMathBlock.append("\n")
                        currentMathBlock.append(paragraph)
                    } else if (cleanTrimmed.startsWith("$$") && cleanTrimmed.endsWith("$$") && cleanTrimmed.length > 2) {
                        val cleanFormula = cleanTrimmed.substring(2, cleanTrimmed.length - 2)
                            .replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
                        if (cleanFormula.isNotBlank()) mathBlocks.add(cleanFormula)
                    } else if (cleanTrimmed.startsWith("$$")) {
                        inMath = true
                        currentMathBlock.clear()
                        val content = cleanTrimmed.substring(2)
                        if (content.isNotEmpty()) currentMathBlock.append(content)
                    } else if (inMath && cleanTrimmed.endsWith("$$")) {
                        inMath = false
                        val content = cleanTrimmed.substring(0, cleanTrimmed.length - 2)
                        if (content.isNotEmpty()) {
                            if (currentMathBlock.isNotEmpty()) currentMathBlock.append("\n")
                            currentMathBlock.append(content)
                        }
                        val cleanFormula = currentMathBlock.toString()
                            .replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
                        if (cleanFormula.isNotBlank()) mathBlocks.add(cleanFormula)
                    }
                }
                
                val mathBitmaps = mutableMapOf<String, android.graphics.Bitmap>()
                for ((blockIndex, block) in mathBlocks.withIndex()) {
                    val bitmap = com.google.hamahang.core.pdf.MathRenderer.renderToBitmap(context, block)
                    if (bitmap != null) {
                        mathBitmaps[block] = bitmap
                        mathBitmaps["__math_idx_$blockIndex"] = bitmap
                    }
                }

                // 4. Pre-download all image bitmaps
                val imageBitmaps = mutableMapOf<String, android.graphics.Bitmap>()
                val imgRegex = Regex("!\\[([^\\]]*)\\]\\(([^\\)]+?)\\)")
                val imageMatches = imgRegex.findAll(correctedText)
                for (match in imageMatches) {
                    val rawUrl = match.groupValues[2].trim()
                    val urlParts = rawUrl.split(Regex("[\\s\\u00A0]+"))
                    val url = urlParts[0].replace("^[\"']".toRegex(), "").replace("[\"']$".toRegex(), "")
                    if (!imageBitmaps.containsKey(url)) {
                        try {
                            val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                kotlinx.coroutines.withTimeoutOrNull(10000L) {
                                    val loader = coil.ImageLoader.Builder(context)
                                        .components {
                                            add(coil.decode.SvgDecoder.Factory())
                                        }
                                        .build()
                                    val request = coil.request.ImageRequest.Builder(context)
                                        .data(url)
                                        .allowHardware(false)
                                        .build()
                                    val drawable = (loader.execute(request) as? coil.request.SuccessResult)?.drawable
                                    if (drawable is android.graphics.drawable.BitmapDrawable) {
                                        drawable.bitmap
                                    } else if (drawable != null) {
                                        val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 300
                                        val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 80
                                        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                                        val cv = android.graphics.Canvas(bmp)
                                        drawable.setBounds(0, 0, w, h)
                                        drawable.draw(cv)
                                        bmp
                                    } else null
                                }
                            }
                            if (bitmap != null) {
                                imageBitmaps[url] = bitmap
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("PdfExport", "Failed to load image: $url", e)
                        }
                    }
                }

                // 5. Render and save the PDF
                val downloadsFolder = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val pdfFile = getUniqueFile(downloadsFolder, "CleanRTL_Corrected", "pdf")
                val outputStream = java.io.FileOutputStream(pdfFile)
                
                com.google.hamahang.core.pdf.NativePdfExporter.exportToPdf(
                    context = context,
                    text = correctedText,
                    outputStream = outputStream,
                    title = "CleanRTL Document",
                    baseFontSize = fontSizeSp.toFloat(),
                    mermaidBitmaps = mermaidBitmaps,
                    mathBitmaps = mathBitmaps,
                    imageBitmaps = imageBitmaps,
                    isJustified = isJustified
                )
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "${Loc.tr("toast_pdf_saved", currentLanguage)} (${pdfFile.name})", Toast.LENGTH_LONG).show()
                }
            } catch (e: java.lang.Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                isExportingPdf = false
            }
        }
    }

    fun handleExportHtml() {
        try {
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val htmlFile = getUniqueFile(downloadsFolder, "CleanRTL_Corrected", "html")
            val outputStream = FileOutputStream(htmlFile)
            
            HtmlExporter.exportToHtml(
                text = correctedText,
                outputStream = outputStream,
                title = "CleanRTL Web Document",
                fontSizePx = fontSizeSp,
                isJustified = isJustified
            )
            Toast.makeText(context, "${Loc.tr("toast_html_saved", currentLanguage)} (${htmlFile.name})", Toast.LENGTH_SHORT).show()
        } catch (e: java.lang.Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val layoutDirection = if (currentLanguage == AppLanguage.FA) LayoutDirection.Rtl else LayoutDirection.Ltr
    
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        if (isExportingPdf) {
            Dialog(onDismissRequest = {}) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.FA) "در حال تولید فایل PDF و رندر گراف‌ها..." else "Generating PDF and rendering graphs...",
                            style = TextStyle(fontSize = 14.sp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .systemBarsPadding()
                .imePadding()
        ) {
            AnimatedVisibility(
                visible = !isImeVisible,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                CurveHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (isImeVisible) 16.dp else 96.dp)
            ) {
                AnimatedVisibility(visible = !isImeVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = Loc.tr("app_title", currentLanguage),
                            style = TextStyle(
                                fontSize = (26.sp * uiFontScale),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                        
                        if (navigationTab == 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { rawText = TextFieldValue("") },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.2f),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = Loc.tr("clear_desc", currentLanguage),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    if (navigationTab == 0) {
                        // WORKSPACE
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp)
                        ) {
                            AnimatedVisibility(visible = !isImeVisible) {
                                Column {
                                    // Row 1: Paste and Copy strictly text-only
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = { handlePaste() },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        ) {
                                            Text(
                                                text = Loc.tr("btn_paste", currentLanguage), 
                                                fontSize = (14.sp * uiFontScale), 
                                                fontWeight = FontWeight.Bold, 
                                                maxLines = 1
                                            )
                                        }
                                        Button(
                                            onClick = { handleCopy() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant, 
                                                contentColor = MaterialTheme.colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        ) {
                                            Text(
                                                text = Loc.tr("btn_copy", currentLanguage), 
                                                fontSize = (14.sp * uiFontScale), 
                                                fontWeight = FontWeight.Bold, 
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    
                                    // Row 2: PDF and HTML strictly text-only gradient buttons
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        GradientButton(
                                            text = Loc.tr("btn_pdf", currentLanguage),
                                            colors = listOf(CoralStart, CoralEnd),
                                            onClick = { handleExportPdf() },
                                            uiFontScale = uiFontScale,
                                            modifier = Modifier.weight(1f)
                                        )
                                        GradientButton(
                                            text = Loc.tr("btn_html", currentLanguage),
                                            colors = listOf(RoyalPurple, MaterialTheme.colorScheme.primary),
                                            onClick = { handleExportHtml() },
                                            uiFontScale = uiFontScale,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${Loc.tr("font_size_label", currentLanguage)}: $fontSizeSp sp",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (14.sp * uiFontScale),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Custom Pilcrow Paragraph Direction Switch Button
                                        IconButton(
                                            onClick = { handleToggleParagraphDirection() },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Text("¶", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Toggle Paragraph Direction",
                                                    modifier = Modifier.size(10.dp),
                                                    tint = Color(0xFF7C83FD)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { isJustified = !isJustified },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = if (isJustified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (isJustified) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Text("≡", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        IconButton(
                                            onClick = { if (fontSizeSp > 10) fontSizeSp-- },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        IconButton(
                                            onClick = { if (fontSizeSp < 32) fontSizeSp++ },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                            if (windowWidthSizeClass == WindowWidthSizeClass.Expanded) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f)
                                        .padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        EditorPane(
                                            title = Loc.tr("input_label", currentLanguage),
                                            value = rawText,
                                            onValueChange = { rawText = it },
                                            textDirection = TextDirection.ContentOrRtl,
                                            fontSize = fontSizeSp,
                                            uiFontScale = uiFontScale,
                                            scrollState = editorScrollState
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        MarkdownPreviewPane(
                                            title = Loc.tr("preview_label", currentLanguage),
                                            text = correctedText, 
                                            baseFontSize = fontSizeSp,
                                            uiFontScale = uiFontScale,
                                            isJustified = isJustified,
                                            scrollState = previewScrollState
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    val tabLabels = listOf(
                                        Loc.tr("editor_tab_raw", currentLanguage),
                                        Loc.tr("editor_tab_corrected", currentLanguage),
                                        Loc.tr("editor_tab_preview", currentLanguage)
                                    )
                                    tabLabels.forEachIndexed { index, label ->
                                        val selected = activeTab == index
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                .clickable { activeTab = index }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = (12.sp * uiFontScale)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f)
                                        .padding(bottom = 12.dp)
                                ) {
                                    when (activeTab) {
                                        0 -> EditorPane(
                                            title = Loc.tr("input_label", currentLanguage),
                                            value = rawText,
                                            onValueChange = { rawText = it },
                                            textDirection = TextDirection.ContentOrRtl,
                                            fontSize = fontSizeSp,
                                            uiFontScale = uiFontScale,
                                            scrollState = editorScrollState
                                        )
                                        1 -> EditorPane(
                                            title = Loc.tr("output_label", currentLanguage),
                                            value = TextFieldValue(correctedText),
                                            onValueChange = {},
                                            textDirection = TextDirection.ContentOrRtl,
                                            fontSize = fontSizeSp,
                                            uiFontScale = uiFontScale,
                                            isReadOnly = true
                                        )
                                        2 -> MarkdownPreviewPane(
                                            title = Loc.tr("preview_label", currentLanguage),
                                            text = correctedText, 
                                            baseFontSize = fontSizeSp,
                                            uiFontScale = uiFontScale,
                                            isJustified = isJustified,
                                            scrollState = previewScrollState
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // SETTINGS
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = Loc.tr("settings_title", currentLanguage),
                                fontSize = (20.sp * uiFontScale),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
                            )

                            // 1. Language Toggle
                            SettingsOptionSelector(
                                title = Loc.tr("settings_lang", currentLanguage),
                                options = listOf("فارسی", "English"),
                                selectedIndex = if (currentLanguage == AppLanguage.FA) 0 else 1,
                                onOptionSelected = { index ->
                                    val lang = if (index == 0) AppLanguage.FA else AppLanguage.EN
                                    onLanguageChange(lang)
                                },
                                uiFontScale = uiFontScale
                            )

                            // 2. Theme Toggle
                            val themeOptions = listOf(
                                Loc.tr("theme_system", currentLanguage),
                                Loc.tr("theme_light", currentLanguage),
                                Loc.tr("theme_dark", currentLanguage)
                            )
                            SettingsOptionSelector(
                                title = Loc.tr("settings_theme", currentLanguage),
                                options = themeOptions,
                                selectedIndex = when (themeMode) {
                                    AppThemeMode.SYSTEM -> 0
                                    AppThemeMode.LIGHT -> 1
                                    AppThemeMode.DARK -> 2
                                },
                                onOptionSelected = { index ->
                                    val mode = when (index) {
                                        0 -> AppThemeMode.SYSTEM
                                        1 -> AppThemeMode.LIGHT
                                        else -> AppThemeMode.DARK
                                    }
                                    onThemeChange(mode)
                                },
                                uiFontScale = uiFontScale
                            )

                            // 3. UI Font Scale Selector
                            val uiScaleOptions = if (currentLanguage == AppLanguage.FA) {
                                listOf("کوچک", "متوسط", "بزرگ", "خیلی بزرگ")
                            } else {
                                listOf("Small", "Medium", "Large", "X-Large")
                            }
                            val uiScaleValues = listOf(0.85f, 1.0f, 1.15f, 1.3f)
                            val selectedScaleIndex = uiScaleValues.indexOf(uiFontScale).let { if (it == -1) 1 else it }

                            SettingsOptionSelector(
                                title = Loc.tr("settings_ui_font_scale", currentLanguage),
                                options = uiScaleOptions,
                                selectedIndex = selectedScaleIndex,
                                onOptionSelected = { index ->
                                    onUiFontScaleChange(uiScaleValues[index])
                                },
                                uiFontScale = uiFontScale
                            )

                            // 4. Normalization Toggle
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = Loc.tr("norm_switch_label", currentLanguage),
                                        fontSize = (13.sp * uiFontScale),
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                                    )
                                    Switch(
                                        checked = enableNormalization,
                                        onCheckedChange = { onNormalizationChange(it) }
                                    )
                                }
                            }

                            // 5. Elegant About Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Brush.horizontalGradient(listOf(CoralStart, CoralEnd))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = "About icon",
                                                tint = Color.White
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = Loc.tr("about_title", currentLanguage),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = (16.sp * uiFontScale),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "v2.5.0 • CleanRTL App",
                                                fontSize = (11.sp * uiFontScale),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = Loc.tr("about_desc", currentLanguage),
                                        fontSize = (13.sp * uiFontScale),
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = !isImeVisible,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                FloatingBottomNavigation(
                    selectedTab = navigationTab,
                    onTabSelected = { navigationTab = it },
                    currentLanguage = currentLanguage,
                    uiFontScale = uiFontScale,
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
        }
    }
}

private fun getUniqueFile(directory: File, baseName: String, extension: String): File {
    var file = File(directory, "$baseName.$extension")
    var counter = 1
    while (file.exists()) {
        file = File(directory, "${baseName}_$counter.$extension")
        counter++
    }
    return file
}

@Composable
fun MarkdownPreviewPane(
    title: String,
    text: String,
    baseFontSize: Int,
    uiFontScale: Float,
    isJustified: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontSize = (12.sp * uiFontScale),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            MarkdownPreviewPaneContents(
                text = text,
                baseFontSize = baseFontSize,
                uiFontScale = uiFontScale,
                isJustified = isJustified
            )
        }
    }
}

@Composable
fun MarkdownPreviewPaneContents(
    text: String,
    baseFontSize: Int,
    uiFontScale: Float,
    isJustified: Boolean = false
) {
    val rawLines = text.split("\n")
    val mergedLines = mutableListOf<String>()
    val currentPara = StringBuilder()
    var inMath = false
    var inCode = false
    for (line in rawLines) {
        val noCr = line.removeSuffix("\r")
        val cleanTrimmed = noCr.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
        if (cleanTrimmed.startsWith("```")) {
            inCode = !inCode
        }
        if (cleanTrimmed == "$$" || (cleanTrimmed.startsWith("$$") && !cleanTrimmed.endsWith("$$"))) {
            inMath = !inMath
        }
        
        if (inMath || inCode) {
            if (currentPara.isNotEmpty()) {
                mergedLines.add(currentPara.toString())
                currentPara.clear()
            }
            mergedLines.add(noCr)
            continue
        }
        
        if (noCr.endsWith("\\") && !noCr.endsWith("\\\\")) {
            currentPara.append(noCr.removeSuffix("\\")).append("\n")
        } else if (noCr.endsWith("  ")) {
            currentPara.append(noCr.removeSuffix("  ")).append("\n")
        } else {
            currentPara.append(noCr)
            mergedLines.add(currentPara.toString())
            currentPara.clear()
        }
    }
    if (currentPara.isNotEmpty()) {
        mergedLines.add(currentPara.toString())
    }

    val paragraphs = mutableListOf<String>()
    val referenceMap = mutableMapOf<String, Pair<String, String?>>()
    val footnoteMap = mutableMapOf<String, String>()

    val refDefRegex = Regex("""^\s*\[([^\]]+)\]:\s*(\S+)(?:\s+["'(]([^"')]*)["'))]?)?\s*$""")
    val footnoteDefRegex = Regex("""^\s*\[\^([^\]]+)\]:\s*(.*)$""")

    for (p in mergedLines) {
        val cleanP = p.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
        val footnoteMatch = footnoteDefRegex.matchEntire(cleanP)
        val match = refDefRegex.matchEntire(cleanP)
        
        if (footnoteMatch != null) {
            val label = footnoteMatch.groupValues[1].trim()
            val footnoteText = footnoteMatch.groupValues[2].trim()
            footnoteMap[label] = footnoteText
        } else if (match != null && !match.groupValues[1].startsWith("^")) {
            val label = match.groupValues[1].trim().lowercase()
            val url = match.groupValues[2].trim()
            val title = match.groupValues[3].trim().takeIf { it.isNotEmpty() }
            referenceMap[label] = Pair(url, title)
        } else {
            paragraphs.add(p)
        }
    }

    var inCodeBlock = false
    val codeLines = mutableListOf<String>()
    var inMathBlock = false
    val mathLines = mutableListOf<String>()
    var inMermaidBlock = false
    val mermaidLines = mutableListOf<String>()
    var inDetailsBlock = false
    val detailsLines = mutableListOf<String>()

    var idx = 0
    while (idx < paragraphs.size) {
        val paragraph = paragraphs[idx]

        if (inDetailsBlock) {
            val cleanTrimmed = paragraph.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
            if (cleanTrimmed.lowercase().contains("</details>")) {
                val cleanLine = cleanTrimmed.replace(Regex("(?i)</details>"), "").trim()
                if (cleanLine.isNotEmpty()) {
                    detailsLines.add(cleanLine)
                }
                val fullContent = detailsLines.joinToString("\n")
                var summaryText = "Details"
                var contentText = fullContent
                val summaryRegex = Regex("(?is)<summary>(.*?)</summary>")
                val summaryMatch = summaryRegex.find(fullContent)
                if (summaryMatch != null) {
                    summaryText = summaryMatch.groupValues[1].trim()
                    contentText = fullContent.replace(summaryMatch.value, "").trim()
                }
                MarkdownDetails(
                    summary = summaryText,
                    content = contentText,
                    baseFontSize = (baseFontSize * uiFontScale).sp,
                    uiFontScale = uiFontScale,
                    referenceMap = referenceMap,
                    isJustified = isJustified
                )
                detailsLines.clear()
                inDetailsBlock = false
            } else {
                detailsLines.add(paragraph)
            }
            idx++
            continue
        }

        if (inMermaidBlock) {
            val cleanTrimmed = paragraph.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
            if (cleanTrimmed.startsWith("```")) {
                val fullMermaid = mermaidLines.joinToString("\n")
                ComposeMermaidBlock(code = fullMermaid)
                mermaidLines.clear()
                inMermaidBlock = false
            } else {
                mermaidLines.add(paragraph)
            }
            idx++
            continue
        }

        if (inMathBlock) {
            val trimmed = paragraph.trim()
            val cleanTrimmed = trimmed
                .replace(Regex("^[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]+"), "")
                .replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]+$"), "")
                .trim()
            if (cleanTrimmed.endsWith("$$")) {
                val cleanLine = cleanTrimmed.removeSuffix("$$")
                if (cleanLine.isNotEmpty()) {
                    mathLines.add(cleanLine)
                }
                val fullFormula = mathLines.filter { it.isNotBlank() }.joinToString("\n")
                ComposeMathBlock(formula = fullFormula, fontSize = (baseFontSize * 1.1).sp)
                mathLines.clear()
                inMathBlock = false
            } else {
                mathLines.add(paragraph)
            }
            idx++
            continue
        }

        if (inCodeBlock) {
            val cleanTrimmed = paragraph.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
            if (cleanTrimmed.startsWith("```") || cleanTrimmed.startsWith("~~~")) {
                ComposeCodeBlock(lines = codeLines, fontSize = (baseFontSize * 0.85).sp)
                codeLines.clear()
                inCodeBlock = false
            } else {
                codeLines.add(paragraph)
            }
            idx++
            continue
        }

        if (paragraph.isBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            idx++
            continue
        }

        // Extract bidi override mark if present at the start of the paragraph
        val bidiPrefix = when {
            paragraph.startsWith("\u200F") -> "\u200F"
            paragraph.startsWith("\u200E") -> "\u200E"
            else -> ""
        }
        val cleanParagraph = if (bidiPrefix.isNotEmpty()) paragraph.substring(1) else paragraph
        val trimmed = cleanParagraph.trim()
        
        val bidiChars = setOf(
            '\u200E', '\u200F', '\u202A', '\u202B', '\u202C', '\u202D', '\u202E',
            '\u2066', '\u2067', '\u2068', '\u2069', '\u200C', '\u200D'
        )
        var indentCount = 0
        for (char in cleanParagraph) {
            if (char == ' ') {
                indentCount++
            } else if (char == '\t') {
                indentCount += 4
            } else if (char in bidiChars) {
                continue
            } else {
                break
            }
        }
        val listLevel = indentCount / 2

        val isIndentedCode = (paragraph.startsWith("    ") || paragraph.startsWith("\t")) &&
                !trimmed.startsWith("- ") && !trimmed.startsWith("* ") && !trimmed.startsWith("• ") &&
                !Regex("^[0-9]+\\.").containsMatchIn(trimmed) && !trimmed.startsWith(">")
        if (isIndentedCode) {
            val indentedLines = mutableListOf<String>()
            var k = idx
            while (k < paragraphs.size) {
                val pLine = paragraphs[k]
                if (pLine.startsWith("    ")) {
                    indentedLines.add(pLine.substring(4))
                } else if (pLine.startsWith("\t")) {
                    indentedLines.add(pLine.substring(1))
                } else if (pLine.isBlank() && k + 1 < paragraphs.size && (paragraphs[k + 1].startsWith("    ") || paragraphs[k + 1].startsWith("\t"))) {
                    indentedLines.add("")
                } else {
                    break
                }
                k++
            }
            if (indentedLines.isNotEmpty()) {
                ComposeCodeBlock(lines = indentedLines, fontSize = (baseFontSize * 0.85).sp)
                idx = k
                continue
            }
        }

        // Robustly strip any leading/trailing bidi control characters for math block checks
        val bidiRegex = Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069\\u200C\\u200D\\uFEFF]+")
        val cleanTrimmed = trimmed
            .replace(Regex("^[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]+"), "")
            .replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]+$"), "")
            .trim()
        // Strip ALL bidi chars from everywhere — use for pattern matching only, not for displayed text
        val fullyCleanTrimmed = trimmed.replace(bidiRegex, "").trim()

        val cleanTrimmedLower = cleanTrimmed.lowercase()
        if (cleanTrimmedLower.startsWith("<details")) {
            val contentAfterTag = cleanTrimmed.replace(Regex("(?i)^<details[^>]*>"), "").trim()
            if (cleanTrimmedLower.contains("</details>")) {
                val fullContent = contentAfterTag.replace(Regex("(?i)</details>$"), "").trim()
                var summaryText = "Details"
                var contentText = fullContent
                val summaryRegex = Regex("(?is)<summary>(.*?)</summary>")
                val summaryMatch = summaryRegex.find(fullContent)
                if (summaryMatch != null) {
                    summaryText = summaryMatch.groupValues[1].trim()
                    contentText = fullContent.replace(summaryMatch.value, "").trim()
                }
                MarkdownDetails(
                    summary = summaryText,
                    content = contentText,
                    baseFontSize = (baseFontSize * uiFontScale).sp,
                    uiFontScale = uiFontScale,
                    referenceMap = referenceMap,
                    isJustified = isJustified
                )
                idx++
                continue
            } else {
                inDetailsBlock = true
                if (contentAfterTag.isNotEmpty()) {
                    detailsLines.add(contentAfterTag)
                }
                idx++
                continue
            }
        }

        // Check if this line starts a math block
        if (cleanTrimmed.startsWith("$$")) {
            if (cleanTrimmed.endsWith("$$") && cleanTrimmed.length > 2) {
                val cleanFormula = cleanTrimmed.removePrefix("$$").removeSuffix("$$").trim()
                ComposeMathBlock(formula = cleanFormula, fontSize = (baseFontSize * 1.1).sp)
            } else {
                inMathBlock = true
                val cleanLine = cleanTrimmed.removePrefix("$$")
                if (cleanLine.isNotEmpty()) {
                    mathLines.add(cleanLine)
                }
            }
            idx++
            continue
        }

        // HTML Pre block (<pre><code>...</code></pre>)
        if (cleanTrimmedLower.startsWith("<pre")) {
            val preLines = mutableListOf<String>()
            var k = idx
            while (k < paragraphs.size) {
                val line = paragraphs[k]
                preLines.add(line)
                if (line.trim().lowercase().contains("</pre>")) {
                    k++
                    break
                }
                k++
            }
            val rawPre = preLines.joinToString("\n")
            val codeOnly = rawPre
                .replace(Regex("(?is)^<pre[^>]*>"), "")
                .replace(Regex("(?is)</pre>$"), "")
                .replace(Regex("(?is)^<code[^>]*>"), "")
                .replace(Regex("(?is)</code>$"), "")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .trim()
            val lines = codeOnly.split("\n")
            ComposeCodeBlock(lines = lines, fontSize = (baseFontSize * 0.85).sp)
            idx = k
            continue
        }

        // Check if this line starts an HTML table
        if (cleanTrimmedLower.startsWith("<table")) {
            val tableLines = mutableListOf<String>()
            var k = idx
            while (k < paragraphs.size) {
                val line = paragraphs[k]
                tableLines.add(line)
                if (line.trim().lowercase().contains("</table>")) {
                    k++
                    break
                }
                k++
            }
            val parsed = parseHtmlTable(tableLines.joinToString("\n"))
            if (parsed != null) {
                MarkdownTable(
                    headerColumns = parsed.headerColumns,
                    dataRows = parsed.dataRows,
                    alignments = parsed.alignments,
                    baseFontSize = baseFontSize,
                    referenceMap = referenceMap
                )
                idx = k
                continue
            }
        }

        // Check if this line is an HTML Progress Bar: <progress value="..." max="...">
        if (cleanTrimmedLower.startsWith("<progress")) {
            val progressMatch = Regex("""(?i)<progress\s+value=["']?(\d+(?:\.\d+)?)["']?(?:\s+max=["']?(\d+(?:\.\d+)?)["']?)?[^>]*>""").find(cleanTrimmed)
            if (progressMatch != null) {
                val value = progressMatch.groupValues[1].toFloatOrNull() ?: 0f
                val max = progressMatch.groupValues[2].toFloatOrNull().takeIf { it != null && it > 0 } ?: 100f
                MarkdownProgressBar(value = value, max = max)
                idx++
                continue
            }
        }

        // Check if this line is an HTML div / center / p alignment container
        val isAlignmentBlock = cleanTrimmedLower.startsWith("<div") || 
                               cleanTrimmedLower.startsWith("<center") || 
                               (cleanTrimmedLower.startsWith("<p") && (cleanTrimmedLower.contains("align=") || cleanTrimmedLower.contains("text-align") || cleanTrimmedLower.contains("style=")))
        if (isAlignmentBlock) {
            val divStyle = parseDivBoxStyle(cleanTrimmed)
            val isClosingSameLine = cleanTrimmedLower.contains("</div>") || cleanTrimmedLower.contains("</center>") || cleanTrimmedLower.contains("</p>")
            val divContentLines = mutableListOf<String>()

            if (isClosingSameLine) {
                val inner = cleanTrimmed
                    .replace(Regex("(?i)^<[\\s\\u00A0]*(div[^>]*|center|p[^>]*)[\\s\\u00A0]*>"), "")
                    .replace(Regex("(?i)<[\\s\\u00A0]*/[\\s\\u00A0]*(div|center|p)[\\s\\u00A0]*>$"), "")
                    .trim()
                if (inner.isNotEmpty()) {
                    inner.split(Regex("(?i)<br\\s*/?>")).forEach {
                        if (it.isNotBlank()) divContentLines.add(it.trim())
                    }
                }
                idx++
            } else {
                var k = idx + 1
                while (k < paragraphs.size) {
                    val line = paragraphs[k]
                    val lineClean = line.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim().lowercase()
                    if (lineClean.startsWith("</div") || lineClean.startsWith("</center") || lineClean.startsWith("</p") ||
                        lineClean.contains("</div>") || lineClean.contains("</center>") || lineClean.contains("</p>")) {
                        k++
                        break
                    }
                    if (line.isNotBlank()) {
                        line.split(Regex("(?i)<br\\s*/?>")).forEach {
                            if (it.isNotBlank()) divContentLines.add(it.trim())
                        }
                    }
                    k++
                }
                idx = k
            }

            if (divStyle.isStyledBox) {
                ComposeStyledBox(
                    style = divStyle,
                    lines = divContentLines,
                    baseFontSize = baseFontSize,
                    uiFontScale = uiFontScale,
                    referenceMap = referenceMap,
                    isJustified = isJustified
                )
            } else {
                // Render the lines of the alignment container with inner block support
                var dIdx = 0
                while (dIdx < divContentLines.size) {
                    val dLine = divContentLines[dIdx]
                    val dClean = dLine.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
                    val dLower = dClean.lowercase()

                    // 1. HTML table inside container
                    if (dLower.startsWith("<table")) {
                        val tableLines = mutableListOf<String>()
                        var tk = dIdx
                        while (tk < divContentLines.size) {
                            val tLine = divContentLines[tk]
                            tableLines.add(tLine)
                            if (tLine.trim().lowercase().contains("</table>")) {
                                tk++
                                break
                            }
                            tk++
                        }
                        val parsed = parseHtmlTable(tableLines.joinToString("\n"))
                        if (parsed != null) {
                            MarkdownTable(
                                headerColumns = parsed.headerColumns,
                                dataRows = parsed.dataRows,
                                alignments = parsed.alignments,
                                baseFontSize = baseFontSize,
                                referenceMap = referenceMap
                            )
                            dIdx = tk
                            continue
                        }
                    }

                    // 2. Markdown table inside container
                    if (dClean.startsWith("|") && dClean.endsWith("|")) {
                        if (dIdx + 1 < divContentLines.size) {
                            val nextLine = divContentLines[dIdx + 1]
                            val nextClean = nextLine.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
                            if (isTableDivider(nextClean)) {
                                val headerCols = parseTableLine(dClean)
                                val dividerCols = parseTableLine(nextClean)
                                val alignments = dividerCols.map { parseAlignment(it) }
                                val dataRows = mutableListOf<List<String>>()
                                var rk = dIdx + 2
                                while (rk < divContentLines.size) {
                                    val rLine = divContentLines[rk]
                                    val rClean = rLine.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
                                    if (rClean.startsWith("|") && rClean.endsWith("|")) {
                                        dataRows.add(parseTableLine(rClean))
                                        rk++
                                    } else break
                                }
                                MarkdownTable(
                                    headerColumns = headerCols,
                                    dataRows = dataRows,
                                    alignments = alignments,
                                    baseFontSize = baseFontSize,
                                    referenceMap = referenceMap
                                )
                                dIdx = rk
                                continue
                            }
                        }
                    }

                    // 3. Progress bar
                    if (dLower.startsWith("<progress")) {
                        val progressMatch = Regex("""(?i)<progress\s+value=["']?(\d+(?:\.\d+)?)["']?(?:\s+max=["']?(\d+(?:\.\d+)?)["']?)?[^>]*>""").find(dClean)
                        if (progressMatch != null) {
                            val value = progressMatch.groupValues[1].toFloatOrNull() ?: 0f
                            val max = progressMatch.groupValues[2].toFloatOrNull().takeIf { it != null && it > 0 } ?: 100f
                            MarkdownProgressBar(value = value, max = max)
                            dIdx++
                            continue
                        }
                    }

                    // 4. Linked image block: [![alt](img)](link)
                    if (dClean.matches(Regex("^\\[!\\[([^\\]]*)\\]\\(([^\\)]+)\\)\\]\\(([^\\)]+)\\)$"))) {
                        val match = Regex("^\\[!\\[([^\\]]*)\\]\\(([^\\)]+)\\)\\]\\(([^\\)]+)\\)$").find(dClean)!!
                        val alt = match.groupValues[1]
                        val imgUrl = match.groupValues[2].trim()
                        val linkUrl = match.groupValues[3].trim()
                        MarkdownImage(url = imgUrl, alt = alt, linkUrl = linkUrl)
                        dIdx++
                        continue
                    }

                    // 5. Image block: ![alt](url)
                    if (dClean.startsWith("![") && dClean.contains("](") && !dClean.startsWith("[![")) {
                        val altStart = 2
                        val altEnd = dClean.indexOf("](")
                        val alt = if (altEnd > altStart) dClean.substring(altStart, altEnd) else ""
                        val remaining = dClean.substring(altEnd + 2)
                        val urlEnd = remaining.indexOfFirst { it == ')' || it == ' ' || it == '\t' || it == '"' || it == '\'' }
                        val imgUrl = if (urlEnd != -1) remaining.substring(0, urlEnd).trim() else remaining.removeSuffix(")").trim()
                        if (imgUrl.isNotEmpty()) MarkdownImage(url = imgUrl, alt = alt, linkUrl = null)
                        dIdx++
                        continue
                    }

                    // 6. Header
                    if (dClean.startsWith("#")) {
                        val hLevel = dClean.takeWhile { it == '#' }.length
                        val hText = dClean.substring(hLevel).trim()
                        val hSize = when (hLevel) {
                            1 -> (baseFontSize * 1.5).sp
                            2 -> (baseFontSize * 1.3).sp
                            3 -> (baseFontSize * 1.15).sp
                            4 -> (baseFontSize * 1.05).sp
                            5 -> (baseFontSize * 0.95).sp
                            else -> (baseFontSize * 0.85).sp
                        }
                        MarkdownHeader(text = hText, size = hSize, weight = FontWeight.Bold, referenceMap = referenceMap)
                        dIdx++
                        continue
                    }

                    // 7. Divider
                    if (dClean == "---" || dClean == "***" || dClean == "___") {
                        MarkdownDivider()
                        dIdx++
                        continue
                    }

                    // 8. Lone closing or empty tag - skip
                    if (dLower == "</div>" || dLower == "</center>" || dLower == "</p>" || dLower == "<p>" || dClean.isEmpty()) {
                        dIdx++
                        continue
                    }

                    // 9. Standard MarkdownParagraph with container's explicit alignment
                    MarkdownParagraph(
                        text = dLine,
                        fontSize = (baseFontSize * uiFontScale).sp,
                        referenceMap = referenceMap,
                        isJustified = isJustified,
                        explicitAlignment = divStyle.textAlign
                    )
                    dIdx++
                }
            }
            continue
        }
        if (cleanTrimmedLower.startsWith("</div") || cleanTrimmedLower.startsWith("</center") || cleanTrimmedLower.startsWith("</p") || cleanTrimmedLower == "<p>" || cleanTrimmedLower == "</p>") {
            idx++
            continue
        }

        // Check if this line starts a table
        if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
            if (idx + 1 < paragraphs.size) {
                val nextLine = paragraphs[idx + 1]
                val nextBidiPrefix = when {
                    nextLine.startsWith("\u200F") -> "\u200F"
                    nextLine.startsWith("\u200E") -> "\u200E"
                    else -> ""
                }
                val nextClean = if (nextBidiPrefix.isNotEmpty()) nextLine.substring(1) else nextLine
                val nextTrimmed = nextClean.trim()
                
                if (isTableDivider(nextTrimmed)) {
                    val headerCols = parseTableLine(cleanParagraph)
                    val dividerCols = parseTableLine(nextTrimmed)
                    val alignments = dividerCols.map { parseAlignment(it) }
                    
                    val dataRows = mutableListOf<List<String>>()
                    var k = idx + 2
                    while (k < paragraphs.size) {
                        val rowLine = paragraphs[k]
                        val rowBidiPrefix = when {
                            rowLine.startsWith("\u200F") -> "\u200F"
                            rowLine.startsWith("\u200E") -> "\u200E"
                            else -> ""
                        }
                        val rowClean = if (rowBidiPrefix.isNotEmpty()) rowLine.substring(1) else rowLine
                        val rowTrimmed = rowClean.trim()
                        
                        if (rowTrimmed.startsWith("|") && rowTrimmed.endsWith("|") && !isTableDivider(rowTrimmed)) {
                            val cells = parseTableLine(rowClean)
                            dataRows.add(cells)
                            k++
                        } else {
                            break
                        }
                    }
                    
                    MarkdownTable(
                        headerColumns = headerCols,
                        dataRows = dataRows,
                        alignments = alignments,
                        baseFontSize = baseFontSize,
                        referenceMap = referenceMap
                    )
                    
                    idx = k
                    continue
                }
            }
        }

        val cleanBlockTrimmed = trimmed.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
        if (cleanBlockTrimmed.startsWith("```") || cleanBlockTrimmed.startsWith("~~~")) {
            val rawLang = cleanBlockTrimmed.substring(3).trim().lowercase()
            val lang = rawLang.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "")
            if (lang == "mermaid") {
                inMermaidBlock = true
            } else {
                inCodeBlock = true
            }
            idx++
            continue
        }

        val numberedListMatch = Regex("^(([a-zA-Z0-9]+)\\.)\\s+(.*)").matchEntire(trimmed)

        when {
            trimmed.startsWith("# ") -> {
                MarkdownHeader(text = bidiPrefix + trimmed.substring(2), size = (baseFontSize * 1.5).sp, weight = FontWeight.Bold, referenceMap = referenceMap)
            }
            trimmed.startsWith("## ") -> {
                MarkdownHeader(text = bidiPrefix + trimmed.substring(3), size = (baseFontSize * 1.3).sp, weight = FontWeight.Bold, referenceMap = referenceMap)
            }
            trimmed.startsWith("### ") -> {
                MarkdownHeader(text = bidiPrefix + trimmed.substring(4), size = (baseFontSize * 1.15).sp, weight = FontWeight.Bold, referenceMap = referenceMap)
            }
            trimmed.startsWith("#### ") -> {
                MarkdownHeader(text = bidiPrefix + trimmed.substring(5), size = (baseFontSize * 1.05).sp, weight = FontWeight.Bold, referenceMap = referenceMap)
            }
            trimmed.startsWith("##### ") -> {
                MarkdownHeader(text = bidiPrefix + trimmed.substring(6), size = (baseFontSize * 0.95).sp, weight = FontWeight.Bold, referenceMap = referenceMap)
            }
            trimmed.startsWith("###### ") -> {
                MarkdownHeader(text = bidiPrefix + trimmed.substring(7), size = (baseFontSize * 0.85).sp, weight = FontWeight.Bold, referenceMap = referenceMap)
            }
            // Task list: - [x] / - [ ]
            (fullyCleanTrimmed.startsWith("- [x] ") || fullyCleanTrimmed.startsWith("- [X] ") ||
             fullyCleanTrimmed.startsWith("* [x] ") || fullyCleanTrimmed.startsWith("* [X] ")) -> {
                MarkdownCheckboxItem(text = bidiPrefix + com.google.hamahang.core.bidi.TextRepairProcessor.stripPrefixKeepingBidi(trimmed, 6), checked = true, fontSize = baseFontSize.sp, level = listLevel, referenceMap = referenceMap)
            }
            (fullyCleanTrimmed.startsWith("- [ ] ") || fullyCleanTrimmed.startsWith("* [ ] ")) -> {
                MarkdownCheckboxItem(text = bidiPrefix + com.google.hamahang.core.bidi.TextRepairProcessor.stripPrefixKeepingBidi(trimmed, 6), checked = false, fontSize = baseFontSize.sp, level = listLevel, referenceMap = referenceMap)
            }
            // Regular list item
            fullyCleanTrimmed.startsWith("- ") || fullyCleanTrimmed.startsWith("* ") || fullyCleanTrimmed.startsWith("• ") -> {
                MarkdownListItem(
                    text = bidiPrefix + com.google.hamahang.core.bidi.TextRepairProcessor.stripPrefixKeepingBidi(trimmed, 2),
                    fontSize = (baseFontSize * uiFontScale).sp,
                    level = listLevel,
                    referenceMap = referenceMap,
                    isJustified = isJustified
                )
            }
            numberedListMatch != null -> {
                val number = numberedListMatch.groupValues[1]
                val content = numberedListMatch.groupValues[3]
                MarkdownNumberedListItem(
                    number = number,
                    text = bidiPrefix + content,
                    fontSize = (baseFontSize * uiFontScale).sp,
                    level = listLevel,
                    referenceMap = referenceMap,
                    isJustified = isJustified
                )
            }
            fullyCleanTrimmed == "---" || fullyCleanTrimmed == "***" || fullyCleanTrimmed == "___" -> {
                MarkdownDivider()
            }
            // Definition list container tags — just add spacing
            fullyCleanTrimmed.lowercase() == "<dl>" || fullyCleanTrimmed.lowercase() == "</dl>" -> {
                Spacer(modifier = Modifier.height(4.dp))
            }
            // Image as Link: [![alt](img_url)](link_url)
            fullyCleanTrimmed.matches(Regex("^\\[!\\[([^\\]]*)\\]\\(([^\\)]+)\\)\\]\\(([^\\)]+)\\)$")) -> {
                val match = Regex("^\\[!\\[([^\\]]*)\\]\\(([^\\)]+)\\)\\]\\(([^\\)]+)\\)$").find(fullyCleanTrimmed)!!
                val alt = match.groupValues[1]
                val imgUrl = match.groupValues[2].trim()
                val linkUrl = match.groupValues[3].trim()
                MarkdownImage(url = imgUrl, alt = alt, linkUrl = linkUrl)
            }
            // Image with title, tooltip, or plain image: ![alt](url...) or ![alt](url) "tooltip"
            fullyCleanTrimmed.startsWith("![") && fullyCleanTrimmed.contains("](") && !fullyCleanTrimmed.startsWith("[![") -> {
                val altStart = 2
                val altEnd = fullyCleanTrimmed.indexOf("](")
                val alt = if (altEnd > altStart) fullyCleanTrimmed.substring(altStart, altEnd) else ""
                val innerStart = altEnd + 2
                val remaining = fullyCleanTrimmed.substring(innerStart)
                val urlEnd = remaining.indexOfFirst { it == ')' || it == ' ' || it == '\t' || it == '"' || it == '\'' }
                val imgUrl = if (urlEnd != -1) remaining.substring(0, urlEnd).trim() else remaining.removeSuffix(")").trim()
                if (imgUrl.isNotEmpty()) MarkdownImage(url = imgUrl, alt = alt, linkUrl = null)
            }
            // HTML Image block: <img ...>
            cleanTrimmedLower.startsWith("<img") -> {
                val srcMatch = Regex("""(?i)src\s*=\s*["']([^"']*)["']""").find(cleanTrimmed)
                val altMatch = Regex("""(?i)alt\s*=\s*["']([^"']*)["']""").find(cleanTrimmed)
                val src = srcMatch?.groupValues?.get(1)?.trim() ?: ""
                val alt = altMatch?.groupValues?.get(1)?.trim() ?: "image"
                if (src.isNotEmpty()) {
                    MarkdownImage(url = src, alt = alt, linkUrl = null)
                }
            }
            fullyCleanTrimmed.startsWith(">") -> {
                var quoteLevel = 0
                var tempStr = cleanTrimmed
                while (tempStr.startsWith(">")) {
                    quoteLevel++
                    tempStr = tempStr.substring(1).trim()
                }
                MarkdownBlockquote(text = bidiPrefix + tempStr, fontSize = (baseFontSize * 0.95).sp, level = quoteLevel, referenceMap = referenceMap)
            }
            else -> {
                MarkdownParagraph(
                    text = bidiPrefix + cleanParagraph,
                    fontSize = (baseFontSize * uiFontScale).sp,
                    referenceMap = referenceMap,
                    isJustified = isJustified
                )
            }
        }
        idx++
    }

    if (inCodeBlock && codeLines.isNotEmpty()) {
        ComposeCodeBlock(lines = codeLines, fontSize = (baseFontSize * 0.85).sp)
    }
    if (inMathBlock && mathLines.isNotEmpty()) {
        val fullFormula = mathLines.joinToString("\n")
        ComposeMathBlock(formula = fullFormula, fontSize = (baseFontSize * 1.1).sp)
    }
    if (inMermaidBlock && mermaidLines.isNotEmpty()) {
        val fullMermaid = mermaidLines.joinToString("\n")
        ComposeMermaidBlock(code = fullMermaid)
    }
    if (inDetailsBlock && detailsLines.isNotEmpty()) {
        val fullContent = detailsLines.joinToString("\n")
        var summaryText = "Details"
        var contentText = fullContent
        val summaryRegex = Regex("(?is)<summary>(.*?)</summary>")
        val summaryMatch = summaryRegex.find(fullContent)
        if (summaryMatch != null) {
            summaryText = summaryMatch.groupValues[1].trim()
            contentText = fullContent.replace(summaryMatch.value, "").trim()
        }
        MarkdownDetails(
            summary = summaryText,
            content = contentText,
            baseFontSize = (baseFontSize * uiFontScale).sp,
            uiFontScale = uiFontScale,
            referenceMap = referenceMap,
            isJustified = isJustified
        )
    }

    if (footnoteMap.isNotEmpty()) {
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))
        footnoteMap.forEach { (label, footnoteText) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    text = "${label.replace("\\", "")}. ",
                    fontSize = (baseFontSize * 0.9).sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                MarkdownParagraph(
                    text = footnoteText,
                    fontSize = (baseFontSize * 0.9).sp,
                    referenceMap = referenceMap,
                    isJustified = isJustified
                )
            }
        }
    }
}

@Composable
fun MarkdownDetails(
    summary: String,
    content: String,
    baseFontSize: androidx.compose.ui.unit.TextUnit,
    uiFontScale: Float,
    referenceMap: Map<String, Pair<String, String?>> = emptyMap(),
    isJustified: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val styledSummary = parseMarkdownInlineStyles(summary, MaterialTheme.colorScheme.secondaryContainer, inlineCodeTextColor = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    text = styledSummary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = baseFontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                MarkdownPreviewPaneContents(
                    text = content,
                    baseFontSize = (baseFontSize.value / uiFontScale).toInt(),
                    uiFontScale = uiFontScale,
                    isJustified = isJustified
                )
            }
        }
    }
}

@Composable
fun MarkdownHeader(
    text: String,
    size: androidx.compose.ui.unit.TextUnit,
    weight: FontWeight,
    referenceMap: Map<String, Pair<String, String?>> = emptyMap()
) {
    val isRtl = TextRepairProcessor.isParagraphRtl(text)
    val codeBgColor = MaterialTheme.colorScheme.secondaryContainer
    Text(
        text = parseMarkdownInlineStyles(text, codeBgColor, referenceMap, inlineCodeTextColor = MaterialTheme.colorScheme.onSecondaryContainer),
        style = TextStyle(
            fontSize = size,
            fontWeight = weight,
            color = MaterialTheme.colorScheme.primary,
            textAlign = if (isRtl) TextAlign.Right else TextAlign.Left,
            textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    )
}

@Composable
fun MarkdownListItem(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    level: Int = 0,
    referenceMap: Map<String, Pair<String, String?>> = emptyMap(),
    isJustified: Boolean = false
) {
    val isRtl = TextRepairProcessor.isParagraphRtl(text)
    val codeBgColor = MaterialTheme.colorScheme.secondaryContainer
    CompositionLocalProvider(
        LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .padding(start = (level * 20 + 8).dp),
            verticalAlignment = Alignment.Top
        ) {
            when (level % 3) {
                1 -> {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp, start = 4.dp, end = 8.dp)
                            .size(6.dp)
                            .border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape)
                    )
                }
                2 -> {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp, start = 5.dp, end = 9.dp)
                            .size(5.dp)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp, start = 4.dp, end = 8.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                }
            }
            Text(
                text = parseMarkdownInlineStyles(text, codeBgColor, referenceMap, inlineCodeTextColor = MaterialTheme.colorScheme.onSecondaryContainer),
                style = TextStyle(
                    fontSize = fontSize,
                    textAlign = if (isJustified) TextAlign.Justify else TextAlign.Start,
                    textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MarkdownCheckboxItem(
    text: String,
    checked: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    level: Int = 0,
    referenceMap: Map<String, Pair<String, String?>> = emptyMap()
) {
    val isRtl = TextRepairProcessor.isParagraphRtl(text)
    val codeBgColor = MaterialTheme.colorScheme.secondaryContainer
    CompositionLocalProvider(
        LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .padding(start = (level * 20 + 8).dp),
            verticalAlignment = Alignment.Top
        ) {
            // Include bullet dot
            when (level % 3) {
                1 -> {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp, start = 4.dp, end = 8.dp)
                            .size(6.dp)
                            .border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape)
                    )
                }
                2 -> {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp, start = 5.dp, end = 9.dp)
                            .size(5.dp)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp, start = 4.dp, end = 8.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(end = 8.dp, top = 2.dp)
                    .size(18.dp)
                    .border(
                        1.5.dp,
                        if (checked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(3.dp)
                    )
                    .background(
                        if (checked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else androidx.compose.ui.graphics.Color.Transparent,
                        RoundedCornerShape(3.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Checked",
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
            Text(
                text = parseMarkdownInlineStyles(text, codeBgColor, referenceMap, inlineCodeTextColor = MaterialTheme.colorScheme.onSecondaryContainer),
                style = TextStyle(
                    fontSize = fontSize,
                    textAlign = TextAlign.Start,
                    textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr,
                    color = if (checked)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (checked)
                        androidx.compose.ui.text.style.TextDecoration.LineThrough
                    else
                        androidx.compose.ui.text.style.TextDecoration.None
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MarkdownImage(url: String, alt: String, linkUrl: String?) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val imageLoader = remember(context) {
        coil.ImageLoader.Builder(context)
            .components {
                add(coil.decode.SvgDecoder.Factory())
            }
            .build()
    }
    val modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)
        .let {
            if (linkUrl != null) {
                it.clickable { 
                    try {
                        uriHandler.openUri(linkUrl) 
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            } else it
        }
    coil.compose.SubcomposeAsyncImage(
        model = url,
        imageLoader = imageLoader,
        contentDescription = alt,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
        },
        error = {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🖼️ ${alt.ifBlank { "Image" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

@Composable
fun MarkdownNumberedListItem(
    number: String,
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    level: Int = 0,
    referenceMap: Map<String, Pair<String, String?>> = emptyMap(),
    isJustified: Boolean = false
) {
    val isRtl = TextRepairProcessor.isParagraphRtl(text)
    val codeBgColor = MaterialTheme.colorScheme.secondaryContainer
    CompositionLocalProvider(
        LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .padding(start = (level * 20 + 8).dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = number,
                style = TextStyle(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = parseMarkdownInlineStyles(text, codeBgColor, referenceMap, inlineCodeTextColor = MaterialTheme.colorScheme.onSecondaryContainer),
                style = TextStyle(
                    fontSize = fontSize,
                    textAlign = if (isJustified) TextAlign.Justify else TextAlign.Start,
                    textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MarkdownBlockquote(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    level: Int = 1,
    referenceMap: Map<String, Pair<String, String?>> = emptyMap(),
    isJustified: Boolean = false
) {
    val isRtl = TextRepairProcessor.isParagraphRtl(text)
    val codeBgColor = MaterialTheme.colorScheme.secondaryContainer
    CompositionLocalProvider(
        LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 4.dp)
                .padding(start = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(level) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(3.dp)
                        .background(MaterialTheme.colorScheme.secondary)
                )
                if (it < level - 1) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = parseMarkdownInlineStyles(text, codeBgColor, referenceMap, inlineCodeTextColor = MaterialTheme.colorScheme.onSecondaryContainer),
                style = TextStyle(
                    fontSize = fontSize,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = if (isJustified) TextAlign.Justify else TextAlign.Start,
                    textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

fun containsPersian(text: String): Boolean {
    return text.any { it in '\u0600'..'\u06FF' || it == '\uFB8A' || it == '\u067E' || it == '\u0686' || it == '\u06AF' }
}

fun preprocessCodeBidi(code: String): String {
    val lines = code.split("\n")
    val processedLines = lines.map { line ->
        if (line.isBlank()) return@map line

        // Check if there is a comment
        val commentMatch = Regex("(.*)(#|//)(.*)").matchEntire(line)
        if (commentMatch != null) {
            val codePart = commentMatch.groupValues[1]
            val prefix = commentMatch.groupValues[2]
            val commentText = commentMatch.groupValues[3]
            if (containsPersian(commentText)) {
                val cleanComment = commentText.trimStart()
                val spaces = commentText.substring(0, commentText.length - cleanComment.length)
                return@map "$codePart\u200E$prefix$spaces\u2067$cleanComment\u2069"
            }
        }

        var processedLine = line
        val hasPersian = containsPersian(processedLine)
        
        if (hasPersian) {
            // 1. Triple quotes docstring on a single line
            val tripleQuoteRegex = Regex("\"\"\"([^\"]*)\"\"\"")
            processedLine = tripleQuoteRegex.replace(processedLine) { match ->
                val content = match.groupValues[1]
                if (containsPersian(content)) {
                    "\"\"\"\u2067$content\u2069\"\"\""
                } else {
                    match.value
                }
            }

            // 2. Normal string literals in quotes
            val stringRegex = Regex("\"([^\"]*)\"|'([^']*)'")
            processedLine = stringRegex.replace(processedLine) { match ->
                val content = match.groupValues[1].ifEmpty { match.groupValues[2] }
                if (containsPersian(content)) {
                    if (match.value.startsWith("\"")) {
                        "\"\u2067$content\u2069\""
                    } else {
                        "'\u2067$content\u2069'"
                    }
                } else {
                    match.value
                }
            }

            // 3. If the entire line is a Farsi docstring line (e.g. inside triple quotes but on its own line)
            if (containsPersian(processedLine) && !processedLine.contains("\"") && !processedLine.contains("'")) {
                val trimmed = processedLine.trimStart()
                val indent = processedLine.substring(0, processedLine.length - trimmed.length)
                processedLine = "$indent\u200E\u2067$trimmed\u2069"
            }
        }

        processedLine
    }
    return processedLines.joinToString("\n")
}

@Composable
fun ComposeCodeBlock(lines: List<String>, fontSize: androidx.compose.ui.unit.TextUnit) {
    val rawCode = lines.joinToString("\n")
    val codeText = preprocessCodeBidi(rawCode)
    val highlightedCode = highlightCode(codeText)
    
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = highlightedCode,
                    style = TextStyle(
                        fontSize = fontSize,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFD4D4D4),
                        textAlign = TextAlign.Left,
                        textDirection = TextDirection.Ltr
                    )
                )
            }
        }
    }
}

sealed interface MathRenderState {
    object Loading : MathRenderState
    object Success : MathRenderState
    data class Error(val message: String) : MathRenderState
}

@Composable
fun ComposeMathBlock(formula: String, fontSize: androidx.compose.ui.unit.TextUnit) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val r = (onSurfaceVariant.red * 255).toInt()
    val g = (onSurfaceVariant.green * 255).toInt()
    val b = (onSurfaceVariant.blue * 255).toInt()
    val textHtmlColor = String.format("#%02X%02X%02X", r, g, b)
    val cleanFormula = remember(formula) {
        formula.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
            .replace(Regex("""\\begin\{align\*?\}"""), "\\\\begin{aligned}")
            .replace(Regex("""\\end\{align\*?\}"""), "\\\\end{aligned}")
    }
    
    val lineCount = remember(cleanFormula) {
        cleanFormula.split("\n", "\\\\").size
    }
    val calculatedHeight = remember(lineCount) {
        (70 + lineCount * 36).coerceIn(70, 500).dp
    }
    
    var renderState by remember(cleanFormula) { mutableStateOf<MathRenderState>(MathRenderState.Loading) }
    
    // Load KaTeX locally from the assets folder to avoid CDN timeouts
    val htmlContent = remember(cleanFormula, textHtmlColor) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <!-- Local KaTeX Loading -->
            <link rel="stylesheet" href="file:///android_asset/katex/katex.min.css">
            <script src="file:///android_asset/katex/katex.min.js"></script>
            <style>
                body {
                    background-color: transparent;
                    color: $textHtmlColor;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    margin: 0;
                    padding: 8px;
                    overflow-x: auto;
                    overflow-y: hidden;
                }
                #math {
                    font-size: 1.2em;
                    text-align: center;
                }
                .katex-display {
                    margin: 0 !important;
                }
            </style>
        </head>
        <body>
            <div id="math"></div>
            <script>
                function tryRender() {
                    try {
                        if (typeof katex === 'undefined') {
                            setTimeout(tryRender, 50);
                            return;
                        }
                        var target = document.getElementById('math');
                        if (!target) {
                            setTimeout(tryRender, 50);
                            return;
                        }
                        katex.render(${JSONString(cleanFormula)}, target, {
                            displayMode: true,
                            throwOnError: false
                        });
                        console.log("MATH_RENDER_SUCCESS");
                    } catch (e) {
                        var target = document.getElementById('math');
                        if (target) {
                            target.textContent = ${JSONString(cleanFormula)};
                        }
                        console.log("MATH_RENDER_ERROR:" + e.message);
                    }
                }
                
                // Start rendering immediately, tryRender will wait for KaTeX to load
                tryRender();
            </script>
        </body>
        </html>
    """.trimIndent()
    }

    // Safety timeout: if rendering doesn't succeed in 4.0 seconds, fallback
    LaunchedEffect(cleanFormula) {
        kotlinx.coroutines.delay(4000)
        if (renderState == MathRenderState.Loading) {
            renderState = MathRenderState.Error("Timeout loading KaTeX")
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp, max = calculatedHeight)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (renderState is MathRenderState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
            
            if (renderState is MathRenderState.Error) {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cleanFormula,
                            style = TextStyle(
                                fontFamily = FontFamily.Serif,
                                fontSize = fontSize,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                textDirection = TextDirection.Ltr,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            } else {
                AndroidView(
                    factory = { context ->
                        android.webkit.WebView(context).apply {
                            webViewClient = object : android.webkit.WebViewClient() {
                                override fun onReceivedError(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?,
                                    error: android.webkit.WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        renderState = MathRenderState.Error("WebView network error")
                                    }
                                }
                            }
                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                    val msg = consoleMessage?.message() ?: ""
                                    when {
                                        msg == "MATH_RENDER_SUCCESS" -> {
                                            renderState = MathRenderState.Success
                                        }
                                        msg.startsWith("MATH_RENDER_ERROR:") -> {
                                            val err = msg.removePrefix("MATH_RENDER_ERROR:")
                                            renderState = MathRenderState.Error(err)
                                        }
                                    }
                                    return true
                                }
                            }
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        }
                    },
                    update = { webView ->
                        val lastLoaded = webView.tag as? String
                        if (lastLoaded != htmlContent) {
                            webView.tag = htmlContent
                            webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun ComposeMermaidBlock(code: String) {
    val isDark = isSystemInDarkTheme()
    val mermaidTheme = if (isDark) "dark" else "default"
    
    // Clean bidi characters to avoid mermaid parsing syntax errors
    val cleanCode = remember(code) {
        code.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
    }

    var webViewHeight by remember { mutableIntStateOf(250) }

    val htmlContent = remember(cleanCode, mermaidTheme) {
        """
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
                    fontFamily: 'Vazirmatn, sans-serif',
                    gantt: {
                        titleTopMargin: 25,
                        barHeight: 24,
                        barGap: 4,
                        topPadding: 50,
                        sidePadding: 75,
                        fontSize: 12,
                        sectionFontSize: 12,
                        numberSectionStyles: 4,
                        axisFormat: '%Y-%m-%d',
                        useMaxWidth: false
                    }
                });
                
                window.onload = function() {
                    var container = document.getElementById('mermaid-container');
                    
                    var svgs = document.getElementsByTagName('svg');
                    if (svgs.length > 0) {
                        reportHeight(svgs[0], container);
                        return;
                    }
                    
                    var observer = new MutationObserver(function(mutations) {
                        var svgs = document.getElementsByTagName('svg');
                        if (svgs.length > 0) {
                            observer.disconnect();
                            setTimeout(function() {
                                reportHeight(svgs[0], container);
                            }, 100);
                        }
                    });
                    observer.observe(document.body, { childList: true, subtree: true });
                };
                
                function reportHeight(svg, container) {
                    var rect = svg.getBoundingClientRect();
                    var h = rect.height || container.scrollHeight;
                    if (window.AndroidInterface && h > 0) {
                        window.AndroidInterface.onRenderComplete(Math.ceil(h));
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
                    padding: 8px;
                    display: block;
                    text-align: center;
                    overflow-x: auto;
                    overflow-y: hidden;
                    font-family: 'Vazirmatn', sans-serif;
                    direction: ltr;
                }
                #mermaid-container {
                    display: inline-block;
                    width: 100%;
                    overflow-x: auto;
                    -webkit-overflow-scrolling: touch;
                    text-align: center;
                }
                .mermaid {
                    display: inline-block;
                    text-align: center;
                    width: auto;
                    min-width: 100%;
                }
                svg {
                    max-width: none;
                    height: auto;
                    overflow: visible;
                }
                text {
                    font-family: 'Vazirmatn', sans-serif !important;
                }
                .titleText, text.titleText, #gantt-title-text {
                    font-family: 'Vazirmatn', sans-serif !important;
                    font-size: 14px !important;
                    text-anchor: middle !important;
                    unicode-bidi: plaintext !important;
                }
            </style>
        </head>
        <body><div id="mermaid-container"><pre class="mermaid">$cleanCode</pre></div></body>
        </html>
        """.trimIndent()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp, max = 2500.dp)
                .height(webViewHeight.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    android.webkit.WebView(context).apply {
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onReceivedError(
                                view: android.webkit.WebView?,
                                request: android.webkit.WebResourceRequest?,
                                error: android.webkit.WebResourceError?
                            ) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    android.util.Log.e("ComposeMermaidBlock", "WebView Error: ${error?.description}")
                                }
                            }
                        }
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                return true
                            }
                        }
                        addJavascriptInterface(object : Any() {
                            @android.webkit.JavascriptInterface
                            fun onRenderComplete(height: Int) {
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    webViewHeight = height + 40
                                }
                            }
                        }, "AndroidInterface")
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                update = { webView ->
                    val lastLoaded = webView.tag as? String
                    if (lastLoaded != htmlContent) {
                        webView.tag = htmlContent
                        webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun JSONString(str: String): String {
    return "\"" + str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r") + "\""
}

fun highlightCode(code: String): AnnotatedString {
    val builder = AnnotatedString.Builder(code)
    val excludedRanges = mutableListOf<IntRange>()

    // 1. Comments: from '#' or '//' to end of line (green-grey like VS Code)
    val commentRegex = Regex("(#|//).*")
    commentRegex.findAll(code).forEach { match ->
        excludedRanges.add(match.range)
        builder.addStyle(
            SpanStyle(color = Color(0xFF6A9955), fontStyle = FontStyle.Italic),
            match.range.first,
            match.range.last + 1
        )
    }

    // 2. Docstrings / Triple quoted strings: """ ... """
    val tripleQuoteRegex = Regex("\"\"\"[\\s\\S]*?\"\"\"")
    tripleQuoteRegex.findAll(code).forEach { match ->
        if (excludedRanges.none { it.contains(match.range.first) }) {
            excludedRanges.add(match.range)
            builder.addStyle(
                SpanStyle(color = Color(0xFF6A9955)),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    // 3. String literals: " ... " or ' ... '
    val stringRegex = Regex("\"[^\"]*\"|'[^']*'")
    stringRegex.findAll(code).forEach { match ->
        if (excludedRanges.none { it.contains(match.range.first) }) {
            excludedRanges.add(match.range)
            builder.addStyle(
                SpanStyle(color = Color(0xFFCE9178)),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    fun isExcluded(idx: Int): Boolean {
        return excludedRanges.any { it.contains(idx) }
    }

    // 4. Keywords: purple like VS Code
    val keywords = setOf(
        "import", "from", "def", "class", "return", "try", "except", "as", "print",
        "fun", "val", "var", "if", "else", "while", "for", "in", "null", "true", "false",
        "None", "Exception", "and", "or", "not", "is", "pass", "lambda", "const", "let",
        "function", "async", "await", "package", "public", "private", "protected"
    )
    val wordRegex = Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b")
    wordRegex.findAll(code).forEach { match ->
        val word = match.value
        if (word in keywords && !isExcluded(match.range.first)) {
            builder.addStyle(
                SpanStyle(color = Color(0xFFC586C0), fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    // 5. Function names: yellow/gold like VS Code
    val functionDefRegex = Regex("(def|fun|function)\\s+([a-zA-Z_][a-zA-Z0-9_]*)")
    functionDefRegex.findAll(code).forEach { match ->
        val group = match.groups[2]
        if (group != null && !isExcluded(group.range.first)) {
            builder.addStyle(
                SpanStyle(color = Color(0xFFDCDCAA)),
                group.range.first,
                group.range.last + 1
            )
        }
    }

    // 6. Numbers: light green/yellow like VS Code
    val numberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
    numberRegex.findAll(code).forEach { match ->
        if (!isExcluded(match.range.first)) {
            builder.addStyle(
                SpanStyle(color = Color(0xFFB5CEA8)),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    return builder.toAnnotatedString()
}

@Composable
fun MarkdownParagraph(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    referenceMap: Map<String, Pair<String, String?>> = emptyMap(),
    isJustified: Boolean = false,
    explicitAlignment: TextAlign? = null
) {
    val isRtl = TextRepairProcessor.isParagraphRtl(text)
    val codeBgColor = MaterialTheme.colorScheme.secondaryContainer
    val textAlign = explicitAlignment ?: if (isJustified) TextAlign.Justify else if (isRtl) TextAlign.Right else TextAlign.Left
    val textDirection = when (explicitAlignment) {
        TextAlign.Center -> if (isRtl) TextDirection.Rtl else TextDirection.Ltr
        TextAlign.Right -> TextDirection.Rtl
        TextAlign.Left -> TextDirection.Ltr
        else -> if (isRtl) TextDirection.Rtl else TextDirection.Ltr
    }
    Text(
        text = parseMarkdownInlineStyles(text, codeBgColor, referenceMap, inlineCodeTextColor = MaterialTheme.colorScheme.onSecondaryContainer),
        style = TextStyle(
            fontSize = fontSize,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = textAlign,
            textDirection = textDirection
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

@Composable
fun MarkdownDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

enum class TableColumnAlignment {
    LEFT, CENTER, RIGHT
}

fun isTableDivider(line: String): Boolean {
    val clean = line.trim()
    if (!clean.startsWith("|") || !clean.endsWith("|")) return false
    val parts = clean.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.isEmpty()) return false
    return parts.all { cell -> cell.all { it == '-' || it == ':' } }
}

fun parseTableLine(line: String): List<String> {
    val trimmed = line.trim()
    if (!trimmed.startsWith("|")) return emptyList()
    val parts = mutableListOf<String>()
    val current = StringBuilder()
    var bracketDepth = 0
    var parenDepth = 0
    var isEscaped = false

    for (i in 1 until trimmed.length) {
        val c = trimmed[i]
        if (isEscaped) {
            current.append(c)
            isEscaped = false
            continue
        }
        if (c == '\\') {
            isEscaped = true
            current.append(c)
            continue
        }
        if (c == '[') bracketDepth++
        else if (c == ']' && bracketDepth > 0) bracketDepth--
        else if (c == '(') parenDepth++
        else if (c == ')' && parenDepth > 0) parenDepth--

        if (c == '|' && bracketDepth == 0 && parenDepth == 0) {
            parts.add(current.toString().trim())
            current.clear()
        } else {
            current.append(c)
        }
    }
    val remaining = current.toString().trim()
    if (remaining.isNotEmpty()) {
        parts.add(remaining)
    }
    return parts
}

fun parseAlignment(dividerCell: String): TableColumnAlignment {
    val clean = dividerCell.trim()
    val startsWithColon = clean.startsWith(":")
    val endsWithColon = clean.endsWith(":")
    return when {
        startsWithColon && endsWithColon -> TableColumnAlignment.CENTER
        endsWithColon -> TableColumnAlignment.RIGHT
        else -> TableColumnAlignment.LEFT
    }
}

data class ParsedHtmlTable(
    val headerColumns: List<String>,
    val dataRows: List<List<String>>,
    val alignments: List<TableColumnAlignment>
)

data class DivBoxStyle(
    val textAlign: TextAlign? = null,
    val backgroundColor: Color? = null,
    val borderLeftColor: Color? = null,
    val borderLeftWidth: Float = 0f,
    val borderColor: Color? = null,
    val borderWidth: Float = 0f,
    val borderRadius: Float = 8f
) {
    val isStyledBox: Boolean
        get() = backgroundColor != null || borderLeftColor != null || borderColor != null
}

fun parseDivBoxStyle(tag: String): DivBoxStyle {
    val styleMatch = Regex("""(?i)style\s*=\s*["']([^"']*)["']""").find(tag)
    val styleContent = styleMatch?.groupValues?.get(1) ?: ""
    val lower = styleContent.lowercase()
    val tagLower = tag.lowercase()

    val textAlign = when {
        lower.contains("text-align:\\s*center".toRegex()) || tagLower.contains("align=[\"']?center".toRegex()) || tagLower.startsWith("<center") -> TextAlign.Center
        lower.contains("text-align:\\s*right".toRegex()) || tagLower.contains("align=[\"']?right".toRegex()) -> TextAlign.Right
        lower.contains("text-align:\\s*left".toRegex()) || tagLower.contains("align=[\"']?left".toRegex()) -> TextAlign.Left
        lower.contains("text-align:\\s*justify".toRegex()) || tagLower.contains("align=[\"']?justify".toRegex()) -> TextAlign.Justify
        else -> null
    }

    fun parseColor(str: String): Color? {
        val s = str.trim()
        return try {
            if (s.startsWith("#")) {
                val hex = s.removePrefix("#")
                when (hex.length) {
                    3 -> {
                        val r = hex[0].toString().repeat(2).toInt(16)
                        val g = hex[1].toString().repeat(2).toInt(16)
                        val b = hex[2].toString().repeat(2).toInt(16)
                        Color(android.graphics.Color.rgb(r, g, b))
                    }
                    6, 8 -> Color(android.graphics.Color.parseColor("#$hex"))
                    else -> null
                }
            } else if (s.startsWith("rgb", ignoreCase = true)) {
                val nums = Regex("""\d+""").findAll(s).map { it.value.toInt() }.toList()
                if (nums.size >= 3) Color(android.graphics.Color.rgb(nums[0], nums[1], nums[2])) else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    val bgMatch = Regex("""(?i)background(?:-color)?\s*:\s*([^;"]+)""").find(styleContent)
    val bgColor = bgMatch?.groupValues?.get(1)?.let { parseColor(it) }

    val borderLeftMatch = Regex("""(?i)border-left\s*:\s*(\d+)px\s+\w+\s+([^;"]+)""").find(styleContent)
    val blWidth = borderLeftMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
    val blColor = borderLeftMatch?.groupValues?.get(2)?.let { parseColor(it) }

    val borderMatch = Regex("""(?i)border\s*:\s*(\d+)px\s+\w+\s+([^;"]+)""").find(styleContent)
    val bWidth = borderMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
    val bColor = borderMatch?.groupValues?.get(2)?.let { parseColor(it) }

    val brMatch = Regex("""(?i)border-radius\s*:\s*(\d+)px""").find(styleContent)
    val bRadius = brMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 8f

    return DivBoxStyle(
        textAlign = textAlign,
        backgroundColor = bgColor,
        borderLeftColor = blColor,
        borderLeftWidth = blWidth,
        borderColor = bColor,
        borderWidth = bWidth,
        borderRadius = bRadius
    )
}

fun parseDivAlignment(tag: String): TextAlign? = parseDivBoxStyle(tag).textAlign

@Composable
fun MarkdownProgressBar(value: Float, max: Float) {
    val progress = (value / max).coerceIn(0f, 1f)
    val percentageText = "${(progress * 100).toInt()}%"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = percentageText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ComposeStyledBox(
    style: DivBoxStyle,
    lines: List<String>,
    baseFontSize: Int,
    uiFontScale: Float,
    referenceMap: Map<String, Pair<String, String?>>,
    isJustified: Boolean
) {
    val shape = RoundedCornerShape(style.borderRadius.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = style.backgroundColor ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (style.borderColor != null && style.borderWidth > 0f) {
            BorderStroke(style.borderWidth.dp, style.borderColor)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            if (style.borderLeftColor != null && style.borderLeftWidth > 0f) {
                Box(
                    modifier = Modifier
                        .width(style.borderLeftWidth.coerceAtLeast(3f).dp)
                        .heightIn(min = 24.dp)
                        .background(style.borderLeftColor, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        MarkdownParagraph(
                            text = line,
                            fontSize = (baseFontSize * uiFontScale).sp,
                            referenceMap = referenceMap,
                            isJustified = isJustified,
                            explicitAlignment = style.textAlign
                        )
                    }
                }
            }
        }
    }
}

fun parseHtmlTable(tableHtml: String): ParsedHtmlTable? {
    val trRegex = Regex("(?is)<tr[^>]*>(.*?)</tr>")
    val trMatches = trRegex.findAll(tableHtml).toList()
    if (trMatches.isEmpty()) return null

    val theadRegex = Regex("(?is)<thead[^>]*>(.*?)</thead>")
    val theadMatch = theadRegex.find(tableHtml)
    val theadTrs = if (theadMatch != null) trRegex.findAll(theadMatch.value).toList() else emptyList()

    fun cleanCellContent(html: String): String {
        return html
            .replace(Regex("(?i)<br\\s*/?>"), " ")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .trim()
    }

    val grid = mutableListOf<MutableList<String?>>()
    var numCols = 0
    val cellRegex = Regex("(?is)<(th|td)([^>]*)>(.*?)</\\1>")

    for (trMatch in trMatches) {
        val trContent = trMatch.groupValues[1]
        val cells = cellRegex.findAll(trContent).toList()
        if (cells.isEmpty()) continue

        var r = grid.indexOfFirst { row -> row.any { it == null } }
        if (r == -1) {
            grid.add(mutableListOf())
            r = grid.lastIndex
        }

        var c = 0
        for (cellMatch in cells) {
            val attrs = cellMatch.groupValues[2]
            val content = cleanCellContent(cellMatch.groupValues[3])

            val rowspanMatch = Regex("""(?i)rowspan\s*=\s*["']?(\d+)["']?""").find(attrs)
            val colspanMatch = Regex("""(?i)colspan\s*=\s*["']?(\d+)["']?""").find(attrs)
            val rowspan = rowspanMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val colspan = colspanMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1

            while (c < grid[r].size && grid[r][c] != null) {
                c++
            }

            for (dr in 0 until rowspan) {
                while (grid.size <= r + dr) {
                    grid.add(mutableListOf())
                }
                for (dc in 0 until colspan) {
                    while (grid[r + dr].size <= c + dc) {
                        grid[r + dr].add(null)
                    }
                    grid[r + dr][c + dc] = if (dr == 0 && dc == 0) content else (if (content.isNotEmpty()) " " else "")
                }
            }
            c += colspan
            if (c > numCols) numCols = c
        }
    }

    for (row in grid) {
        while (row.size < numCols) {
            row.add("")
        }
    }

    val totalRows = grid.size
    if (totalRows == 0 || numCols == 0) return null

    val numHeaderRows = if (theadTrs.isNotEmpty()) theadTrs.size.coerceAtMost(totalRows) else 1
    val headerCols = (0 until numCols).map { col ->
        val nonBlank = (0 until numHeaderRows)
            .map { rowIdx -> grid[rowIdx].getOrNull(col)?.trim() ?: "" }
            .filter { it.isNotEmpty() }
        if (nonBlank.isNotEmpty()) nonBlank.last() else "Col ${col + 1}"
    }

    val dataRows = (numHeaderRows until totalRows).map { rowIdx ->
        (0 until numCols).map { col ->
            grid[rowIdx].getOrNull(col)?.trim() ?: ""
        }
    }

    val alignments = (0 until numCols).map { TableColumnAlignment.LEFT }
    return ParsedHtmlTable(headerCols, dataRows, alignments)
}

@Composable
fun TableCell(
    text: String,
    alignment: TableColumnAlignment,
    isHeader: Boolean,
    baseFontSize: Int,
    referenceMap: Map<String, Pair<String, String?>> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val codeBgColor = MaterialTheme.colorScheme.secondaryContainer
    val resolvedText = parseMarkdownInlineStyles(text, codeBgColor, referenceMap, inlineCodeTextColor = MaterialTheme.colorScheme.onSecondaryContainer)
    
    val isRtl = TextRepairProcessor.isParagraphRtl(text)
    
    val textAlign = when (alignment) {
        TableColumnAlignment.LEFT -> if (isRtl) TextAlign.Right else TextAlign.Left
        TableColumnAlignment.CENTER -> TextAlign.Center
        TableColumnAlignment.RIGHT -> TextAlign.Right
    }
    
    val textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr
    
    Box(
        modifier = modifier
            .padding(horizontal = 6.dp),
        contentAlignment = when (alignment) {
            TableColumnAlignment.LEFT -> if (isRtl) Alignment.CenterEnd else Alignment.CenterStart
            TableColumnAlignment.CENTER -> Alignment.Center
            TableColumnAlignment.RIGHT -> Alignment.CenterEnd
        }
    ) {
        Text(
            text = resolvedText,
            style = TextStyle(
                fontSize = (if (isHeader) baseFontSize + 1 else baseFontSize).sp,
                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                color = if (isHeader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = textAlign,
                textDirection = textDirection
            )
        )
    }
}

@Composable
fun MarkdownTable(
    headerColumns: List<String>,
    dataRows: List<List<String>>,
    alignments: List<TableColumnAlignment>,
    baseFontSize: Int,
    referenceMap: Map<String, Pair<String, String?>> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val isTableRtl = TextRepairProcessor.isParagraphRtl(headerColumns.firstOrNull() ?: "")
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            CompositionLocalProvider(
                LocalLayoutDirection provides (if (isTableRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
            ) {
                Column {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                            .padding(vertical = 12.dp, horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        headerColumns.forEachIndexed { colIdx, headerText ->
                            val align = alignments.getOrNull(colIdx) ?: TableColumnAlignment.LEFT
                            TableCell(
                                text = headerText,
                                alignment = align,
                                isHeader = true,
                                baseFontSize = baseFontSize,
                                referenceMap = referenceMap,
                                modifier = Modifier.width(160.dp)
                            )
                        }
                    }
                    
                    // Border separator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5.dp)
                            .background(outlineColor)
                    )
                    
                    // Data Rows
                    dataRows.forEachIndexed { rowIdx, rowCells ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (rowIdx % 2 == 0) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                )
                                .padding(vertical = 10.dp, horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (colIdx in headerColumns.indices) {
                                val cellText = rowCells.getOrNull(colIdx) ?: ""
                                val align = alignments.getOrNull(colIdx) ?: TableColumnAlignment.LEFT
                                TableCell(
                                    text = cellText,
                                    alignment = align,
                                    isHeader = false,
                                    baseFontSize = baseFontSize,
                                    referenceMap = referenceMap,
                                    modifier = Modifier.width(160.dp)
                                )
                            }
                        }
                        if (rowIdx < dataRows.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.5.dp)
                                    .background(outlineColor.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditorPane(
    title: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    textDirection: TextDirection,
    fontSize: Int,
    uiFontScale: Float,
    isReadOnly: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontSize = (12.sp * uiFontScale),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    readOnly = isReadOnly,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontSize = fontSize.sp, // Strictly editor text size
                        textAlign = TextAlign.Start,
                        textDirection = textDirection,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

/**
 * Converts a simple inline math expression into a styled AnnotatedString.
 * Handles:
 *   - ^{expr} / ^x  → superscript Unicode chars
 *   - _{expr} / _x  → subscript Unicode chars
 *   - \frac{a}{b}   → a/b
 *   - \sqrt{x}      → √x
 *   - \times        → ×, \cdot → ·, \pm → ±, \infty → ∞ etc.
 *   - = sign + Italic+Bold for the full expression
 */
fun renderInlineMath(content: String, codeBgColor: Color): AnnotatedString {
    val superMap = mapOf(
        '0' to "⁰", '1' to "¹", '2' to "²", '3' to "³", '4' to "⁴",
        '5' to "⁵", '6' to "⁶", '7' to "⁷", '8' to "⁸", '9' to "⁹",
        '+' to "⁺", '-' to "⁻", '=' to "⁼", '(' to "⁽", ')' to "⁾",
        'a' to "ᵃ", 'b' to "ᵇ", 'c' to "ᶜ", 'd' to "ᵈ", 'e' to "ᵉ",
        'f' to "ᶠ", 'g' to "ᵍ", 'h' to "ʰ", 'i' to "ⁱ", 'j' to "ʲ",
        'k' to "ᵏ", 'l' to "ˡ", 'm' to "ᵐ", 'n' to "ⁿ", 'o' to "ᵒ",
        'p' to "ᵖ", 'r' to "ʳ", 's' to "ˢ", 't' to "ᵗ", 'u' to "ᵘ",
        'v' to "ᵛ", 'w' to "ʷ", 'x' to "ˣ", 'y' to "ʸ", 'z' to "ᶻ",
        'n' to "ⁿ"
    )
    val subMap = mapOf(
        '0' to "₀", '1' to "₁", '2' to "₂", '3' to "₃", '4' to "₄",
        '5' to "₅", '6' to "₆", '7' to "₇", '8' to "₈", '9' to "₉",
        '+' to "₊", '-' to "₋", '=' to "₌", '(' to "₍", ')' to "₎",
        'a' to "ₐ", 'e' to "ₑ", 'o' to "ₒ", 'i' to "ᵢ", 'u' to "ᵤ",
        'x' to "ₓ", 'r' to "ᵣ", 'v' to "ᵥ", 'j' to "ⱼ", 'h' to "ₕ",
        'k' to "ₖ", 'l' to "ₗ", 'm' to "ₘ", 'n' to "ₙ", 'p' to "ₚ",
        's' to "ₛ", 't' to "ₜ"
    )

    fun toSup(s: String) = s.map { superMap[it] ?: it.toString() }.joinToString("")
    fun toSub(s: String) = s.map { subMap[it] ?: it.toString() }.joinToString("")

    fun processTokens(expr: String): String {
        var s = expr
        // Spacing & formatting helpers
        s = s.replace("\\left", "").replace("\\right", "")
        s = s.replace("\\{", "{").replace("\\}", "}")
        s = s.replace("\\,", " ").replace("\\!", "").replace("\\;", " ").replace("\\:", " ")
        s = s.replace("\\quad", "  ").replace("\\qquad", "    ")
        
        // Text & font formatting
        s = s.replace(Regex("\\\\(text|mathbf|mathrm|textbf|mathit)\\{([^{}]*)\\}")) { m -> m.groupValues[2] }

        // Standard functions
        s = s.replace(Regex("\\\\(ln|log|exp|sin|cos|tan|cot|sec|csc|arcsin|arccos|arctan|lim|det|max|min)(?![a-zA-Z])")) { m -> m.groupValues[1] }

        // LaTeX commands
        s = s.replace(Regex("\\\\frac\\{([^}]*)\\}\\{([^}]*)\\}")) { m -> "${m.groupValues[1]}/${m.groupValues[2]}" }
        s = s.replace(Regex("\\\\sqrt\\{([^}]*)\\}")) { m -> "√${m.groupValues[1]}" }

        // Symbols
        s = s.replace("\\times", "×").replace("\\cdot", "·").replace("\\div", "÷")
        s = s.replace("\\pm", "±").replace("\\mp", "∓").replace("\\infty", "∞")
        s = s.replace("\\partial", "∂").replace("\\nabla", "∇")
        s = s.replace("\\int", "∫").replace("\\sum", "∑").replace("\\prod", "∏")
        s = s.replace("\\leq", "≤").replace("\\geq", "≥").replace("\\neq", "≠")
        s = s.replace("\\le", "≤").replace("\\ge", "≥").replace("\\ne", "≠")
        s = s.replace("\\approx", "≈").replace("\\equiv", "≡")
        s = s.replace("\\rightarrow", "→").replace("\\leftarrow", "←").replace("\\to", "→")

        // Greek uppercase
        s = s.replace("\\Omega", "Ω").replace("\\Gamma", "Γ").replace("\\Delta", "Δ")
        s = s.replace("\\Theta", "Θ").replace("\\Lambda", "Λ").replace("\\Xi", "Ξ")
        s = s.replace("\\Pi", "Π").replace("\\Sigma", "Σ").replace("\\Phi", "Φ").replace("\\Psi", "Ψ")

        // Greek lowercase
        s = s.replace("\\alpha", "α").replace("\\beta", "β").replace("\\gamma", "γ")
        s = s.replace("\\delta", "δ").replace("\\pi", "π").replace("\\sigma", "σ")
        s = s.replace("\\theta", "θ").replace("\\lambda", "λ").replace("\\mu", "μ").replace("\\nu", "ν")
        s = s.replace("\\tau", "τ").replace("\\xi", "ξ").replace("\\zeta", "ζ").replace("\\eta", "η")
        s = s.replace("\\epsilon", "ε").replace("\\varepsilon", "ε").replace("\\rho", "ρ")
        s = s.replace("\\psi", "ψ").replace("\\phi", "φ").replace("\\varphi", "φ")
        s = s.replace("\\omega", "ω").replace("\\chi", "χ").replace("\\kappa", "κ")

        // Superscript: ^{...} or ^x
        s = s.replace(Regex("\\^\\{([^}]*)\\}")) { m -> toSup(m.groupValues[1]) }
        s = s.replace(Regex("\\^([0-9a-zA-Z+\\-])")) { m -> toSup(m.groupValues[1]) }
        // Subscript: _{...} or _x
        s = s.replace(Regex("_\\{([^}]*)\\}")) { m -> toSub(m.groupValues[1]) }
        s = s.replace(Regex("_([0-9a-zA-Z])")) { m -> toSub(m.groupValues[1]) }

        // Clean unknown LaTeX commands and lone braces
        s = s.replace(Regex("\\\\[a-zA-Z]+"), "")
        s = s.replace("{", "").replace("}", "")

        return s
    }

    val processed = processTokens(content)
    val builder2 = AnnotatedString.Builder()
    builder2.pushStyle(SpanStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold))
    builder2.append(processed)
    builder2.pop()
    return builder2.toAnnotatedString()
}

fun parseMarkdownInlineStyles(
    input: String,
    codeBgColor: Color,
    referenceMap: Map<String, Pair<String, String?>> = emptyMap(),
    inlineCodeTextColor: Color = Color(0xFFD81B60)
): AnnotatedString {

    val escapeMap = listOf(
        "\\\\" to "\uE000",
        "\\`"  to "\uE001",
        "\\*"  to "\uE002",
        "\\_"  to "\uE003",
        "\\{"  to "\uE004",
        "\\}"  to "\uE005",
        "\\["  to "\uE006",
        "\\]"  to "\uE007",
        "\\("  to "\uE008",
        "\\)"  to "\uE009",
        "\\#"  to "\uE00A",
        "\\+"  to "\uE00B",
        "\\-"  to "\uE00C",
        "\\."  to "\uE00D",
        "\\!"  to "\uE00E",
        "\\|"  to "\uE00F",
        "\\~"  to "\uE010"
    )

    fun encodeEscapes(str: String): String {
        var res = str
        for (pair in escapeMap) {
            res = res.replace(pair.first, pair.second)
        }
        return res
    }

    fun decodeEscapesUnescaped(str: String): String {
        var res = str
        for (pair in escapeMap) {
            val unescaped = pair.first.substring(1)
            res = res.replace(pair.second, unescaped)
        }
        return res
    }

    fun decodeEscapesEscaped(str: String): String {
        var res = str
        for (pair in escapeMap) {
            res = res.replace(pair.second, pair.first)
        }
        return res
    }

    fun decodeAnnotatedString(annotated: AnnotatedString): AnnotatedString {
        val decodedText = decodeEscapesUnescaped(annotated.text)
        return AnnotatedString(
            text = decodedText,
            spanStyles = annotated.spanStyles
        )
    }

    val encodedInput = encodeEscapes(input)
    val builder = AnnotatedString.Builder()
    var index = 0

    fun cleanQuotes(value: String): String {
        var clean = value.trim()
        val quotePairs = listOf(
            "\"" to "\"",
            "'" to "'",
            "“" to "”",
            "“" to "“",
            "”" to "”",
            "‘" to "’",
            "‘" to "‘",
            "’" to "’",
            "«" to "»",
            "„" to "‟",
            "＂" to "＂",
            "＇" to "＇"
        )
        for (pair in quotePairs) {
            if (clean.startsWith(pair.first) && clean.endsWith(pair.second)) {
                clean = clean.removeSurrounding(pair.first, pair.second)
                break
            }
        }
        return clean
    }

    fun stripMarkdownEscapes(text: String): String {
        val escapeRegex = Regex("""\\([\\`*_{}\[\]()#+\-.!|])""")
        return text.replace(escapeRegex, "$1")
    }

    // Match images, bold+italic, bold, italic, ins, strong, em, dt, dd, inline code, inline math, HTML span/font/abbr, autolinks, auto-emails, footnotes, kbd, reference links, line breaks, emojis
    val regex = Regex("(?i)(\\[!\\[[^\\]]*?\\]\\([^\\)]+?\\)\\]\\([^\\)]+?\\)|!\\[[^\\]]*?\\]\\([^\\)]+?\\)|<b>[^\\n]*?</b>|<i>[^\\n]*?</i>|<ruby>[\\s\\S]*?</ruby>|\\*\\*\\*[^\\n]+?\\*\\*\\*|\\*\\*[^\\n]+?\\*\\*|__[^\\n]+?__|\\*[^\\n\\*]+?\\*|_[^_\\n\\r]+?_|~~.*?~~|<del>.*?</del>|<ins>.*?</ins>|<mark>.*?</mark>|<u>.*?</u>|<sub>.*?</sub>|<sup>.*?</sup>|<img\\b[^>]*\\/?>|<strong>.*?</strong>|<em>.*?</em>|<dt>.*?</dt>|<dd>.*?</dd>|\\[![^\\]]+?\\]\\([^\\)]+?\\)|\\[[^\\]]+?\\]\\([^\\)]+?\\)|\\[[^\\]]+?\\]\\[[^\\]]*?\\]|\\[\\^[^\\]]+\\]|`.*?`|\\$\\$.*?\\$\\$|\\$.*?\\$|\\\\\\([\\s\\S]*?\\\\\\)|\\\\\\[[\\s\\S]*?\\\\\\]|<https?://[^>\\s]+>|https?://[^\\s<>\\[\\]\\(\\)،,؛;。！？!?]+|<[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}>|[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}(?![\\w>])|<kbd>.*?</kbd>|<[\\s\\u00A0]*abbr[^>]*>.*?<[\\s\\u00A0]*/[\\s\\u00A0]*abbr[\\s\\u00A0]*>|<[\\s\\u00A0]*span[^>]*>.*?<[\\s\\u00A0]*/[\\s\\u00A0]*span[\\s\\u00A0]*>|<[\\s\\u00A0]*font[^>]*>.*?<[\\s\\u00A0]*/[\\s\\u00A0]*font[\\s\\u00A0]*>|<br\\s*/?>|:[a-zA-Z0-9_+\\-]+:|\\\\$|  $)")
    val matches = regex.findAll(encodedInput)

    for (match in matches) {
        if (match.range.first > index) {
            builder.append(encodedInput.substring(index, match.range.first))
        }

        val matchedText = match.value
        val matchedTextClean = matchedText.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "")
        val matchedTextLower = matchedTextClean.trim().lowercase()
        when {
            // Linked Image: [![alt](img_url)](link_url)
            matchedTextLower.startsWith("[![") && matchedTextLower.contains("](") -> {
                val imgLinkRegex = Regex("\\[!\\[([^\\]]*)\\]\\(([^\\)]+?)\\)\\]\\(([^\\)]+?)\\)")
                val imgLinkMatch = imgLinkRegex.find(matchedTextClean)
                if (imgLinkMatch != null) {
                    val altText = imgLinkMatch.groupValues[1].ifEmpty { "image" }
                    builder.pushStyle(SpanStyle(
                        color = Color(0xFF0E8457),
                        fontStyle = FontStyle.Italic,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    ))
                    builder.append("\uD83D\uDDBC $altText")
                    builder.pop()
                } else {
                    builder.append(matchedText)
                }
            }
            // Inline Image: ![alt](url)
            matchedTextLower.startsWith("![") -> {
                val imgRegex = Regex("!\\[([^\\]]*)\\]\\(([^\\)]+?)\\)")
                val imgMatch = imgRegex.matchEntire(matchedTextClean)
                if (imgMatch != null) {
                    val altText = imgMatch.groupValues[1].ifEmpty { "image" }
                    builder.pushStyle(SpanStyle(
                        color = Color(0xFF0E8457),
                        fontStyle = FontStyle.Italic
                    ))
                    builder.append("\uD83D\uDDBC $altText")
                    builder.pop()
                } else {
                    builder.append(matchedText)
                }
            }
            // Bold + Italic: ***text***
            matchedTextLower.startsWith("***") && matchedTextLower.endsWith("***") -> {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                val content = matchedTextClean.substring(3, matchedTextClean.length - 3)
                builder.append(parseMarkdownInlineStyles(content, codeBgColor, referenceMap))
                builder.pop()
            }
            // Bold: **text** or __text__
            matchedTextLower.startsWith("**") && matchedTextLower.endsWith("**") -> {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                val content = matchedTextClean.substring(2, matchedTextClean.length - 2)
                builder.append(parseMarkdownInlineStyles(content, codeBgColor, referenceMap))
                builder.pop()
            }
            matchedTextLower.startsWith("__") && matchedTextLower.endsWith("__") -> {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                val content = matchedTextClean.substring(2, matchedTextClean.length - 2)
                builder.append(parseMarkdownInlineStyles(content, codeBgColor, referenceMap))
                builder.pop()
            }
            // Italic: *text* or _text_
            matchedTextLower.startsWith("*") && matchedTextLower.endsWith("*") -> {
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                val content = matchedTextClean.substring(1, matchedTextClean.length - 1)
                builder.append(parseMarkdownInlineStyles(content, codeBgColor, referenceMap))
                builder.pop()
            }
            matchedTextLower.startsWith("_") && matchedTextLower.endsWith("_") -> {
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                val content = matchedTextClean.substring(1, matchedTextClean.length - 1)
                builder.append(parseMarkdownInlineStyles(content, codeBgColor, referenceMap))
                builder.pop()
            }
            // Strikethrough: ~~text~~ or <del>text</del>
            matchedTextLower.startsWith("~~") && matchedTextLower.endsWith("~~") -> {
                builder.pushStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                val content = matchedTextClean.substring(2, matchedTextClean.length - 2)
                builder.append(parseMarkdownInlineStyles(content, codeBgColor, referenceMap))
                builder.pop()
            }
            matchedTextLower.startsWith("<del>") && matchedTextLower.endsWith("</del>") -> {
                builder.pushStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                val content = matchedTextClean.substring(5, matchedTextClean.length - 6)
                builder.append(parseMarkdownInlineStyles(content, codeBgColor, referenceMap))
                builder.pop()
            }
            // Underline: <ins>text</ins>
            matchedTextLower.startsWith("<ins>") && matchedTextLower.endsWith("</ins>") -> {
                builder.pushStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
                val content = matchedTextClean.substring(5, matchedTextClean.length - 6)
                builder.append(parseMarkdownInlineStyles(content, codeBgColor, referenceMap))
                builder.pop()
            }
            matchedTextLower.startsWith("<mark>") && matchedTextLower.endsWith("</mark>") -> {
                builder.pushStyle(SpanStyle(background = Color(0xFFFFF176), color = Color.Black))
                val content = matchedTextClean.substring(6, matchedTextClean.length - 7)
                builder.append(parseMarkdownInlineStyles(content, codeBgColor, referenceMap, inlineCodeTextColor))
                builder.pop()
            }
            matchedTextLower.startsWith("<u>") && matchedTextLower.endsWith("</u>") -> {
                builder.pushStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
                val content = matchedTextClean.substring(3, matchedTextClean.length - 4)
                builder.append(parseMarkdownInlineStyles(content, codeBgColor, referenceMap, inlineCodeTextColor))
                builder.pop()
            }
            matchedTextLower.startsWith("<sub>") && matchedTextLower.endsWith("</sub>") -> {
                builder.pushStyle(SpanStyle(baselineShift = androidx.compose.ui.text.style.BaselineShift.Subscript, fontSize = 0.75.em))
                val content = matchedTextClean.substring(5, matchedTextClean.length - 6)
                builder.append(parseMarkdownInlineStyles(content, codeBgColor, referenceMap, inlineCodeTextColor))
                builder.pop()
            }
            matchedTextLower.startsWith("<sup>") && matchedTextLower.endsWith("</sup>") -> {
                builder.pushStyle(SpanStyle(baselineShift = androidx.compose.ui.text.style.BaselineShift.Superscript, fontSize = 0.75.em))
                val content = matchedTextClean.substring(5, matchedTextClean.length - 6)
                builder.append(parseMarkdownInlineStyles(content, codeBgColor, referenceMap, inlineCodeTextColor))
                builder.pop()
            }
            matchedTextLower.startsWith("<img") -> {
                val altMatch = Regex("""(?i)alt\s*=\s*["']([^"']*)["']""").find(matchedTextClean)
                val altText = altMatch?.groupValues?.get(1)?.ifEmpty { "image" } ?: "image"
                builder.pushStyle(SpanStyle(
                    color = Color(0xFF0E8457),
                    fontStyle = FontStyle.Italic
                ))
                builder.append("\uD83D\uDDBC $altText")
                builder.pop()
            }
            matchedTextLower.startsWith("<b>") && matchedTextLower.endsWith("</b>") -> {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                val content = matchedTextClean.substring(3, matchedTextClean.length - 4)
                builder.append(parseMarkdownInlineStyles(content, codeBgColor, referenceMap))
                builder.pop()
            }
            matchedTextLower.startsWith("<i>") && matchedTextLower.endsWith("</i>") -> {
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                val content = matchedTextClean.substring(3, matchedTextClean.length - 4)
                builder.append(parseMarkdownInlineStyles(content, codeBgColor, referenceMap))
                builder.pop()
            }
            matchedTextLower.startsWith("<ruby") && matchedTextLower.endsWith("</ruby>") -> {
                val withoutRp = matchedTextClean.replace(Regex("(?is)<rp>.*?</rp>"), "")
                val rubyInner = withoutRp.replace(Regex("(?i)^<ruby>|</ruby>$"), "").trim()
                val rtRegex = Regex("(?is)(.*?)<rt>(.*?)</rt>")
                val rtMatches = rtRegex.findAll(rubyInner).toList()
                if (rtMatches.isNotEmpty()) {
                    for (m in rtMatches) {
                        val base = m.groupValues[1].replace(Regex("(?i)<rb>|</rb>"), "").trim()
                        val rt = m.groupValues[2].trim()
                        if (base.isNotEmpty()) {
                            builder.append(parseMarkdownInlineStyles(base, codeBgColor, referenceMap))
                        }
                        if (rt.isNotEmpty()) {
                            builder.pushStyle(SpanStyle(fontSize = 0.75.em, color = Color.Gray))
                            builder.append(" ($rt)")
                            builder.pop()
                        }
                    }
                } else {
                    val clean = matchedTextClean.replace(Regex("(?i)<[^>]+>"), "")
                    builder.append(clean)
                }
            }
            matchedTextLower.startsWith("<strong>") && matchedTextLower.endsWith("</strong>") -> {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                val content = matchedTextClean.substring(8, matchedTextClean.length - 9)
                builder.append(parseMarkdownInlineStyles(content, codeBgColor, referenceMap))
                builder.pop()
            }
            matchedTextLower.startsWith("<em>") && matchedTextLower.endsWith("</em>") -> {
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                val content = matchedTextClean.substring(4, matchedTextClean.length - 5)
                builder.append(parseMarkdownInlineStyles(content, codeBgColor, referenceMap))
                builder.pop()
            }
            // Definition term: <dt>text</dt> — rendered bold
            matchedTextLower.startsWith("<dt>") && matchedTextLower.endsWith("</dt>") -> {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                val content = matchedTextClean.substring(4, matchedTextClean.length - 5)
                builder.append(parseMarkdownInlineStyles(content, codeBgColor, referenceMap))
                builder.pop()
            }
            // Definition description: <dd>text</dd> — rendered italic with indent
            matchedTextLower.startsWith("<dd>") && matchedTextLower.endsWith("</dd>") -> {
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = androidx.compose.ui.graphics.Color.Unspecified))
                val content = matchedTextClean.substring(4, matchedTextClean.length - 5)
                builder.append("    ") // indent
                builder.append(parseMarkdownInlineStyles(content, codeBgColor, referenceMap))
                builder.pop()
            }
            // Inline footnote: [^1]
            matchedTextLower.startsWith("[^") && matchedTextLower.endsWith("]") && !matchedTextLower.contains("](") && !matchedTextLower.contains("][") -> {
                val label = matchedTextClean.substring(2, matchedTextClean.length - 1)
                builder.pushStyle(SpanStyle(
                    color = Color(0xFF0E8457), // Accent green link color
                    baselineShift = androidx.compose.ui.text.style.BaselineShift.Superscript,
                    fontSize = 12.sp,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ))
                builder.append(decodeEscapesUnescaped(label))
                builder.pop()
            }
            matchedTextLower.startsWith("[") && matchedTextLower.contains("][") -> {
                val refRegex = Regex("\\[([^\\]]+?)\\]\\[([^\\]]*?)\\]")
                val refMatch = refRegex.matchEntire(matchedTextClean)
                if (refMatch != null) {
                    val linkText = refMatch.groupValues[1]
                    val label = refMatch.groupValues[2].trim().lowercase().ifEmpty { linkText.trim().lowercase() }
                    val decodedLabel = decodeEscapesUnescaped(label)
                    val refVal = referenceMap[decodedLabel]
                    if (refVal != null) {
                        builder.pushStyle(SpanStyle(
                            color = Color(0xFF0E8457), // Accent green link color
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        ))
                        builder.append(parseMarkdownInlineStyles(linkText, codeBgColor, referenceMap))
                        builder.pop()
                    } else {
                        builder.append(matchedText)
                    }
                } else {
                    builder.append(matchedText)
                }
            }
            matchedTextLower.startsWith("[") && matchedTextLower.contains("](") -> {
                val linkRegex = Regex("\\[([^\\]]+?)\\]\\(([^\\)]+?)\\)")
                val linkMatch = linkRegex.matchEntire(matchedTextClean)
                if (linkMatch != null) {
                    val linkText = linkMatch.groupValues[1]
                    builder.pushStyle(SpanStyle(
                        color = Color(0xFF0E8457), // Accent green link color
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    ))
                    builder.append(parseMarkdownInlineStyles(linkText, codeBgColor, referenceMap))
                    builder.pop()
                } else {
                    builder.append(matchedText)
                }
            }
            // Bare URL: https://... (no angle brackets)
            matchedTextLower.startsWith("http://") || matchedTextLower.startsWith("https://") -> {
                val url = matchedTextClean.trimEnd('.', ',', ')', ']', ';', '\u060C', '\u061B')
                builder.pushStyle(SpanStyle(
                    color = Color(0xFF0E8457),
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ))
                builder.append(url)
                builder.pop()
            }
            // Bracketed autolink: <https://...>
            matchedTextLower.startsWith("<http") && matchedTextLower.endsWith(">") -> {
                val url = matchedTextClean.substring(1, matchedTextClean.length - 1)
                builder.pushStyle(SpanStyle(
                    color = Color(0xFF0E8457), // Accent green link color
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ))
                builder.append(url)
                builder.pop()
            }
            // Bracketed email: <email@domain.com>
            matchedTextLower.startsWith("<") && matchedTextLower.contains("@") && matchedTextLower.endsWith(">") -> {
                val email = matchedTextClean.substring(1, matchedTextClean.length - 1)
                builder.pushStyle(SpanStyle(
                    color = Color(0xFF0E8457), // Accent green link color
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ))
                builder.append(email)
                builder.pop()
            }
            // Bare email: email@domain.com (no angle brackets)
            matchedTextLower.contains("@") && !matchedTextLower.startsWith("<") -> {
                builder.pushStyle(SpanStyle(
                    color = Color(0xFF0E8457),
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ))
                builder.append(matchedTextClean)
                builder.pop()
            }
            matchedTextLower.startsWith("<kbd>") && matchedTextLower.endsWith("</kbd>") -> {
                val keyText = matchedTextClean.substring(5, matchedTextClean.length - 6)
                builder.pushStyle(SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = codeBgColor,
                    color = inlineCodeTextColor,
                    fontWeight = FontWeight.Bold
                ))
                builder.append(" ${decodeEscapesUnescaped(keyText)} ")
                builder.pop()
            }
            matchedTextLower.startsWith("`") && matchedTextLower.endsWith("`") -> {
                builder.pushStyle(SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = codeBgColor,
                    color = inlineCodeTextColor
                ))
                val content = matchedTextClean.substring(1, matchedTextClean.length - 1)
                builder.append(decodeEscapesEscaped(content))
                builder.pop()
            }
            matchedTextLower.startsWith(":") && matchedTextLower.endsWith(":") -> {
                val emojiMap = mapOf(
                    ":memo:" to "📝",
                    ":heart:" to "❤️",
                    ":sparkles:" to "✨",
                    ":smile:" to "😄",
                    ":+1:" to "👍",
                    ":-1:" to "👎",
                    ":tada:" to "🎉",
                    ":rocket:" to "🚀",
                    ":fire:" to "🔥",
                    ":white_check_mark:" to "✅",
                    ":x:" to "❌",
                    ":warning:" to "⚠️",
                    ":construction:" to "🚧",
                    ":bug:" to "🐛",
                    ":bulb:" to "💡",
                    ":star:" to "⭐"
                )
                val emoji = emojiMap[matchedTextLower]
                if (emoji != null) {
                    builder.append(emoji)
                } else {
                    builder.append(matchedTextClean)
                }
            }
            matchedTextLower.startsWith("$$") && matchedTextLower.endsWith("$$") -> {
                val content = matchedTextClean.substring(2, matchedTextClean.length - 2)
                builder.append(renderInlineMath(content, codeBgColor))
            }
            matchedTextLower.startsWith("$") && matchedTextLower.endsWith("$") -> {
                val content = matchedTextClean.substring(1, matchedTextClean.length - 1)
                builder.append(renderInlineMath(content, codeBgColor))
            }
            matchedTextLower.startsWith("\\(") && matchedTextLower.endsWith("\\)") -> {
                val content = matchedTextClean.substring(2, matchedTextClean.length - 2)
                builder.append(renderInlineMath(content, codeBgColor))
            }
            matchedTextLower.startsWith("\\[") && matchedTextLower.endsWith("\\]") -> {
                val content = matchedTextClean.substring(2, matchedTextClean.length - 2)
                builder.append(renderInlineMath(content, codeBgColor))
            }
            matchedTextLower.startsWith("<abbr") || matchedTextLower.contains("abbr") -> {
                val reallyCleanText = matchedTextClean.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "")
                val abbrRegex = Regex("(?is)<[\\s\\u00A0]*abbr([^>]*)>(.*?)<[\\s\\u00A0]*/[\\s\\u00A0]*abbr[\\s\\u00A0]*>")
                val abbrMatch = abbrRegex.find(reallyCleanText)
                if (abbrMatch != null) {
                    val innerText = abbrMatch.groupValues[2]
                    builder.pushStyle(SpanStyle(
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        color = Color(0xFF0E8457) // Accent green for tooltip
                    ))
                    builder.append(parseMarkdownInlineStyles(innerText, codeBgColor, referenceMap, inlineCodeTextColor))
                    builder.pop()
                } else {
                    builder.append(matchedText)
                }
            }
            matchedTextLower.startsWith("<font") || matchedTextLower.contains("font") -> {
                val reallyCleanText = matchedTextClean.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "")
                val fontRegex = Regex("(?is)<[\\s\\u00A0]*font([^>]*)>(.*?)<[\\s\\u00A0]*/[\\s\\u00A0]*font[\\s\\u00A0]*>")
                val fontMatch = fontRegex.find(reallyCleanText)
                if (fontMatch != null) {
                    val attrsStr = fontMatch.groupValues[1]
                    val innerText = fontMatch.groupValues[2]

                    var color: Color? = null
                    var fontSize: androidx.compose.ui.unit.TextUnit? = null

                    val colorMatch = Regex("(?i)color[\\s\\u00A0]*=[\\s\\u00A0]*([^\\s\\u00A0>]+|'[^']*'|\"[^\"]*\")").find(attrsStr)
                    if (colorMatch != null) {
                        val colorValue = decodeEscapesUnescaped(cleanQuotes(colorMatch.groupValues[1]))
                        color = parseHtmlColor(colorValue)
                    }

                    val sizeMatch = Regex("(?i)size[\\s\\u00A0]*=[\\s\\u00A0]*([^\\s\\u00A0>]+|'[^']*'|\"[^\"]*\")").find(attrsStr)
                    if (sizeMatch != null) {
                        val sizeValue = decodeEscapesUnescaped(cleanQuotes(sizeMatch.groupValues[1]))
                        fontSize = parseHtmlFontSizeAttribute(sizeValue)
                    }

                    builder.pushStyle(SpanStyle(
                        color = color ?: Color.Unspecified,
                        fontSize = fontSize ?: androidx.compose.ui.unit.TextUnit.Unspecified
                    ))
                    builder.append(parseMarkdownInlineStyles(innerText, codeBgColor, referenceMap, inlineCodeTextColor))
                    builder.pop()
                } else {
                    builder.append(matchedText)
                }
            }
            matchedTextLower.startsWith("<span") || matchedTextLower.contains("span") -> {
                val reallyCleanText = matchedTextClean.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "")
                val spanRegex = Regex("(?is)<[\\s\\u00A0]*span([^>]*)>(.*?)<[\\s\\u00A0]*/[\\s\\u00A0]*span[\\s\\u00A0]*>")
                val spanMatch = spanRegex.find(reallyCleanText)
                if (spanMatch != null) {
                    val attrsStr = spanMatch.groupValues[1]
                    val innerText = spanMatch.groupValues[2]

                    var color: Color? = null
                    var fontSize: androidx.compose.ui.unit.TextUnit? = null
                    var fontWeight: FontWeight? = null
                    var fontStyle: FontStyle? = null

                    val styleMatch = Regex("(?i)style[\\s\\u00A0]*=[\\s\\u00A0]*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\\u00A0>]+))").find(attrsStr)
                    if (styleMatch != null) {
                        val rawStyle = styleMatch.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: ""
                        val styleStr = decodeEscapesUnescaped(rawStyle.trim())

                        styleStr.split(";").forEach { stylePart ->
                            val parts = stylePart.split(":")
                            if (parts.size == 2) {
                                val key = parts[0].replace(Regex("[\\s\\u00A0]+"), "").trim().lowercase()
                                val value = parts.drop(1).joinToString(":").replace(Regex("[\\s\\u00A0]+"), " ").trim()
                                if (key == "color") {
                                    color = parseHtmlColor(value)
                                } else if (key == "font-size") {
                                    fontSize = parseHtmlFontSize(value)
                                } else if (key == "font-weight") {
                                    if (value.lowercase() == "bold" || value.lowercase() == "700" || value.lowercase() == "800") {
                                        fontWeight = FontWeight.Bold
                                    }
                                } else if (key == "font-style") {
                                    if (value.lowercase() == "italic") {
                                        fontStyle = FontStyle.Italic
                                    }
                                }
                            }
                        }
                    }

                    builder.pushStyle(SpanStyle(
                        color = color ?: Color.Unspecified,
                        fontSize = fontSize ?: androidx.compose.ui.unit.TextUnit.Unspecified,
                        fontWeight = fontWeight,
                        fontStyle = fontStyle
                    ))
                    builder.append(parseMarkdownInlineStyles(innerText, codeBgColor, referenceMap, inlineCodeTextColor))
                    builder.pop()
                } else {
                    builder.append(matchedText)
                }
            }
            matchedTextLower.startsWith("<br") -> {
                builder.append("\n")
            }
            matchedTextLower == "  " || matchedTextLower == "\\" -> {
                builder.append("\n")
            }
            else -> {
                builder.append(matchedText)
            }
        }
        index = match.range.last + 1
    }

    if (index < encodedInput.length) {
        builder.append(encodedInput.substring(index))
    }

    val finalAnnotated = builder.toAnnotatedString()
    return decodeAnnotatedString(finalAnnotated)
}

fun parseHtmlColor(colorStr: String): Color? {
    val clean = colorStr.trim().lowercase().replace(Regex("[\\s\\u00A0]+"), "")
    val colorMap = mapOf(
        "red" to Color(0xFFFF0000),
        "green" to Color(0xFF008000),
        "lime" to Color(0xFF00FF00),
        "blue" to Color(0xFF0000FF),
        "yellow" to Color(0xFFFFFF00),
        "orange" to Color(0xFFFF8C00),
        "purple" to Color(0xFF800080),
        "pink" to Color(0xFFFF69B4),
        "black" to Color(0xFF000000),
        "white" to Color(0xFFFFFFFF),
        "gray" to Color(0xFF808080),
        "grey" to Color(0xFF808080),
        "cyan" to Color(0xFF00FFFF),
        "magenta" to Color(0xFFFF00FF),
        "brown" to Color(0xFFA52A2A),
        "silver" to Color(0xFFC0C0C0),
        "gold" to Color(0xFFFFD700),
        "navy" to Color(0xFF000080),
        "teal" to Color(0xFF008080),
        "maroon" to Color(0xFF800000),
        "olive" to Color(0xFF808000),
        "aqua" to Color(0xFF00FFFF),
        "fuchsia" to Color(0xFFFF00FF),
        "coral" to Color(0xFFFF6347),
        "salmon" to Color(0xFFFA8072),
        "violet" to Color(0xFFEE82EE),
        "indigo" to Color(0xFF4B0082),
        "turquoise" to Color(0xFF40E0D0),
        "crimson" to Color(0xFFDC143C),
        "tomato" to Color(0xFFFF6347),
        "chocolate" to Color(0xFFD2691E),
        "forestgreen" to Color(0xFF228B22),
        "darkorange" to Color(0xFFFF8C00),
        "darkred" to Color(0xFF8B0000),
        "darkblue" to Color(0xFF00008B),
        "darkgreen" to Color(0xFF006400),
        "deeppink" to Color(0xFFFF1493),
        "hotpink" to Color(0xFFFF69B4),
        "dodgerblue" to Color(0xFF1E90FF),
        "royalblue" to Color(0xFF4169E1),
        "steelblue" to Color(0xFF4682B4),
        "skyblue" to Color(0xFF87CEEB),
        "lightblue" to Color(0xFFADD8E6),
        "lightgreen" to Color(0xFF90EE90),
        "lightcoral" to Color(0xFFF08080),
        "lightyellow" to Color(0xFFFFFFE0),
        "lightsalmon" to Color(0xFFFFA07A),
        "khaki" to Color(0xFFF0E68C),
        "beige" to Color(0xFFF5F5DC),
        "lavender" to Color(0xFFE6E6FA),
        "plum" to Color(0xFFDDA0DD)
    )
    if (colorMap.containsKey(clean)) {
        return colorMap[clean]
    }

    val hex = clean.removePrefix("#")
    return try {
        when (hex.length) {
            3 -> {
                val r = hex[0].toString().repeat(2).toInt(16)
                val g = hex[1].toString().repeat(2).toInt(16)
                val b = hex[2].toString().repeat(2).toInt(16)
                val argb = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                Color(argb)
            }
            6 -> {
                val r = hex.substring(0, 2).toInt(16)
                val g = hex.substring(2, 4).toInt(16)
                val b = hex.substring(4, 6).toInt(16)
                val argb = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                Color(argb)
            }
            8 -> {
                val a = hex.substring(0, 2).toInt(16)
                val r = hex.substring(2, 4).toInt(16)
                val g = hex.substring(4, 6).toInt(16)
                val b = hex.substring(6, 8).toInt(16)
                val argb = (a shl 24) or (r shl 16) or (g shl 8) or b
                Color(argb)
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

fun parseHtmlFontSize(sizeStr: String): androidx.compose.ui.unit.TextUnit? {
    val clean = sizeStr.trim().lowercase()
    val numberPart = clean.filter { it.isDigit() || it == '.' }
    val num = numberPart.toFloatOrNull() ?: return null
    return when {
        clean.endsWith("px") -> num.sp
        clean.endsWith("sp") -> num.sp
        clean.endsWith("pt") -> (num * 1.33f).sp
        clean.endsWith("em") -> (num * 16f).sp
        clean.endsWith("%") -> (num * 0.16f).sp
        else -> num.sp
    }
}

fun parseHtmlFontSizeAttribute(sizeStr: String): androidx.compose.ui.unit.TextUnit? {
    val clean = sizeStr.trim()
    val intVal = clean.toIntOrNull()
    if (intVal != null) {
        return when (intVal) {
            1 -> 10.sp
            2 -> 12.sp
            3 -> 14.sp
            4 -> 18.sp
            5 -> 24.sp
            6 -> 32.sp
            7 -> 42.sp
            else -> if (intVal > 7) intVal.sp else null
        }
    }
    return parseHtmlFontSize(clean)
}


