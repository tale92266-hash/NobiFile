package com.nobifile.manager.ops

data class RenameRule(
    val find: String = "",
    val replace: String = "",
    val prefix: String = "",
    val suffix: String = "",
    val startNumber: Int = 1,
    val padDigits: Int = 2,
    val useNumbering: Boolean = false,
    val uppercase: Boolean = false,
    val lowercase: Boolean = false,
    val changeExtension: String = ""
)

object BatchRename {
    fun preview(names: List<String>, rule: RenameRule): List<String> {
        return names.mapIndexed { index, original ->
            var base = original.substringBeforeLast('.', "")
            var ext = original.substringAfterLast('.', "")
            val hasExt = original.contains('.')
            if (!hasExt) { base = original; ext = "" }
            var out = base
            if (rule.find.isNotEmpty()) out = out.replace(rule.find, rule.replace)
            if (rule.prefix.isNotEmpty()) out = rule.prefix + out
            if (rule.suffix.isNotEmpty()) out = out + rule.suffix
            if (rule.useNumbering) {
                val num = (rule.startNumber + index).toString().padStart(rule.padDigits, '0')
                out = "${num}_$out"
            }
            if (rule.uppercase) out = out.uppercase()
            if (rule.lowercase) out = out.lowercase()
            val finalExt = if (rule.changeExtension.isNotBlank()) rule.changeExtension.trim().removePrefix(".") else ext
            if (hasExt || finalExt.isNotBlank()) "$out.$finalExt" else out
        }
    }
}

object TextCompare {
    data class DiffLine(val kind: Char, val text: String) // ' ' same, '-' left only, '+' right only
    fun diff(left: List<String>, right: List<String>): List<DiffLine> {
        // Simple LCS-based diff, suitable for small config/text files.
        val n = left.size
        val m = right.size
        if (n * m > 200000) {
            // Fallback: line-set comparison for very large files.
            val rightSet = right.toSet()
            val leftSet = left.toSet()
            val out = mutableListOf<DiffLine>()
            left.forEach { if (it !in rightSet) out.add(DiffLine('-', it)) }
            right.forEach { if (it !in leftSet) out.add(DiffLine('+', it)) }
            return out
        }
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) for (j in m - 1 downTo 0) {
            dp[i][j] = if (left[i] == right[j]) dp[i + 1][j + 1] + 1 else maxOf(dp[i + 1][j], dp[i][j + 1])
        }
        val out = mutableListOf<DiffLine>()
        var i = 0; var j = 0
        while (i < n && j < m) {
            if (left[i] == right[j]) { out.add(DiffLine(' ', left[i])); i++; j++ }
            else if (dp[i + 1][j] >= dp[i][j + 1]) { out.add(DiffLine('-', left[i])); i++ }
            else { out.add(DiffLine('+', right[j])); j++ }
        }
        while (i < n) { out.add(DiffLine('-', left[i])); i++ }
        while (j < m) { out.add(DiffLine('+', right[j])); j++ }
        return out
    }
}
