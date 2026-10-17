package br.com.tecoff.data.remote

import android.content.Context
import android.content.SharedPreferences
import br.com.tecoff.data.model.TecSession
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Response

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("tec_session_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _sessionState = MutableStateFlow(loadSession())
    val sessionState: StateFlow<TecSession> = _sessionState.asStateFlow()

    private val _isSessionExpired = MutableStateFlow(false)
    val isSessionExpired: StateFlow<Boolean> = _isSessionExpired.asStateFlow()

    fun getSession(): TecSession = _sessionState.value

    fun isLogged(): Boolean = _sessionState.value.logged

    fun saveCookies(cookies: Map<String, String>, via: String = "webview", email: String? = null) {
        val currentCookies = _sessionState.value.cookies.toMutableMap()
        currentCookies.putAll(cookies)

        val newSession = TecSession(
            logged = currentCookies.containsKey("TecPermanecerLogado") || currentCookies.containsKey("JSESSIONID"),
            cookies = currentCookies,
            email = email ?: _sessionState.value.email,
            via = via,
            loginTimestamp = System.currentTimeMillis()
        )
        persistSession(newSession)
        _isSessionExpired.value = false
    }

    fun importCookiesFromText(rawText: String) {
        val jar = mutableMapOf<String, String>()
        val trimmed = rawText.trim()
        if (trimmed.startsWith("[")) {
            // JSON array of cookie objects [{name, value}]
            val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
            val list: List<Map<String, Any>> = gson.fromJson(trimmed, listType)
            for (c in list) {
                val name = c["name"]?.toString()
                val value = c["value"]?.toString()
                if (name != null && value != null) jar[name] = value
            }
        } else if (trimmed.startsWith("{")) {
            // JSON object {name: value}
            val mapType = object : TypeToken<Map<String, String>>() {}.type
            val map: Map<String, String> = gson.fromJson(trimmed, mapType)
            jar.putAll(map)
        } else {
            // Header cookie string "name=val; name2=val2"
            for (part in trimmed.split(";")) {
                val idx = part.indexOf("=")
                if (idx > 0) {
                    val name = part.substring(0, idx).trim()
                    val value = part.substring(idx + 1).trim()
                    jar[name] = value
                }
            }
        }
        saveCookies(jar, via = "import")
    }

    /**
     * Intercepts Set-Cookie headers from TEC responses to update session cookies.
     */
    fun absorbResponseCookies(response: Response) {
        val setCookies = response.headers("Set-Cookie")
        if (setCookies.isEmpty()) return

        val current = _sessionState.value.cookies.toMutableMap()
        var changed = false

        for (header in setCookies) {
            val parts = header.split(";")
            val pair = parts[0]
            val eqIdx = pair.indexOf("=")
            if (eqIdx > 0) {
                val name = pair.substring(0, eqIdx).trim()
                val value = pair.substring(eqIdx + 1).trim()
                val isExpired = parts.any { it.trim().startsWith("max-age=0", ignoreCase = true) } || value.isEmpty() || value == "deleted"

                if (isExpired) {
                    if (current.remove(name) != null) changed = true
                } else {
                    if (current[name] != value) {
                        current[name] = value
                        changed = true
                    }
                }
            }
        }

        if (changed) {
            val updated = _sessionState.value.copy(
                cookies = current,
                logged = current.containsKey("TecPermanecerLogado") || current.containsKey("JSESSIONID")
            )
            persistSession(updated)
        }
    }

    fun markSessionExpired() {
        val expired = _sessionState.value.copy(logged = false)
        persistSession(expired)
        _isSessionExpired.value = true
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        _sessionState.value = TecSession()
        _isSessionExpired.value = false
    }

    private fun loadSession(): TecSession {
        val json = prefs.getString("session_data", null) ?: return TecSession()
        return try {
            gson.fromJson(json, TecSession::class.java)
        } catch (e: Exception) {
            TecSession()
        }
    }

    private fun persistSession(session: TecSession) {
        _sessionState.value = session
        prefs.edit().putString("session_data", gson.toJson(session)).apply()
    }
}
