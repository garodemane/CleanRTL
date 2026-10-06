package com.google.hamahang.core.bidi

import java.util.regex.Pattern

object TextRepairProcessor {
    private const val LRI = "\u2066" // Left-to-Right Isolate
    private const val PDI = "\u2069" // Pop Directional Isolate
    private const val RLM = "\u200F" // Right-to-Left Mark

    // Persian/Arabic-script character ranges
    private val PERSIAN_CHAR_PATTERN = Pattern.compile("[\\u0600-\\u06FF\\uFB8A\\u067E\\u0686\\u06AF]")
    // Strong Latin ranges (letters only, ignoring digits for directional classification)
    private val STRONG_LATIN_PATTERN = Pattern.compile("[A-Za-z]")
    private val LATIN_CHAR_PATTERN = Pattern.compile("[A-Za-z0-9]")

    /**
     * Core repair pipeline. Receives a mixed-language body of text and formats
     * it non-destructively for correct bidi layout rendering.
     */
    fun repairText(input: String, enableNormalization: Boolean = true): String {
        if (input.isBlank()) return input

        // Split text by newlines safely supporting Windows, Linux, Mac endings
        val paragraphs = input.split(Regex("\\R"))
        var inCodeBlock = false
        var inMermaidBlock = false
        var inMathBlock = false
        var inPreBlock = false
        var inTableBlock = false
        var inDetailsBlock = false
        val repairedParagraphs = paragraphs.map { paragraph ->
            val trimmed = paragraph.trim()
            // Strip bidi marks to robustly identify code blocks and language starts
            val cleanCodeBlockTrim = trimmed.replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]"), "").trim()
            val cleanLower = cleanCodeBlockTrim.lowercase()
            if (cleanLower.startsWith("<pre")) {
                inPreBlock = true
                return@map paragraph
            }
            if (inPreBlock) {
                if (cleanLower.contains("</pre>")) {
                    inPreBlock = false
                }
                return@map paragraph
            }

            if (cleanLower.startsWith("<table")) {
                inTableBlock = true
                return@map paragraph
            }
            if (inTableBlock) {
                if (cleanLower.contains("</table>")) {
                    inTableBlock = false
                }
                return@map paragraph
            }

            if (cleanLower.startsWith("<details")) {
                inDetailsBlock = true
                return@map paragraph
            }
            if (inDetailsBlock) {
                if (cleanLower.contains("</details>")) {
                    inDetailsBlock = false
                }
                return@map paragraph
            }

            // HTML structural lines that should NEVER be processed as markdown paragraphs
            if (cleanLower.startsWith("<tr") || cleanLower.startsWith("</tr") ||
                cleanLower.startsWith("<td") || cleanLower.startsWith("</td") ||
                cleanLower.startsWith("<th") || cleanLower.startsWith("</th") ||
                cleanLower.startsWith("</table") ||
                cleanLower.startsWith("<div") || cleanLower.startsWith("</div") ||
                cleanLower.startsWith("<center") || cleanLower.startsWith("</center") ||
                cleanLower.startsWith("<p align=") || cleanLower.startsWith("<p style=") ||
                cleanLower == "<p>" || cleanLower == "</p>" ||
                cleanLower == "<sub>" || cleanLower.startsWith("<sub ") || cleanLower.startsWith("<sub/") ||
                cleanLower == "</sub>" || cleanLower.startsWith("</sub") ||
                cleanLower == "<sup>" || cleanLower.startsWith("<sup ") || cleanLower.startsWith("<sup/") ||
                cleanLower == "</sup>" || cleanLower.startsWith("</sup")) {
                return@map paragraph
            }

            if (cleanCodeBlockTrim.startsWith("```") || cleanCodeBlockTrim.startsWith("~~~")) {
                if (cleanCodeBlockTrim.startsWith("```mermaid")) {
                    inMermaidBlock = true
                } else if (inCodeBlock || inMermaidBlock) {
                    inCodeBlock = false
                    inMermaidBlock = false
                } else {
                    inCodeBlock = true
                }
                return@map paragraph
            }

            if (inCodeBlock || inMermaidBlock) {
                // Return code block and mermaid block lines completely untouched!
                return@map paragraph
            }

            // 4-space or tab indented code block line
            if ((paragraph.startsWith("    ") || paragraph.startsWith("\t")) &&
                !trimmed.startsWith("- ") && !trimmed.startsWith("* ") && !trimmed.startsWith("• ") &&
                !Regex("^[0-9]+\\.").containsMatchIn(trimmed) && !trimmed.startsWith(">")) {
                return@map paragraph
            }

            // Robustly strip any leading/trailing bidi control characters for math block checks
            val cleanTrimmed = trimmed
                .replace(Regex("^[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]+"), "")
                .replace(Regex("[\\u200E\\u200F\\u202A\\u202B\\u202C\\u202D\\u202E\\u2066\\u2067\\u2068\\u2069]+$"), "")
                .trim()

            if (cleanTrimmed.startsWith("$$")) {
                if (cleanTrimmed.endsWith("$$") && cleanTrimmed.length > 2) {
                    // Single line math block
                    return@map paragraph
                } else {
                    inMathBlock = !inMathBlock
                    return@map paragraph
                }
            }

            if (inMathBlock) {
                // Return math block lines completely untouched!
                return@map paragraph
            }

            if (paragraph.isBlank()) return@map paragraph

            val isRtl = isParagraphRtl(paragraph)
            var result = paragraph

            if (isRtl) {
                // Protect inline math runs before doing bidi repair
                val mathPlaceholderMap = mutableListOf<String>()
                val mathRegex = Regex("(\\$\\$\\s*.*?\\s*\\$\\$|\\$\\s*.*?\\s*\\$|\\\\\\(.*?\\\\\\)|\\\\\\[.*?\\\\\\]|(?:[a-zA-Z0-9_\'()\\s=+\\-*/^.]*?\\\\[a-zA-Z]+[a-zA-Z0-9_\'()\\s=+\\-*/^.\\{},]*))")

                // Replace inline math runs with alphanumeric placeholders to avoid bidi corruption
                result = mathRegex.replace(result) { matchResult ->
                    val placeholder = "MATHPLCHLDR${mathPlaceholderMap.size}"
                    mathPlaceholderMap.add(matchResult.value)
                    placeholder
                }

                // Protect ruby annotation blocks before doing bidi repair
                val rubyPlaceholderMap = mutableListOf<String>()
                val rubyRegex = Regex("(?is)<ruby>.*?</ruby>")
                result = rubyRegex.replace(result) { matchResult ->
                    val placeholder = "RUBYPLCHLDR${rubyPlaceholderMap.size}"
                    rubyPlaceholderMap.add(matchResult.value)
                    placeholder
                }

                // Protect footnote citations before doing bidi repair
                val footnotePlaceholderMap = mutableListOf<String>()
                val footnoteRegex = Regex("\\[\\^[^\\]]+\\]")
                result = footnoteRegex.replace(result) { matchResult ->
                    val placeholder = "FNREFPLCHLDR${footnotePlaceholderMap.size}"
                    footnotePlaceholderMap.add(matchResult.value)
                    placeholder
                }

                // Step 1: Normalize Persian glyphs if enabled
                if (enableNormalization) {
                    result = normalizeCharacters(result)
                }

                // Step 2: Safe isolation of LTR runs (preserving HTML tags and markdown tokens)
                result = isolateLtrSubRuns(result)

                // Step 3: Handle punctuation at sentence ends
                result = fixTrailingPunctuation(result)

                // Restore footnote citations from placeholders
                footnotePlaceholderMap.forEachIndexed { index, originalFn ->
                    val placeholder = "FNREFPLCHLDR$index"
                    result = result.replace(placeholder, originalFn)
                }

                // Restore ruby annotation blocks from placeholders
                rubyPlaceholderMap.forEachIndexed { index, originalRuby ->
                    val placeholder = "RUBYPLCHLDR$index"
                    result = result.replace(placeholder, originalRuby)
                }

                // Restore inline math runs from placeholders
                mathPlaceholderMap.forEachIndexed { index, originalMath ->
                    val placeholder = "MATHPLCHLDR$index"
                    result = result.replace(placeholder, originalMath)
                }
            }

            result
        }

