package com.google.hamahang.core.bidi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import com.google.hamahang.features.editor.isTableDivider
import com.google.hamahang.features.editor.parseTableLine
import com.google.hamahang.features.editor.parseAlignment
import com.google.hamahang.features.editor.TableColumnAlignment

class TextRepairProcessorTest {

    @Test
    fun testTableParsing() {
        val header = "| نام پیشنهادی | حس و معنی کلی |"
        val divider = "|---|---|"
        val dataRow = "| BidiFix | تاکید روی درست‌کردن متن دوجهته (RTL/LTR) |"

        // Verify start/end checks on trimmed text
        assertTrue(header.trim().startsWith("|") && header.trim().endsWith("|"))
        assertTrue(divider.trim().startsWith("|") && divider.trim().endsWith("|"))
        assertTrue(dataRow.trim().startsWith("|") && dataRow.trim().endsWith("|"))

        // Verify isTableDivider
        assertTrue(isTableDivider(divider))
        assertTrue(!isTableDivider(header))
        assertTrue(!isTableDivider(dataRow))

        // Verify parseTableLine
        val headerCols = parseTableLine(header)
        assertEquals(2, headerCols.size)
        assertEquals("نام پیشنهادی", headerCols[0])
        assertEquals("حس و معنی کلی", headerCols[1])

        val dataCols = parseTableLine(dataRow)
        assertEquals(2, dataCols.size)
        assertEquals("BidiFix", dataCols[0])
        assertEquals("تاکید روی درست‌کردن متن دوجهته (RTL/LTR)", dataCols[1])

        // Verify parseAlignment
        assertEquals(TableColumnAlignment.LEFT, parseAlignment("---"))
        assertEquals(TableColumnAlignment.CENTER, parseAlignment(":---:"))
        assertEquals(TableColumnAlignment.RIGHT, parseAlignment("---:"))
    }


    @Test
    fun testParagraphDirectionDetection() {
        val rtlText = "این یک متن فارسی است."
        val ltrText = "This is a simple English sentence."
        val mixedText = "این متن شامل کلمات English زیادی است."

        assertTrue(TextRepairProcessor.isParagraphRtl(rtlText))
        assertTrue(TextRepairProcessor.isParagraphRtl(mixedText))
        assertTrue(!TextRepairProcessor.isParagraphRtl(ltrText))
    }

    @Test
    fun testNormalizationArabicToPersian() {
        // Raw text contains Arabic Yeh (ي) and Kaf (ك)
        val rawInput = "يك كتاب يگانه‌ي فارسي براي استفاده در كشور."
        val expected = "یک کتاب یگانه‌ی فارسی برای استفاده در کشور."
        val result = TextRepairProcessor.normalizeCharacters(rawInput)
        assertEquals(expected, result)
    }

    @Test
    fun testSmartZwnjInsertion() {
        val rawInput = "کتاب ها بزرگ تر و زیبا ترین هستند."
        val expected = "کتاب‌ها بزرگ‌تر و زیبا‌ترین هستند."
        val result = TextRepairProcessor.normalizeCharacters(rawInput)
        assertEquals(expected, result)
    }

    @Test
    fun testIsolateLtrSubRuns() {
        val rawInput = "متن شامل کلمه English و URL https://google.com است."
        val result = TextRepairProcessor.isolateLtrSubRuns(rawInput)
        
        // Assert that the English word and URL are wrapped with Left-to-Right Isolate marks
        assertTrue(result.contains("\u2066English\u2069"))
        assertTrue(result.contains("\u2066URL https://google.com\u2069"))
    }

    @Test
    fun testFixTrailingPunctuation() {
        // RTL sentence ending in an LTR word
        val rawInput = "این پروژه است در Android."
        val result = TextRepairProcessor.repairText(rawInput)
        
        // Assert that a Right-to-Left Mark (RLM) is appended to resolve end period layout placement
        assertTrue(result.endsWith(".\u200F"))
    }

    @Test
    fun testNumberedListParagraphDirection() {
        // List items starting with numbers and periods
        val rtlListItem = "1. فایل gradle/libs.versions.toml را باز کن."
        val ltrListItem = "1. Open gradle/libs.versions.toml file."
        
        assertTrue(TextRepairProcessor.isParagraphRtl(rtlListItem))
        assertTrue(!TextRepairProcessor.isParagraphRtl(ltrListItem))
    }

