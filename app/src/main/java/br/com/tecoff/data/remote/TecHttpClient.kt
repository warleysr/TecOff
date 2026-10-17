package br.com.tecoff.data.remote

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

class TecHttpClient(
    private val sessionManager: SessionManager,
    private val minIntervalMs: Long = 350L,
    private val maxRetries: Int = 3
) {
    companion object {
        const val BASE_URL = "https://www.tecconcursos.com.br"
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
    }

    private val mutex = Mutex()
    private var lastRequestTime = 0L

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(false) // Manual redirect handling to detect /login redirect
        .followSslRedirects(false)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("User-Agent", USER_AGENT)
                .header("Referer", "$BASE_URL/questoes/")
                .header("Origin", BASE_URL)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")

            val cookieHeader = sessionManager.getSession().cookieHeader
            if (cookieHeader.isNotBlank()) {
                requestBuilder.header("Cookie", cookieHeader)
            }

            val response = chain.proceed(requestBuilder.build())
            sessionManager.absorbResponseCookies(response)

            // Check if redirected to login or unauthorized
            val statusCode = response.code
            val location = response.header("Location") ?: ""
            if (statusCode == 401 || (statusCode in 300..399 && location.contains("/login"))) {
                Log.w("TecHttpClient", "Detected expired session (code $statusCode, location: $location)")
                sessionManager.markSessionExpired()
            }

            response
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    /**
     * Executes an HTTP request with rate limiting and exponential backoff on 429 errors.
     */
    suspend fun execute(request: Request): Response {
        var attempt = 0
        while (true) {
            // Apply rate limit interval
            mutex.withLock {
                val now = System.currentTimeMillis()
                val elapsed = now - lastRequestTime
                if (elapsed < minIntervalMs) {
                    delay(minIntervalMs - elapsed)
                }
                lastRequestTime = System.currentTimeMillis()
            }

            try {
                val response = okHttpClient.newCall(request).execute()

                // If 429 Too Many Requests or 503, retry with backoff
                if (response.code == 429 || response.code in 500..599) {
                    response.close()
                    attempt++
                    if (attempt <= maxRetries) {
                        val backoffMs = (1000L * (1 shl (attempt - 1))) + (100..500).random()
                        Log.w("TecHttpClient", "Rate limit / Server error (${response.code}). Backing off for ${backoffMs}ms (attempt $attempt/$maxRetries)")
                        delay(backoffMs)
                        continue
                    } else {
                        throw IOException("HTTP ${response.code}: Limite de requisições ou erro de servidor após $maxRetries tentativas.")
                    }
                }

                return response
            } catch (e: IOException) {
                attempt++
                if (attempt <= maxRetries) {
                    val backoffMs = 800L * attempt
                    Log.w("TecHttpClient", "Network error: ${e.message}. Retrying in ${backoffMs}ms...")
                    delay(backoffMs)
                } else {
                    throw e
                }
            }
        }
    }
}
