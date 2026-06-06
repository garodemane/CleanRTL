import os
import io
import re

path = r'app\src\main\java\com\google\hamahang\core\pdf\NativePdfExporter.kt'
with io.open(path, 'r', encoding='utf-8') as f:
    content = f.read()

target = '''            val cleanTrimmedForList = trimmed
                .replace(Regex("^[\\\\u200E\\\\u200F\\\\u202A\\\\u202B\\\\u202C\\\\u202D\\\\u202E\\\\u2066\\\\u2067\\\\u2068\\\\u2069]+"), "")
                .trim()
            if (cleanTrimmedForList.startsWith("- [x] ") || cleanTrimmedForList.startsWith("- [X] ") ||
                cleanTrimmedForList.startsWith("* [x] ") || cleanTrimmedForList.startsWith("* [X] ") ||
                cleanTrimmedForList.startsWith("• [x] ") || cleanTrimmedForList.startsWith("• [X] ")) {
                val bullet = "☑  "
                displayText = bullet + cleanTrimmedForList.substring(6)
                isList = true
            } else if (cleanTrimmedForList.startsWith("- [ ] ") || cleanTrimmedForList.startsWith("* [ ] ") || cleanTrimmedForList.startsWith("• [ ] ")) {
                val bullet = "☐  "
                displayText = bullet + cleanTrimmedForList.substring(6)
                isList = true
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
                val bullet = when (listLevel % 3) {
                    1 -> "◦  "
                    2 -> "▪  "
                    else -> "•  "
                }
                displayText = bullet + trimmed.substring(2)
                isList = true
            } else if (numberedListMatch != null) {'''

replacement = '''            val cleanTrimmedForList = trimmed
                .replace(Regex("[\\\\u200E\\\\u200F\\\\u202A\\\\u202B\\\\u202C\\\\u202D\\\\u202E\\\\u2066\\\\u2067\\\\u2068\\\\u2069]"), "")
                .trim()
            if (cleanTrimmedForList.startsWith("- [x] ") || cleanTrimmedForList.startsWith("- [X] ") ||
                cleanTrimmedForList.startsWith("* [x] ") || cleanTrimmedForList.startsWith("* [X] ") ||
                cleanTrimmedForList.startsWith("• [x] ") || cleanTrimmedForList.startsWith("• [X] ")) {
                val bullet = "☑  "
                displayText = bullet + com.google.hamahang.core.bidi.TextRepairProcessor.stripPrefixKeepingBidi(trimmed, 6)
                isList = true
            } else if (cleanTrimmedForList.startsWith("- [ ] ") || cleanTrimmedForList.startsWith("* [ ] ") || cleanTrimmedForList.startsWith("• [ ] ")) {
                val bullet = "☐  "
                displayText = bullet + com.google.hamahang.core.bidi.TextRepairProcessor.stripPrefixKeepingBidi(trimmed, 6)
                isList = true
            } else if (cleanTrimmedForList.startsWith("- ") || cleanTrimmedForList.startsWith("* ") || cleanTrimmedForList.startsWith("• ")) {
                val bullet = when (listLevel % 3) {
                    1 -> "◦  "
                    2 -> "▪  "
                    else -> "•  "
                }
                displayText = bullet + com.google.hamahang.core.bidi.TextRepairProcessor.stripPrefixKeepingBidi(trimmed, 2)
                isList = true
            } else if (numberedListMatch != null) {'''

content = content.replace(target, replacement)

with io.open(path, 'w', encoding='utf-8') as f:
    f.write(content)