    @Test
    fun testUserReportedAlignments() {
        val checkListItem = "[x] کراتین مونوهیدرات — ۵ گرم روزانه"
        val literalBulletCheckList = "• [x] کراتین مونوهیدرات — ۵ گرم روزانه"
        val literalBulletFormula = "• W = وزن بر حسب کیلوگرم"
        val formulaW = "W = وزن بر حسب کیلوگرم"
        val formulaH = "H = قد بر حسب سانتی‌متر"
        val formulaA = "A = سن بر حسب سال"
        val normalTextWithEnglish = "شما: 2847.5 کالری در روز TDEE"

        // Wrapped in bidi isolate control characters by TextRepairProcessor.isolateLtrSubRuns
        val checkListItemIsolated = "[\u2066x\u2069] کراتین مونوهیدرات — ۵ گرم روزانه"
        val literalBulletCheckListIsolated = "• [\u2066x\u2069] کراتین مونوهیدرات — ۵ گرم روزانه"
        val literalBulletCheckListDoubleSpaceIsolated = "•  [\u2066x\u2069] کراتین مونوهیدرات — ۵ گرم روزانه"
        val formulaWIsolated = "\u2066W\u2069 = وزن بر حسب کیلوگرم"
        val formulaWIsolatedWithBullet = "• \u2066W\u2069 = وزن بر حسب کیلوگرم"

        assertTrue("Checklist should be RTL", TextRepairProcessor.isParagraphRtl(checkListItem))
        assertTrue("Literal bullet checklist should be RTL", TextRepairProcessor.isParagraphRtl(literalBulletCheckList))
        assertTrue("Literal bullet formula should be RTL", TextRepairProcessor.isParagraphRtl(literalBulletFormula))
        assertTrue("Formula W should be RTL", TextRepairProcessor.isParagraphRtl(formulaW))
        assertTrue("Formula H should be RTL", TextRepairProcessor.isParagraphRtl(formulaH))

        assertTrue("Formula A should be RTL", TextRepairProcessor.isParagraphRtl(formulaA))
        assertTrue("Normal text with trailing LTR should be RTL", TextRepairProcessor.isParagraphRtl(normalTextWithEnglish))

        // Assert isolated/formatted variants are correctly identified as RTL
        assertTrue("Isolated checklist should be RTL", TextRepairProcessor.isParagraphRtl(checkListItemIsolated))
        assertTrue("Isolated literal bullet checklist should be RTL", TextRepairProcessor.isParagraphRtl(literalBulletCheckListIsolated))
        assertTrue("Isolated double spaced literal bullet checklist should be RTL", TextRepairProcessor.isParagraphRtl(literalBulletCheckListDoubleSpaceIsolated))
        assertTrue("Isolated formula W should be RTL", TextRepairProcessor.isParagraphRtl(formulaWIsolated))
        assertTrue("Isolated formula W with bullet should be RTL", TextRepairProcessor.isParagraphRtl(formulaWIsolatedWithBullet))
    }

