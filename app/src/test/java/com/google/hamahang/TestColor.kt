package com.google.hamahang

import androidx.compose.ui.graphics.Color

fun testColor() {
    val r = 231
    val g = 76
    val b = 60
    val c = Color(red = r, green = g, blue = b)
    println("Color value: " + c.value.toString(16))
}
