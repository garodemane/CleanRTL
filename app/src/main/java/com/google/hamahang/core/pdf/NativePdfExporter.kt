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

        var idx = 0
        while (idx < paragraphs.size) {
            val paragraph = paragraphs[idx]
            val trimmed = paragraph.trim()

            // 1. Code Block parsing (```)
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
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
                    inCodeBlock = true
                }
                idx++
                continue
            }

            if (inCodeBlock) {
                codeBlockLines.add(paragraph)
                idx++
                continue
            }

            if (paragraph.isBlank()) {
                yOffset += baseFontSize * 0.8f
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
            // 3. Bullet Lists (- or *)
            else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
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

            // Bold/Italic replacements for simple inline formatting
            displayText = displayText.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            displayText = displayText.replace(Regex("\\*(.*?)\\*"), "$1")
            displayText = displayText.replace(Regex("`(.*?)`"), "$1")

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
                displayText,
                0,
                displayText.length,
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
            drawCodeBlock(
                canvas, codeBlockLines, textPaint, monospaceTypeface,
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
        val codeText = lines.joinToString("\n")

        paint.apply {
            textSize = 10f
            this.typeface = typeface
            color = Color.rgb(80, 80, 80)
        }

        val textLayout = StaticLayout.Builder.obtain(
            codeText,
            0,
            codeText.length,
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

        // Draw Card Background for Code Block
        val bgPaint = Paint().apply {
            color = Color.rgb(240, 241, 245)
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
                val cleanCellText = cellText
                    .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
                    .replace(Regex("\\*(.*?)\\*"), "$1")
                    .replace(Regex("`(.*?)`"), "$1")

                val isRtl = TextRepairProcessor.isParagraphRtl(cleanCellText)
                val directionHeuristic = if (isRtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR

                val layoutWidth = (colWidth - 16f).toInt().coerceAtLeast(10)
                val cellLayout = StaticLayout.Builder.obtain(
                    cleanCellText,
                    0,
                    cleanCellText.length,
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
}
