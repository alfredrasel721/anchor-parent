package com.example.data.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Small HTTPS client for the Anchor Cloudflare Worker backend.
 * Firebase ID tokens are sent on every request; no service-account credential is stored in the apps.
 */
internal object AnchorWorkerApi {
    private const val BASE_URL = "https://anchor-family-api.anchorfamily.workers.dev"

    suspend fun post(
        auth: FirebaseAuth,
        route: String,
        payload: Map<String, Any?>
    ): JSONObject {
        require(route.matches(Regex("[a-z0-9/-]+"))) { "Invalid Anchor API route." }
        val user = auth.currentUser ?: throw IOException("Sign in before using Anchor.")
        val idToken = user.getIdToken(false).await().token
            ?: throw IOException("Firebase did not provide a sign-in token.")

        return withContext(Dispatchers.IO) {
            val connection = (URL("$BASE_URL/v1/$route").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                doOutput = true
                useCaches = false
                instanceFollowRedirects = false
                setRequestProperty("Authorization", "Bearer $idToken")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }

            try {
                val requestBytes = JSONObject(payload).toString().toByteArray(Charsets.UTF_8)
                connection.outputStream.use { output ->
                    output.write(requestBytes)
                }

                val statusCode = connection.responseCode
                val responseStream = if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                val responseText = responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val response = try {
                    if (responseText.isBlank()) JSONObject() else JSONObject(responseText)
                } catch (_: Exception) {
                    JSONObject()
                }

                if (statusCode !in 200..299) {
                    val message = response.optJSONObject("error")
                        ?.optString("message")
                        ?.takeIf { it.isNotBlank() }
                        ?: "Anchor service request failed (HTTP $statusCode)."
                    throw IOException(message)
                }
                response
            } finally {
                connection.disconnect()
            }
        }
    }
}
