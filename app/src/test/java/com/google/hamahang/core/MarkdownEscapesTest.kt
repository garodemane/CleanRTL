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
}
