package com.google.hamahang.core.html

import com.google.hamahang.core.bidi.TextRepairProcessor
import java.io.OutputStream

object HtmlExporter {

    /**
     * Converts a body of Markdown-aware corrected text into a premium, responsive,
     * beautiful HTML document, and writes it directly to the output stream.
     */
    fun exportToHtml(
        text: String,
        outputStream: OutputStream,
        title: String = "خروجی CleanRTL (CleanRTL Web Document)",
        fontSizePx: Int = 16
    ) {
        val paragraphs = text.split("\n")
        val htmlContent = StringBuilder()

        var inCodeBlock = false
        val codeLines = mutableListOf<String>()

        var idx = 0
        while (idx < paragraphs.size) {
            val paragraph = paragraphs[idx]
            val trimmed = paragraph.trim()

            // 1. Code Block parsing
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    val codeContent = codeLines.joinToString("\n")
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                    htmlContent.append("<pre><code>$codeContent</code></pre>\n")
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

            if (paragraph.isBlank()) {
                htmlContent.append("<div class='spacer'></div>\n")
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
                        
                        // Render HTML Table!
                        val isTableRtl = TextRepairProcessor.isParagraphRtl(cleanParagraph)
                        val tableDir = if (isTableRtl) "dir='rtl' class='rtl'" else "dir='ltr' class='ltr'"
                        
                        htmlContent.append("<div class='table-wrapper' $tableDir>\n")
                        htmlContent.append("<table>\n<thead>\n<tr>\n")
                        
                        headerCols.forEachIndexed { colIdx, headerColText ->
                            val align = alignments.getOrNull(colIdx) ?: TableColumnAlignment.LEFT
                            val alignClass = when (align) {
                                TableColumnAlignment.LEFT -> "text-left"
                                TableColumnAlignment.CENTER -> "text-center"
                                TableColumnAlignment.RIGHT -> "text-right"
                            }
                            val headerCellFormatted = formatHtmlInlineStyles(headerColText)
                            val cellRtl = TextRepairProcessor.isParagraphRtl(headerColText)
                            val cellDir = if (cellRtl) "dir='rtl' class='rtl'" else "dir='ltr' class='ltr'"
                            htmlContent.append("<th class='$alignClass' $cellDir>$headerCellFormatted</th>\n")
                        }
                        htmlContent.append("</tr>\n</thead>\n<tbody>\n")
                        
                        dataRows.forEachIndexed { rIdx, rowCells ->
                            htmlContent.append("<tr>\n")
                            for (colIdx in headerCols.indices) {
                                val cellText = rowCells.getOrNull(colIdx) ?: ""
                                val align = alignments.getOrNull(colIdx) ?: TableColumnAlignment.LEFT
                                val alignClass = when (align) {
                                    TableColumnAlignment.LEFT -> "text-left"
                                    TableColumnAlignment.CENTER -> "text-center"
                                    TableColumnAlignment.RIGHT -> "text-right"
                                }
                                val cellFormatted = formatHtmlInlineStyles(cellText)
                                val cellRtl = TextRepairProcessor.isParagraphRtl(cellText)
                                val cellDir = if (cellRtl) "dir='rtl' class='rtl'" else "dir='ltr' class='ltr'"
                                htmlContent.append("<td class='$alignClass' $cellDir>$cellFormatted</td>\n")
                            }
                            htmlContent.append("</tr>\n")
                        }
                        htmlContent.append("</tbody>\n</table>\n</div>\n")
                        
                        idx = k
                        continue
                    }
                }
            }

            // 2. Horizontal Divider Line (--- or *** or ___)
            if (trimmedClean == "---" || trimmedClean == "***" || trimmedClean == "___") {
                htmlContent.append("<hr class='horizontal-divider'>\n")
                idx++
                continue
            }

            // Determine tag type and direction
            var tag = "p"
            var displayText = paragraph
            var isList = false
            var isQuote = false

            when {
                trimmed.startsWith("# ") -> {
                    tag = "h1"
                    displayText = trimmed.substring(2)
                }
                trimmed.startsWith("## ") -> {
                    tag = "h2"
                    displayText = trimmed.substring(3)
                }
                trimmed.startsWith("### ") -> {
                    tag = "h3"
                    displayText = trimmed.substring(4)
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                    tag = "li"
                    displayText = trimmed.substring(2)
                    isList = true
                }
                trimmed.startsWith("> ") || trimmed.startsWith(">") -> {
                    tag = "blockquote"
                    displayText = if (trimmed.startsWith("> ")) trimmed.substring(2) else trimmed.substring(1)
                    isQuote = true
                }
            }

            // Inline styles parse (bold, italic, inline code)
            displayText = formatHtmlInlineStyles(displayText)

            val isRtl = TextRepairProcessor.isParagraphRtl(displayText)
            val dirAttr = if (isRtl) "dir='rtl' class='rtl'" else "dir='ltr' class='ltr'"

