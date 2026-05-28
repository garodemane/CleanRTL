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
        baseFontSize: Float = 12f
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
            textSize = baseFontSize
            typeface = regularTypeface
        }

        val paragraphs = text.split("\n")
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

        var idx = 0
        while (idx < paragraphs.size) {
            val paragraph = paragraphs[idx]

            // 1. Math block accumulation check first
            if (inMathBlock) {
                val trimmed = paragraph.trim()
                val cleanTrimmed = trimmed
                    .replace(Regex("^[\\u200E\\u200F\\u2066\\u2067\\u2068\\u2069]+"), "")
                    .replace(Regex("[\\u200E\\u200F\\u2066\\u2067\\u2068\\u2069]+$"), "")
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

            if (inCodeBlock) {
                val trimmed = paragraph.trim()
                if (trimmed.startsWith("```")) {
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
                .replace(Regex("^[\\u200E\\u200F\\u2066\\u2067\\u2068\\u2069]+"), "")
                .replace(Regex("[\\u200E\\u200F\\u2066\\u2067\\u2068\\u2069]+$"), "")
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

            // 3. Code Block boundary check
            if (trimmed.startsWith("```")) {
                inCodeBlock = true
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
            }
            // 3. Bullet Lists (- or * or •)
            else if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
                displayText = "•  " + trimmed.substring(2)
                isList = true
            }
            // 4. Blockquotes (>)
            else if (trimmed.startsWith(">")) {
                displayText = trimmed.substring(1).trim()
                currentTypeface = italicTypeface
                currentTextColor = Color.DKGRAY
                isQuote = true
            }

            // Parse markdown and HTML style tags into spanned text
            val spannedText = parseMarkdownAndHtmlToSpannable(
                displayText,
                currentTextSize,
                boldTypeface,
                italicTypeface
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

            val indentMargin = if (isList) 20f else 0f
            val layoutWidth = (printableWidth - indentMargin).toInt()

            val textLayout = StaticLayout.Builder.obtain(
                spannedText,
                0,
                spannedText.length,
                textPaint,
                layoutWidth
            )
            .setAlignment(alignment)
            .setTextDirection(directionHeuristic)
            .setLineSpacing(0f, 1.2f)
            .build()

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

            // Draw Blockquote decorative border
            if (isQuote) {
                val borderPaint = Paint().apply {
                    color = Color.rgb(83, 217, 164) // Mint Secondary Accent
                    strokeWidth = 3f
                    style = Paint.Style.STROKE
                }
                if (isRtl) {
                    canvas.drawLine(pageWidth - margin, yOffset, pageWidth - margin, yOffset + layoutHeight, borderPaint)
                } else {
                    canvas.drawLine(margin, yOffset, margin, yOffset + layoutHeight, borderPaint)
                }
                canvas.translate(if (isRtl) -15f else 15f, 0f)
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

        // Finish accumulated math block if document ends inside it
        if (inMathBlock && mathBlockLines.isNotEmpty()) {
            drawBlockMath(
                canvas, mathBlockLines, textPaint, serifItalicTypeface,
                margin, yOffset, printableWidth, pageHeight - margin,
                onNewPage = { canvas }
            )
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
            typeface = regularTypeface
        }
        val headerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = baseFontSize - 1f
            color = Color.rgb(14, 17, 25) // Deep Slate
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
                val cellSpanned = parseMarkdownAndHtmlToSpannable(
                    cellText,
                    paint.textSize,
                    boldTypeface,
                    italicTypeface
                )

                val isRtl = TextRepairProcessor.isParagraphRtl(cellText)
                val directionHeuristic = if (isRtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR

                val layoutWidth = (colWidth - 16f).toInt().coerceAtLeast(10)
                val cellLayout = StaticLayout.Builder.obtain(
                    cellSpanned,
                    0,
                    cellSpanned.length,
                    paint,
                    layoutWidth
                )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
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
                val cellText = rowCells.getOrNull(colIdx) ?: ""
                val isRtl = TextRepairProcessor.isParagraphRtl(cellText)
                
                val align = alignments.getOrNull(colIdx) ?: TableColumnAlignment.LEFT

                currentCanvas.save()
                
                // Calculate horizontal position
                val cellX = margin + (if (isTableRtl) (colCount - 1 - colIdx) else colIdx) * colWidth
                
                // Draw text layout
                val textWidth = cellLayout.getLineWidth(0)
                val xOffset = when (align) {
                    TableColumnAlignment.LEFT -> 8f
                    TableColumnAlignment.CENTER -> ((colWidth - textWidth) / 2f).coerceAtLeast(8f)
                    TableColumnAlignment.RIGHT -> (colWidth - textWidth - 8f).coerceAtLeast(8f)
                }

                currentCanvas.translate(cellX + xOffset, yOffset + 8f)
                cellLayout.draw(currentCanvas)
                currentCanvas.restore()
            }

            // Draw horizontal bottom border line
            currentCanvas.drawLine(margin, yOffset + rowHeight, margin + width, yOffset + rowHeight, borderPaint)

            yOffset += rowHeight
            return rowHeight
        }

        // Draw header
        drawSingleRow(headerColumns, isHeader = true, isZebra = false)

        // Draw rows
        dataRows.forEachIndexed { rIdx, rowCells ->
            drawSingleRow(rowCells, isHeader = false, isZebra = rIdx % 2 != 0)
        }

        return yOffset
    }

    private fun parseMarkdownAndHtmlToSpannable(
        input: String,
        baseFontSize: Float,
        boldTypeface: Typeface,
        italicTypeface: Typeface
    ): Spanned {
        val builder = SpannableStringBuilder()
        var index = 0

        // Match bold, italic, inline code, inline math, or HTML span tags
        val regex = Regex("(\\*\\*.*?\\*\\*|\\*.*?\\*|`.*?`|\\$\\$.*?\\$\\$|\\$.*?\\$|<\\s*span\\s+style\\s*=\\s*[\"']([^\"']*)[\"']\\s*>.*?<\\s*/\\s*span\\s*>)")
        val matches = regex.findAll(input)

        for (match in matches) {
            if (match.range.first > index) {
                builder.append(input.substring(index, match.range.first))
            }

            val matchedText = match.value
            when {
                matchedText.startsWith("**") && matchedText.endsWith("**") -> {
                    val start = builder.length
                    val content = matchedText.substring(2, matchedText.length - 2)
                    builder.append(parseMarkdownAndHtmlToSpannable(content, baseFontSize, boldTypeface, italicTypeface))
                    builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedText.startsWith("*") && matchedText.endsWith("*") -> {
                    val start = builder.length
                    val content = matchedText.substring(1, matchedText.length - 1)
                    builder.append(parseMarkdownAndHtmlToSpannable(content, baseFontSize, boldTypeface, italicTypeface))
                    builder.setSpan(StyleSpan(Typeface.ITALIC), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedText.startsWith("`") && matchedText.endsWith("`") -> {
                    val start = builder.length
                    val content = matchedText.substring(1, matchedText.length - 1)
                    builder.append(content)
                }
                matchedText.startsWith("$$") && matchedText.endsWith("$$") -> {
                    val start = builder.length
                    val content = matchedText.substring(2, matchedText.length - 2)
                    builder.append(content)
                    builder.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(android.text.style.TypefaceSpan("serif"), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedText.startsWith("$") && matchedText.endsWith("$") -> {
                    val start = builder.length
                    val content = matchedText.substring(1, matchedText.length - 1)
                    builder.append(content)
                    builder.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    builder.setSpan(android.text.style.TypefaceSpan("serif"), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                matchedText.startsWith("<") && matchedText.endsWith(">") -> {
                    val spanRegex = Regex("<\\s*span\\s+style\\s*=\\s*[\"']([^\"']*)[\"']\\s*>(.*?)<\\s*/\\s*span\\s*>")
                    val spanMatch = spanRegex.matchEntire(matchedText)
                    if (spanMatch != null) {
                        val styleStr = spanMatch.groupValues[1]
                        val innerText = spanMatch.groupValues[2]

                        var color: Int? = null
                        var fontSizePx: Float? = null

                        styleStr.split(";").forEach { stylePart ->
                            val parts = stylePart.split(":")
                            if (parts.size == 2) {
                                val key = parts[0].trim().lowercase()
                                val value = parts[1].trim()
                                if (key == "color") {
                                    color = parseHtmlColorToInt(value)
                                } else if (key == "font-size") {
                                    fontSizePx = parseHtmlFontSizeToPx(value, baseFontSize)
                                }
                            }
                        }

                        val start = builder.length
                        builder.append(parseMarkdownAndHtmlToSpannable(innerText, fontSizePx ?: baseFontSize, boldTypeface, italicTypeface))

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
                else -> {
                    builder.append(matchedText)
                }
            }
            index = match.range.last + 1
        }

        if (index < input.length) {
            builder.append(input.substring(index))
        }

        return builder
    }

    private fun parseHtmlColorToInt(colorStr: String): Int? {
        val clean = colorStr.trim().lowercase()
        val colorMap = mapOf(
            "red" to Color.RED,
            "green" to Color.rgb(0, 255, 0),
            "blue" to Color.BLUE,
            "yellow" to Color.YELLOW,
            "black" to Color.BLACK,
            "white" to Color.WHITE,
            "gray" to Color.GRAY,
            "grey" to Color.GRAY,
            "cyan" to Color.CYAN,
            "magenta" to Color.MAGENTA
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
                Color.rgb(r, g, b)
            } else if (hex.length == 6) {
                val r = hex.substring(0, 2).toInt(16)
                val g = hex.substring(2, 4).toInt(16)
                val b = hex.substring(4, 6).toInt(16)
                Color.rgb(r, g, b)
            } else if (hex.length == 8) {
                val a = hex.substring(0, 2).toInt(16)
                val r = hex.substring(2, 4).toInt(16)
                val g = hex.substring(4, 6).toInt(16)
                val b = hex.substring(6, 8).toInt(16)
                Color.argb(a, r, g, b)
            } else {
                null
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
        val formulaText = lines.joinToString("\n").trim()

        paint.apply {
            textSize = 12f
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