    @Test
    fun testNewScreenshots() {
        val mathLine = "فرمول درون‌خطی: نرخ تغییرات با رابطه \$f'(x) = \\lim_{h \\to 0} \\frac{f(x+h) - f(x)}{h}\$ تعریف می‌شود."
        val mathRepaired = TextRepairProcessor.repairText(mathLine)
        assertTrue(mathRepaired.contains("\$f'(x) = \\lim_{h \\to 0} \\frac{f(x+h) - f(x)}{h}\$"))
        val styledMath = com.google.hamahang.features.editor.parseMarkdownInlineStyles(mathRepaired, androidx.compose.ui.graphics.Color.White)
        assertTrue(styledMath.text.contains("f'(x) ="))
        assertTrue(styledMath.text.contains("lim"))

        val rubyLine = "• راهنمای تلفظ (Ruby Text): <ruby>東京<rp>(</rp><rt>Tōkyō</rt><rp>)</rp></ruby> - <ruby>برلین<rp>(</rp><rt>Berlin</rt><rp>)</rp></ruby>"
        val rubyRepaired = TextRepairProcessor.repairText(rubyLine)
        assertTrue(rubyRepaired.contains("<ruby>東京<rp>(</rp><rt>Tōkyō</rt><rp>)</rp></ruby>"))
        assertTrue(rubyRepaired.contains("<ruby>برلین<rp>(</rp><rt>Berlin</rt><rp>)</rp></ruby>"))

        val subLine = "• زیرنویس (Subscript): H~2~O (آب / Wasser / Water)"
        val subLineHtml = "• زیرنویس (Subscript): H<sub>2</sub>O (آب / Wasser / Water)"
        val superLine = "• بالانویس (Superscript): E = mc^2^ (انرژی / Energie / Energy)"
        val superLineHtml = "• بالانویس (Superscript): E = mc<sup>2</sup> (انرژی / Energie / Energy)"
        
        val subRepaired = TextRepairProcessor.repairText(subLine)
        val subHtmlRepaired = TextRepairProcessor.repairText(subLineHtml)
        val superRepaired = TextRepairProcessor.repairText(superLine)
        val superHtmlRepaired = TextRepairProcessor.repairText(superLineHtml)

        assertTrue(subRepaired.contains("\u2066H~2~O\u2069"))
        assertTrue(subHtmlRepaired.contains("\u2066H<sub>2</sub>O\u2069"))
        assertTrue(superRepaired.contains("\u2066E = mc^2^\u2069"))
        assertTrue(superHtmlRepaired.contains("\u2066E = mc<sup>2</sup>\u2069"))

        val summaryLine = "<summary><b>Click to expand / Zum Erweitern klicken / برای مشاهده جزئیات کلیک کنید 🔽</b></summary>"
        val summaryRepaired = TextRepairProcessor.repairText(summaryLine)
        assertTrue(summaryRepaired.startsWith("<summary><b>"))
        assertTrue(summaryRepaired.endsWith("</b></summary>"))
        assertTrue(summaryRepaired.contains("Click to expand"))
        assertTrue(summaryRepaired.contains("برای مشاهده جزئیات کلیک کنید"))

        val parsedSummary = com.google.hamahang.features.editor.parseMarkdownInlineStyles(summaryRepaired, androidx.compose.ui.graphics.Color.White)
        assertTrue(!parsedSummary.text.contains("<b>"))
        assertTrue(!parsedSummary.text.contains("</b>"))
    }

    @Test
    fun testExplicitParagraphDirectionOverrides() {
        val lrm = 0x200E.toChar()
        val rlm = 0x200F.toChar()
        
        // Text that would normally be RTL but is overridden to LTR with LRM
        val overriddenToLtr = "${lrm}این متن با نشانگر چپ‌به‌راست شروع شده است."
        // Text that would normally be LTR but is overridden to RTL with RLM
        val overriddenToRtl = "${rlm}This sentence starts with a Right-to-Left Mark."

        assertTrue(!TextRepairProcessor.isParagraphRtl(overriddenToLtr))
        assertTrue(TextRepairProcessor.isParagraphRtl(overriddenToRtl))
    }

