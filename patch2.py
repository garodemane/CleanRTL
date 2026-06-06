import os
import io
import re

path = r'app\src\main\java\com\google\hamahang\core\pdf\NativePdfExporter.kt'
with io.open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace the bidi regex to drop the ^
content = re.sub(r'replace\(Regex\("\^\[\\\\u200E(.*?)\+''\), ""\)', r'replace(Regex("[\\\\u200E\1"), "")', content)

# Replace the substring calls with stripPrefixKeepingBidi
content = re.sub(
    r'val bullet = "☑  "\s+displayText = bullet \+ cleanTrimmedForList\.substring\(6\)',
    r'val bullet = "☑  "\n                displayText = bullet + com.google.hamahang.core.bidi.TextRepairProcessor.stripPrefixKeepingBidi(trimmed, 6)',
    content
)
content = re.sub(
    r'val bullet = "☐  "\s+displayText = bullet \+ cleanTrimmedForList\.substring\(6\)',
    r'val bullet = "☐  "\n                displayText = bullet + com.google.hamahang.core.bidi.TextRepairProcessor.stripPrefixKeepingBidi(trimmed, 6)',
    content
)
content = re.sub(
    r'displayText = bullet \+ trimmed\.substring\(2\)',
    r'displayText = bullet + com.google.hamahang.core.bidi.TextRepairProcessor.stripPrefixKeepingBidi(trimmed, 2)',
    content
)
content = re.sub(
    r'\} else if \(trimmed\.startsWith\("- "\) \|\| trimmed\.startsWith\("\* "\) \|\| trimmed\.startsWith\("• "\)\)',
    r'} else if (cleanTrimmedForList.startsWith("- ") || cleanTrimmedForList.startsWith("* ") || cleanTrimmedForList.startsWith("• "))',
    content
)

with io.open(path, 'w', encoding='utf-8') as f:
    f.write(content)
