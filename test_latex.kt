import java.io.File

fun main() {
    val superMap = mapOf(
        '0' to "\u2070", '1' to "\u00B9", '2' to "\u00B2", '3' to "\u00B3", '4' to "\u2074",
        '5' to "\u2075", '6' to "\u2076", '7' to "\u2077", '8' to "\u2078", '9' to "\u2079",
        '+' to "\u207A", '-' to "\u207B", '=' to "\u207C", '(' to "\u207D", ')' to "\u207E",
        'n' to "\u207F", 'i' to "\u2071",
        'x' to "\u02E3", 'y' to "\u02B8", 'a' to "\u1D43", 'b' to "\u1D47", 'c' to "\u1D9C"
    )

    val subMap = mapOf(
        '0' to "\u2080", '1' to "\u2081", '2' to "\u2082", '3' to "\u2083", '4' to "\u2084",
        '5' to "\u2085", '6' to "\u2086", '7' to "\u2087", '8' to "\u2088", '9' to "\u2089",
        '+' to "\u208A", '-' to "\u208B", '=' to "\u208C", '(' to "\u208D", ')' to "\u208E",
        'a' to "\u2090", 'e' to "\u2091", 'o' to "\u2092", 'i' to "\u1D62", 'u' to "\u1D64",
        'x' to "\u2093", 'r' to "\u1D63", 'v' to "\u1D65", 'j' to "\u2C7C"
    )

    fun toSup(s: String) = s.map { superMap[it] ?: it.toString() }.joinToString("")
    fun toSub(s: String) = s.map { subMap[it] ?: it.toString() }.joinToString("")

    fun latexSymbolToStr(sym: String): String {
        var r = sym
        r = r.replace("\\infty", "\u221E")
        return r
    }

    var s = "\\int_{-\\infty}^{\\infty} e^{-x^2} dx = \\sqrt(\\pi)\n\\sum_{n=0}^{\\infty} r^n = \\frac{1}{1-r}, \\quad |r| < 1"
    
    // Step 1
    s = s.replace("\\left", "").replace("\\right", "")
    s = s.replace("\\{", "{").replace("\\}", "}")
    s = s.replace("\\,", " ").replace("\\!", "").replace("\\;", " ").replace("\\:", " ")
    s = s.replace("\\\\", "\n")

    // Step 4
    s = s.replace(Regex("\\^\\{([^}]*)\\}")) { m ->
        val inner = latexSymbolToStr(m.groupValues[1])
        toSup(inner)
    }
    s = s.replace(Regex("_\\{([^}]*)\\}")) { m ->
        val inner = latexSymbolToStr(m.groupValues[1])
        toSub(inner)
    }
    s = s.replace(Regex("\\^([0-9a-zA-Z+\\-])")) { m -> toSup(m.groupValues[1]) }
    s = s.replace(Regex("_([0-9a-zA-Z+\\-])")) { m -> toSub(m.groupValues[1]) }

    // Step 5
    s = s.replace("\\int", "\u222B")
    s = s.replace("\\sum", "\u2211")
    s = s.replace("\\infty", "\u221E")

    println(s)
}
