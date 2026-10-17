package br.com.tecoff.data.remote

import android.util.Log
import br.com.tecoff.data.local.entity.CadernoEntity
import br.com.tecoff.data.local.entity.PastaEntity
import br.com.tecoff.data.local.entity.ResolutionEntity
import br.com.tecoff.data.local.entity.SubpastaEntity
import br.com.tecoff.data.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.io.IOException

class TecApiService(
    private val client: TecHttpClient,
    private val sessionManager: SessionManager
) {
    private val gson = Gson()
    private val baseUrl = TecHttpClient.BASE_URL

    suspend fun whoami(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/pastas-cadernos")
                .header("Accept", "application/json")
                .get()
                .build()

            client.execute(request).use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext false
                    val data = gson.fromJson(body, TecPastasResponse::class.java)
                    val isValid = data.pastas != null
                    if (isValid) {
                        sessionManager.saveCookies(emptyMap(), via = "validated")
                    }
                    isValid
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("TecApiService", "Error checking whoami", e)
            false
        }
    }

    suspend fun getPastas(): List<PastaEntity> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/pastas-cadernos")
            .header("Accept", "application/json")
            .get()
            .build()

        client.execute(request).use { response ->
            if (!response.isSuccessful) throw IOException("Falha ao buscar pastas: HTTP ${response.code}")
            val body = response.body?.string() ?: ""
            val parsed = gson.fromJson(body, TecPastasResponse::class.java)
            parsed.pastas?.map {
                PastaEntity(
                    id = it.id,
                    nome = it.nome,
                    qtdCadernos = it.qtdCadernos,
                    ativa = it.ativa
                )
            } ?: emptyList()
        }
    }

    suspend fun getSubpastas(pastaId: Long): List<SubpastaEntity> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/pastas-cadernos/$pastaId/itens")
            .header("Accept", "application/json")
            .get()
            .build()

        client.execute(request).use { response ->
            if (!response.isSuccessful) throw IOException("Falha ao buscar subpastas: HTTP ${response.code}")
            val body = response.body?.string() ?: ""
            val parsed = gson.fromJson(body, TecItensResponse::class.java)
            parsed.itens?.filter { it.subpasta }?.map {
                SubpastaEntity(
                    id = it.id,
                    pastaId = pastaId,
                    nome = it.nome
                )
            } ?: emptyList()
        }
    }

    suspend fun getCadernos(pastaId: Long, subpastaId: Long? = null): List<CadernoEntity> = withContext(Dispatchers.IO) {
        val url = if (subpastaId != null) {
            "$baseUrl/api/pastas-cadernos/$pastaId/subpastas/$subpastaId/itens?ordenacao=nome&paginar=false"
        } else {
            "$baseUrl/api/pastas-cadernos/$pastaId/itens?ordenacao=nome&paginar=false"
        }

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()

        client.execute(request).use { response ->
            if (!response.isSuccessful) throw IOException("Falha ao buscar cadernos: HTTP ${response.code}")
            val body = response.body?.string() ?: ""
            val parsed = gson.fromJson(body, TecItensResponse::class.java)
            parsed.itens?.filter { !it.subpasta }?.map {
                CadernoEntity(
                    id = it.id,
                    pastaId = pastaId,
                    subpastaId = subpastaId,
                    nome = it.nome,
                    quantidadeItens = it.quantidadeItens,
                    data = it.data
                )
            } ?: emptyList()
        }
    }

    /**
     * BULK endpoint to fetch question IDs, order, and existing resolution state for a caderno.
     * Pages through results (~100 items per page).
     */
    suspend fun getCadernoGabarito(cadernoId: Long, pagina: Int = 1): TecGabaritoResponse = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/cadernos/$cadernoId/gabarito?pagina=$pagina&exibicao=TODAS"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()

        client.execute(request).use { response ->
            if (!response.isSuccessful) throw IOException("Falha ao buscar gabarito do caderno $cadernoId: HTTP ${response.code}")
            val body = response.body?.string() ?: ""
            gson.fromJson(body, TecGabaritoResponse::class.java)
        }
    }

    suspend fun getQuestao(id: Long): TecQuestaoDto? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/questoes/$id")
            .header("Accept", "application/json")
            .get()
            .build()

        client.execute(request).use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val parsed = gson.fromJson(body, TecQuestaoRawResponse::class.java)
            parsed.questao
        }
    }

    suspend fun getComentario(id: Long): TecComentarioDto? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/questoes/$id/comentario")
            .header("Accept", "application/json")
            .get()
            .build()

        client.execute(request).use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val parsed = gson.fromJson(body, TecComentarioResponse::class.java)
            parsed.comentario
        }
    }

    suspend fun getDetalhes(id: Long): TecDetalhesDto? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/questoes/$id/detalhes")
            .header("Accept", "application/json")
            .get()
            .build()

        client.execute(request).use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val parsed = gson.fromJson(body, TecDetalhesResponse::class.java)
            parsed.informacoes
        }
    }

    /**
     * BULK resolution sync: sends multiple question resolutions to TEC in a single request.
     * Endpoint: POST /api/cadernos/{idCaderno}/gabarito/resolver
     * Payload: questoes[0].posicaoCaderno=..., questoes[0].idQuestao=..., questoes[0].alternativa=...
     */
    suspend fun resolverQuestoesEmLote(
        cadernoId: Long,
        resolucoes: List<ResolutionEntity>
    ): TecBatchResolverResponse = withContext(Dispatchers.IO) {
        val formBuilder = FormBody.Builder()
        resolucoes.forEachIndexed { i, res ->
            formBuilder.add("questoes[$i].posicaoCaderno", res.posicaoCaderno.toString())
            formBuilder.add("questoes[$i].idQuestao", res.questionId.toString())
            formBuilder.add("questoes[$i].alternativa", res.selectedAlternative.toString())
        }

        val request = Request.Builder()
            .url("$baseUrl/api/cadernos/$cadernoId/gabarito/resolver")
            .header("Accept", "application/json")
            .post(formBuilder.build())
            .build()

        client.execute(request).use { response ->
            if (!response.isSuccessful) {
                throw IOException("Falha na sincronização em lote de resoluções: HTTP ${response.code}")
            }
            val body = response.body?.string() ?: ""
            gson.fromJson(body, TecBatchResolverResponse::class.java)
        }
    }

    /**
     * Fallback resolution sync for a single question.
     * Endpoint: PUT /api/questoes/{id}/resolucao
     */
    suspend fun resolverQuestaoIndividual(idQuestao: Long, alternativa: Int): Boolean = withContext(Dispatchers.IO) {
        val form = FormBody.Builder()
            .add("alternativa", alternativa.toString())
            .add("questaoAdaptadaOuInedita", "false")
            .build()

        val request = Request.Builder()
            .url("$baseUrl/api/questoes/$idQuestao/resolucao")
            .header("Accept", "application/json")
            .put(form)
            .build()

        client.execute(request).use { response ->
            response.isSuccessful
        }
    }
}
