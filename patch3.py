import io
path = r'app\src\main\java\com\google\hamahang\core\pdf\NativePdfExporter.kt'

with io.open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('.trim()\n            if (cleanTrimmedForList.startsWith("- [x] ")', '.trim()\n            if (cleanCodeBlockTrim.startsWith("- [x] ")')
content = content.replace('val bullet = "𝘛  "\n                displayText = bullet + cleanTrimmedForList.substring(6)', 'val bullet = "𝘛  "\n                displayText = bullet + com.google.hamahang.core.bidi.TextRepairProcessor.stripPrefixKeepingBidi(trimmed, 6)')

with io.open(path, 'w', encoding='utf-8') as f:
    f.write(content)