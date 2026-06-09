val emojiMap = mapOf(":heart:" to "\u2764\uFE0F")
var res = "This is :heart:"
res = res.replace(Regex(":[a-zA-Z0-9_+\\-]+:")) { match ->
    emojiMap[match.value] ?: match.value
}
println(res)