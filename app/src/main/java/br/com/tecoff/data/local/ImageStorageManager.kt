package br.com.tecoff.data.local

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.regex.Pattern

class ImageStorageManager(private val context: Context, private val okHttpClient: OkHttpClient) {

    private val imagesDir: File by lazy {
        File(context.filesDir, "tecoff_images").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Scans an HTML text for <img> tags, downloads the images locally,
     * and rewrites src="http..." to src="file:///path/to/local/file".
     */
    fun processAndCacheHtmlImages(html: String?): String {
        if (html.isNullOrBlank()) return ""

        val imgPattern = Pattern.compile("""<img[^>]+src=["']([^"']+)["'][^>]*>""", Pattern.CASE_INSENSITIVE)
        val matcher = imgPattern.matcher(html)
        val sb = StringBuffer()

        while (matcher.find()) {
            val originalImgTag = matcher.group(0) ?: ""
            val originalSrc = matcher.group(1) ?: ""

            val localUri = downloadAndGetLocalUri(originalSrc)
            if (localUri != null) {
                val replacedTag = originalImgTag.replace(originalSrc, localUri)
                matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacedTag))
            } else {
                matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(originalImgTag))
            }
        }
        matcher.appendTail(sb)
        return sb.toString()
    }

    private fun downloadAndGetLocalUri(url: String): String? {
        if (url.startsWith("file://") || url.startsWith("data:")) {
            return url
        }

        val fullUrl = if (url.startsWith("//")) {
            "https:$url"
        } else if (url.startsWith("/")) {
            "https://www.tecconcursos.com.br$url"
        } else {
            url
        }

        try {
            val extension = getExtension(fullUrl)
            val fileName = md5(fullUrl) + extension
            val targetFile = File(imagesDir, fileName)

            if (!targetFile.exists() || targetFile.length() == 0L) {
                val request = Request.Builder()
                    .url(fullUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                    .header("Referer", "https://www.tecconcursos.com.br/questoes/")
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.byteStream()?.use { input ->
                            FileOutputStream(targetFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } else {
                        Log.w("ImageStorageManager", "Failed to download image: $fullUrl (HTTP ${response.code})")
                    }
                }
            }

            if (targetFile.exists() && targetFile.length() > 0) {
                return "file://${targetFile.absolutePath}"
            }
        } catch (e: Exception) {
            Log.e("ImageStorageManager", "Error downloading image: $fullUrl", e)
        }
        return null
    }

    private fun getExtension(url: String): String {
        val cleanUrl = url.substringBefore("?").substringBefore("#")
        val dotIndex = cleanUrl.lastIndexOf(".")
        return if (dotIndex > 0 && dotIndex > cleanUrl.lastIndexOf("/")) {
            cleanUrl.substring(dotIndex)
        } else {
            ".png"
        }
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
