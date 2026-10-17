package br.com.tecoff.ui.screens.question

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.tecoff.data.local.AppDatabase
import br.com.tecoff.data.local.entity.QuestionEntity
import br.com.tecoff.data.local.entity.ResolutionEntity
import br.com.tecoff.data.model.Question
import br.com.tecoff.domain.GabaritoExtractor
import br.com.tecoff.domain.QuestionNavigator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class QuestionUiState(
    val isLoading: Boolean = true,
    val cadernoId: Long = 0,
    val cadernoNome: String = "",
    val totalQuestions: Int = 0,
    val currentIndex: Int = 0,
    val currentQuestion: Question? = null,
    val selectedAlternative: Int? = null,
    val isAnswered: Boolean = false,
    val isCorrect: Boolean? = null,
    val strikethroughList: Set<Int> = emptySet(), // Set of 1-based indices that are struck through
    val showProfessorComment: Boolean = false,
    val canUndo: Boolean = false,
    val canGoForward: Boolean = false,
    val canGoBackward: Boolean = false,
    val unsolvedCount: Int = 0
)

class QuestionViewModel(
    private val cadernoId: Long,
    private val db: AppDatabase
) : ViewModel() {

    private val gson = Gson()
    private val navigator = QuestionNavigator()

    private val _uiState = MutableStateFlow(QuestionUiState(cadernoId = cadernoId))
    val uiState: StateFlow<QuestionUiState> = _uiState.asStateFlow()

    private var allQuestions: List<QuestionEntity> = emptyList()
    private var allResolutions: Map<Long, ResolutionEntity> = emptyMap()

    init {
        loadCadernoAndQuestions()
    }

    private fun loadCadernoAndQuestions() {
        viewModelScope.launch {
            val caderno = db.cadernoDao().getCadernoById(cadernoId)
            val cadernoTitle = caderno?.nome ?: "Caderno"

            // Observe questions and resolutions
            combine(
                db.questionDao().getQuestionsByCadernoFlow(cadernoId),
                db.resolutionDao().getResolutionsByCadernoFlow(cadernoId)
            ) { questions, resolutions ->
                allQuestions = questions
                allResolutions = resolutions.associateBy { it.questionId }

                navigator.setTotal(questions.size)

                updateCurrentQuestionState(cadernoTitle)
            }.collect()
        }
    }

    private fun updateCurrentQuestionState(cadernoTitle: String) {
        val total = allQuestions.size
        if (total == 0) {
            _uiState.update { it.copy(isLoading = false, totalQuestions = 0) }
            return
        }

        val idx = navigator.currentIndex
        val entity = allQuestions.getOrNull(idx)

        if (entity != null) {
            val listType = object : TypeToken<List<String>>() {}.type
            val alternativas: List<String> = try {
                gson.fromJson(entity.alternativasJson, listType) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            val res = allResolutions[entity.id]
            val isAnswered = res != null
            val selected = res?.selectedAlternative
            val isCorrect = res?.isCorrect

            // Fallback gabarito extraction if not populated previously
            val (resolvedGabarito, resolvedNumero) = if (entity.numeroAlternativaCorreta != null) {
                Pair(entity.gabarito, entity.numeroAlternativaCorreta)
            } else {
                GabaritoExtractor.extract(
                    numeroCorretoRaw = null,
                    tipoQuestao = entity.tipo,
                    alternativas = alternativas,
                    comentarioHtml = entity.comentarioHtml
                )
            }

            // If we resolved the gabarito on the fly, persist it back to the database
            if (entity.numeroAlternativaCorreta == null && resolvedNumero != null) {
                viewModelScope.launch {
                    db.questionDao().insertQuestion(
                        entity.copy(
                            gabarito = resolvedGabarito,
                            numeroAlternativaCorreta = resolvedNumero
                        )
                    )
                }
            }

            val questionDomain = Question(
                id = entity.id,
                posicaoCaderno = idx + 1,
                tipo = entity.tipo,
                formato = entity.formato,
                materiaId = entity.materiaId,
                materiaNome = entity.materiaNome,
                assuntoId = entity.assuntoId,
                assuntoNome = entity.assuntoNome,
                banca = entity.banca,
                orgao = entity.orgao,
                cargo = entity.cargo,
                ano = entity.ano,
                dificuldade = entity.dificuldade,
                anulada = entity.anulada,
                desatualizada = entity.desatualizada,
                enunciadoHtml = entity.enunciadoHtml,
                alternativasHtml = alternativas,
                gabarito = resolvedGabarito ?: entity.gabarito,
                numeroAlternativaCorreta = resolvedNumero ?: entity.numeroAlternativaCorreta,
                comentarioHtml = entity.comentarioHtml,
                comentarioTexto = entity.comentarioTexto,
                alternativaSelecionada = selected,
                acertou = isCorrect,
                resolvidaOffline = res?.pendingSync == true,
                pendingSync = res?.pendingSync == true
            )

            val unsolvedIndices = getUnsolvedIndices()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    cadernoNome = cadernoTitle,
                    totalQuestions = total,
                    currentIndex = idx,
                    currentQuestion = questionDomain,
                    selectedAlternative = selected,
                    isAnswered = isAnswered,
                    isCorrect = isCorrect,
                    strikethroughList = emptySet(),
                    showProfessorComment = isAnswered,
                    canUndo = navigator.canUndo(),
                    canGoForward = navigator.canGoForward(),
                    canGoBackward = navigator.canGoBackward(),
                    unsolvedCount = unsolvedIndices.size
                )
            }
        }
    }

    private fun getUnsolvedIndices(): List<Int> {
        return allQuestions.indices.filter { idx ->
            val q = allQuestions[idx]
            !allResolutions.containsKey(q.id)
        }
    }

    fun selectAlternative(index: Int) {
        if (_uiState.value.isAnswered) return
        _uiState.update { it.copy(selectedAlternative = index) }
    }

    fun toggleStrikethrough(index: Int) {
        if (_uiState.value.isAnswered) return
        val currentSet = _uiState.value.strikethroughList.toMutableSet()
        if (currentSet.contains(index)) {
            currentSet.remove(index)
        } else {
            currentSet.add(index)
        }
        _uiState.update { it.copy(strikethroughList = currentSet) }
    }

    /**
     * Submits answer offline, calculates accuracy, and saves to Room with pendingSync = true.
     */
    fun submitAnswer() {
        val q = _uiState.value.currentQuestion ?: return
        val selected = _uiState.value.selectedAlternative ?: return

        val correctIndex = q.numeroAlternativaCorreta ?: run {
            val gab = q.gabarito?.trim()?.uppercase()
            when {
                gab == null -> null
                q.isCertoErrado -> if (gab.startsWith("C") || gab == "1") 1 else 2
                else -> {
                    val letters = "ABCDEFGHIJ"
                    if (letters.contains(gab)) letters.indexOf(gab) + 1 else null
                }
            }
        }

        val isCorrect = correctIndex != null && selected == correctIndex

        val resolution = ResolutionEntity(
            cadernoId = cadernoId,
            questionId = q.id,
            posicaoCaderno = q.posicaoCaderno,
            selectedAlternative = selected,
            isCorrect = isCorrect,
            solvedAt = System.currentTimeMillis(),
            pendingSync = true
        )

        viewModelScope.launch {
            db.resolutionDao().insertResolution(resolution)
            _uiState.update {
                it.copy(
                    isAnswered = true,
                    isCorrect = isCorrect,
                    showProfessorComment = true
                )
            }
        }
    }

    fun toggleProfessorComment() {
        _uiState.update { it.copy(showProfessorComment = !it.showProfessorComment) }
    }

    // 1. Pra frente
    fun goForward() {
        if (navigator.forward()) {
            updateCurrentQuestionState(_uiState.value.cadernoNome)
        }
    }

    // 2. Pra trás
    fun goBackward() {
        if (navigator.backward()) {
            updateCurrentQuestionState(_uiState.value.cadernoNome)
        }
    }

    // 3. Próxima não resolvida
    fun goNextUnsolved() {
        val unsolved = getUnsolvedIndices()
        if (navigator.nextUnsolved(unsolved)) {
            updateCurrentQuestionState(_uiState.value.cadernoNome)
        }
    }

    // 4. Aleatória não resolvida
    fun goRandomUnsolved() {
        val unsolved = getUnsolvedIndices()
        if (navigator.randomUnsolved(unsolved)) {
            updateCurrentQuestionState(_uiState.value.cadernoNome)
        }
    }

    // 5. Desfazer navegação
    fun undoNavigation() {
        if (navigator.undo()) {
            updateCurrentQuestionState(_uiState.value.cadernoNome)
        }
    }
}
