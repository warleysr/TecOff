package br.com.tecoff.domain

import java.util.regex.Pattern

object GabaritoExtractor {

    private val LETTERS = "ABCDEFGHIJ"

    fun htmlToText(html: String): String {
        return html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</(p|div|li|tr)>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Extracts gabarito and 1-based correct alternative index (1 -> A/Certo, 2 -> B/Errado, etc.).
     */
    fun extract(
        numeroCorretoRaw: Int?,
        tipoQuestao: String?,
        alternativas: List<String>,
        comentarioHtml: String?
    ): Pair<String?, Int?> {
        val isCertoErrado = tipoQuestao == "CERTO_ERRADO" ||
                (alternativas.size == 2 && alternativas.any { it.contains("Certo", ignoreCase = true) })

        // 1. If the server provided an integer directly
        if (numeroCorretoRaw != null && numeroCorretoRaw > 0) {
            val gabaritoStr = if (isCertoErrado) {
                if (numeroCorretoRaw == 1) "Certo" else "Errado"
            } else {
                LETTERS.getOrNull(numeroCorretoRaw - 1)?.toString() ?: numeroCorretoRaw.toString()
            }
            return Pair(gabaritoStr, numeroCorretoRaw)
        }

        if (comentarioHtml.isNullOrBlank()) {
            return Pair(null, null)
        }

        val plainText = htmlToText(comentarioHtml)

        // Pattern 1: Gabarito: ERRADO / Gabarito: Certo / Gabarito: Letra B / Gabarito: A
        val p1 = Pattern.compile(
            """(?:Gabarito|Resposta|Opção)\s*(?:Oficial)?(?:\s*é)?(?:\s*a)?\s*[:\s-]\s*(?:Letra\s*)?([A-J]|Certo|Errado)\b""",
            Pattern.CASE_INSENSITIVE
        )
        val m1 = p1.matcher(plainText)
        if (m1.find()) {
            val matched = m1.group(1) ?: ""
            return resolveMatch(matched, isCertoErrado)
        }

        // Pattern 2: Letra B é a correta / Alternativa C está certa
        val p2 = Pattern.compile(
            """(?:Letra|Alternativa)\s*([A-J])\s*(?:é a|está)?\s*(?:correta|certa)\b""",
            Pattern.CASE_INSENSITIVE
        )
        val m2 = p2.matcher(plainText)
        if (m2.find()) {
            val matched = m2.group(1) ?: ""
            return resolveMatch(matched, isCertoErrado)
        }

        // Pattern 3: B – <texto> ... Certa / Correta
        val p3 = Pattern.compile(
            """([A-J])\s*[–\-—:]\s*[^\n]+\n+\s*(?:Certa|Correta)\b""",
            Pattern.CASE_INSENSITIVE
        )
        val m3 = p3.matcher(plainText)
        if (m3.find()) {
            val matched = m3.group(1) ?: ""
            return resolveMatch(matched, isCertoErrado)
        }

        // Pattern 4: Certo / Errado specific phrases
        if (isCertoErrado) {
            val pCerto = Pattern.compile(
                """\b(?:item|afirmativa|proposição|questão|assertiva)\s*(?:é|está)?\s*(?:certo|certa|correto|correta)\b""",
                Pattern.CASE_INSENSITIVE
            )
            if (pCerto.matcher(plainText).find()) {
                return Pair("Certo", 1)
            }

            val pErrado = Pattern.compile(
                """\b(?:item|afirmativa|proposição|questão|assertiva)\s*(?:é|está)?\s*(?:errado|errada|incorreto|incorreta)\b""",
                Pattern.CASE_INSENSITIVE
            )
            if (pErrado.matcher(plainText).find()) {
                return Pair("Errado", 2)
            }
        }

        return Pair(null, null)
    }

    private fun resolveMatch(raw: String, isCertoErrado: Boolean): Pair<String, Int?> {
        val upper = raw.trim().uppercase()
        if (isCertoErrado) {
            return if (upper.startsWith("C") || upper == "A") {
                Pair("Certo", 1)
            } else {
                Pair("Errado", 2)
            }
        }

        if (upper.length == 1 && LETTERS.contains(upper)) {
            val index = LETTERS.indexOf(upper) + 1
            return Pair(upper, index)
        }

        if (upper.contains("CERTO")) {
            return Pair("Certo", 1)
        }
        if (upper.contains("ERRADO")) {
            return Pair("Errado", 2)
        }

        return Pair(raw, null)
    }
}
