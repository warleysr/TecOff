package br.com.tecoff.domain

import java.util.ArrayDeque

class QuestionNavigator(
    private var totalQuestions: Int = 0,
    initialIndex: Int = 0
) {
    var currentIndex: Int = initialIndex
        private set

    private val historyStack = ArrayDeque<Int>()

    fun setTotal(total: Int) {
        this.totalQuestions = total
        if (currentIndex >= total && total > 0) {
            currentIndex = total - 1
        }
    }

    fun canGoForward(): Boolean = currentIndex < totalQuestions - 1
    fun canGoBackward(): Boolean = currentIndex > 0
    fun canUndo(): Boolean = historyStack.isNotEmpty()

    fun navigateTo(newIndex: Int): Boolean {
        if (newIndex in 0 until totalQuestions && newIndex != currentIndex) {
            historyStack.push(currentIndex)
            currentIndex = newIndex
            return true
        }
        return false
    }

    /** 1. Pra frente (Avançar) */
    fun forward(): Boolean {
        if (canGoForward()) {
            return navigateTo(currentIndex + 1)
        }
        return false
    }

    /** 2. Pra trás (Voltar) */
    fun backward(): Boolean {
        if (canGoBackward()) {
            return navigateTo(currentIndex - 1)
        }
        return false
    }

    /** 3. Próxima não resolvida */
    fun nextUnsolved(unsolvedIndices: List<Int>): Boolean {
        if (unsolvedIndices.isEmpty()) return false

        // Look for the first unsolved question with index > currentIndex
        val nextAfterCurrent = unsolvedIndices.firstOrNull { it > currentIndex }
        val target = nextAfterCurrent ?: unsolvedIndices.firstOrNull { it < currentIndex }

        return if (target != null) {
            navigateTo(target)
        } else false
    }

    /** 4. Aleatória (não resolvida) */
    fun randomUnsolved(unsolvedIndices: List<Int>): Boolean {
        if (unsolvedIndices.isEmpty()) return false

        val candidates = if (unsolvedIndices.size > 1) {
            unsolvedIndices.filter { it != currentIndex }
        } else {
            unsolvedIndices
        }

        val target = candidates.randomOrNull() ?: return false
        return navigateTo(target)
    }

    /** 5. Desfazer a última navegação (Undo) */
    fun undo(): Boolean {
        if (historyStack.isNotEmpty()) {
            val prevIndex = historyStack.pop()
            if (prevIndex in 0 until totalQuestions) {
                currentIndex = prevIndex
                return true
            }
        }
        return false
    }
}