    @Test
    fun testHtmlSpanHandling() {
        val rawInput = "نمودار پیشرفت شامل <span style=\"color:#00ff00\">2020</span> است."

        // isolateLtrSubRuns should NOT wrap HTML tags or style attributes in bidi control marks
        val isolated = TextRepairProcessor.isolateLtrSubRuns(rawInput)
        assertEquals("HTML span tags should not be isolated", rawInput, isolated)

        // isParagraphRtl should correctly identify the paragraph as RTL
        assertTrue("RTL text with inline HTML spans should be detected as RTL", TextRepairProcessor.isParagraphRtl(rawInput))

        val emojiSpanInput = "🔴 <span style=\"color: #d9534f; font-weight: bold; font-size: 1.1em\">هشدار حریم خصوصی:</span>"
        val repairedSpan = TextRepairProcessor.repairText(emojiSpanInput)
        println("REPAIRED SPAN: " + repairedSpan)
        assertEquals("repairedSpan should not corrupt HTML tags", emojiSpanInput, repairedSpan)

        // The quote-agnostic regex compiled with (?isU)
        val inlineRegex = java.util.regex.Pattern.compile("(?isU)(\\*\\*.*?\\*\\*|\\*.*?\\*|`.*?`|\\$\\$.*?\\$\\$|\\$.*?\\$|<\\s*span\\s+style\\s*=\\s*[^>]+>.*?<\\s*/\\s*span\\s*>|<\\s*font\\s+[^>]*>.*?<\\s*/\\s*font\\s*>)")
        val matcher = inlineRegex.matcher(repairedSpan)
        val matched = matcher.find()
        println("REGEX MATCHED: " + matched)
        assertTrue("Regex should match repairedSpan", matched)

        // Test with smart curly double quotes
        val smartCurlyDoubleInput = "🔴 <span style=“color: #d9534f; font-weight: bold; font-size: 1.1em”>هشدار حریم خصوصی:</span>"
        val repairedSmartCurly = TextRepairProcessor.repairText(smartCurlyDoubleInput)
        assertEquals("repairedSmartCurly should preserve smart quotes", smartCurlyDoubleInput, repairedSmartCurly)

        val matcherSmart = inlineRegex.matcher(repairedSmartCurly)
        assertTrue("Regex should match smart quote style", matcherSmart.find())

        // Test with angled quotes
        val angledInput = "🔴 <span style=«color: #d9534f»>هشدار حریم خصوصی:</span>"
        val repairedAngled = TextRepairProcessor.repairText(angledInput)
        assertEquals("repairedAngled should preserve angled quotes", angledInput, repairedAngled)
        assertTrue("Regex should match angled quote style", inlineRegex.matcher(repairedAngled).find())

        // Test uppercase case insensitivity
        val uppercaseInput = "🔴 <SPAN STYLE=\"COLOR: #D9534F\">هشدار</SPAN>"
        assertTrue("Regex should match uppercase tags", inlineRegex.matcher(uppercaseInput).find())

        // Test non-breaking space (Unicode whitespace \u00A0)
        val nbspInput = "🔴 <span style=\"color:#00ff00\">2020</span>" // contains non-breaking space \u00A0 between span and style
        assertTrue("Regex should match tags containing non-breaking spaces", inlineRegex.matcher(nbspInput).find())

        // Test multiline tag / newline inside span content
        val multilineInput = "🔴 <span style=\"color:#00ff00\">هشدار\nحریم\nخصوصی</span>"
        assertTrue("Regex should match tags containing newline characters", inlineRegex.matcher(multilineInput).find())
    }

    @Test
    fun testHtmlFontHandling() {
        val rawInput = "<font size=\"6\">تحول پزشکی در عصر دیجیتال</font> (H1 - تیتر اصلی)"
        assertTrue("RTL text with leading HTML font tag should be detected as RTL", TextRepairProcessor.isParagraphRtl(rawInput))
    }

    @Test
    fun testCodeBlockBypassInRepairText() {
        val codeText = """
            ```python
            # تست تابع
            user_prompt = "چرا یادگیری گیتار سخت است..."
            ```
        """.trimIndent()
        
        val repaired = TextRepairProcessor.repairText(codeText)
        assertEquals(codeText, repaired)
    }

    @Test
    fun testBlockMathBypassInRepairText() {
        val b = '\\'
        val d = '$'
        val blockMathSingle = "${d}${d}L = -${b}frac{1}{N} ${b}sum_{i=1}^{N} y_i ${b}log(p_i)${d}${d}"
        val blockMathMulti = "${d}${d}\nL = -${b}frac{1}{N} ${b}sum_{i=1}^{N} y_i ${b}log(p_i)\n${d}${d}"

        // Block math lines should be returned completely untouched
        assertEquals(blockMathSingle, TextRepairProcessor.repairText(blockMathSingle))
        assertEquals(blockMathMulti, TextRepairProcessor.repairText(blockMathMulti))
        
        // Also verify block math with leading bidi overrides is bypassed
        val blockMathSingleWithBidi = "\u200F${d}${d}L = -${b}frac{1}{N} ${b}sum_{i=1}^{N} y_i ${b}log(p_i)${d}${d}"
        assertEquals(blockMathSingleWithBidi, TextRepairProcessor.repairText(blockMathSingleWithBidi))
    }

