import java.util.regex.Regex

fun main() {
    val input = "<span style="color: #e74c3c;">???? — Red — Rot</span> |"
    val regex = Regex("(?is)<[\\s\\u00A0]*span[^>]*>.*?<[\\s\\u00A0]*/[\\s\\u00A0]*span[\\s\\u00A0]*>")
    val matches = regex.findAll(input)
    for (match in matches) {
        val matchedTextClean = match.value
        val spanRegex = Regex("(?is)<[\\s\\u00A0]*span([^>]*)>(.*?)<[\\s\\u00A0]*/[\\s\\u00A0]*span[\\s\\u00A0]*>")
        val spanMatch = spanRegex.matchEntire(matchedTextClean)
        println("matchedTextClean: ${matchedTextClean}")
        if (spanMatch != null) {
            val attrsStr = spanMatch.groupValues[1]
            println("attrsStr: ${attrsStr}")
            val styleMatch = Regex("(?i)style[\\s\\u00A0]*=[\\s\\u00A0]*([^\\s\\u00A0>]+|'[^']*'|\"[^\"]*\")").find(attrsStr)
            if (styleMatch != null) {
                val rawStyle = styleMatch.groupValues[1]
                println("rawStyle: ${rawStyle}")
                var clean = rawStyle.trim()
                if (clean.startsWith("\"") && clean.endsWith("\"")) {
                    clean = clean.removeSurrounding("\"", "\"")
                }
                println("cleanQuotes: ${clean}")
            } else {
                println("styleMatch FAILED")
            }
        } else {
            println("spanMatch FAILED")
        }
    }
}
