package com.example.util

import android.os.Build
import android.text.Html

object TextCleaner {

    fun sanitizeText(input: String?): String {
        if (input.isNullOrBlank()) return ""

        var s = input

        // 1. Convert HTML break/paragraph tags to newlines
        s = s.replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)<p[^>]*>"), "\n")
            .replace(Regex("(?i)</p>"), "\n")

        // 2. Remove remaining HTML tags
        s = s.replace(Regex("<[^>]*>"), "")

        // 3. Decode common double-encoded or mangled Telegram/web patterns like #%39, &#39;, &#039;, %27, %20, etc.
        s = s.replace("#%39", "'")
            .replace("#%20", " ")
            .replace("#%22", "\"")
            .replace("#%26", "&")
            .replace("%27", "'")
            .replace("%20", " ")
            .replace("%22", "\"")
            .replace("%26", "&")
            .replace("&amp;#39;", "'")
            .replace("&#39;", "'")
            .replace("&#039;", "'")
            .replace("&#x27;", "'")
            .replace("&apos;", "'")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&#8217;", "'")
            .replace("&#8216;", "'")
            .replace("&#8220;", "\"")
            .replace("&#8221;", "\"")
            .replace("&#8211;", "-")
            .replace("&#8212;", "-")

        // 4. Decode numeric HTML entities (e.g. &#39; &#34; &#60; etc.)
        s = decodeNumericEntities(s)

        // 5. Try Android's Html.fromHtml for any remaining entities
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                s = Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY).toString()
            } else {
                @Suppress("DEPRECATION")
                s = Html.fromHtml(s).toString()
            }
        } catch (_: Exception) { }

        // 6. Clean up residual escape sequences or trailing hashtag noise
        s = s.replace("#%39", "'")
            .replace("&#39;", "'")
            .replace("&#039;", "'")

        // 7. Normalize spaces and newlines
        s = s.replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

        return s
    }

    private fun decodeNumericEntities(text: String): String {
        return try {
            val pattern = java.util.regex.Pattern.compile("&#(\\d+);")
            val matcher = pattern.matcher(text)
            val sb = StringBuffer()
            while (matcher.find()) {
                val code = matcher.group(1)?.toIntOrNull()
                if (code != null) {
                    val replacement = code.toChar().toString()
                    matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement))
                }
            }
            matcher.appendTail(sb)
            sb.toString()
        } catch (_: Exception) {
            text
        }
    }
}