    @Test
    fun testInlineMathProtectionInRepairText() {
        val lri = 0x2066.toChar()
        val pdi = 0x2069.toChar()
        val d = '$'
        val rawInput = "فرمول انتروپی متقاطع (${d}Loss${d} ${d}Cross-Entropy${d}) برای پیش‌بینی کلمات بعدی به شکل زیر تعریف می‌شود:"
        val result = TextRepairProcessor.repairText(rawInput)

        // The formulas ($Loss$ and $Cross-Entropy$) should remain completely untouched inside
        // and should NOT have directional isolates injected between the dollar signs and the letters.
        assertTrue("Should preserve Loss inline math cleanly", result.contains("${lri}${d}Loss${d}${pdi}") || result.contains("${d}Loss${d}"))
        assertTrue("Should preserve Cross-Entropy inline math cleanly", result.contains("${lri}${d}Cross-Entropy${d}${pdi}") || result.contains("${d}Cross-Entropy${d}"))
    }

    @Test
    fun testCodeBraceAndHtmlTagsAreNotRtl() {
        assertFalse("Closing brace '}' must be LTR", TextRepairProcessor.isParagraphRtl("}"))
        assertFalse("Opening brace '{' must be LTR", TextRepairProcessor.isParagraphRtl("{"))
        assertFalse("Closing paren with semicolon ');' must be LTR", TextRepairProcessor.isParagraphRtl(");"))
        assertFalse("HTML div tag must be LTR", TextRepairProcessor.isParagraphRtl("<div style=\"text-align: center\">"))
        assertFalse("HTML closing div tag must be LTR", TextRepairProcessor.isParagraphRtl("</div>"))
        assertFalse("HTML table tag must be LTR", TextRepairProcessor.isParagraphRtl("<table>"))
        assertFalse("HTML closing tr tag must be LTR", TextRepairProcessor.isParagraphRtl("</tr>"))
        assertFalse("HTML img tag must be LTR", TextRepairProcessor.isParagraphRtl("<img alt=\"logo\" src=\"icon.jpg\">"))
        assertTrue("Persian paragraph must be RTL", TextRepairProcessor.isParagraphRtl("سلام دنیا"))
        assertTrue("Persian paragraph with code symbols must be RTL", TextRepairProcessor.isParagraphRtl("متن فارسی با نماد }"))
        assertFalse("Progress tag must be LTR", TextRepairProcessor.isParagraphRtl("<progress value=\"75\" max=\"100\"> 75% </progress> 75%"))
        assertFalse("Div style tag must be LTR", TextRepairProcessor.isParagraphRtl("<div style=\"background-color: #e3f2fd; border-left: 5px solid #2196F3;\">"))
    }

    @Test
    fun testFootnoteCitationPreservedInRtlParagraph() {
        val lri = 0x2066.toChar()
        val pdi = 0x2069.toChar()
        val inputWithPeriodFirst = "مارک‌داون در سال ۲۰۰۴ معرفی شد.[^1]"
        val repaired1 = TextRepairProcessor.repairText(inputWithPeriodFirst)
        
        // Assert that [^1] is preserved intact and NOT broken into [ + isolate + ^1 + ]
        assertTrue("Footnote [^1] must be preserved intact without broken brackets",
            repaired1.contains("[^1]") || repaired1.contains("${lri}[^1]${pdi}"))
        assertFalse("Footnote citation must NOT have isolate inside brackets",
            repaired1.contains("[${lri}^1${pdi}]") || repaired1.contains("[${lri}^1"))

        val inputWithPeriodAfter = "مارک‌داون در سال ۲۰۰۴ معرفی شد[^1]."
        val repaired2 = TextRepairProcessor.repairText(inputWithPeriodAfter)
        assertTrue("Footnote [^1] must be preserved intact with trailing period",
            repaired2.contains("[^1]") || repaired2.contains("${lri}[^1]${pdi}"))
    }

    @Test
    fun testSubAndSupTagsSkippedAsStructuralLines() {
        assertEquals("<sub>", TextRepairProcessor.repairText("<sub>"))
        assertEquals("</sub>", TextRepairProcessor.repairText("</sub>"))
        assertEquals("<sup>", TextRepairProcessor.repairText("<sup>"))
        assertEquals("</sup>", TextRepairProcessor.repairText("</sup>"))
    }
}













