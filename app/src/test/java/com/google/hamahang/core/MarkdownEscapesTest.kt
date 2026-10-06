package com.google.hamahang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.google.hamahang.core.html.HtmlExporter
import java.io.ByteArrayOutputStream

class MarkdownEscapesTest {

    private fun exportToHtmlString(text: String): String {
        val out = ByteArrayOutputStream()
        HtmlExporter.exportToHtml(text, out)
        return out.toString("UTF-8")
    }

    @Test
    fun testEscapedCharactersInNormalText() {
        val input = "برای نمایش \\*ستاره\\*، \\`بکتیک\\`، \\[کروشه\\]، \\#هشتگ و \\_زیرخط\\_ از بک‌اسلش استفاده کنید."
        val html = exportToHtmlString(input)
        
        // Ensure no formatting triggers
        assertTrue("Should not contain <em> or <strong> tags for escaped star", !html.contains("<em>"))
        assertTrue("Should not contain <code> for escaped backtick", !html.contains("<code>"))
        assertTrue("Should not contain <a> for escaped bracket", !html.contains("<a href"))

        // Ensure backslashes are stripped and literal punctuation is shown
        assertTrue("Should contain *ستاره*", html.contains("*ستاره*"))
        assertTrue("Should contain `بکتیک`", html.contains("`بکتیک`"))
        assertTrue("Should contain [کروشه]", html.contains("[کروشه]"))
        assertTrue("Should contain #هشتگ", html.contains("#هشتگ"))
        assertTrue("Should contain _زیرخط_", html.contains("_زیرخط_"))
    }

    @Test
    fun testEscapesInsideInlineCode() {
        val input = "این یک کد است: `\\*ستاره\\*`"
        val html = exportToHtmlString(input)
        
        // Ensure inline code contains escaped characters literally (with their backslashes)
        assertTrue("Should contain escaped text literally inside code tag", html.contains("<code>\\*ستاره\\*</code>"))
    }

    @Test
    fun testCenteredFooterWithDividerAndLinks() {
        val input = """
            <center>
            
            ---
            
            [گزارش باگ](https://example.com/bug) | [درخواست ویژگی](https://example.com/feat) | [کانال تلگرام](https://t.me/example)
            
            ساخته شده با ❤️ توسط تیم کلین‌آر‌تی‌ال
            تمامی حقوق محفوظ است © ۲۰۲۴
            </center>
        """.trimIndent()
        val html = exportToHtmlString(input)

        assertTrue("Should contain horizontal divider", html.contains("<hr class='horizontal-divider'>"))
        assertTrue("Should contain bug report link", html.contains("href=\"https://example.com/bug\""))
        assertTrue("Should contain feature request link", html.contains("href=\"https://example.com/feat\""))
        assertTrue("Should contain telegram link", html.contains("href=\"https://t.me/example\""))
        assertTrue("Should be wrapped in text-center container", html.contains("<div class=\"text-center\""))
        assertTrue("Should contain footer text", html.contains("ساخته شده با ❤️ توسط تیم کلین‌آر‌تی‌ال"))
    }

    @Test
    fun testCenteredBadgesAndSubtitle() {
        val input = """
            <center>
            [![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://example.com/v)
            [![License](https://img.shields.io/badge/license-MIT-green.svg)](https://example.com/lic)
            ![Platform](https://img.shields.io/badge/platform-Android-brightgreen.svg)
            
            **مستندات رسمی و راهنمای جامع توسعه‌دهندگان**
            </center>
        """.trimIndent()
        val html = exportToHtmlString(input)

        assertTrue("Should contain version badge", html.contains("src=\"https://img.shields.io/badge/version-1.0.0-blue.svg\""))
        assertTrue("Should contain license badge", html.contains("src=\"https://img.shields.io/badge/license-MIT-green.svg\""))
        assertTrue("Should contain platform badge", html.contains("src=\"https://img.shields.io/badge/platform-Android-brightgreen.svg\""))
        assertTrue("Should render bold subtitle", html.contains("<strong>مستندات رسمی و راهنمای جامع توسعه‌دهندگان</strong>"))
        assertTrue("Should have inline-img class", html.contains("class='inline-img'"))
    }

    @Test
    fun testCenteredMarkdownTable() {
        val input = """
            <center>
            | 📈 مشارکت‌کنندگان | 🐛 ایشوهای حل شده | ⭐ ستاره‌ها | 🔄 آخرین به‌روزرسانی |
            | :---: | :---: | :---: | :---: |
            | **۴۷** | **۱۲۸** | **۱.۲k** | **امروز** |
            </center>
        """.trimIndent()
        val html = exportToHtmlString(input)

        assertTrue("Should contain table tag", html.contains("<table>"))
        assertTrue("Should contain table headers", html.contains("<th class='text-center'") && html.contains("📈 مشارکت‌کنندگان"))
        assertTrue("Should contain formatted bold cell", html.contains("<td class='text-center'") && html.contains("<strong>۴۷</strong>"))
        assertTrue("Should contain table wrapper", html.contains("<div class='table-wrapper'"))
    }
}
