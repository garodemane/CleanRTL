# CleanRTL

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material_3-757575?style=flat-square)](https://m3.material.io)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)

---

### [راهنمای فارسی](#-فارسی) | [English Guide](#-english)

---

## 🇮🇷 فارسی

**CleanRTL** یک ابزار تماماً مدرن، بومی (Native) و پیشرفته برای دستگاه‌های اندرویدی است که جهت هم‌ترازی، مرتب‌سازی و رفع به هم ریختگی متون ترکیبی راست‌چین (RTL) مانند فارسی/عربی و چپ‌چین (LTR) مانند انگلیسی، بدون آسیب رساندن به سینتکس نشانه‌گذاری مارک‌داون (Markdown) طراحی شده است.

<p align="center">
  <img src="https://img.shields.io/badge/تمرکز_پروژه-RTL_Formatting-blue?style=for-the-badge" alt="Focus">
</p>

### 🚀 ویژگی‌های کلیدی

* **اصلاح خودکار چیدمان دوجهته (BiDi):** مرتب‌سازی خودکار پرانتزها، نقطه‌گذاری‌ها، پرانتزهای جفت و کلمات انگلیسی درون متون فارسی برای پیشگیری از به هم ریختگی ظاهری.
* **پشتیبانی کامل از جداول مارک‌داون:** رندر و نمایش خیره‌کننده جداول مارک‌داون با چیدمان پویای محتوای سلول‌ها بر اساس جهت‌یابی متن و زبان هر خانه به صورت مجزا.
* **کنترل جهت پاراگراف (به سبک MS Word):** دکمه اختصاصی جهت‌یابی (مشابه دکمه Direction در مایکروسافت ورد) برای معکوس‌سازی جهت پاراگراف حاوی مکان‌نما با استفاده از کنترل‌کننده‌های یونیکد بدون تخریب کد اصلی.
* **خروجی‌های حرفه‌ای (PDF & HTML):**
  * **HTML واکنش‌گرا:** تولید اسناد تمیز با استایل‌های مدرن شیشه‌ای (شب و روز متناسب با تم سیستم).
  * **PDF بومی:** خروجی چند صفحه‌ای با بازنویسی و پیچش هوشمند ستون‌های جداول راست‌چین و تکرار خودکار سربرگ جدول در صفحات جدید.
* **رابط کاربری پیشرفته و مدرن:** طراحی شده با جت‌پک کامپوز و متریال ۳، پشتیبانی از تم تاریک/روشن سیستم، قابلیت بزرگ‌نمایی پویا مقیاس قلم کل رابط کاربری و ذخیره‌سازی ابری/محلی تنظیمات شما.

### 🛠️ نیازمندی‌های توسعه
* اندروید استودیو (نسخه Ladybug یا جدیدتر)
* جاوا نسخه JDK 17 یا بالاتر
* Android SDK 35 (حداقل SDK 26)

### 💻 نحوه اجرا و توسعه
کافیست پروژه را در اندروید استودیو باز کنید و یا از دستور زیر در خط فرمان برای بیلد استفاده کنید:
```bash
# بیلد نسخه مخصوص تست و خطایابی
./gradlew assembleDebug
```

---

## 🇬🇧 English

**CleanRTL** is a premium, fully native, state-of-the-art formatting utility for Android designed to fix scrambled layouts, parentheses, and trailing punctuations on mixed RTL/LTR texts (like Persian/Arabic and English) while preserving standard Markdown codes.

### 🚀 Core Features

* **Auto BiDi Corrections:** Restructures scrambled parenthesis layouts and misplaced trailing punctuations on mixed-direction text strings.
* **Robust Markdown Table Processing:** High-fidelity markdown table rendering across the UI, HTML outputs, and native PDF canvases with per-cell direction heuristics.
* **MS Word-Style Direction Toggle:** Forces paragraph alignment (RTL/LTR) instantly utilizing non-destructive standard Unicode direction override marks (LRM/RLM).
* **Premium Multi-Format Exporters:**
  * **Responsive HTML:** Generates modern CSS-styled documents matching system visual themes (Dark/Light Mode).
  * **Native PDF Exporter:** Multi-page PDF generation supporting multi-line text wrapping within cells and automatic table-header repetition on page breaks.
* **Modern Material 3 Interface:** Dynamic system dark mode, overall UI font-scaling factor, and local configuration persistence via Jetpack Compose.

### 🛠️ Development Requirements
* Android Studio (Ladybug or later)
* JDK 17+
* Android SDK 35 (Min SDK 26)

### 💻 Build & Run
Clone the repository, open it in Android Studio, or run the following Gradle task to generate the executable package:
```bash
# Assembles the debug APK
./gradlew assembleDebug
```

---

## 📄 License
This project is licensed under the MIT License - see the LICENSE file for details.
