package com.google.hamahang.core.bidi

enum class AppLanguage { FA, EN }
enum class AppThemeMode { SYSTEM, LIGHT, DARK }

object Loc {
    fun tr(key: String, lang: AppLanguage): String {
        return when (lang) {
            AppLanguage.FA -> mapFa[key] ?: key
            AppLanguage.EN -> mapEn[key] ?: key
        }
    }
    
    private val mapFa = mapOf(
        "app_title" to "CleanRTL",
        "editor_tab_raw" to "متن خام",
        "editor_tab_corrected" to "اصلاح‌شده",
        "editor_tab_preview" to "پیش‌نمایش",
        "input_label" to "متن ورودی غیر هم‌تراز شما",
        "output_label" to "خروجی اصلاح شده با هم‌ترازی صحیح",
        "preview_label" to "پیش‌نمایش زنده مارک‌داون سند",
        "btn_paste" to "چسباندن",
        "btn_copy" to "کپی",
        "btn_pdf" to "PDF",
        "btn_html" to "HTML",
        "norm_switch_label" to "اصلاح خودکار نویسه‌ها (ی/ک/نیم‌فاصله)",
        "font_size_label" to "اندازه قلم ویرایشگر",
        "settings_ui_font_scale" to "اندازه قلم پوسته برنامه (UI)",
        "menu_editor" to "ویرایشگر",
        "menu_settings" to "تنظیمات",
        "settings_title" to "تنظیمات برنامه",
        "settings_lang" to "زبان برنامه (Language)",
        "settings_theme" to "تم برنامه (Theme)",
        "theme_system" to "پیش‌فرض سیستم",
        "theme_light" to "تم روشن",
        "theme_dark" to "تم تاریک",
        "toast_pasted" to "متن از حافظه موقت چسبانده شد!",
        "toast_copied" to "متن اصلاح‌شده در حافظه کپی شد!",
        "toast_pdf_saved" to "فایل PDF با موفقیت ذخیره شد!",
        "toast_html_saved" to "فایل HTML با موفقیت ذخیره شد!",
        "clear_desc" to "پاک‌سازی متن",
        "about_title" to "درباره CleanRTL",
        "about_desc" to "یک ابزار تماماً مدرن و نیتیو برای هم‌ترازی و مرتب‌سازی به هم ریختگی متون ترکیبی فارسی و انگلیسی، بدون آسیب رساندن به سینتکس نشانه‌گذاری مارک‌داون."
    )

    private val mapEn = mapOf(
        "app_title" to "CleanRTL",
        "editor_tab_raw" to "Raw Text",
        "editor_tab_corrected" to "Corrected",
        "editor_tab_preview" to "Preview",
        "input_label" to "Your unaligned input text",
        "output_label" to "Corrected & aligned text output",
        "preview_label" to "Live Markdown Document Preview",
        "btn_paste" to "Paste",
        "btn_copy" to "Copy",
        "btn_pdf" to "PDF",
        "btn_html" to "HTML",
        "norm_switch_label" to "Auto-normalize (Yeh/Kaf/ZWNJ)",
        "font_size_label" to "Editor Font Size",
        "settings_ui_font_scale" to "App UI Font Scale",
        "menu_editor" to "Workspace",
        "menu_settings" to "Settings",
        "settings_title" to "Application Settings",
        "settings_lang" to "Application Language",
        "settings_theme" to "App Visual Theme",
        "theme_system" to "System Default",
        "theme_light" to "Light Mode",
        "theme_dark" to "Dark Mode",
        "toast_pasted" to "Text successfully pasted!",
        "toast_copied" to "Corrected text copied to clipboard!",
        "toast_pdf_saved" to "PDF saved successfully in Downloads!",
        "toast_html_saved" to "HTML saved successfully in Downloads!",
        "clear_desc" to "Clear text",
        "about_title" to "About CleanRTL",
        "about_desc" to "A fully native, state-of-the-art formatting utility that fixes scrambled layouts and trailing punctuations on mixed RTL/LTR texts while preserving standard Markdown codes."
    )
}
