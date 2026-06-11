fun main() {
    var s = "\\begin{pmatrix}\n a & b \\\\\n c & d\n\\end{pmatrix}"
    
    // Step 1
    s = s.replace("\\left", "").replace("\\right", "")
    s = s.replace("\\{", "{").replace("\\}", "}")
    s = s.replace("\\,", " ").replace("\\!", "").replace("\\;", " ").replace("\\:", " ")
    s = s.replace("\\\\", "\n") // LaTeX line break

    // Step 1b
    val matrixEnvRegex = Regex("\\\\begin\\{(p?matrix|b?matrix|Bmatrix|vmatrix|Vmatrix|array)\\}([\\s\\S]*?)\\\\end\\{\\1\\}", RegexOption.DOT_MATCHES_ALL)
    s = matrixEnvRegex.replace(s) { m ->
        val rows = m.groupValues[2].split("\\\\").map { row ->
            row.split("&").joinToString(" | ") { it.trim() }
        }.filter { it.isNotBlank() }
        "[\n" + rows.joinToString("\n") + "\n]"
    }
    
    println(s)
}
