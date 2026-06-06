package com.google.hamahang.core.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import com.google.hamahang.core.bidi.TextRepairProcessor
import java.io.OutputStream

object NativePdfExporter {

    /**
     * Advanced Markdown-aware PDF Exporter.
     * Parses markdown structures (headings, bullet lists, blockquotes, code blocks)
     * and renders them into formatted PDF pages natively with proper RTL/LTR alignment.
     */
    fun exportToPdf(
        context: Context,
        text: String,
        outputStream: OutputStream,
        title: String = "CleanRTL Document",
        baseFontSize: Float = 12f,
        mermaidBitmaps: Map<String, android.graphics.Bitmap> = emptyMap(),
        isJustified: Boolean = false
    ) {
        val pdfDocument = PdfDocument()

        // Standard A4 Dimensions in points: 595 x 842
        val pageWidth = 595
        val pageHeight = 842
        val margin = 50f
        val printableWidth = pageWidth - (margin * 2)

        // Typeface definitions
        val regularTypeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        val boldTypeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val italicTypeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
        val monospaceTypeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        val serifItalicTypeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)

        // TextPaint configurations
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            linkColor = Color.BLUE
            textSize = baseFontSize
            typeface = regularTypeface
        }

        val rawParagraphs = text.split("\n")
        val paragraphs = mutableListOf<String>()
        val referenceMap = mutableMapOf<String, Pair<String, String?>>()

        val refDefRegex = Regex("""^\s*\[([^\]]+)\]:\s*(\S+)(?:\s+["'(]([^"')]*)["'))]?)?\s*$""")
        val footnoteRegex = Regex("""^\s*\[\^([^\]]+)\]:\s*(.+)$""")
        val footnotesMap = mutableMapOf<String, String>()

        for (p in rawParagraphs) {
            val cleanP = p.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
            val match = refDefRegex.matchEntire(cleanP)
            val fnMatch = footnoteRegex.matchEntire(cleanP)
            if (match != null) {
                val label = match.groupValues[1].trim().lowercase()
                val url = match.groupValues[2].trim()
                val titleVal = match.groupValues[3].trim().takeIf { it.isNotEmpty() }
                referenceMap[label] = Pair(url, titleVal)
            } else if (fnMatch != null) {
                val id = fnMatch.groupValues[1].trim()
                val fnText = fnMatch.groupValues[2].trim()
                footnotesMap[id] = fnText
            } else {
                paragraphs.add(p)
            }
        }

        var currentPageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas
        
        // Start layout directly with standard top margin offset since header is removed
        var yOffset = margin

        var inCodeBlock = false
        val codeBlockLines = mutableListOf<String>()
        var inMathBlock = false
        val mathBlockLines = mutableListOf<String>()
        var inMermaidBlock = false
        val mermaidBlockLines = mutableListOf<String>()
        var inDetailsBlock = false

        var idx = 0
        while (idx < paragraphs.size) {
            val paragraph = paragraphs[idx]

            // 1. Math block accumulation check first
            if (inMathBlock) {
                val trimmed = paragraph.trim()
                val cleanTrimmed = trimmed
                    .replace(Regex("^[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]+"), "")
                    .replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]+$"), "")
                    .trim()
                if (cleanTrimmed.endsWith("$$")) {
                    val cleanLine = cleanTrimmed.removeSuffix("$$")
                    if (cleanLine.isNotEmpty()) {
                        mathBlockLines.add(cleanLine)
                    }
                    yOffset = drawBlockMath(
                        canvas, mathBlockLines, textPaint, serifItalicTypeface,
                        margin, yOffset, printableWidth, pageHeight - margin,
                        onNewPage = {
                            pdfDocument.finishPage(currentPage)
                            currentPageNumber++
                            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                            currentPage = pdfDocument.startPage(pageInfo)
                            canvas = currentPage.canvas
                            yOffset = margin
                            canvas
                        }
                    )
                    mathBlockLines.clear()
                    inMathBlock = false
                } else {
                    mathBlockLines.add(paragraph)
                }
                idx++
                continue
            }

            // 1b. Mermaid block accumulation check
            if (inMermaidBlock) {
                val trimmed = paragraph.trim()
                val cleanCodeBlockTrim = trimmed.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
                if (cleanCodeBlockTrim.startsWith("```")) {
                    yOffset = drawMermaidBlock(
                        canvas, mermaidBlockLines, textPaint, monospaceTypeface,
                        margin, yOffset, printableWidth, pageHeight - margin,
                        mermaidBitmaps,
                        onNewPage = {
                            pdfDocument.finishPage(currentPage)
                            currentPageNumber++
                            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                            currentPage = pdfDocument.startPage(pageInfo)
                            canvas = currentPage.canvas
                            yOffset = margin
                            canvas
                        }
                    )
                    mermaidBlockLines.clear()
                    inMermaidBlock = false
                } else {
                    mermaidBlockLines.add(paragraph)
                }
                idx++
                continue
            }

            if (inCodeBlock) {
                val trimmed = paragraph.trim()
                val cleanCodeBlockTrim = trimmed.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
                if (cleanCodeBlockTrim.startsWith("```")) {
                    // Draw accumulated code block
                    yOffset = drawCodeBlock(
                        canvas, codeBlockLines, textPaint, monospaceTypeface,
                        margin, yOffset, printableWidth, pageHeight - margin,
                        onNewPage = {
                            pdfDocument.finishPage(currentPage)
                            currentPageNumber++
                            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                            currentPage = pdfDocument.startPage(pageInfo)
                            canvas = currentPage.canvas
                            yOffset = margin
                            canvas
                        }
                    )
                    codeBlockLines.clear()
                    inCodeBlock = false
                } else {
                    codeBlockLines.add(paragraph)
                }
                idx++
                continue
            }

            if (paragraph.isBlank()) {
                yOffset += baseFontSize * 0.8f
                idx++
                continue
            }

            val trimmed = paragraph.trim()

            // 2. Math Block parsing ($$)
            val cleanTrimmed = trimmed
                .replace(Regex("^[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]+"), "")
                .replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]+$"), "")
                .trim()

            if (cleanTrimmed.startsWith("$$")) {
                if (cleanTrimmed.endsWith("$$") && cleanTrimmed.length > 2) {
                    val cleanFormula = cleanTrimmed.removePrefix("$$").removeSuffix("$$").trim()
                    yOffset = drawBlockMath(
                        canvas, listOf(cleanFormula), textPaint, serifItalicTypeface,
                        margin, yOffset, printableWidth, pageHeight - margin,
                        onNewPage = {
                            pdfDocument.finishPage(currentPage)
                            currentPageNumber++
                            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                            currentPage = pdfDocument.startPage(pageInfo)
                            canvas = currentPage.canvas
                            yOffset = margin
                            canvas
                        }
                    )
                } else {
                    inMathBlock = true
                    val cleanLine = cleanTrimmed.removePrefix("$$")
                    if (cleanLine.isNotEmpty()) {
                        mathBlockLines.add(cleanLine)
                    }
                }
                idx++
                continue
            }

            // 3. Code/Mermaid Block boundary check
            val cleanCodeBlockTrim = trimmed.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
            if (cleanCodeBlockTrim.startsWith("```")) {
                val rawLang = cleanCodeBlockTrim.substring(3).trim().lowercase()
                val lang = rawLang.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "")
                if (lang == "mermaid") {
                    inMermaidBlock = true
                } else {
                    inCodeBlock = true
                }
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
            val trimmedClean = cleanParagraph.trim()

            val cleanTrimmedLower = trimmedClean.lowercase()
            if (cleanTrimmedLower == "<dl>" || cleanTrimmedLower == "</dl>") {
                idx++
                continue
            }

            if (cleanTrimmedLower.startsWith("<details") || cleanTrimmedLower.startsWith("<summary>") || cleanTrimmedLower == "</details>") {
                if (cleanTrimmedLower.startsWith("<details")) {
                    inDetailsBlock = true
                    
                    var summary = ""
                    val summaryMatch = Regex("(?is)<summary>(.*?)</summary>").find(cleanParagraph)
                    if (summaryMatch != null) {
                        summary = summaryMatch.groupValues[1].trim()
                    } else {
                        // Check if the next line is a summary
                        if (idx + 1 < paragraphs.size) {
                            val nextLine = paragraphs[idx + 1].trim().replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "")
                            val nextLineLower = nextLine.lowercase()
                            if (nextLineLower.startsWith("<summary>") && nextLineLower.endsWith("</summary>")) {
                                summary = nextLine.substring(9, nextLine.length - 10).trim()
                                idx++ // consume next line
                            } else if (nextLineLower.startsWith("<summary>")) {
                                val nextSummaryMatch = Regex("(?is)<summary>(.*?)</summary>").find(nextLine)
                                if (nextSummaryMatch != null) {
                                    summary = nextSummaryMatch.groupValues[1].trim()
                                    idx++
                                }
                            }
                        }
                    }
                    if (summary.isEmpty()) {
                        summary = "Details"
                    }

                    // Render beautiful summary card header
                    val summaryHeaderSpanned = parseMarkdownAndHtmlToSpannable(context, 
                        "▼  $summary",
                        baseFontSize * 1.05f,
                        boldTypeface,
                        italicTypeface,
                        referenceMap
                    )
                    
                    val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = baseFontSize * 1.05f
                        typeface = boldTypeface
                        color = Color.rgb(14, 132, 87) // Accent green color
                    }

                    val isHeaderRtl = TextRepairProcessor.isParagraphRtl(summary)
                    val headerLayout = StaticLayout.Builder.obtain(
                        summaryHeaderSpanned,
                        0,
                        summaryHeaderSpanned.length,
                        headerPaint,
                        (printableWidth - 24).toInt()
                    )
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setTextDirection(if (isHeaderRtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
                    .build()

                    val headerHeight = headerLayout.height + 16

                    // Overflow check
                    if (yOffset + headerHeight > pageHeight - margin) {
                        pdfDocument.finishPage(currentPage)
                        currentPageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                        currentPage = pdfDocument.startPage(pageInfo)
                        canvas = currentPage.canvas
                        yOffset = margin
                    }

                    // Draw the summary header card
                    val cardPaint = Paint().apply {
                        color = Color.rgb(240, 244, 241) // Light gray-green
                        style = Paint.Style.FILL
                    }
                    val borderPaint = Paint().apply {
                        color = Color.rgb(200, 210, 202)
                        strokeWidth = 1f
                        style = Paint.Style.STROKE
                    }

                    val rect = android.graphics.RectF(margin, yOffset, margin + printableWidth, yOffset + headerHeight)
                    canvas.drawRoundRect(rect, 8f, 8f, cardPaint)
                    canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

                    canvas.save()
                    canvas.translate(margin + 12f, yOffset + 8f)
                    headerLayout.draw(canvas)
                    canvas.restore()

                    yOffset += headerHeight + baseFontSize * 0.5f
                } else if (cleanTrimmedLower == "</details>") {
                    inDetailsBlock = false
                }
                
                idx++
                continue
            }
            
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


            // Check if this line starts a table (and not inside code block)
            if (trimmedClean.startsWith("|") && trimmedClean.endsWith("|")) {
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
                        
                        // Draw PDF Table!
                        yOffset = drawPdfTable(
                            context = context,
                            pdfDocument = pdfDocument,
                            canvas = canvas,
                            headerColumns = headerCols,
                            dataRows = dataRows,
                            alignments = alignments,
                            baseFontSize = baseFontSize,
                            margin = margin,
                            yStart = yOffset,
                            width = printableWidth,
                            pageHeight = pageHeight.toFloat(),
                            pageWidth = pageWidth,
                            regularTypeface = regularTypeface,
                            boldTypeface = boldTypeface,
                            italicTypeface = italicTypeface,
                            isTableRtl = TextRepairProcessor.isParagraphRtl(cleanParagraph),
                            onNewPage = {
                                pdfDocument.finishPage(currentPage)
                                currentPageNumber++
                                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                                currentPage = pdfDocument.startPage(pageInfo)
                                canvas = currentPage.canvas
                                canvas
                            }
                        )
                        
                        idx = k
                        continue
                    }
                }
            }

            // 2. Horizontal Divider Line (--- or *** or ___)
            if (trimmedClean == "---" || trimmedClean == "***" || trimmedClean == "___") {
                val dividerHeight = baseFontSize * 1.5f
                if (yOffset + dividerHeight > pageHeight - margin) {
                    pdfDocument.finishPage(currentPage)
                    currentPageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                    currentPage = pdfDocument.startPage(pageInfo)
                    canvas = currentPage.canvas
                    yOffset = margin
                }

                val linePaint = Paint().apply {
                    color = Color.rgb(226, 228, 236)
                    strokeWidth = 1.5f
                    style = Paint.Style.STROKE
                }
                val lineY = yOffset + (dividerHeight / 2f)
                canvas.drawLine(margin, lineY, margin + printableWidth, lineY, linePaint)

                yOffset += dividerHeight
                idx++
                continue
            }

            // Determine paragraph style and direction
            var currentTextSize = baseFontSize
            var currentTypeface = regularTypeface
            var currentTextColor = Color.BLACK
            var isHeader = false
            var isList = false
            var isQuote = false
            var quoteLevel = 0
            var displayText = paragraph

            // 2. Headings (#)
            if (trimmed.startsWith("# ")) {
                currentTextSize = baseFontSize * 1.8f
                currentTypeface = boldTypeface
                currentTextColor = Color.rgb(14, 17, 25) // Deep Slate CleanRTL Primary
                displayText = trimmed.substring(2)
                isHeader = true
            } else if (trimmed.startsWith("## ")) {
                currentTextSize = baseFontSize * 1.5f
                currentTypeface = boldTypeface
                currentTextColor = Color.rgb(14, 17, 25)
                displayText = trimmed.substring(3)
                isHeader = true
            } else if (trimmed.startsWith("### ")) {
                currentTextSize = baseFontSize * 1.3f
                currentTypeface = boldTypeface
                displayText = trimmed.substring(4)
                isHeader = true
            } else if (trimmed.startsWith("#### ")) {
                currentTextSize = baseFontSize * 1.15f
                currentTypeface = boldTypeface
                displayText = trimmed.substring(5)
                isHeader = true
            } else if (trimmed.startsWith("##### ")) {
                currentTextSize = baseFontSize * 1.0f
                currentTypeface = boldTypeface
                displayText = trimmed.substring(6)
                isHeader = true
            } else if (trimmed.startsWith("###### ")) {
                currentTextSize = baseFontSize * 0.85f
                currentTypeface = boldTypeface
                displayText = trimmed.substring(7)
                isHeader = true
            }
            val numberedListMatch = Regex("^(([a-zA-Z0-9]+)\\.)\\s+(.*)").matchEntire(trimmed)

            // 3. Bullet Lists (- or * or •) — task list first
            if (cleanCodeBlockTrim.startsWith("- [x] ") || cleanCodeBlockTrim.startsWith("- [X] ") ||
                cleanCodeBlockTrim.startsWith("* [x] ") || cleanCodeBlockTrim.startsWith("* [X] ") ||
                cleanCodeBlockTrim.startsWith("• [x] ") || cleanCodeBlockTrim.startsWith("• [X] ")) {
                val bullet = "☑  "
                displayText = bullet + com.google.hamahang.core.bidi.TextRepairProcessor.stripPrefixKeepingBidi(trimmed, 6)
                isList = true
            } else if (cleanCodeBlockTrim.startsWith("- [ ] ") || cleanCodeBlockTrim.startsWith("* [ ] ") || cleanCodeBlockTrim.startsWith("• [ ] ")) {
                val bullet = "☐  "
                displayText = bullet + com.google.hamahang.core.bidi.TextRepairProcessor.stripPrefixKeepingBidi(trimmed, 6)
                isList = true
            } else if (cleanCodeBlockTrim.startsWith("- ") || cleanCodeBlockTrim.startsWith("* ") || cleanCodeBlockTrim.startsWith("• ")) {
                val bullet = when (listLevel % 3) {
                    1 -> "◦  "
                    2 -> "▪  "
                    else -> "•  "
                }
                displayText = bullet + com.google.hamahang.core.bidi.TextRepairProcessor.stripPrefixKeepingBidi(trimmed, 2)
                isList = true
            }
            // 3b. Numbered/Alphabetical Lists
            else if (numberedListMatch != null) {
                val number = numberedListMatch.groupValues[1]
                val content = numberedListMatch.groupValues[3]
                displayText = "$number  $content"
                isList = true
            }
            // 4. Blockquotes (>)
            else if (trimmed.startsWith(">")) {
                var qL = 0
                var tempStr = trimmed
                while (tempStr.startsWith(">")) {
                    qL++
                    tempStr = tempStr.substring(1).trim()
                }
                displayText = tempStr
                currentTypeface = italicTypeface
                currentTextColor = Color.DKGRAY
                isQuote = true
                quoteLevel = qL
            }

            // Parse markdown and HTML style tags into spanned text
            val spannedText = parseMarkdownAndHtmlToSpannable(context, 
                displayText,
                currentTextSize,
                boldTypeface,
                italicTypeface,
                referenceMap
            )

            // Setup Paint
            textPaint.apply {
                textSize = currentTextSize
                typeface = currentTypeface
                color = currentTextColor
            }

            // Resolve layout alignment and critical text direction heuristics
            val isRtl = TextRepairProcessor.isParagraphRtl(displayText)
            val alignment = Layout.Alignment.ALIGN_NORMAL
            val directionHeuristic = if (isRtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR

            val quoteIndent = if (isQuote) quoteLevel * 8f + 12f else 0f
            val listIndent = listLevel * 16f
            val detailsIndent = if (inDetailsBlock) 20f else 0f
            val indentMargin = (if (isList) listIndent + 16f else listIndent) + detailsIndent
            val layoutWidth = (printableWidth - indentMargin - quoteIndent).toInt()

            val textLayoutBuilder = StaticLayout.Builder.obtain(
                spannedText,
                0,
                spannedText.length,
                textPaint,
                layoutWidth
            )
            .setAlignment(alignment)
            .setTextDirection(directionHeuristic)
            .setLineSpacing(0f, 1.2f)
            
            if (isJustified && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                textLayoutBuilder.setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_INTER_WORD)
            }
            
            val textLayout = textLayoutBuilder.build()

            val layoutHeight = textLayout.height
            val blockSpacing = if (isHeader) baseFontSize else baseFontSize * 0.5f

            // Multi-page overflow check
            if (yOffset + layoutHeight > pageHeight - margin) {
                pdfDocument.finishPage(currentPage)
                currentPageNumber++
                
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                
                yOffset = margin
            }

            canvas.save()

            // Draw details block decorative vertical border line!
            if (inDetailsBlock) {
                val detailsBorderPaint = Paint().apply {
                    color = Color.rgb(200, 210, 202) // Light gray-green
                    strokeWidth = 2f
                    style = Paint.Style.STROKE
                }
                if (isRtl) {
                    canvas.drawLine(pageWidth - margin - 4f, yOffset, pageWidth - margin - 4f, yOffset + layoutHeight, detailsBorderPaint)
                } else {
                    canvas.drawLine(margin + 4f, yOffset, margin + 4f, yOffset + layoutHeight, detailsBorderPaint)
                }
            }

            // Draw Blockquote decorative border
            if (isQuote) {
                val borderPaint = Paint().apply {
                    color = Color.rgb(83, 217, 164) // Mint Secondary Accent
                    strokeWidth = 3f
                    style = Paint.Style.STROKE
                }
                for (i in 0 until quoteLevel) {
                    val offset = i * 8f + (if (inDetailsBlock) 20f else 0f)
                    if (isRtl) {
                        canvas.drawLine(pageWidth - margin - offset, yOffset, pageWidth - margin - offset, yOffset + layoutHeight, borderPaint)
                    } else {
                        canvas.drawLine(margin + offset, yOffset, margin + offset, yOffset + layoutHeight, borderPaint)
                    }
                }
                val totalTranslate = quoteLevel * 8f + 6f
                canvas.translate(if (isRtl) -totalTranslate else totalTranslate, 0f)
            }

            canvas.translate(margin + if (isRtl) 0f else indentMargin, yOffset)
            textLayout.draw(canvas)
            canvas.restore()

            yOffset += layoutHeight + blockSpacing
            idx++
        }

        // Finish accumulated code block if document ends inside it
        if (inCodeBlock && codeBlockLines.isNotEmpty()) {
            yOffset = drawCodeBlock(
                canvas, codeBlockLines, textPaint, monospaceTypeface,
                margin, yOffset, printableWidth, pageHeight - margin,
                onNewPage = { canvas }
            )
        }

        // Finish accumulated mermaid block if document ends inside it
        if (inMermaidBlock && mermaidBlockLines.isNotEmpty()) {
            yOffset = drawMermaidBlock(
                canvas, mermaidBlockLines, textPaint, monospaceTypeface,
                margin, yOffset, printableWidth, pageHeight - margin,
                mermaidBitmaps,
                onNewPage = { canvas }
            )
        }

        // Finish accumulated math block if document ends inside it
        if (inMathBlock && mathBlockLines.isNotEmpty()) {
            drawBlockMath(
                canvas, mathBlockLines, textPaint, serifItalicTypeface,
                margin, yOffset, printableWidth, pageHeight - margin,
                onNewPage = { canvas }
            )
        }

        // Draw Footnotes
        if (footnotesMap.isNotEmpty()) {
            if (yOffset + 40f > pageHeight - margin) {
                pdfDocument.finishPage(currentPage)
                currentPageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                yOffset = margin
            }
            
            yOffset += 20f
            val dividerPaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 2f
            }
            canvas.drawLine(margin, yOffset, margin + 200f, yOffset, dividerPaint)
            yOffset += 10f

            textPaint.apply {
                textSize = baseFontSize * 0.8f
                typeface = regularTypeface
                color = Color.DKGRAY
            }

            for ((id, text) in footnotesMap) {
                val fnText = "[$id] $text"
                val spannedText = parseMarkdownAndHtmlToSpannable(context, fnText, baseFontSize * 0.8f, boldTypeface, italicTypeface, referenceMap)
                
                val isRtl = TextRepairProcessor.isParagraphRtl(fnText)
                val textLayoutBuilder = StaticLayout.Builder.obtain(
                    spannedText, 0, spannedText.length, textPaint, printableWidth.toInt()
                )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(if (isRtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
                
                if (isJustified && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    textLayoutBuilder.setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_INTER_WORD)
                }
                
                val textLayout = textLayoutBuilder.build()

                if (yOffset + textLayout.height > pageHeight - margin) {
                    pdfDocument.finishPage(currentPage)
                    currentPageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                    currentPage = pdfDocument.startPage(pageInfo)
                    canvas = currentPage.canvas
                    yOffset = margin
                }

                canvas.save()
                canvas.translate(margin, yOffset)
                textLayout.draw(canvas)
                canvas.restore()
                yOffset += textLayout.height + 5f
            }
        }

        pdfDocument.finishPage(currentPage)

        try {
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    private fun drawCodeBlock(
        canvas: Canvas,
        lines: List<String>,
        paint: TextPaint,
        typeface: Typeface,
        margin: Float,
        yStart: Float,
        width: Float,
        maxHeight: Float,
        onNewPage: () -> Canvas
    ): Float {
        var currentCanvas = canvas
        var yOffset = yStart
        val rawCode = lines.joinToString("\n")
        val preprocessedCode = preprocessCodeBidi(rawCode)
        val highlightedCode = highlightPdfCode(preprocessedCode)

        paint.apply {
            textSize = 10f
            this.typeface = typeface
            color = Color.rgb(212, 212, 212) // Default text color: #D4D4D4
        }

        val textLayout = StaticLayout.Builder.obtain(
            highlightedCode,
            0,
            highlightedCode.length,
            paint,
            (width - 24f).toInt()
        )
        .setAlignment(Layout.Alignment.ALIGN_NORMAL) // Code listings are always LTR
        .setTextDirection(TextDirectionHeuristics.LTR)
        .setLineSpacing(0f, 1.1f)
        .build()

        val blockHeight = textLayout.height + 20f

        if (yOffset + blockHeight > maxHeight) {
            currentCanvas = onNewPage()
            yOffset = margin
        }

        // Draw Card Background for Code Block (Dark Theme to match modern preview)
        val bgPaint = Paint().apply {
            color = Color.rgb(30, 30, 30) // Dark background #1E1E1E
            style = Paint.Style.FILL
        }
        val cardRect = RectF(margin, yOffset, margin + width, yOffset + blockHeight)
        currentCanvas.drawRoundRect(cardRect, 6f, 6f, bgPaint)

        // Draw Code lines
        currentCanvas.save()
        currentCanvas.translate(margin + 12f, yOffset + 10f)
        textLayout.draw(currentCanvas)
        currentCanvas.restore()

        return yOffset + blockHeight + 10f
    }

    private fun drawMermaidBlock(
        canvas: Canvas,
        lines: List<String>,
        paint: TextPaint,
        typeface: Typeface,
        margin: Float,
        yStart: Float,
        width: Float,
        maxHeight: Float,
        mermaidBitmaps: Map<String, android.graphics.Bitmap>,
        onNewPage: () -> Canvas
    ): Float {
        var currentCanvas = canvas
        var yOffset = yStart
        // Do NOT run any bidi preprocessing on Mermaid diagram code!
        val rawCode = lines.joinToString("\n")
        val cleanCode = rawCode.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()

        val renderedBitmap = mermaidBitmaps[cleanCode] ?: mermaidBitmaps[rawCode]
        if (renderedBitmap != null) {
            val scale = width / renderedBitmap.width.toFloat()
            val finalScale = if (scale < 1f) scale else 1f
            val drawWidth = renderedBitmap.width * finalScale
            val drawHeight = renderedBitmap.height * finalScale

            // Draw visual graph with perfect page break
            if (yOffset + drawHeight > maxHeight) {
                currentCanvas = onNewPage()
                yOffset = margin
            }

            val xOffset = margin + (width - drawWidth) / 2f
            val destRect = RectF(xOffset, yOffset, xOffset + drawWidth, yOffset + drawHeight)
            currentCanvas.drawBitmap(renderedBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))

            return yOffset + drawHeight + 15f
        }

        paint.apply {
            textSize = 10f
            this.typeface = typeface
            color = Color.rgb(212, 212, 212) // Default text color: #D4D4D4
        }

        // Draw a label above the Mermaid code block
        val labelPaint = TextPaint().apply {
            textSize = 9f
            this.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(14, 132, 87) // Accent green #0E8457
        }
        val label = "Mermaid Diagram (PDF Code Fallback):"
        val labelHeight = 15f

        val textLayout = StaticLayout.Builder.obtain(
            cleanCode,
            0,
            cleanCode.length,
            paint,
            (width - 24f).toInt()
        )
        .setAlignment(Layout.Alignment.ALIGN_NORMAL) // Code listings are always LTR
        .setTextDirection(TextDirectionHeuristics.LTR)
        .setLineSpacing(0f, 1.1f)
        .build()

        val blockHeight = textLayout.height + 25f + labelHeight

        if (yOffset + blockHeight > maxHeight) {
            currentCanvas = onNewPage()
            yOffset = margin
        }

        // Draw Label
        currentCanvas.drawText(label, margin, yOffset + 10f, labelPaint)

        // Draw Card Background for Code Block (Dark Theme to match modern preview)
        val bgPaint = Paint().apply {
            color = Color.rgb(30, 30, 30) // Dark background #1E1E1E
            style = Paint.Style.FILL
        }
        val cardRect = RectF(margin, yOffset + labelHeight + 5f, margin + width, yOffset + blockHeight)
        currentCanvas.drawRoundRect(cardRect, 6f, 6f, bgPaint)

        // Draw Code lines
        currentCanvas.save()
        currentCanvas.translate(margin + 12f, yOffset + labelHeight + 15f)
        textLayout.draw(currentCanvas)
        currentCanvas.restore()

        return yOffset + blockHeight + 10f
    }

    private fun replaceLatexWithUnicode(latex: String): String {
        return latex
            .replace("\\int", "∫")
            .replace("\\infty", "∞")
            .replace("\\sqrt", "√")
            .replace("\\pi", "π")
            .replace("\\pm", "±")
            .replace("\\alpha", "α")
            .replace("\\beta", "β")
            .replace("\\gamma", "γ")
            .replace("\\theta", "θ")
            .replace("\\sum", "∑")
            .replace("\\approx", "≈")
            .replace("\\neq", "≠")
            .replace("\\leq", "≤")
            .replace("\\geq", "≥")
            .replace("\\times", "×")
            .replace("\\div", "÷")
            .replace("\\rightarrow", "→")
            .replace("\\Rightarrow", "⇒")
            .replace("\\left", "")
            .replace("\\right", "")
            .replace("\\{", "{")
            .replace("\\}", "}")
            .replace("\\,", " ")
            .replace("_{", "_")
            .replace("^{", "^")
            .replace("}", "") // Remove remaining right braces from frac/sub/sup if simple enough
    }

    private enum class TableColumnAlignment {
        LEFT, CENTER, RIGHT
    }

    private fun isTableDivider(line: String): Boolean {
        val clean = line.trim()
        if (!clean.startsWith("|") || !clean.endsWith("|")) return false
        val parts = clean.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return false
        return parts.all { cell -> cell.all { it == '-' || it == ':' } }
    }

    private fun parseTableLine(line: String): List<String> {
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

    private fun parseAlignment(dividerCell: String): TableColumnAlignment {
        val clean = dividerCell.trim()
        val startsWithColon = clean.startsWith(":")
        val endsWithColon = clean.endsWith(":")
        return when {
            startsWithColon && endsWithColon -> TableColumnAlignment.CENTER
            endsWithColon -> TableColumnAlignment.RIGHT
            else -> TableColumnAlignment.LEFT
        }
    }

    private fun drawPdfTable(
        context: Context,
        pdfDocument: PdfDocument,
        canvas: Canvas,
        headerColumns: List<String>,
        dataRows: List<List<String>>,
        alignments: List<TableColumnAlignment>,
        baseFontSize: Float,
        margin: Float,
        yStart: Float,
        width: Float,
        pageHeight: Float,
        pageWidth: Int,
        regularTypeface: Typeface,
        boldTypeface: Typeface,
        italicTypeface: Typeface,
        isTableRtl: Boolean,
        onNewPage: () -> Canvas
    ): Float {
        var currentCanvas = canvas
        var yOffset = yStart

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = baseFontSize - 1f
            color = Color.BLACK
            linkColor = Color.BLUE
            typeface = regularTypeface
        }
        val headerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = baseFontSize - 1f
            color = Color.rgb(14, 17, 25) // Deep Slate
            linkColor = Color.BLUE
            typeface = boldTypeface
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(226, 228, 236)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val headerBgPaint = Paint().apply {
            color = Color.rgb(240, 241, 245)
            style = Paint.Style.FILL
        }
        val zebraBgPaint = Paint().apply {
            color = Color.rgb(250, 251, 253)
            style = Paint.Style.FILL
        }

        val colCount = headerColumns.size
        if (colCount == 0) return yOffset
        val colWidth = width / colCount

        fun drawSingleRow(
            rowCells: List<String>,
            isHeader: Boolean,
            isZebra: Boolean
        ): Float {
            // Measure cells
            val layouts = mutableListOf<StaticLayout>()
            var maxCellHeight = 0
            val paint = if (isHeader) headerTextPaint else textPaint

            for (colIdx in 0 until colCount) {
                val cellText = rowCells.getOrNull(colIdx) ?: ""
                val cellSpanned = parseMarkdownAndHtmlToSpannable(context, 
                    cellText,
                    paint.textSize,
                    boldTypeface,
                    italicTypeface,
                    emptyMap()
                )

                val isRtl = TextRepairProcessor.isParagraphRtl(cellText)
                val directionHeuristic = if (isRtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR

                val androidAlignment = when (alignments.getOrNull(colIdx) ?: TableColumnAlignment.LEFT) {
                    TableColumnAlignment.LEFT -> Layout.Alignment.ALIGN_NORMAL
                    TableColumnAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
                    TableColumnAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                }

                val layoutWidth = (colWidth - 16f).toInt().coerceAtLeast(10)
                val cellLayout = StaticLayout.Builder.obtain(
                    cellSpanned,
                    0,
                    cellSpanned.length,
                    paint,
                    layoutWidth
                )
                .setAlignment(androidAlignment)
                .setTextDirection(directionHeuristic)
                .setLineSpacing(0f, 1.1f)
                .build()

                layouts.add(cellLayout)
                if (cellLayout.height > maxCellHeight) {
                    maxCellHeight = cellLayout.height
                }
            }

            val rowHeight = maxCellHeight + 16f // 8pt top/bottom padding

            // Check page overflow before drawing
            if (yOffset + rowHeight > pageHeight - margin) {
                currentCanvas = onNewPage()
                yOffset = margin
                
                // If this is a data row, draw header first on new page!
                if (!isHeader) {
                    drawSingleRow(headerColumns, isHeader = true, isZebra = false)
                }
            }

            // Draw row background
            if (isHeader) {
                val rect = RectF(margin, yOffset, margin + width, yOffset + rowHeight)
                currentCanvas.drawRect(rect, headerBgPaint)
            } else if (isZebra) {
                val rect = RectF(margin, yOffset, margin + width, yOffset + rowHeight)
                currentCanvas.drawRect(rect, zebraBgPaint)
            }

            // Draw cells
            for (colIdx in 0 until colCount) {
                val cellLayout = layouts.getOrNull(colIdx) ?: continue
                currentCanvas.save()
                
                // Calculate horizontal position
                val cellX = margin + (if (isTableRtl) (colCount - 1 - colIdx) else colIdx) * colWidth
                
                currentCanvas.translate(cellX + 8f, yOffset + 8f)
                cellLayout.draw(currentCanvas)
                currentCanvas.restore()

                // Draw vertical divider between columns
                if (colIdx > 0) {
                    val dividerX = margin + colIdx * colWidth
                    currentCanvas.drawLine(dividerX, yOffset, dividerX, yOffset + rowHeight, borderPaint)
                }
            }

            // Draw horizontal bottom border line
            currentCanvas.drawLine(margin, yOffset + rowHeight, margin + width, yOffset + rowHeight, borderPaint)
            
            // Draw left and right outer borders
            currentCanvas.drawLine(margin, yOffset, margin, yOffset + rowHeight, borderPaint)
            currentCanvas.drawLine(margin + width, yOffset, margin + width, yOffset + rowHeight, borderPaint)

            yOffset += rowHeight
            return rowHeight
        }

        // Draw top border
        currentCanvas.drawLine(margin, yOffset, margin + width, yOffset, borderPaint)

        // Draw header
        drawSingleRow(headerColumns, isHeader = true, isZebra = false)

        // Draw rows
        dataRows.forEachIndexed { rIdx, rowCells ->
            drawSingleRow(rowCells, isHeader = false, isZebra = rIdx % 2 != 0)
        }

        return yOffset
    }

    private fun parseMarkdownAndHtmlToSpannable(context: Context, 
        input: String,
        baseFontSize: Float,
        boldTypeface: Typeface,
        italicTypeface: Typeface,
        referenceMap: Map<String, Pair<String, String?>> = emptyMap()
    ): Spanned {
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
            var r = str
            for (pair in escapeMap) {
                r = r.replace(pair.first, pair.second)
            }
            return r
        }

        fun decodeEscapesUnescaped(str: String): String {
            var r = str
            for (pair in escapeMap) {
                val unescaped = pair.first.substring(1)
                r = r.replace(pair.second, unescaped)
            }
            return r
        }

        fun decodeEscapesEscaped(str: String): String {
            var r = str
            for (pair in escapeMap) {
                r = r.replace(pair.second, pair.first)
            }
            return r
        }

        fun decodeSpannable(sb: SpannableStringBuilder): Spanned {
            val decodedText = decodeEscapesUnescaped(sb.toString())
            val finalStr = SpannableStringBuilder(decodedText)
            val spans = sb.getSpans(0, sb.length, Any::class.java)
            for (span in spans) {
                val start = sb.getSpanStart(span)
                val end = sb.getSpanEnd(span)
                val flags = sb.getSpanFlags(span)
                finalStr.setSpan(span, start, end, flags)
            }
            return finalStr
        }

        val encodedInput = encodeEscapes(input)
        val builder = SpannableStringBuilder()
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

        // Match all advanced markdown/html structures precisely
        val regex = Regex("(?is)(\\[!\\[[^\\]]*?\\]\\([^\\)]+?\\)\\]\\([^\\)]+?\\)|!\\[[^\\]]*?\\]\\([^\\)]+?\\)|\\*\\*\\*.*?\\*\\*\\*|___.*?___|\\*\\*.*?\\*\\*|__.*?__|\\*.*?\\*|_[^_\\n\\r]+?_|~~.*?~~|<ins>.*?</ins>|<strong>.*?</strong>|<em>.*?</em>|<dt>.*?</dt>|<dd>.*?</dd>|\\[![^\\]]+?\\]\\([^\\)]+?\\)|\\[[^\\]]+?\\]\\([^\\)]+?\\)|\\[[^\\]]+?\\]\\[[^\\]]*?\\]|\\[\\^[^\\)]+\\]|`.*?`|\\$\\$.*?\\$\\$|\\$.*?\\$|<https?://[^>\\s]+>|https?://[^\\s<>\\[\\]\\(ن)،,؛;。！？!?]+|<[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}>|[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}(?![\\w>])|<kbd>.*?</kbd>|<[\\s\\u00A0]*abbr[^>]*>.*?<[\\s\\u00A0]*/[\\s\\u00A0]*abbr[\\s\\u00A0]*>|<[\\s\\u00A0]*span[^>]*>.*?<[\\s\\u00A0]*/[\\s\\u00A0]*span[\\s\\u00A0]*>|<[\\s\\u00A0]*font[^>]*>.*?<[\\s\\u00A0]*/[\\s\\u00A0]*font[\\s\\u00A0]*>|<br\\s*/?>|:[a-zA-Z0-9_+\\-]+:|\\\\\\$|  $)")
        val matches = regex.findAll(encodedInput)

        for (match in matches) {
            if (match.range.first > index) {
                builder.append(encodedInput.substring(index, match.range.first))
            }

            val matchedText = match.value
            val matchedTextClean = matchedText.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "")
            val matchedTextLower = matchedTextClean.trim().lowercase()
            when {
                matchedTextLower.startsWith("***") && matchedTextLower.endsWith("***") -> {
                    val start = builder.length
                    val content = matchedTextClean.substring(3, matchedTextClean.length - 3)
                    builder.append(parseMarkdownAndHtmlToSpannable(context, content, baseFontSize, boldTypeface, italicTypeface, referenceMap))
                    builder.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("___") && matchedTextLower.endsWith("___") -> {
                    val start = builder.length
                    val content = matchedTextClean.substring(3, matchedTextClean.length - 3)
                    builder.append(parseMarkdownAndHtmlToSpannable(context, content, baseFontSize, boldTypeface, italicTypeface, referenceMap))
                    builder.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("**") && matchedTextLower.endsWith("**") -> {
                    val start = builder.length
                    val content = matchedTextClean.substring(2, matchedTextClean.length - 2)
                    builder.append(parseMarkdownAndHtmlToSpannable(context, content, baseFontSize, boldTypeface, italicTypeface, referenceMap))
                    builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("__") && matchedTextLower.endsWith("__") -> {
                    val start = builder.length
                    val content = matchedTextClean.substring(2, matchedTextClean.length - 2)
                    builder.append(parseMarkdownAndHtmlToSpannable(context, content, baseFontSize, boldTypeface, italicTypeface, referenceMap))
                    builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("*") && matchedTextLower.endsWith("*") -> {
                    val start = builder.length
                    val content = matchedTextClean.substring(1, matchedTextClean.length - 1)
                    builder.append(parseMarkdownAndHtmlToSpannable(context, content, baseFontSize, boldTypeface, italicTypeface, referenceMap))
                    builder.setSpan(StyleSpan(Typeface.ITALIC), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("_") && matchedTextLower.endsWith("_") -> {
                    val start = builder.length
                    val content = matchedTextClean.substring(1, matchedTextClean.length - 1)
                    builder.append(parseMarkdownAndHtmlToSpannable(context, content, baseFontSize, boldTypeface, italicTypeface, referenceMap))
                    builder.setSpan(StyleSpan(Typeface.ITALIC), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("~~") && matchedTextLower.endsWith("~~") -> {
                    val start = builder.length
                    val content = matchedTextClean.substring(2, matchedTextClean.length - 2)
                    builder.append(parseMarkdownAndHtmlToSpannable(context, content, baseFontSize, boldTypeface, italicTypeface, referenceMap))
                    builder.setSpan(android.text.style.StrikethroughSpan(), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("<ins>") && matchedTextLower.endsWith("</ins>") -> {
                    val start = builder.length
                    val content = matchedTextClean.substring(5, matchedTextClean.length - 6)
                    builder.append(parseMarkdownAndHtmlToSpannable(context, content, baseFontSize, boldTypeface, italicTypeface, referenceMap))
                    builder.setSpan(android.text.style.UnderlineSpan(), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("<strong>") && matchedTextLower.endsWith("</strong>") -> {
                    val start = builder.length
                    val content = matchedTextClean.substring(8, matchedTextClean.length - 9)
                    builder.append(parseMarkdownAndHtmlToSpannable(context, content, baseFontSize, boldTypeface, italicTypeface, referenceMap))
                    builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("<em>") && matchedTextLower.endsWith("</em>") -> {
                    val start = builder.length
                    val content = matchedTextClean.substring(4, matchedTextClean.length - 5)
                    builder.append(parseMarkdownAndHtmlToSpannable(context, content, baseFontSize, boldTypeface, italicTypeface, referenceMap))
                    builder.setSpan(StyleSpan(Typeface.ITALIC), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("<dt>") && matchedTextLower.endsWith("</dt>") -> {
                    val start = builder.length
                    val content = matchedTextClean.substring(4, matchedTextClean.length - 5)
                    builder.append(parseMarkdownAndHtmlToSpannable(context, content, baseFontSize, boldTypeface, italicTypeface, referenceMap))
                    builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("<dd>") && matchedTextLower.endsWith("</dd>") -> {
                    val start = builder.length
                    val content = matchedTextClean.substring(4, matchedTextClean.length - 5)
                    builder.append("  ") // Indent dd slightly
                    builder.append(parseMarkdownAndHtmlToSpannable(context, content, baseFontSize, boldTypeface, italicTypeface, referenceMap))
                    builder.setSpan(StyleSpan(Typeface.ITALIC), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("[![") && matchedTextLower.contains("](") -> {
                    val imgLinkRegex = Regex("\\[!\\[([^\\]]*)\\]\\([^\\)]+?\\)\\]\\(([^\\)]+?)\\)")
                    val imgLinkMatch = imgLinkRegex.matchEntire(matchedTextClean)
                    if (imgLinkMatch != null) {
                        val altText = imgLinkMatch.groupValues[1].ifEmpty { "image link" }
                        val rawUrl = imgLinkMatch.groupValues[2].trim()
                        val url = cleanQuotes(rawUrl)
                        val decodedUrl = decodeEscapesUnescaped(url)
                        
                        val start = builder.length
                        builder.append("[🖼️ $altText]")
                        builder.setSpan(ForegroundColorSpan(Color.BLUE), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        builder.setSpan(android.text.style.UnderlineSpan(), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        builder.setSpan(android.text.style.URLSpan(decodedUrl), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    } else {
                        builder.append(matchedText)
                    }
                }
                matchedTextLower.startsWith("![") && matchedTextLower.contains("](" ) -> {
                    val imgRegex = Regex("!\\[([^\\]]*)\\]\\(([^\\)]+?)\\)")
                    val imgMatch = imgRegex.matchEntire(matchedTextClean)
                    if (imgMatch != null) {
                        val altText = imgMatch.groupValues[1].ifEmpty { "image" }
                        val rawUrl = imgMatch.groupValues[2].trim()
                        val url = cleanQuotes(rawUrl)
                        val decodedUrl = decodeEscapesUnescaped(url)
                        
                        val start = builder.length
                        builder.append("[🖼️ $altText]")
                        
                        try {
                            val stream = java.net.URL(decodedUrl).openStream()
                            val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                            if (bitmap != null) {
                                val maxWidth = 400
                                val scaledBitmap = if (bitmap.width > maxWidth) {
                                    android.graphics.Bitmap.createScaledBitmap(bitmap, maxWidth, (bitmap.height * (maxWidth.toFloat() / bitmap.width)).toInt(), true)
                                } else bitmap
                                builder.setSpan(android.text.style.ImageSpan(context, scaledBitmap), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            } else {
                                builder.setSpan(ForegroundColorSpan(Color.rgb(14, 132, 87)), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                        } catch (e: Exception) {
                            builder.setSpan(ForegroundColorSpan(Color.rgb(14, 132, 87)), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    } else {
                        builder.append(matchedText)
                    }
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
                            val start = builder.length
                            builder.append(parseMarkdownAndHtmlToSpannable(context, linkText, baseFontSize, boldTypeface, italicTypeface, referenceMap))
                            builder.setSpan(ForegroundColorSpan(Color.BLUE), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            builder.setSpan(android.text.style.UnderlineSpan(), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            builder.setSpan(android.text.style.URLSpan(refVal.first), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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
                        val rawUrl = linkMatch.groupValues[2]
                        val urlParts = rawUrl.trim().split(Regex("[\\s\\u00A0]+"))
                        val url = cleanQuotes(urlParts[0])
                        val decodedUrl = decodeEscapesUnescaped(url)
                        
                        val start = builder.length
                        builder.append(parseMarkdownAndHtmlToSpannable(context, linkText, baseFontSize, boldTypeface, italicTypeface, referenceMap))
                        
                        builder.setSpan(ForegroundColorSpan(Color.BLUE), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        builder.setSpan(android.text.style.UnderlineSpan(), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        builder.setSpan(android.text.style.URLSpan(decodedUrl), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    } else {
                        builder.append(matchedText)
                    }
                }
                matchedTextLower.startsWith("[^") && matchedTextLower.endsWith("]") -> {
                    val refId = matchedTextClean.substring(2, matchedTextClean.length - 1)
                    val start = builder.length
                    builder.append("[$refId]")
                    builder.setSpan(android.text.style.SuperscriptSpan(), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(android.text.style.RelativeSizeSpan(0.7f), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(ForegroundColorSpan(Color.BLUE), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("<http") && matchedTextLower.endsWith(">") -> {
                    val url = matchedTextClean.substring(1, matchedTextClean.length - 1)
                    val start = builder.length
                    builder.append(url)
                    builder.setSpan(ForegroundColorSpan(Color.BLUE), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(android.text.style.UnderlineSpan(), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(android.text.style.URLSpan(url), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("<") && matchedTextLower.contains("@") && matchedTextLower.endsWith(">") -> {
                    val email = matchedTextClean.substring(1, matchedTextClean.length - 1)
                    val start = builder.length
                    builder.append(email)
                    builder.setSpan(ForegroundColorSpan(Color.BLUE), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(android.text.style.UnderlineSpan(), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(android.text.style.URLSpan("mailto:$email"), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("<kbd>") && matchedTextLower.endsWith("</kbd>") -> {
                    val keyText = matchedTextClean.substring(5, matchedTextClean.length - 6)
                    val start = builder.length
                    builder.append(" ${decodeEscapesUnescaped(keyText)} ")
                    builder.setSpan(android.text.style.TypefaceSpan("monospace"), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(ForegroundColorSpan(Color.rgb(220, 100, 0)), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(android.text.style.BackgroundColorSpan(Color.rgb(230, 230, 230)), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("`") && matchedTextLower.endsWith("`") -> {
                    val start = builder.length
                    val content = matchedTextClean.substring(1, matchedTextClean.length - 1)
                    builder.append(decodeEscapesEscaped(content))
                    builder.setSpan(android.text.style.BackgroundColorSpan(Color.parseColor("#E8F5E9")), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(ForegroundColorSpan(Color.parseColor("#0E8457")), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(android.text.style.TypefaceSpan("monospace"), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("$$") && matchedTextLower.endsWith("$$") -> {
                    val start = builder.length
                    val content = matchedTextClean.substring(2, matchedTextClean.length - 2)
                    builder.append(replaceLatexWithUnicode(decodeEscapesEscaped(content)))
                    builder.setSpan(StyleSpan(Typeface.ITALIC), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(android.text.style.TypefaceSpan("serif"), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("$") && matchedTextLower.endsWith("$") -> {
                    val start = builder.length
                    val content = matchedTextClean.substring(1, matchedTextClean.length - 1)
                    builder.append(replaceLatexWithUnicode(decodeEscapesEscaped(content)))
                    builder.setSpan(StyleSpan(Typeface.ITALIC), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(android.text.style.TypefaceSpan("serif"), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("<abbr") || matchedTextLower.contains("abbr") -> {
                    val abbrRegex = Regex("(?is)<[\\s\\u00A0]*abbr([^>]*)>(.*?)<[\\s\\u00A0]*/[\\s\\u00A0]*abbr[\\s\\u00A0]*>")
                    val abbrMatch = abbrRegex.matchEntire(matchedTextClean)
                    if (abbrMatch != null) {
                        val innerText = abbrMatch.groupValues[2]
                        val start = builder.length
                        builder.append(parseMarkdownAndHtmlToSpannable(context, innerText, baseFontSize, boldTypeface, italicTypeface, referenceMap))
                        builder.setSpan(android.text.style.UnderlineSpan(), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        builder.setSpan(ForegroundColorSpan(Color.rgb(100, 100, 100)), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    } else {
                        builder.append(matchedText)
                    }
                }
                matchedTextLower.startsWith("http") -> {
                    val start = builder.length
                    builder.append(matchedTextClean)
                    builder.setSpan(ForegroundColorSpan(Color.BLUE), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(android.text.style.UnderlineSpan(), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(android.text.style.URLSpan(matchedTextClean), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.contains("@") && !matchedTextLower.startsWith("<") -> {
                    val start = builder.length
                    builder.append(matchedTextClean)
                    builder.setSpan(ForegroundColorSpan(Color.BLUE), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(android.text.style.UnderlineSpan(), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(android.text.style.URLSpan("mailto:$matchedTextClean"), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedTextLower.startsWith("<font") || matchedTextLower.contains("font") -> {
                    val fontRegex = Regex("(?is)<[\\s\\u00A0]*font([^>]*)>(.*?)<[\\s\\u00A0]*/[\\s\\u00A0]*font[\\s\\u00A0]*>")
                    val fontMatch = fontRegex.matchEntire(matchedTextClean)
                    if (fontMatch != null) {
                        val attrsStr = fontMatch.groupValues[1]
                        val innerText = fontMatch.groupValues[2]

                        var color: Int? = null
                        var fontSizePx: Float? = null

                        val colorMatch = Regex("(?i)color[\\s\\u00A0]*=[\\s\\u00A0]*([^\\s\\u00A0>]+|'[^']*'|\"[^\"]*\")").find(attrsStr)
                        if (colorMatch != null) {
                            val colorValue = cleanQuotes(colorMatch.groupValues[1])
                            color = parseHtmlColorToInt(colorValue)
                        }

                        val sizeMatch = Regex("(?i)size[\\s\\u00A0]*=[\\s\\u00A0]*([^\\s\\u00A0>]+|'[^']*'|\"[^\"]*\")").find(attrsStr)
                        if (sizeMatch != null) {
                            val sizeValue = cleanQuotes(sizeMatch.groupValues[1])
                            fontSizePx = parseHtmlFontSizeAttributeToPx(sizeValue, baseFontSize)
                        }

                        val start = builder.length
                        builder.append(parseMarkdownAndHtmlToSpannable(context, innerText, fontSizePx ?: baseFontSize, boldTypeface, italicTypeface, referenceMap))

                        if (color != null) {
                            builder.setSpan(ForegroundColorSpan(color!!), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        if (fontSizePx != null) {
                            builder.setSpan(AbsoluteSizeSpan(fontSizePx!!.toInt()), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    } else {
                        builder.append(matchedText)
                    }
                }
                matchedTextLower.startsWith("<span") || matchedTextLower.contains("span") -> {
                    val spanRegex = Regex("(?is)<[\\s\\u00A0]*span([^>]*)>(.*?)<[\\s\\u00A0]*/[\\s\\u00A0]*span[\\s\\u00A0]*>")
                    val spanMatch = spanRegex.matchEntire(matchedTextClean)
                    if (spanMatch != null) {
                        val attrsStr = spanMatch.groupValues[1]
                        val innerText = spanMatch.groupValues[2]

                        var color: Int? = null
                        var fontSizePx: Float? = null
                        var isBold = false
                        var isItalic = false

                        val styleMatch = Regex("(?i)style[\\s\\u00A0]*=[\\s\\u00A0]*([^\\s\\u00A0>]+|'[^']*'|\"[^\"]*\")").find(attrsStr)
                        if (styleMatch != null) {
                            val rawStyle = styleMatch.groupValues[1]
                            val styleStr = cleanQuotes(rawStyle)

                            styleStr.split(";").forEach { stylePart ->
                                val colonIdx = stylePart.indexOf(':')
                                if (colonIdx > 0) {
                                    val key = stylePart.substring(0, colonIdx)
                                        .replace(Regex("[\\s\\u00A0]+"), "").trim().lowercase()
                                    val value = stylePart.substring(colonIdx + 1)
                                        .replace(Regex("[\\s\\u00A0]+"), " ").trim()
                                    if (key == "color") {
                                        color = parseHtmlColorToInt(value)
                                    } else if (key == "font-size") {
                                        fontSizePx = parseHtmlFontSizeToPx(value, baseFontSize)
                                    } else if (key == "font-weight") {
                                        if (value.lowercase() == "bold" || value.lowercase() == "700" || value.lowercase() == "800") {
                                            isBold = true
                                        }
                                    } else if (key == "font-style") {
                                        if (value.lowercase() == "italic") {
                                            isItalic = true
                                        }
                                    }
                                }
                            }
                        }

                        val start = builder.length
                        builder.append(parseMarkdownAndHtmlToSpannable(context, innerText, fontSizePx ?: baseFontSize, boldTypeface, italicTypeface, referenceMap))

                        if (color != null) {
                            builder.setSpan(ForegroundColorSpan(color!!), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        if (fontSizePx != null) {
                            builder.setSpan(AbsoluteSizeSpan(fontSizePx!!.toInt()), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        if (isBold && isItalic) {
                            builder.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        } else if (isBold) {
                            builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        } else if (isItalic) {
                            builder.setSpan(StyleSpan(Typeface.ITALIC), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    } else {
                        builder.append(matchedText)
                    }
                }
                matchedTextLower.startsWith("<br") -> {
                    builder.append("\n")
                }
                matchedTextLower.startsWith("\\$") || matchedTextLower == "  $" -> {
                    builder.append("\n")
                }
                matchedTextLower.matches(Regex(":[a-zA-Z0-9_+\\-]+:")) -> {
                    val emojiMap = mapOf(
                        ":memo:" to "\uD83D\uDCDD",
                        ":heart:" to "\u2764\uFE0F",
                        ":sparkles:" to "\u2728",
                        ":smile:" to "\uD83D\uDE04",
                        ":-1:" to "\uD83D\uDC4E",
                        ":+1:" to "\uD83D\uDC4D",
                        ":tada:" to "\uD83C\uDF89",
                        ":rocket:" to "\uD83D\uDE80",
                        ":fire:" to "\uD83D\uDD25",
                        ":white_check_mark:" to "\u2705",
                        ":x:" to "\u274C",
                        ":warning:" to "\u26A0\uFE0F",
                        ":construction:" to "\uD83D\uDEA7",
                        ":bug:" to "\uD83D\uDC1B",
                        ":bulb:" to "\uD83D\uDCA1",
                        ":star:" to "\u2B50"
                    )
                    builder.append(emojiMap[matchedTextLower] ?: matchedText)
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

        return decodeSpannable(builder)
    }

    private fun parseHtmlColorToInt(colorStr: String): Int? {
        val clean = colorStr.trim().lowercase()
        val colorMap = mapOf(
            "red" to Color.rgb(255, 0, 0),
            "green" to Color.rgb(0, 128, 0),
            "blue" to Color.rgb(0, 0, 255),
            "yellow" to Color.rgb(255, 255, 0),
            "black" to Color.rgb(0, 0, 0),
            "white" to Color.rgb(255, 255, 255),
            "gray" to Color.rgb(128, 128, 128),
            "grey" to Color.rgb(128, 128, 128),
            "cyan" to Color.rgb(0, 255, 255),
            "magenta" to Color.rgb(255, 0, 255),
            "orange" to Color.rgb(255, 165, 0),
            "purple" to Color.rgb(128, 0, 128),
            "pink" to Color.rgb(255, 192, 203),
            "brown" to Color.rgb(165, 42, 42),
            "lime" to Color.rgb(0, 255, 0),
            "navy" to Color.rgb(0, 0, 128),
            "teal" to Color.rgb(0, 128, 128),
            "maroon" to Color.rgb(128, 0, 0),
            "olive" to Color.rgb(128, 128, 0),
            "silver" to Color.rgb(192, 192, 192),
            "aqua" to Color.rgb(0, 255, 255),
            "fuchsia" to Color.rgb(255, 0, 255),
            "coral" to Color.rgb(255, 127, 80),
            "salmon" to Color.rgb(250, 128, 114),
            "gold" to Color.rgb(255, 215, 0),
            "violet" to Color.rgb(238, 130, 238),
            "indigo" to Color.rgb(75, 0, 130),
            "turquoise" to Color.rgb(64, 224, 208),
            "crimson" to Color.rgb(220, 20, 60),
            "tomato" to Color.rgb(255, 99, 71),
            "chocolate" to Color.rgb(210, 105, 30),
            "forestgreen" to Color.rgb(34, 139, 34),
            "darkorange" to Color.rgb(255, 140, 0),
            "darkred" to Color.rgb(139, 0, 0),
            "darkblue" to Color.rgb(0, 0, 139),
            "darkgreen" to Color.rgb(0, 100, 0),
            "deeppink" to Color.rgb(255, 20, 147),
            "hotpink" to Color.rgb(255, 105, 180),
            "dodgerblue" to Color.rgb(30, 144, 255),
            "royalblue" to Color.rgb(65, 105, 225),
            "steelblue" to Color.rgb(70, 130, 180),
            "skyblue" to Color.rgb(135, 206, 235),
            "lightblue" to Color.rgb(173, 216, 230),
            "lightgreen" to Color.rgb(144, 238, 144),
            "lightcoral" to Color.rgb(240, 128, 128),
            "lightyellow" to Color.rgb(255, 255, 224),
            "lightsalmon" to Color.rgb(255, 160, 122),
            "transparent" to Color.TRANSPARENT
        )
        if (colorMap.containsKey(clean)) return colorMap[clean]

        val hex = clean.removePrefix("#")
        return try {
            when (hex.length) {
                3 -> {
                    val r = hex[0].toString().repeat(2).toInt(16)
                    val g = hex[1].toString().repeat(2).toInt(16)
                    val b = hex[2].toString().repeat(2).toInt(16)
                    Color.rgb(r, g, b)
                }
                6 -> {
                    val r = hex.substring(0, 2).toInt(16)
                    val g = hex.substring(2, 4).toInt(16)
                    val b = hex.substring(4, 6).toInt(16)
                    Color.rgb(r, g, b)
                }
                8 -> {
                    val a = hex.substring(0, 2).toInt(16)
                    val r = hex.substring(2, 4).toInt(16)
                    val g = hex.substring(4, 6).toInt(16)
                    val b = hex.substring(6, 8).toInt(16)
                    Color.argb(a, r, g, b)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseHtmlFontSizeToPx(sizeStr: String, baseFontSize: Float): Float? {
        val clean = sizeStr.trim().lowercase()
        val numberPart = clean.filter { it.isDigit() || it == '.' }
        val num = numberPart.toFloatOrNull() ?: return null
        return when {
            clean.endsWith("px") -> num
            clean.endsWith("sp") -> num
            clean.endsWith("pt") -> num * 1.33f
            clean.endsWith("em") -> num * baseFontSize
            clean.endsWith("%") -> num * 0.01f * baseFontSize
            else -> num
        }
    }

    private fun parseHtmlFontSizeAttributeToPx(sizeStr: String, baseFontSize: Float): Float? {
        val clean = sizeStr.trim()
        val intVal = clean.toIntOrNull()
        if (intVal != null) {
            return when (intVal) {
                1 -> 10f
                2 -> 12f
                3 -> 14f
                4 -> 18f
                5 -> 24f
                6 -> 32f
                7 -> 42f
                else -> if (intVal > 7) intVal.toFloat() else null
            }
        }
        return parseHtmlFontSizeToPx(clean, baseFontSize)
    }

    private fun containsPersian(text: String): Boolean {
        return text.any { it in '\u0600'..'\u06FF' || it == '\uFB8A' || it == '\u067E' || it == '\u0686' || it == '\u06AF' }
    }

    private fun preprocessCodeBidi(code: String): String {
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

    private fun highlightPdfCode(code: String): SpannableStringBuilder {
        val ssb = SpannableStringBuilder(code)
        val excludedRanges = mutableListOf<IntRange>()

        // 1. Comments: from '#' or '//' to end of line (grey-green like VS Code)
        val commentRegex = Regex("(#|//).*")
        commentRegex.findAll(code).forEach { match ->
            excludedRanges.add(match.range)
            ssb.setSpan(
                ForegroundColorSpan(Color.rgb(106, 153, 85)),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            ssb.setSpan(
                StyleSpan(Typeface.ITALIC),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // 2. Docstrings / Triple quoted strings: """ ... """
        val tripleQuoteRegex = Regex("\"\"\"[\\s\\S]*?\"\"\"")
        tripleQuoteRegex.findAll(code).forEach { match ->
            if (excludedRanges.none { it.contains(match.range.first) }) {
                excludedRanges.add(match.range)
                ssb.setSpan(
                    ForegroundColorSpan(Color.rgb(106, 153, 85)),
                    match.range.first,
                    match.range.last + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        // 3. String literals: " ... " or ' ... '
        val stringRegex = Regex("\"[^\"]*\"|'[^']*'")
        stringRegex.findAll(code).forEach { match ->
            if (excludedRanges.none { it.contains(match.range.first) }) {
                excludedRanges.add(match.range)
                ssb.setSpan(
                    ForegroundColorSpan(Color.rgb(206, 145, 120)),
                    match.range.first,
                    match.range.last + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
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
                ssb.setSpan(
                    ForegroundColorSpan(Color.rgb(197, 134, 192)),
                    match.range.first,
                    match.range.last + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                ssb.setSpan(
                    StyleSpan(Typeface.BOLD),
                    match.range.first,
                    match.range.last + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        // 5. Function names: yellow/gold like VS Code
        val functionDefRegex = Regex("(def|fun|function)\\s+([a-zA-Z_][a-zA-Z0-9_]*)")
        functionDefRegex.findAll(code).forEach { match ->
            val group = match.groups[2]
            if (group != null && !isExcluded(group.range.first)) {
                ssb.setSpan(
                    ForegroundColorSpan(Color.rgb(220, 220, 170)),
                    group.range.first,
                    group.range.last + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        // 6. Numbers: light green/yellow like VS Code
        val numberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
        numberRegex.findAll(code).forEach { match ->
            if (!isExcluded(match.range.first)) {
                ssb.setSpan(
                    ForegroundColorSpan(Color.rgb(181, 206, 168)),
                    match.range.first,
                    match.range.last + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        return ssb
    }

    private fun drawBlockMath(
        canvas: Canvas,
        lines: List<String>,
        paint: TextPaint,
        typeface: Typeface,
        margin: Float,
        yStart: Float,
        width: Float,
        maxHeight: Float,
        onNewPage: () -> Canvas
    ): Float {
        var currentCanvas = canvas
        var yOffset = yStart
        val rawFormulaText = lines.joinToString("\n").replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
        val formulaText = replaceLatexWithUnicode(rawFormulaText)

        paint.apply {
            textSize = 14f
            this.typeface = typeface
            color = Color.BLACK
        }

        val textLayout = StaticLayout.Builder.obtain(
            formulaText,
            0,
            formulaText.length,
            paint,
            (width - 40f).toInt()
        )
        .setAlignment(Layout.Alignment.ALIGN_CENTER) // Centered formula layout
        .setTextDirection(TextDirectionHeuristics.LTR)
        .setLineSpacing(0f, 1.2f)
        .build()

        val blockHeight = textLayout.height + 24f

        if (yOffset + blockHeight > maxHeight) {
            currentCanvas = onNewPage()
            yOffset = margin
        }

        // Draw a premium soft light background card for mathematical formula
        val bgPaint = Paint().apply {
            color = Color.rgb(245, 246, 249) // Soft grey background
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(220, 224, 230) // Soft border
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val cardRect = RectF(margin, yOffset, margin + width, yOffset + blockHeight)
        currentCanvas.drawRoundRect(cardRect, 8f, 8f, bgPaint)
        currentCanvas.drawRoundRect(cardRect, 8f, 8f, borderPaint)

        // Draw Formula text centered
        currentCanvas.save()
        currentCanvas.translate(margin + 20f, yOffset + 12f)
        textLayout.draw(currentCanvas)
        currentCanvas.restore()

        return yOffset + blockHeight + 12f
    }
}
