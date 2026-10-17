package br.com.tecoff.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pastas")
data class PastaEntity(
    @PrimaryKey val id: Long,
    val nome: String,
    val qtdCadernos: Int,
    val ativa: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "subpastas",
    indices = [Index(value = ["pastaId"])]
)
data class SubpastaEntity(
    @PrimaryKey val id: Long,
    val pastaId: Long,
    val nome: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cadernos",
    indices = [Index(value = ["pastaId"]), Index(value = ["subpastaId"])]
)
data class CadernoEntity(
    @PrimaryKey val id: Long,
    val pastaId: Long,
    val subpastaId: Long? = null,
    val nome: String,
    val quantidadeItens: Int,
    val data: String? = null,
    val downloadedOffline: Boolean = false,
    val lastSyncAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: Long,
    val tipo: String, // "MULTIPLA_ESCOLHA", "CERTO_ERRADO"
    val formato: String,
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
    val alternativasJson: String, // JSON Array of String
    val gabarito: String? = null,
    val numeroAlternativaCorreta: Int? = null,
    val comentarioHtml: String? = null,
    val comentarioTexto: String? = null,
    val downloadedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "caderno_questions",
    primaryKeys = ["cadernoId", "questionId"],
    indices = [Index(value = ["cadernoId"]), Index(value = ["questionId"]), Index(value = ["posicaoCaderno"])]
)
data class CadernoQuestionCrossRef(
    val cadernoId: Long,
    val questionId: Long,
    val posicaoCaderno: Int
)

@Entity(
    tableName = "resolutions",
    indices = [
        Index(value = ["cadernoId", "questionId"], unique = true),
        Index(value = ["pendingSync"])
    ]
)
data class ResolutionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cadernoId: Long,
    val questionId: Long,
    val posicaoCaderno: Int,
    val selectedAlternative: Int, // 1-based index (1 -> A, 2 -> B...)
    val isCorrect: Boolean,
    val solvedAt: Long = System.currentTimeMillis(),
    val pendingSync: Boolean = true,
    val syncedAt: Long? = null
)
