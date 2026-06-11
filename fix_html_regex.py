import re

# Fix EditorScreen.kt - fix broken aHrefRegex
path_editor = r'app/src/main/java/com/google/hamahang/features/editor/EditorScreen.kt'
with open(path_editor, encoding='utf-8') as f:
    content = f.read()

pos = content.find('val aHrefRegex = Regex(')
if pos >= 0:
    end = content.find('\n', pos)
    bad_line = content[pos:end]
    print("Bad EditorScreen line:", repr(bad_line))
    # Replace with correct version
    good_line = '                val aHrefRegex = Regex("""(?is)<a[^>]*?href=["\'']?([^"\\' >]+)["\\'']?[^>]*>(.*?)</a>""")'
    # Actually, let's just use a safe form
    good_line = '                val aHrefRegex = Regex("(?is)<a[^>]*?href=(?:[\"\\']([^\"\\'>\\s]+)[\"\\']|([^>\\s\"\\'+]+))[^>]*>(.*?)</a>")'
    content2 = content[:pos] + good_line + content[end:]
    with open(path_editor, 'w', encoding='utf-8') as f:
        f.write(content2)
    print("Fixed EditorScreen.kt aHrefRegex")
else:
    print("aHrefRegex NOT FOUND in EditorScreen.kt")

# Fix NativePdfExporter.kt - fix broken aHrefRegex
path_pdf = r'app/src/main/java/com/google/hamahang/core/pdf/NativePdfExporter.kt'
with open(path_pdf, encoding='utf-8') as f:
    content = f.read()

pos = content.find('val aHrefRegex = Regex(')
if pos >= 0:
    end = content.find('\n', pos)
    bad_line = content[pos:end]
    print("Bad PDF line:", repr(bad_line))
    good_line = '                    val aHrefRegex = Regex("(?is)<a[^>]*?href=(?:[\"\\\']((?:[^\"\\\'> ])+)[\"\\\'|([^>\\s\"\\\']+))[^>]*>(.*?)</a>")'
    # Simplify
    good_line = '                    val aHrefRegex = Regex("(?is)<a[^>]*?href=(?:[\"\\\'']([^\"\\\'> ]+)[\"\\\'']|([^>\\s\"\\\']+))[^>]*>(.*?)</a>")'
    content2 = content[:pos] + good_line + content[end:]
    with open(path_pdf, 'w', encoding='utf-8') as f:
        f.write(content2)
    print("Fixed NativePdfExporter.kt aHrefRegex")
else:
    print("aHrefRegex NOT FOUND in NativePdfExporter.kt")

print("Done!")
