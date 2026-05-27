package com.google.hamahang.core.bidi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertTrue(result.contains("\u2066https://google.com\u2069"))
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

        assertTrue("Checklist should be RTL", TextRepairProcessor.isParagraphRtl(checkListItem))
        assertTrue("Literal bullet checklist should be RTL", TextRepairProcessor.isParagraphRtl(literalBulletCheckList))
        assertTrue("Literal bullet formula should be RTL", TextRepairProcessor.isParagraphRtl(literalBulletFormula))
        assertTrue("Formula W should be RTL", TextRepairProcessor.isParagraphRtl(formulaW))
        assertTrue("Formula H should be RTL", TextRepairProcessor.isParagraphRtl(formulaH))
        assertTrue("Formula A should be RTL", TextRepairProcessor.isParagraphRtl(formulaA))
        assertTrue("Normal text with trailing LTR should be RTL", TextRepairProcessor.isParagraphRtl(normalTextWithEnglish))
    }

    @Test
    fun testExplicitParagraphDirectionOverrides() {
        // Text that would normally be RTL but is overridden to LTR with LRM
        val overriddenToLtr = "\u200Eاین متن با نشانگر چپ‌به‌راست شروع شده است."
        // Text that would normally be LTR but is overridden to RTL with RLM
        val overriddenToRtl = "\u200FThis sentence starts with a Right-to-Left Mark."

        assertTrue(!TextRepairProcessor.isParagraphRtl(overriddenToLtr))
        assertTrue(TextRepairProcessor.isParagraphRtl(overriddenToRtl))
    }
}
