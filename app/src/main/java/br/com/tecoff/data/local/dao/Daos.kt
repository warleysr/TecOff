package br.com.tecoff.data.local.dao

import androidx.room.*
import br.com.tecoff.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CadernoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPastas(pastas: List<PastaEntity>)

    @Query("SELECT * FROM pastas ORDER BY nome ASC")
    fun getAllPastasFlow(): Flow<List<PastaEntity>>

    @Query("SELECT * FROM pastas ORDER BY nome ASC")
    suspend fun getAllPastas(): List<PastaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubpastas(subpastas: List<SubpastaEntity>)

    @Query("SELECT * FROM subpastas WHERE pastaId = :pastaId ORDER BY nome ASC")
    fun getSubpastasFlow(pastaId: Long): Flow<List<SubpastaEntity>>

    @Query("SELECT * FROM subpastas WHERE pastaId = :pastaId ORDER BY nome ASC")
    suspend fun getSubpastas(pastaId: Long): List<SubpastaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCadernos(cadernos: List<CadernoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaderno(caderno: CadernoEntity)

    @Update
    suspend fun updateCaderno(caderno: CadernoEntity)

    @Query("SELECT * FROM cadernos WHERE id = :id")
    suspend fun getCadernoById(id: Long): CadernoEntity?

    @Query("SELECT * FROM cadernos WHERE id = :id")
    fun getCadernoByIdFlow(id: Long): Flow<CadernoEntity?>

    @Query("SELECT * FROM cadernos WHERE pastaId = :pastaId AND subpastaId IS NULL ORDER BY nome ASC")
    fun getCadernosByPastaFlow(pastaId: Long): Flow<List<CadernoEntity>>

    @Query("SELECT * FROM cadernos WHERE subpastaId = :subpastaId ORDER BY nome ASC")
    fun getCadernosBySubpastaFlow(subpastaId: Long): Flow<List<CadernoEntity>>

    @Query("SELECT * FROM cadernos ORDER BY nome ASC")
    fun getAllCadernosFlow(): Flow<List<CadernoEntity>>

    @Query("SELECT COUNT(*) FROM caderno_questions WHERE cadernoId = :cadernoId")
    suspend fun countDownloadedQuestions(cadernoId: Long): Int

    @Query("SELECT COUNT(*) FROM resolutions WHERE cadernoId = :cadernoId")
    suspend fun countResolutions(cadernoId: Long): Int

    @Query("SELECT COUNT(*) FROM resolutions WHERE cadernoId = :cadernoId AND isCorrect = 1")
    suspend fun countCorrectResolutions(cadernoId: Long): Int

    @Query("SELECT COUNT(*) FROM resolutions WHERE cadernoId = :cadernoId AND pendingSync = 1")
    suspend fun countPendingSync(cadernoId: Long): Int

    @Query("SELECT COUNT(*) FROM resolutions WHERE pendingSync = 1")
    suspend fun countTotalPendingSync(): Int

    @Query("SELECT COUNT(*) FROM resolutions WHERE pendingSync = 1")
    fun countTotalPendingSyncFlow(): Flow<Int>
}

@Dao
interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<CadernoQuestionCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: CadernoQuestionCrossRef)

    @Query("""
        SELECT q.* FROM questions q
        INNER JOIN caderno_questions cq ON q.id = cq.questionId
        WHERE cq.cadernoId = :cadernoId
        ORDER BY cq.posicaoCaderno ASC
    """)
    fun getQuestionsByCadernoFlow(cadernoId: Long): Flow<List<QuestionEntity>>

    @Query("""
        SELECT q.* FROM questions q
        INNER JOIN caderno_questions cq ON q.id = cq.questionId
        WHERE cq.cadernoId = :cadernoId
        ORDER BY cq.posicaoCaderno ASC
    """)
    suspend fun getQuestionsByCaderno(cadernoId: Long): List<QuestionEntity>

    @Query("""
        SELECT cq.posicaoCaderno FROM caderno_questions cq
        WHERE cq.cadernoId = :cadernoId AND cq.questionId = :questionId
    """)
    suspend fun getPosicaoCaderno(cadernoId: Long, questionId: Long): Int?

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getQuestionById(id: Long): QuestionEntity?

    @Query("SELECT * FROM questions WHERE id = :id")
    fun getQuestionByIdFlow(id: Long): Flow<QuestionEntity?>

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getTotalQuestionsCount(): Int
}

@Dao
interface ResolutionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResolution(resolution: ResolutionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResolutions(resolutions: List<ResolutionEntity>)

    @Query("SELECT * FROM resolutions WHERE cadernoId = :cadernoId AND questionId = :questionId")
    suspend fun getResolution(cadernoId: Long, questionId: Long): ResolutionEntity?

    @Query("SELECT * FROM resolutions WHERE cadernoId = :cadernoId AND questionId = :questionId")
    fun getResolutionFlow(cadernoId: Long, questionId: Long): Flow<ResolutionEntity?>

    @Query("SELECT * FROM resolutions WHERE cadernoId = :cadernoId")
    suspend fun getResolutionsByCaderno(cadernoId: Long): List<ResolutionEntity>

    @Query("SELECT * FROM resolutions WHERE cadernoId = :cadernoId")
    fun getResolutionsByCadernoFlow(cadernoId: Long): Flow<List<ResolutionEntity>>

    @Query("SELECT * FROM resolutions WHERE pendingSync = 1")
    suspend fun getAllPendingResolutions(): List<ResolutionEntity>

    @Query("SELECT * FROM resolutions WHERE cadernoId = :cadernoId AND pendingSync = 1")
    suspend fun getPendingResolutionsByCaderno(cadernoId: Long): List<ResolutionEntity>

    @Query("UPDATE resolutions SET pendingSync = 0, syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>, syncedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM resolutions WHERE cadernoId = :cadernoId")
    suspend fun clearResolutionsByCaderno(cadernoId: Long)
}