        return repairedParagraphs.joinToString("\n")
    }

    /**
     * Determines whether a paragraph is directionally dominant RTL using standard UBA rules.
     */
    fun isParagraphRtl(text: String): Boolean {
        val trimmedStr = text.trimStart()
        if (trimmedStr.startsWith("\u200F")) return true
        if (trimmedStr.startsWith("\u200E")) return false

        // Strip bidi control characters for accurate directional and prefix analysis
        var cleanStr = trimmedStr
            .replace("\u2066", "")
            .replace("\u2067", "")
            .replace("\u2068", "")
            .replace("\u2069", "")
            .replace("\u200E", "")
            .replace("\u200F", "")
            .replace("\u200C", "")
            .replace("\u200D", "")

        // 1. Strip markdown checklist prefix (with or without bullet)
        val checklistMatch = Regex("^([-*•]\\s*)?\\[[ xX]\\]\\s*(.*)").matchEntire(cleanStr)
        if (checklistMatch != null) {
            cleanStr = checklistMatch.groupValues[2].trimStart()
        } else {
            // 2. Strip simple bullet prefix if no checklist
            val bulletMatch = Regex("^[-*•]\\s*(.*)").matchEntire(cleanStr)
            if (bulletMatch != null) {
                cleanStr = bulletMatch.groupValues[1].trimStart()
            }
        }

        // 3. Strip numbered list prefix (e.g. "1. ", "12. ")
        // Only strip ASCII digits. If the user used Persian digits (e.g. "۱. "), keep it
        // so it immediately triggers RTL direction based on explicit language choice!
        val numberedListMatch = Regex("^[0-9]+\\.\\s+(.*)").matchEntire(cleanStr)
        if (numberedListMatch != null) {
            cleanStr = numberedListMatch.groupValues[1].trimStart()
        }

        // 4. Strip formula assignment prefix (e.g. "W = ", "H = ", "temp = ")
        val formulaMatch = Regex("^[a-zA-Z_][a-zA-Z0-9_]*\\s*=\\s+(.*)").matchEntire(cleanStr)
        if (formulaMatch != null) {
            cleanStr = formulaMatch.groupValues[1].trimStart()
        }

        // 4b. Strip HTML tags to ensure correct directional analysis for text starting with HTML markup
        val directionAnalysisStr = cleanStr.replace(Regex("<[^>]*>"), "")

        // 5. If the paragraph contains ANY Persian character, it must be RTL
        if (PERSIAN_CHAR_PATTERN.matcher(directionAnalysisStr).find() ||
            (directionAnalysisStr.isBlank() && PERSIAN_CHAR_PATTERN.matcher(cleanStr).find())) {
            return true
        }

        // 6. If no Persian characters exist, check if there are any strong Latin characters
        if (STRONG_LATIN_PATTERN.matcher(directionAnalysisStr).find() ||
            (directionAnalysisStr.isBlank() && STRONG_LATIN_PATTERN.matcher(cleanStr).find())) {
            return false
        }

        // 7. If line contains common code/syntax characters (braces, brackets, parentheses, HTML, operators, quotes)
        // and has NO Persian characters, it must be LTR so code blocks, braces like '}', and tags are not mirrored!
        val codeSymbolPattern = Regex("[{}()\\[\\];=<>/\\\\_+\\-*\"'|&^%#@~`:]")
        if (codeSymbolPattern.containsMatchIn(cleanStr)) {
            return false
        }

        // If no strong characters exist, default to RTL in a Persian-first app
        return true
    }


    /**
     * Substitutes Arabic Yeh and Kaf characters with native Persian alternatives,
     * and maps spacing offsets around common suffix boundaries using ZWNJs.
     */
    fun normalizeCharacters(text: String): String {
        return text
            // Arabic Yeh to Persian Yeh
            .replace('\u064A', '\u06CC') // ي -> ی
            .replace('\u0649', '\u06CC') // ى -> ی
            // Arabic Kaf to Persian Keheh
            .replace('\u0643', '\u06A9') // ك -> ک
            // ZWNJ formatting for plural suffix "ها"
            .replace(Regex("(\\s+)(ها)(\\s+|$)"), "\u200C$2$3") // کتاب ها -> کتاب‌ها
            // ZWNJ formatting for comparative/superlative suffixes "تر" & "ترین"
            .replace(Regex("(\\s+)(ترین|تر)(\\s+|$)"), "\u200C$2$3") // بزرگ ترین -> بزرگ‌ترین
    }

    /**
     * Wraps inline Latin text runs inside RTL paragraphs using Unicode bidi isolation markers.
     * Prevents text layout leakage to surrounding Persian glyphs.
     */
    fun isolateLtrSubRuns(text: String): String {
        // Match either an HTML tag (excluding sub/sup so formulas stay together) OR a strong Latin/math run (including extended Latin, sub/superscript, and inline sub/sup tags)
        val token = "(?:<sub[^>]*>.*?</sub>|<sup[^>]*>.*?</sup>|~[^~\\s]+~|\\^[^\\^\\s]+\\^|\\^[0-9a-zA-Z]+|[a-zA-Z0-9\\u00C0-\\u024F\\u00B2\\u00B3\\u00B9\\u2070-\\u209F\\u0370-\\u03FF\\u2200-\\u22FF])"
        val connectors = "[ _:\\/.\\-@#\\$|\\\\+, \t=+]"
        val combinedRegex = Regex("(<(?!/?(?:sub|sup)\\b)[^>]+>)|($token(?:$connectors*$token)*)")
        
        return text.replace(combinedRegex) { matchResult ->
            val htmlTag = matchResult.groups[1]?.value
            val ltrRun = matchResult.groups[2]?.value
            
            when {
                htmlTag != null -> {
                    // Do not isolate HTML tags! Return them exactly as they are.
                    htmlTag
                }
                ltrRun != null -> {
                    // This is a normal Latin run, isolate it!
                    if (ltrRun.startsWith(LRI) || ltrRun.all { it.isDigit() }) {
                        ltrRun
                    } else {
                        "$LRI$ltrRun$PDI"
                    }
                }
                else -> matchResult.value
            }
        }
    }

    /**
     * Fixes layout behavior where terminal periods or marks on RTL lines ending with
     * an LTR word get rendered on the wrong far-right side of the layout bounding frame.
     */
    fun fixTrailingPunctuation(text: String): String {
        // Targets trailing sentence punctuation preceded immediately by the LTR Isolate block end
        val endingPunctuationRegex = Regex("(\\u2069)([.!?؟:])(\\s*)$")
        return if (endingPunctuationRegex.containsMatchIn(text)) {
            text.replace(endingPunctuationRegex) { match ->
                val groupValues = match.groupValues
                groupValues[1] + groupValues[2] + RLM + groupValues[3]
            }
        } else {
            text
        }
    }

    /**
     * Strips a prefix of a certain length (ignoring bidi control characters) from the original string,
     * preserving all bidi control characters in the remaining string.
     */
    fun stripPrefixKeepingBidi(original: String, prefixLengthWithoutBidi: Int): String {
        var nonBidiCount = 0
        val bidiChars = setOf('\u200E', '\u200F', '\u202A', '\u202B', '\u202C', '\u202D', '\u202E', '\u2066', '\u2067', '\u2068', '\u2069')
        for (i in original.indices) {
            if (original[i] !in bidiChars) {
                nonBidiCount++
            }
            if (nonBidiCount == prefixLengthWithoutBidi) {
                return original.substring(i + 1)
            }
        }
        return ""
    }
}
