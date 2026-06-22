package com.easytier.ui

/**
 * Shared translation utility for Compose UI files.
 * Returns [zh] when langZh=true, [en] otherwise.
 */
fun T(zh: String, en: String, langZh: Boolean): String = if (langZh) zh else en
