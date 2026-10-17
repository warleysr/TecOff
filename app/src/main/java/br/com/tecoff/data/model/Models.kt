package br.com.tecoff.data.model

import com.google.gson.annotations.SerializedName

data class Pasta(
    val id: Long,
    val nome: String,
    val qtdCadernos: Int = 0,
    val ativa: Boolean = false,
    val subpastas: List<Subpasta> = emptyList(),
    val cadernos: List<Caderno> = emptyList()
)

data class Subpasta(
    val id: Long,
    val pastaId: Long,
    val nome: String,
    val cadernos: List<Caderno> = emptyList()
)

data class Caderno(
    val id: Long,
    val pastaId: Long,
    val subpastaId: Long? = null,
    val nome: String,
    val quantidadeItens: Int = 0,
    val data: String? = null,
    val downloadedOffline: Boolean = false,
    val lastSyncAt: Long? = null,
    val totalQuestoesBaixadas: Int = 0,
    val totalResolucoes: Int = 0,
    val totalAcertos: Int = 0,
    val pendenciasSync: Int = 0
) {
    val taxaAcerto: Float
        get() = if (totalResolucoes > 0) (totalAcertos.toFloat() / totalResolucoes) * 100f else 0f
}

data class Question(
    val id: Long,
    val posicaoCaderno: Int,
    val tipo: String = "MULTIPLA_ESCOLHA", // CERTO_ERRADO or MULTIPLA_ESCOLHA
    val formato: String = "OBJETIVA",
    val materiaId: Long? = null,
    val materiaNome: String? = null,
    val assuntoId: Long? = null,
    val assuntoNome: String? = null,
    val banca: String? = null,
    val orgao: String? = null,
    val cargo: String? = null,
    val ano: Int? = null,
    val dificuldade: String? = null,
    val anulada: Boolean = false,
    val desatualizada: Boolean = false,
    val enunciadoHtml: String,
    val alternativasHtml: List<String>,
    val gabarito: String? = null,
    val numeroAlternativaCorreta: Int? = null,
    val comentarioHtml: String? = null,
    val comentarioTexto: String? = null,
    // Resolution state
    val alternativaSelecionada: Int? = null,
    val acertou: Boolean? = null,
    val resolvidaOffline: Boolean = false,
    val pendingSync: Boolean = false
) {
    val isCertoErrado: Boolean
        get() = tipo == "CERTO_ERRADO" || alternativasHtml.size == 2 && alternativasHtml.any { it.contains("Certo", ignoreCase = true) }
}

data class Alternative(
    val index: Int, // 1-based (1 -> A, 2 -> B...)
    val letter: String, // "A", "B", "C", "D", "E" or "Certo", "Errado"
    val htmlText: String,
    val plainText: String,
    val isCorrect: Boolean = false,
    val isSelected: Boolean = false,
    val isStrikethrough: Boolean = false
)

data class Resolution(
    val id: Long = 0,
    val cadernoId: Long,
    val questionId: Long,
    val posicaoCaderno: Int,
    val selectedAlternative: Int, // 1-based index
    val isCorrect: Boolean,
    val solvedAt: Long = System.currentTimeMillis(),
    val pendingSync: Boolean = true,
    val syncedAt: Long? = null
)

data class TecSession(
    val logged: Boolean = false,
    val cookies: Map<String, String> = emptyMap(),
    val email: String? = null,
    val via: String? = null,
    val loginTimestamp: Long = 0
) {
    val cookieHeader: String
        get() = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }

    val hasLoginCookie: Boolean
        get() = cookies.containsKey("TecPermanecerLogado") || cookies.containsKey("JSESSIONID")
}

// Responses from TEC API
data class TecPastasResponse(
    val pastas: List<TecPastaDto>? = null,
    val erro: TecErrorDto? = null
)

data class TecPastaDto(
    val id: Long,
    val nome: String,
    val qtdCadernos: Int = 0,
    val ativa: Boolean = false
)

data class TecItensResponse(
    val itens: List<TecItemDto>? = null,
    val erro: TecErrorDto? = null
)

data class TecItemDto(
    val id: Long,
    val nome: String,
    val quantidadeItens: Int = 0,
    val data: String? = null,
    val subpasta: Boolean = false
)

data class TecGabaritoResponse(
    val list: List<TecGabaritoItemDto>? = null,
    val resultCount: Int = 0,
    val totalPaginas: Int = 1,
    val erro: TecErrorDto? = null
)

data class TecGabaritoItemDto(
    val posicaoCaderno: Int,
    val idQuestao: Long,
    val alternativa: Int? = null,
    val acertou: Boolean? = null,
    val anulada: Boolean = false,
    val data: String? = null
)

data class TecQuestaoRawResponse(
    val questao: TecQuestaoDto? = null,
    val erro: TecErrorDto? = null
)

data class TecQuestaoDto(
    val idQuestao: Long,
    val tipoQuestao: String? = null,
    val formatoQuestao: String? = null,
    val idMateria: Long? = null,
    val nomeMateria: String? = null,
    val idAssunto: Long? = null,
    val nomeAssunto: String? = null,
    val bancaSigla: String? = null,
    val orgaoSigla: String? = null,
    val orgaoNome: String? = null,
    val cargoSigla: String? = null,
    val concursoAno: Int? = null,
    val concursoArea: String? = null,
    val anulada: Boolean = false,
    val desatualizada: Boolean = false,
    val enunciado: String? = null,
    val alternativas: List<String>? = null,
    val numeroAlternativaCorreta: Int? = null
)

data class TecComentarioResponse(
    val comentario: TecComentarioDto? = null,
    val erro: TecErrorDto? = null
)

data class TecComentarioDto(
    val id: Long? = null,
    val textoComentario: String? = null,
    val dataPublicacao: String? = null
)

data class TecDetalhesResponse(
    val informacoes: TecDetalhesDto? = null,
    val erro: TecErrorDto? = null
)

data class TecDetalhesDto(
    val bancaNome: String? = null,
    val orgaoNome: String? = null,
    val orgaoSigla: String? = null,
    val cargoNome: String? = null,
    val concursoAno: Int? = null,
    val questaoDificuldade: String? = null,
    val questaoProva: String? = null,
    val questaoNumero: Int? = null
)

data class TecBatchResolverResponse(
    val correcoes: List<TecCorrecaoDto>? = null,
    val finalizado: Boolean = false,
    val erro: TecErrorDto? = null
)

data class TecCorrecaoDto(
    val idQuestao: Long,
    val acertou: Boolean,
    val alternativa: Int? = null,
    val data: String? = null
)

data class TecErrorDto(
    val mensagem: String? = null,
    val error: String? = null
)
