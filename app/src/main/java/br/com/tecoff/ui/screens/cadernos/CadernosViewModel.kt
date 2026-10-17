package br.com.tecoff.ui.screens.cadernos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.tecoff.data.local.AppDatabase
import br.com.tecoff.data.local.entity.CadernoEntity
import br.com.tecoff.data.local.entity.PastaEntity
import br.com.tecoff.data.local.entity.SubpastaEntity
import br.com.tecoff.data.model.Caderno
import br.com.tecoff.data.remote.SessionManager
import br.com.tecoff.domain.SyncManager
import br.com.tecoff.domain.SyncProgress
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CadernosUiState(
    val isLoading: Boolean = false,
    val isLogged: Boolean = false,
    val isSessionExpired: Boolean = false,
    val pastas: List<PastaEntity> = emptyList(),
    val subpastasByPasta: Map<Long, List<SubpastaEntity>> = emptyMap(),
    val cadernosByFolder: Map<String, List<Caderno>> = emptyMap(),
    val totalPendingSync: Int = 0,
    val syncProgress: SyncProgress = SyncProgress(),
    val errorMessage: String? = null
)

class CadernosViewModel(
    private val db: AppDatabase,
    private val syncManager: SyncManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CadernosUiState())
    val uiState: StateFlow<CadernosUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            sessionManager.sessionState.collect { session ->
                _uiState.update { it.copy(isLogged = session.logged) }
            }
        }

        viewModelScope.launch {
            sessionManager.isSessionExpired.collect { expired ->
                _uiState.update { it.copy(isSessionExpired = expired) }
            }
        }

        viewModelScope.launch {
            syncManager.syncProgress.collect { progress ->
                _uiState.update { it.copy(syncProgress = progress) }
            }
        }

        viewModelScope.launch {
            db.cadernoDao().countTotalPendingSyncFlow().collect { pending ->
                _uiState.update { it.copy(totalPendingSync = pending) }
            }
        }

        // Combine DB flows to build the full folders / subfolders / cadernos hierarchy
        viewModelScope.launch {
            combine(
                db.cadernoDao().getAllPastasFlow(),
                db.cadernoDao().getAllCadernosFlow()
            ) { pastas, cadernosEntities ->
                val enrichedCadernos = cadernosEntities.map { entity ->
                    val downloadedCount = db.cadernoDao().countDownloadedQuestions(entity.id)
                    val resolutionsCount = db.cadernoDao().countResolutions(entity.id)
                    val correctCount = db.cadernoDao().countCorrectResolutions(entity.id)
                    val pendingCount = db.cadernoDao().countPendingSync(entity.id)

                    Caderno(
                        id = entity.id,
                        pastaId = entity.pastaId,
                        subpastaId = entity.subpastaId,
                        nome = entity.nome,
                        quantidadeItens = entity.quantidadeItens,
                        data = entity.data,
                        downloadedOffline = entity.downloadedOffline || downloadedCount > 0,
                        lastSyncAt = entity.lastSyncAt,
                        totalQuestoesBaixadas = downloadedCount,
                        totalResolucoes = resolutionsCount,
                        totalAcertos = correctCount,
                        pendenciasSync = pendingCount
                    )
                }

                // Group by "pasta_${pastaId}" or "subpasta_${subpastaId}"
                val cadernosMap = mutableMapOf<String, List<Caderno>>()
                for (pasta in pastas) {
                    cadernosMap["pasta_${pasta.id}"] = enrichedCadernos.filter { it.pastaId == pasta.id && it.subpastaId == null }
                    val subs = db.cadernoDao().getSubpastas(pasta.id)
                    for (sub in subs) {
                        cadernosMap["subpasta_${sub.id}"] = enrichedCadernos.filter { it.subpastaId == sub.id }
                    }
                }

                val subpastasMap = pastas.associate { pasta ->
                    pasta.id to db.cadernoDao().getSubpastas(pasta.id)
                }

                _uiState.update {
                    it.copy(
                        pastas = pastas,
                        subpastasByPasta = subpastasMap,
                        cadernosByFolder = cadernosMap
                    )
                }
            }.collect()
        }
    }

    fun syncAllPastasAndCadernos() {
        viewModelScope.launch {
            try {
                syncManager.syncPastasAndCadernos()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun downloadCaderno(cadernoId: Long) {
        viewModelScope.launch {
            try {
                syncManager.downloadCaderno(cadernoId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun syncOfflineResolutions(cadernoId: Long? = null) {
        viewModelScope.launch {
            try {
                syncManager.syncOfflineResolutions(cadernoId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
