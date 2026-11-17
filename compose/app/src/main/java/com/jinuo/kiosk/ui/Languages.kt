package com.jinuo.kiosk.ui

/**
 * The three selectable kiosk languages. Interface copy stays English-only in
 * this prototype (project convention), but the language pill reflects the
 * selection and the picker matches the HTML #k-lang dropdown exactly.
 */
internal data class LangOption(val code: String, val flag: String, val label: String)

internal val LANG_OPTIONS = listOf(
    LangOption("en", "\uD83C\uDDEC\uD83C\uDDE7", "English"), // 🇬🇧
    LangOption("fr", "\uD83C\uDDEB\uD83C\uDDF7", "French"),  // 🇫🇷
    LangOption("zh", "\uD83C\uDDE8\uD83C\uDDF3", "Chinese"), // 🇨🇳
)

internal fun langByCode(code: String): LangOption =
    LANG_OPTIONS.firstOrNull { it.code == code } ?: LANG_OPTIONS[0]
