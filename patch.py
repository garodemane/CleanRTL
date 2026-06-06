import os
import re

pdf_file = "app/src/main/java/com/google/hamahang/core/pdf/NativePdfExporter.kt"
with open(pdf_file, "r", encoding="utf-8") as f:
    pdf_content = f.read()

# Fix regex in NativePdfExporter
regex_pattern = r"""val regex = Regex\(""" + "\"(?is)(\\Q(?is)(\\E.*?)\\)\""""
if "___.*?___" not in pdf_content:
    pdf_content = pdf_content.replace(
        "|\\*\\*\\*.*?\\*\\*\\*|",
        "|***.*?***|___.*?___|"
    )

with open(pdf_file, "w", encoding="utf-8") as f:
    f.write(pdf_content)

html_file = "app/src/main/java/com/google/hamahang/core/html/HtmlExporter.kt"
with open(html_file, "r", encoding="utf-8") as f:
    html_content = f.read()

if "___.*?___" not in html_content:
    html_content = html_content.replace(
        "|\\*\\*\\*.*?\\*\\*\\*|",
        "|***.*?***|___.*?___|"
    )

with open(html_file, "w", encoding="utf-8") as f:
    f.write(html_content)

print("Regex patched")

