import io

path = r'app\src\main\java\com\google\hamahang\core\html\HtmlExporter.kt'
with io.open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace paragraphs generation
old_loop = """        for (p in rawParagraphs) {
            val cleanP = p.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
            val match = refDefRegex.matchEntire(cleanP)
            if (match != null) {
                val label = match.groupValues[1].trim().lowercase()
                val url = match.groupValues[2].trim()
                val titleVal = match.groupValues[3].trim().takeIf { it.isNotEmpty() }
                referenceMap[label] = Pair(url, titleVal)
            } else {
                paragraphs.add(p)
            }
        }"""

new_loop = """        val footnoteRegex = Regex("^\\s*\\[\\^([^\\]]+)\\]:\\s*(.+)$")
        val footnoteMap = mutableMapOf<String, String>()

        for (p in rawParagraphs) {
            val bidiFreeP = p.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "")
            val cleanP = bidiFreeP.trim()
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
                footnoteMap[id] = fnText
            } else {
                paragraphs.add(bidiFreeP)
            }
        }"""

content = content.replace(old_loop, new_loop)

# Append footnoteMap output
old_end = """        // Final code block fallback
        if (inCodeBlock && codeLines.isNotEmpty()) {"""

new_end = """        if (footnoteMap.isNotEmpty()) {
            htmlContent.append("<hr style='margin-top: 40px; border: 0; border-top: 1px solid var(--border-color);'>\\n")
            htmlContent.append("<div class='footnotes'>\\n")
            footnoteMap.forEach { (id, text) ->
                val formattedText = formatHtmlInlineStyles(text, referenceMap)
                val isRtl = TextRepairProcessor.isParagraphRtl(formattedText)
                val dirAttr = if (isRtl) "dir='rtl' class='rtl'" else "dir='ltr' class='ltr'"
                htmlContent.append("<p id='fn-$id' $dirAttr><sup>$id</sup> $formattedText</p>\\n")
            }
            htmlContent.append("</div>\\n")
        }

        // Final code block fallback
        if (inCodeBlock && codeLines.isNotEmpty()) {"""

content = content.replace(old_end, new_end)

with io.open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("HtmlExporter updated")