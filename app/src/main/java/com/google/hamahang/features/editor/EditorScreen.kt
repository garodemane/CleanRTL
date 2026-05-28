package com.google.hamahang.features.editor

import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

    var navigationTab by remember { mutableStateOf(0) }

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
        try {
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val pdfFile = getUniqueFile(downloadsFolder, "CleanRTL_Corrected", "pdf")
            val outputStream = FileOutputStream(pdfFile)
            
            NativePdfExporter.exportToPdf(
                context = context,
                text = correctedText,
                outputStream = outputStream,
                title = "CleanRTL Document",
                baseFontSize = fontSizeSp.toFloat()
            )
            Toast.makeText(context, "${Loc.tr("toast_pdf_saved", currentLanguage)} (${pdfFile.name})", Toast.LENGTH_LONG).show()
        } catch (e: java.lang.Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
                fontSizePx = fontSizeSp
            )
            Toast.makeText(context, "${Loc.tr("toast_html_saved", currentLanguage)} (${htmlFile.name})", Toast.LENGTH_LONG).show()
        } catch (e: java.lang.Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    val layoutDirection = if (currentLanguage == AppLanguage.FA) LayoutDirection.Rtl else LayoutDirection.Ltr
    
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            CurveHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.TopCenter)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 96.dp)
            ) {
                // Customized dynamic Top Bar
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
                                            uiFontScale = uiFontScale
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        MarkdownPreviewPane(
                                            title = Loc.tr("preview_label", currentLanguage),
                                            text = correctedText, 
                                            baseFontSize = fontSizeSp,
                                            uiFontScale = uiFontScale
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
                                            uiFontScale = uiFontScale
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
                                            uiFontScale = uiFontScale
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

            FloatingBottomNavigation(
                selectedTab = navigationTab,
                onTabSelected = { navigationTab = it },
                currentLanguage = currentLanguage,
                uiFontScale = uiFontScale,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            )
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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontSize = (12.sp * uiFontScale),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val paragraphs = text.split("\n")
            var inCodeBlock = false
            val codeLines = mutableListOf<String>()

            var idx = 0
            while (idx < paragraphs.size) {
                val paragraph = paragraphs[idx]
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

                // Check if this line starts a table (and not inside code block)
                if (!inCodeBlock && trimmed.startsWith("|") && trimmed.endsWith("|")) {
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
                                baseFontSize = baseFontSize
                            )
                            
                            idx = k
                            continue
                        }
                    }
                }

                if (trimmed.startsWith("```")) {
                    if (inCodeBlock) {
                        ComposeCodeBlock(lines = codeLines, fontSize = (baseFontSize * 0.85).sp)
                        codeLines.clear()
                        inCodeBlock = false
                    } else {
                        inCodeBlock = true
                    }
                    idx++
                    continue
                }

                if (inCodeBlock) {
                    codeLines.add(paragraph)
                    idx++
                    continue
                }

                val numberedListMatch = Regex("^(\\d+\\.)\\s+(.*)").matchEntire(trimmed)

                when {
                    trimmed.startsWith("# ") -> {
                        MarkdownHeader(text = bidiPrefix + trimmed.substring(2), size = (baseFontSize * 1.5).sp, weight = FontWeight.Bold)
                    }
                    trimmed.startsWith("## ") -> {
                        MarkdownHeader(text = bidiPrefix + trimmed.substring(3), size = (baseFontSize * 1.3).sp, weight = FontWeight.Bold)
                    }
                    trimmed.startsWith("### ") -> {
                        MarkdownHeader(text = bidiPrefix + trimmed.substring(4), size = (baseFontSize * 1.15).sp, weight = FontWeight.Bold)
                    }
                    trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                        MarkdownListItem(text = bidiPrefix + trimmed.substring(2), fontSize = baseFontSize.sp)
                    }
                    numberedListMatch != null -> {
                        val number = numberedListMatch.groupValues[1]
                        val content = numberedListMatch.groupValues[2]
                        MarkdownNumberedListItem(number = number, text = bidiPrefix + content, fontSize = baseFontSize.sp)
                    }
                    trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                        MarkdownDivider()
                    }
                    trimmed.startsWith("> ") || trimmed.startsWith(">") -> {
                        val blockquoteText = if (trimmed.startsWith("> ")) trimmed.substring(2) else trimmed.substring(1)
                        MarkdownBlockquote(text = bidiPrefix + blockquoteText, fontSize = (baseFontSize * 0.95).sp)
                    }
                    else -> {
                        MarkdownParagraph(text = bidiPrefix + cleanParagraph, fontSize = baseFontSize.sp)
                    }
                }
                idx++
            }

            if (inCodeBlock && codeLines.isNotEmpty()) {
                ComposeCodeBlock(lines = codeLines, fontSize = (baseFontSize * 0.85).sp)
            }
        }
    }
}

