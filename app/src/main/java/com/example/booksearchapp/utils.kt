package com.example.booksearchapp

import android.text.Html
import android.text.Spanned

fun String.stripHtml(): String {
    // Convert HTML formatted string into a spanned text (plain text with formatting)
    val spanned: Spanned = Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
    // Convert the spanned text back to a regular string
    return spanned.toString()
}