            if (isList) {
                htmlContent.append("<ul $dirAttr><li>$displayText</li></ul>\n")
            } else {
                htmlContent.append("<$tag $dirAttr>$displayText</$tag>\n")
            }
            idx++
        }

        // Final code block fallback
        if (inCodeBlock && codeLines.isNotEmpty()) {
            val codeContent = codeLines.joinToString("\n")
            htmlContent.append("<pre><code>$codeContent</code></pre>\n")
        }

        // Build premium, responsive HTML template with CSS styling
        val fullHtml = """
            <!DOCTYPE html>
            <html lang="fa">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>$title</title>
                <!-- Include beautiful web fonts from Google Fonts -->
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700&family=Vazirmatn:wght@300;400;700&display=swap" rel="stylesheet">
                
                <!-- KaTeX for beautiful math formula rendering -->
                <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css">
                <script src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js"></script>
                <script src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/contrib/auto-render.min.js"></script>
                <script>
                    document.addEventListener("DOMContentLoaded", function() {
                        renderMathInElement(document.body, {
                            delimiters: [
                                {left: "\$\$", right: "\$\$", display: true},
                                {left: "\$", right: "\$", display: false}
                            ],
                            throwOnError : false
                        });
                    });
                </script>
                
                <style>
                    :root {
                        --bg-color: #F8F9FC;
                        --card-bg: #FFFFFF;
                        --primary-color: #1D203C;
                        --accent-color: #0E8457;
                        --text-color: #2D3748;
                        --code-bg: #1E1E1E;
                        --code-text: #D4D4D4;
                        --quote-bg: #F0F4F2;
                        --border-color: #E2E8F0;
                        --table-header-bg: rgba(29, 32, 60, 0.05);
                        --table-zebra-bg: rgba(45, 55, 72, 0.02);
                    }

                    @media (prefers-color-scheme: dark) {
                        :root {
                            --bg-color: #0E1119;
                            --card-bg: #171B26;
                            --primary-color: #B2C1FC;
                            --accent-color: #53D9A4;
                            --text-color: #E2E4EC;
                            --quote-bg: #222C28;
                            --border-color: #2D3748;
                            --table-header-bg: rgba(178, 193, 252, 0.12);
                            --table-zebra-bg: rgba(226, 228, 236, 0.04);
                        }
                    }

                    body {
                        font-family: 'Vazirmatn', 'Inter', sans-serif;
                        background-color: var(--bg-color);
                        color: var(--text-color);
                        line-height: 1.8;
                        margin: 0;
                        padding: 40px 20px;
                        display: flex;
                        justify-content: center;
                    }

                    .container {
                        max-width: 800px;
                        width: 100%;
                        background: var(--card-bg);
                        padding: 40px;
                        border-radius: 12px;
                        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
                    }

                    h1, h2, h3 {
                        color: var(--primary-color);
                        margin-top: 1.5em;
                        margin-bottom: 0.5em;
                    }

                    h1 { font-size: 2.2em; border-bottom: 2px solid var(--accent-color); padding-bottom: 8px; }
                    h2 { font-size: 1.7em; }
                    h3 { font-size: 1.3em; }

                    p, li {
                        font-size: ${fontSizePx}px;
                        margin-bottom: 1em;
                    }

                    .rtl {
                        text-align: right;
                        direction: rtl;
                    }

                    .ltr {
                        text-align: left;
                        direction: ltr;
                        font-family: 'Inter', sans-serif;
                    }

                    ul {
                        list-style-type: square;
                        padding-right: 20px;
                        padding-left: 20px;
                        margin-bottom: 1.2em;
                    }

                    blockquote {
                        background-color: var(--quote-bg);
                        border-right: 4px solid var(--accent-color);
                        border-left: none;
                        padding: 15px 20px;
                        margin: 20px 0;
                        font-style: italic;
                        border-radius: 4px;
                    }

                    blockquote.ltr {
                        border-left: 4px solid var(--accent-color);
                        border-right: none;
                    }

                    pre {
                        background-color: var(--code-bg);
                        color: var(--code-text);
                        padding: 16px;
                        border-radius: 8px;
                        overflow-x: auto;
                        font-family: monospace;
                        font-size: 0.9em;
                        direction: ltr;
                        text-align: left;
                        box-shadow: inset 0 2px 8px rgba(0,0,0,0.3);
                    }

                    code {
                        background-color: rgba(0,0,0,0.06);
                        padding: 2px 6px;
                        border-radius: 4px;
                        font-family: monospace;
                        font-size: 0.9em;
                    }

                    pre code {
                        background-color: transparent;
                        padding: 0;
                        border-radius: 0;
                    }

                    .spacer {
                        height: 12px;
                    }

                    hr.horizontal-divider {
                        border: none;
                        border-top: 1.5px solid var(--border-color, #E2E8F0);
                        margin: 24px 0;
                    }

                    /* Premium Table Styles */
                    .table-wrapper {
                        max-width: 100%;
                        overflow-x: auto;
                        margin: 24px 0;
                        border-radius: 8px;
                        border: 1px solid var(--border-color, #E2E8F0);
                        box-shadow: 0 1px 3px rgba(0,0,0,0.02);
                    }

                    table {
                        width: 100%;
                        border-collapse: collapse;
                        font-size: 0.95em;
                        text-align: inherit;
                    }

                    th, td {
                        padding: 12px 16px;
                        border-bottom: 1px solid var(--border-color, #E2E8F0);
                    }

                    th {
                        background-color: var(--table-header-bg, rgba(29, 32, 60, 0.05));
                        color: var(--primary-color);
                        font-weight: 700;
                    }

                    tr:nth-child(even) td {
                        background-color: var(--table-zebra-bg, rgba(45, 55, 72, 0.02));
                    }

                    .text-left { text-align: left; }
                    .text-center { text-align: center; }
                    .text-right { text-align: right; }
                </style>
            </head>
            <body>
                <div class="container">
                    $htmlContent
                </div>
            </body>
            </html>
        """.trimIndent()

        outputStream.write(fullHtml.toByteArray(Charsets.UTF_8))
        outputStream.flush()
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

    private fun formatHtmlInlineStyles(input: String): String {
        var res = input
        res = res.replace(Regex("\\*\\*(.*?)\\*\\*"), "<strong>$1</strong>")
        res = res.replace(Regex("\\*(.*?)\\*"), "<em>$1</em>")
        res = res.replace(Regex("`(.*?)`"), "<code>$1</code>")
        return res
    }
}