@Composable
fun MarkdownHeader(
    text: String,
    size: androidx.compose.ui.unit.TextUnit,
    weight: FontWeight
) {
    val isRtl = TextRepairProcessor.isParagraphRtl(text)
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant
    Text(
        text = parseMarkdownInlineStyles(text, codeBgColor),
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
fun MarkdownListItem(text: String, fontSize: androidx.compose.ui.unit.TextUnit) {
    val isRtl = TextRepairProcessor.isParagraphRtl(text)
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant
    CompositionLocalProvider(
        LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, start = 4.dp, end = 8.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary)
            )
            Text(
                text = parseMarkdownInlineStyles(text, codeBgColor),
                style = TextStyle(
                    fontSize = fontSize,
                    textAlign = TextAlign.Start,
                    textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MarkdownNumberedListItem(
    number: String,
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    val isRtl = TextRepairProcessor.isParagraphRtl(text)
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant
    CompositionLocalProvider(
        LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
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
                text = parseMarkdownInlineStyles(text, codeBgColor),
                style = TextStyle(
                    fontSize = fontSize,
                    textAlign = TextAlign.Start,
                    textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MarkdownBlockquote(text: String, fontSize: androidx.compose.ui.unit.TextUnit) {
    val isRtl = TextRepairProcessor.isParagraphRtl(text)
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            if (!isRtl) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(MaterialTheme.colorScheme.secondary)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = parseMarkdownInlineStyles(text, codeBgColor),
                style = TextStyle(
                    fontSize = fontSize,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = if (isRtl) TextAlign.Right else TextAlign.Left,
                    textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr
                ),
                modifier = Modifier.weight(1f)
            )
            if (isRtl) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }
        }
    }
}

@Composable
fun ComposeCodeBlock(lines: List<String>, fontSize: androidx.compose.ui.unit.TextUnit) {
    val codeText = lines.joinToString("\n")
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
                text = codeText,
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

@Composable
fun MarkdownParagraph(text: String, fontSize: androidx.compose.ui.unit.TextUnit) {
    val isRtl = TextRepairProcessor.isParagraphRtl(text)
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant
    Text(
        text = parseMarkdownInlineStyles(text, codeBgColor),
        style = TextStyle(
            fontSize = fontSize,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = if (isRtl) TextAlign.Right else TextAlign.Left,
            textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr
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
    val rawParts = trimmed.split("|")
    val parts = mutableListOf<String>()
    for (idx in 1 until rawParts.size - 1) {
        parts.add(rawParts[idx].trim())
    }
    if (rawParts.size <= 2 && trimmed.contains("|")) {
        return trimmed.split("|").map { it.trim() }.filter { it.isNotEmpty() }
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

@Composable
fun TableCell(
    text: String,
    alignment: TableColumnAlignment,
    isHeader: Boolean,
    baseFontSize: Int,
    modifier: Modifier = Modifier
) {
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant
    val resolvedText = parseMarkdownInlineStyles(text, codeBgColor)
    
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
                    .verticalScroll(rememberScrollState())
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

fun parseMarkdownInlineStyles(input: String, codeBgColor: Color): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var index = 0

    // Match bold, italic, inline code, or HTML span tags
    val regex = Regex("(\\*\\*.*?\\*\\*|\\*.*?\\*|`.*?`|<\\s*span\\s+style\\s*=\\s*[\"']([^\"']*)[\"']\\s*>.*?<\\s*/\\s*span\\s*>)")
    val matches = regex.findAll(input)

    for (match in matches) {
        if (match.range.first > index) {
            builder.append(input.substring(index, match.range.first))
        }

        val matchedText = match.value
        when {
            matchedText.startsWith("**") && matchedText.endsWith("**") -> {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                builder.append(parseMarkdownInlineStyles(matchedText.substring(2, matchedText.length - 2), codeBgColor))
                builder.pop()
            }
            matchedText.startsWith("*") && matchedText.endsWith("*") -> {
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                builder.append(parseMarkdownInlineStyles(matchedText.substring(1, matchedText.length - 1), codeBgColor))
                builder.pop()
            }
            matchedText.startsWith("`") && matchedText.endsWith("`") -> {
                builder.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBgColor))
                builder.append(matchedText.substring(1, matchedText.length - 1))
                builder.pop()
            }
            matchedText.startsWith("<") && matchedText.endsWith(">") -> {
                val spanRegex = Regex("<\\s*span\\s+style\\s*=\\s*[\"']([^\"']*)[\"']\\s*>(.*?)<\\s*/\\s*span\\s*>")
                val spanMatch = spanRegex.matchEntire(matchedText)
                if (spanMatch != null) {
                    val styleStr = spanMatch.groupValues[1]
                    val innerText = spanMatch.groupValues[2]

                    var color: Color? = null
                    var fontSize: androidx.compose.ui.unit.TextUnit? = null

                    styleStr.split(";").forEach { stylePart ->
                        val parts = stylePart.split(":")
                        if (parts.size == 2) {
                            val key = parts[0].trim().lowercase()
                            val value = parts[1].trim()
                            if (key == "color") {
                                color = parseHtmlColor(value)
                            } else if (key == "font-size") {
                                fontSize = parseHtmlFontSize(value)
                            }
                        }
                    }

                    builder.pushStyle(SpanStyle(
                        color = color ?: Color.Unspecified,
                        fontSize = fontSize ?: androidx.compose.ui.unit.TextUnit.Unspecified
                    ))
                    builder.append(parseMarkdownInlineStyles(innerText, codeBgColor))
                    builder.pop()
                } else {
                    builder.append(matchedText)
                }
            }
            else -> {
                builder.append(matchedText)
            }
        }
        index = match.range.last + 1
    }

    if (index < input.length) {
        builder.append(input.substring(index))
    }

    return builder.toAnnotatedString()
}

fun parseHtmlColor(colorStr: String): Color? {
    val clean = colorStr.trim().lowercase()
    val colorMap = mapOf(
        "red" to Color.Red,
        "green" to Color(0xFF00FF00),
        "blue" to Color.Blue,
        "yellow" to Color.Yellow,
        "black" to Color.Black,
        "white" to Color.White,
        "gray" to Color.Gray,
        "grey" to Color.Gray,
        "cyan" to Color.Cyan,
        "magenta" to Color.Magenta
    )
    if (colorMap.containsKey(clean)) {
        return colorMap[clean]
    }

    val hex = clean.removePrefix("#")
    return try {
        if (hex.length == 3) {
            val r = hex[0].toString().repeat(2).toInt(16)
            val g = hex[1].toString().repeat(2).toInt(16)
            val b = hex[2].toString().repeat(2).toInt(16)
            Color(red = r, green = g, blue = b)
        } else if (hex.length == 6) {
            val r = hex.substring(0, 2).toInt(16)
            val g = hex.substring(2, 4).toInt(16)
            val b = hex.substring(4, 6).toInt(16)
            Color(red = r, green = g, blue = b)
        } else if (hex.length == 8) {
            val a = hex.substring(0, 2).toInt(16)
            val r = hex.substring(2, 4).toInt(16)
            val g = hex.substring(4, 6).toInt(16)
            val b = hex.substring(6, 8).toInt(16)
            Color(red = r, green = g, blue = b, alpha = a)
        } else {
            null
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


