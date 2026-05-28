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
        var currentCodeLang = "code"
        val codeLines = mutableListOf<String>()

        var inMathBlock = false
        val mathLines = mutableListOf<String>()

        var inMermaidBlock = false
        val mermaidLines = mutableListOf<String>()

        var idx = 0
        while (idx < paragraphs.size) {
            val paragraph = paragraphs[idx]
            val trimmed = paragraph.trim()

            // Robustly strip any leading/trailing bidi control characters for math block checks
            val cleanTrimmed = trimmed
                .replace(Regex("^[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]+"), "")
                .replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]+$"), "")
                .trim()

            if (inMermaidBlock) {
                val cleanBlockTrim = trimmed.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
                if (cleanBlockTrim.startsWith("```")) {
                    val mermaidCode = mermaidLines.joinToString("\n")
                        .replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "")
                    htmlContent.append("<pre class='mermaid'>$mermaidCode</pre>\n")
                    mermaidLines.clear()
                    inMermaidBlock = false
                } else {
                    mermaidLines.add(paragraph)
                }
                idx++
                continue
            }

            if (inMathBlock) {
                if (cleanTrimmed.endsWith("$$")) {
                    val cleanLine = cleanTrimmed.removeSuffix("$$")
                    if (cleanLine.isNotEmpty()) {
                        mathLines.add(cleanLine)
                    }
                    val fullFormula = mathLines.joinToString("\n")
                        .replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "")
                    htmlContent.append("<div class='block-math' dir='ltr'>\n\$\$\n$fullFormula\n\$\$\n</div>\n")
                    mathLines.clear()
                    inMathBlock = false
                } else {
                    mathLines.add(paragraph)
                }
                idx++
                continue
            }

            if (cleanTrimmed.startsWith("$$")) {
                if (cleanTrimmed.endsWith("$$") && cleanTrimmed.length > 2) {
                    val cleanFormula = cleanTrimmed.removePrefix("$$").removeSuffix("$$")
                        .replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "")
                    htmlContent.append("<div class='block-math' dir='ltr'>\n\$\$\n$cleanFormula\n\$\$\n</div>\n")
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

            // 1. Code Block parsing
            val cleanCodeBlockTrim = trimmed.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
            if (cleanCodeBlockTrim.startsWith("```")) {
                if (inCodeBlock) {
                    val rawCode = codeLines.joinToString("\n")
                    val preprocessed = preprocessCodeBidi(rawCode)
                    val codeContent = preprocessed
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                    
                    val displayLang = if (currentCodeLang == "code") "CODE" else currentCodeLang.uppercase()
                    htmlContent.append("""
                        <div class="code-window">
                            <div class="code-header">
                                <div class="code-dots">
                                    <span class="dot red"></span>
                                    <span class="dot yellow"></span>
                                    <span class="dot green"></span>
                                </div>
                                <span class="code-lang">$displayLang</span>
                                <button class="copy-btn" onclick="copyCode(this)">کپی</button>
                            </div>
                            <pre><code class="language-$currentCodeLang">$codeContent</code></pre>
                        </div>
                    """.trimIndent() + "\n")
                    
                    codeLines.clear()
                    inCodeBlock = false
                } else {
                    val rawLang = cleanCodeBlockTrim.substring(3).trim().lowercase()
                    val lang = rawLang.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "")
                    if (lang == "mermaid") {
                        inMermaidBlock = true
                    } else {
                        inCodeBlock = true
                        currentCodeLang = if (lang.isEmpty()) "code" else lang
                    }
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
                trimmed.startsWith("#### ") -> {
                    tag = "h4"
                    displayText = trimmed.substring(5)
                }
                trimmed.startsWith("##### ") -> {
                    tag = "h5"
                    displayText = trimmed.substring(6)
                }
                trimmed.startsWith("###### ") -> {
                    tag = "h6"
                    displayText = trimmed.substring(7)
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
            val rawCode = codeLines.joinToString("\n")
            val preprocessed = preprocessCodeBidi(rawCode)
            val codeContent = preprocessed
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
            val displayLang = if (currentCodeLang == "code") "CODE" else currentCodeLang.uppercase()
            htmlContent.append("""
                <div class="code-window">
                    <div class="code-header">
                        <div class="code-dots">
                            <span class="dot red"></span>
                            <span class="dot yellow"></span>
                            <span class="dot green"></span>
                        </div>
                        <span class="code-lang">$displayLang</span>
                        <button class="copy-btn" onclick="copyCode(this)">کپی</button>
                    </div>
                    <pre><code class="language-$currentCodeLang">$codeContent</code></pre>
                </div>
            """.trimIndent() + "\n")
        }

        // Final math block fallback
        if (inMathBlock && mathLines.isNotEmpty()) {
            val fullFormula = mathLines.joinToString("\n")
                .replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "")
            htmlContent.append("<div class='block-math' dir='ltr'>\n\$\$\n$fullFormula\n\$\$\n</div>\n")
        }

        // Final mermaid block fallback
        if (inMermaidBlock && mermaidLines.isNotEmpty()) {
            val mermaidCode = mermaidLines.joinToString("\n")
                .replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "")
            htmlContent.append("<pre class='mermaid'>$mermaidCode</pre>\n")
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
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700&family=JetBrains+Mono:wght@400;700&family=Vazirmatn:wght@300;400;700&display=swap" rel="stylesheet">
                
                <!-- Highlight.js for beautiful code syntax highlighting -->
                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.8.0/styles/github-dark.min.css">
                <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.8.0/highlight.min.js"></script>
                <script>
                    document.addEventListener("DOMContentLoaded", function() {
                        hljs.highlightAll();
                    });
                </script>
                
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

                <!-- Mermaid for beautiful interactive graphs -->
                <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
                <script>
                    document.addEventListener("DOMContentLoaded", function() {
                        mermaid.initialize({
                            startOnLoad: true,
                            theme: (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) ? 'dark' : 'default',
                            securityLevel: 'loose'
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

                    h1, h2, h3, h4, h5, h6 {
                        color: var(--primary-color);
                        margin-top: 1.5em;
                        margin-bottom: 0.5em;
                    }

                    h1 { font-size: 2.2em; border-bottom: 2px solid var(--accent-color); padding-bottom: 8px; }
                    h2 { font-size: 1.7em; }
                    h3 { font-size: 1.3em; }
                    h4 { font-size: 1.15em; }
                    h5 { font-size: 1.0em; }
                    h6 { font-size: 0.85em; }

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

                    /* Premium Code Window Style */
                    .code-window {
                        background-color: #1E1E1E;
                        border-radius: 12px;
                        margin: 24px 0;
                        overflow: hidden;
                        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
                        border: 1px solid rgba(255, 255, 255, 0.08);
                    }

                    .code-header {
                        background-color: #181818;
                        padding: 10px 16px;
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        border-bottom: 1px solid rgba(255, 255, 255, 0.05);
                        user-select: none;
                    }

                    .code-dots {
                        display: flex;
                        gap: 6px;
                    }

                    .dot {
                        width: 12px;
                        height: 12px;
                        border-radius: 50%;
                        display: inline-block;
                    }

                    .dot.red { background-color: #FF5F56; }
                    .dot.yellow { background-color: #FFBD2E; }
                    .dot.green { background-color: #27C93F; }

                    .code-lang {
                        color: #8E8E93;
                        font-size: 0.8em;
                        font-weight: 600;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        font-family: 'Inter', sans-serif;
                    }

                    .copy-btn {
                        background-color: rgba(255, 255, 255, 0.06);
                        color: #E2E4EC;
                        border: none;
                        padding: 4px 10px;
                        border-radius: 4px;
                        font-size: 0.75em;
                        cursor: pointer;
                        transition: background-color 0.2s, transform 0.1s;
                        font-family: 'Vazirmatn', sans-serif;
                    }

                    .copy-btn:hover {
                        background-color: rgba(255, 255, 255, 0.12);
                    }

                    .copy-btn:active {
                        transform: scale(0.95);
                    }

                    pre {
                        margin: 0;
                        background-color: transparent;
                        color: var(--code-text);
                        padding: 16px;
                        border-radius: 8px;
                        overflow-x: auto;
                        font-family: 'JetBrains Mono', 'Fira Code', monospace;
                        font-size: 0.9em;
                        direction: ltr;
                        text-align: left;
                    }

                    .code-window pre {
                        margin: 0;
                        border-radius: 0;
                        padding: 18px;
                    }

                    code {
                        background-color: rgba(0,0,0,0.06);
                        padding: 2px 6px;
                        border-radius: 4px;
                        font-family: 'JetBrains Mono', 'Fira Code', monospace;
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

                    .block-math {
                        margin: 24px 0;
                        padding: 16px;
                        background-color: var(--table-zebra-bg);
                        border: 1px solid var(--border-color);
                        border-radius: 8px;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        overflow-x: auto;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    $htmlContent
                </div>
                <script>
                    function copyCode(button) {
                        var pre = button.parentElement.nextElementSibling;
                        var code = pre.querySelector('code');
                        navigator.clipboard.writeText(code.innerText).then(function() {
                            var originalText = button.innerText;
                            button.innerText = 'کپی شد!';
                            button.style.backgroundColor = '#0E8457';
                            setTimeout(function() {
                                button.innerText = originalText;
                                button.style.backgroundColor = '';
                            }, 2000);
                        });
                    }
                </script>
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
        
        // 1. Bold: **text** or __text__
        res = res.replace(Regex("\\*\\*(.*?)\\*\\*"), "<strong>$1</strong>")
        res = res.replace(Regex("__(.*?)__"), "<strong>$1</strong>")
        
        // 2. Italic: *text* or _text_
        res = res.replace(Regex("\\*(.*?)\\*"), "<em>$1</em>")
        res = res.replace(Regex("_(.*?)_"), "<em>$1</em>")
        
        // 3. Strikethrough: ~~text~~
        res = res.replace(Regex("~~(.*?)~~"), "<del>$1</del>")
        
        // 4. Inline code: `code`
        res = res.replace(Regex("`(.*?)`"), "<code>$1</code>")
        
        // 5. Links: [text](url)
        res = res.replace(Regex("\\[([^\\]]+?)\\]\\(([^\\)]+?)\\)"), { match ->
            val text = match.groupValues[1]
            val rawUrl = match.groupValues[2].trim()
            val urlParts = rawUrl.split(Regex("[\\s\\u00A0]+"))
            val url = urlParts[0].replace("\"", "").replace("'", "")
            "<a href=\"$url\" target=\"_blank\" style=\"color: var(--accent-color); text-decoration: underline;\">$text</a>"
        })
        
        return res
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

                // 2. Double quotes single line string
                val doubleQuoteRegex = Regex("\"([^\"]*)\"")
                processedLine = doubleQuoteRegex.replace(processedLine) { match ->
                    val content = match.groupValues[1]
                    if (containsPersian(content)) {
                        "\"\u2067$content\u2069\""
                    } else {
                        match.value
                    }
                }

                // 3. Single quotes single line string
                val singleQuoteRegex = Regex("'([^']*)'")
                processedLine = singleQuoteRegex.replace(processedLine) { match ->
                    val content = match.groupValues[1]
                    if (containsPersian(content)) {
                        "'\u2067$content\u2069'"
                    } else {
                        match.value
                    }
                }
            }

            processedLine
        }
        return processedLines.joinToString("\n")
    }
}
