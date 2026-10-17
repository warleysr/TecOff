package br.com.tecoff.domain

import android.util.Log
import br.com.tecoff.data.local.AppDatabase
import br.com.tecoff.data.local.ImageStorageManager
import br.com.tecoff.data.local.entity.CadernoEntity
import br.com.tecoff.data.local.entity.CadernoQuestionCrossRef
import br.com.tecoff.data.local.entity.QuestionEntity
import br.com.tecoff.data.local.entity.ResolutionEntity
import br.com.tecoff.data.model.TecGabaritoItemDto
import br.com.tecoff.data.remote.TecApiService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class SyncProgress(
    val isSyncing: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val statusText: String = "",
    val error: String? = null
)

class SyncManager(
    private val db: AppDatabase,
    private val apiService: TecApiService,
    private val imageStorage: ImageStorageManager
) {
    private val gson = Gson()
    private val _syncProgress = MutableStateFlow(SyncProgress())
    val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()

    suspend fun syncPastasAndCadernos() = withContext(Dispatchers.IO) {
        _syncProgress.value = SyncProgress(isSyncing = true, statusText = "Atualizando pastas...")
        try {
            val pastas = apiService.getPastas()
            db.cadernoDao().insertPastas(pastas)

            for (pasta in pastas) {
                _syncProgress.value = SyncProgress(isSyncing = true, statusText = "Buscando cadernos de ${pasta.nome}...")
                val subpastas = apiService.getSubpastas(pasta.id)
                db.cadernoDao().insertSubpastas(subpastas)

                // Cadernos diretos da pasta
                val cadernosDiretos = apiService.getCadernos(pasta.id)
                db.cadernoDao().insertCadernos(cadernosDiretos)

                // Cadernos das subpastas
                for (sub in subpastas) {
                    val cadernosSub = apiService.getCadernos(pasta.id, sub.id)
                    db.cadernoDao().insertCadernos(cadernosSub)
                }
            }
            _syncProgress.value = SyncProgress(isSyncing = false, statusText = "Pastas sincronizadas!")
        } catch (e: Exception) {
            Log.e("SyncManager", "Erro ao sincronizar pastas", e)
            _syncProgress.value = SyncProgress(isSyncing = false, error = e.message)
            throw e
        }
    }

    /**
     * Downloads an entire Caderno for 100% offline usage.
     * Uses BULK strategy:
     * 1. Fetches all question IDs and positions via getCadernoGabarito (100 items per page).
     * 2. Checks local DB to avoid re-downloading questions already cached.
     * 3. For uncached questions: downloads statement, alternatives, professor commentary, details.
     * 4. Downloads and caches images offline.
     * 5. Saves everything to Room DB.
     */
    suspend fun downloadCaderno(cadernoId: Long) = withContext(Dispatchers.IO) {
        _syncProgress.value = SyncProgress(isSyncing = true, statusText = "Obtendo gabarito e IDs do caderno...")
        try {
            val caderno = db.cadernoDao().getCadernoById(cadernoId)
                ?: throw IllegalArgumentException("Caderno $cadernoId não encontrado no banco local.")

            // 1. BULK: Page through gabarito to get all questions and their positions
            val allGabaritoItems = mutableListOf<TecGabaritoItemDto>()
            var page = 1
            var totalExpected = caderno.quantidadeItens

            while (true) {
                _syncProgress.value = SyncProgress(
                    isSyncing = true,
                    statusText = "Carregando gabarito (Página $page)...",
                    current = allGabaritoItems.size,
                    total = totalExpected
                )
                val gabaritoRes = apiService.getCadernoGabarito(cadernoId, page)
                val items = gabaritoRes.list ?: emptyList()
                if (items.isEmpty()) break
                allGabaritoItems.addAll(items)
                totalExpected = maxOf(totalExpected, gabaritoRes.resultCount)
                if (allGabaritoItems.size >= gabaritoRes.resultCount || items.size < 100) break
                page++
            }

            val totalQuestions = allGabaritoItems.size
            Log.d("SyncManager", "Caderno $cadernoId tem $totalQuestions questões no gabarito.")

            // 2. Download details and cache images for each question
            val crossRefs = mutableListOf<CadernoQuestionCrossRef>()
            val initialResolutions = mutableListOf<ResolutionEntity>()

            for ((index, item) in allGabaritoItems.withIndex()) {
                val currentNum = index + 1
                _syncProgress.value = SyncProgress(
                    isSyncing = true,
                    current = currentNum,
                    total = totalQuestions,
                    statusText = "Baixando questão $currentNum de $totalQuestions (#${item.idQuestao})..."
                )

                crossRefs.add(
                    CadernoQuestionCrossRef(
                        cadernoId = cadernoId,
                        questionId = item.idQuestao,
                        posicaoCaderno = item.posicaoCaderno
                    )
                )

                // If already solved on TEC, store initial resolution
                if (item.alternativa != null) {
                    initialResolutions.add(
                        ResolutionEntity(
                            cadernoId = cadernoId,
                            questionId = item.idQuestao,
                            posicaoCaderno = item.posicaoCaderno,
                            selectedAlternative = item.alternativa,
                            isCorrect = item.acertou ?: false,
                            pendingSync = false,
                            syncedAt = System.currentTimeMillis()
                        )
                    )
                }

                // Check if question is already saved locally
                val existing = db.questionDao().getQuestionById(item.idQuestao)
                if (existing == null) {
                    // Fetch question content
                    val qDto = apiService.getQuestao(item.idQuestao)
                    val comDto = apiService.getComentario(item.idQuestao)
                    val detDto = apiService.getDetalhes(item.idQuestao)

                    if (qDto != null) {
                        // Cache HTML images offline
                        val offlineEnunciado = imageStorage.processAndCacheHtmlImages(qDto.enunciado ?: "")
                        val offlineAlternativas = (qDto.alternativas ?: emptyList()).map {
                            imageStorage.processAndCacheHtmlImages(it)
                        }
                        val offlineComentario = imageStorage.processAndCacheHtmlImages(comDto?.textoComentario ?: "")

                        val questionEntity = QuestionEntity(
                            id = qDto.idQuestao,
                            tipo = qDto.tipoQuestao ?: "MULTIPLA_ESCOLHA",
                            formato = qDto.formatoQuestao ?: "OBJETIVA",
                            materiaId = qDto.idMateria,
                            materiaNome = qDto.nomeMateria,
                            assuntoId = qDto.idAssunto,
                            assuntoNome = qDto.nomeAssunto,
                            banca = qDto.bancaSigla ?: detDto?.bancaNome,
                            orgao = qDto.orgaoSigla ?: detDto?.orgaoSigla,
                            cargo = qDto.cargoSigla ?: detDto?.cargoNome,
                            ano = qDto.concursoAno ?: detDto?.concursoAno,
                            dificuldade = detDto?.questaoDificuldade,
                            anulada = qDto.anulada,
                            desatualizada = qDto.desatualizada,
                            enunciadoHtml = offlineEnunciado,
                            alternativasJson = gson.toJson(offlineAlternativas),
                            numeroAlternativaCorreta = qDto.numeroAlternativaCorreta,
                            comentarioHtml = offlineComentario,
                            comentarioTexto = null
                        )
                        db.questionDao().insertQuestion(questionEntity)
                    }
                }
            }

            // Save cross-references and initial resolutions
            db.questionDao().insertCrossRefs(crossRefs)
            if (initialResolutions.isNotEmpty()) {
                db.resolutionDao().insertResolutions(initialResolutions)
            }

            // Mark caderno as downloaded
            db.cadernoDao().updateCaderno(
                caderno.copy(
                    downloadedOffline = true,
                    quantidadeItens = totalQuestions,
                    lastSyncAt = System.currentTimeMillis()
                )
            )

            _syncProgress.value = SyncProgress(
                isSyncing = false,
                statusText = "Download concluído com sucesso! $totalQuestions questões prontas para uso offline."
            )
        } catch (e: Exception) {
            Log.e("SyncManager", "Erro ao baixar caderno $cadernoId", e)
            _syncProgress.value = SyncProgress(isSyncing = false, error = e.message)
            throw e
        }
    }

    /**
     * Sincroniza as resoluções feitas offline com o TEC Concursos.
     * Envia em BULK via POST /api/cadernos/{idCaderno}/gabarito/resolver
     * e atualiza o status de sincronização no banco local.
     */
    suspend fun syncOfflineResolutions(cadernoId: Long? = null): Int = withContext(Dispatchers.IO) {
        _syncProgress.value = SyncProgress(isSyncing = true, statusText = "Enviando resoluções ao TEC...")
        try {
            val pending = if (cadernoId != null) {
                db.resolutionDao().getPendingResolutionsByCaderno(cadernoId)
            } else {
                db.resolutionDao().getAllPendingResolutions()
            }

            if (pending.isEmpty()) {
                _syncProgress.value = SyncProgress(isSyncing = false, statusText = "Nenhuma resolução pendente de sincronização.")
                return@withContext 0
            }

            val grouped = pending.groupBy { it.cadernoId }
            var totalSynced = 0

            for ((cid, resolutions) in grouped) {
                _syncProgress.value = SyncProgress(
                    isSyncing = true,
                    current = totalSynced,
                    total = pending.size,
                    statusText = "Sincronizando ${resolutions.size} resoluções do caderno $cid..."
                )

                // Split in batches of 50 to avoid request payload limits
                val batches = resolutions.chunked(50)
                for (batch in batches) {
                    try {
                        apiService.resolverQuestoesEmLote(cid, batch)
                        val syncedIds = batch.map { it.id }
                        db.resolutionDao().markAsSynced(syncedIds)
                        totalSynced += batch.size
                    } catch (e: Exception) {
                        Log.w("SyncManager", "Falha no envio em lote, tentando individualmente...", e)
                        for (res in batch) {
                            val ok = apiService.resolverQuestaoIndividual(res.questionId, res.selectedAlternative)
                            if (ok) {
                                db.resolutionDao().markAsSynced(listOf(res.id))
                                totalSynced++
                            }
                        }
                    }
                }
            }

            _syncProgress.value = SyncProgress(
                isSyncing = false,
                statusText = "Sucesso! $totalSynced resoluções sincronizadas com o TEC."
            )
            totalSynced
        } catch (e: Exception) {
            Log.e("SyncManager", "Erro ao sincronizar resoluções", e)
            _syncProgress.value = SyncProgress(isSyncing = false, error = e.message)
            throw e
        }
    }
}
