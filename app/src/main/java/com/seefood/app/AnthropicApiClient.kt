package com.seefood.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AnthropicApiClient(private val apiKey: String) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val API_URL = "https://api.anthropic.com/v1/messages"
        private const val MODEL = "claude-haiku-4-5-20251001"
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private const val MAX_TOKENS = 10
        private const val PROMPT = "Is this a hotdog? Reply with only 'Hotdog' or 'Not Hotdog'"
    }

    suspend fun classifyImage(base64Image: String): ClassificationResult =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext ClassificationResult.Error(
                    "API key not configured. Add ANTHROPIC_API_KEY to local.properties."
                )
            }

            val body = buildRequestJson(base64Image)
            val request = Request.Builder()
                .url(API_URL)
                .post(body.toRequestBody("application/json".toMediaType()))
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .build()

            try {
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody == null) {
                    return@withContext ClassificationResult.Error(
                        "API error ${response.code}"
                    )
                }
                parseResponse(responseBody)
            } catch (e: Exception) {
                ClassificationResult.Error("Network error: ${e.message}")
            }
        }

    private fun buildRequestJson(base64Image: String): String {
        val imageContent = JSONObject().apply {
            put("type", "image")
            put("source", JSONObject().apply {
                put("type", "base64")
                put("media_type", "image/jpeg")
                put("data", base64Image)
            })
        }
        val textContent = JSONObject().apply {
            put("type", "text")
            put("text", PROMPT)
        }
        val message = JSONObject().apply {
            put("role", "user")
            put("content", JSONArray().apply {
                put(imageContent)
                put(textContent)
            })
        }
        return JSONObject().apply {
            put("model", MODEL)
            put("max_tokens", MAX_TOKENS)
            put("messages", JSONArray().apply { put(message) })
        }.toString()
    }

    private fun parseResponse(responseBody: String): ClassificationResult {
        return try {
            val text = JSONObject(responseBody)
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
                .trim()

            when {
                text.contains("not hotdog", ignoreCase = true) -> ClassificationResult.NotHotdog
                text.contains("hotdog", ignoreCase = true) -> ClassificationResult.Hotdog
                else -> ClassificationResult.NotHotdog
            }
        } catch (e: Exception) {
            ClassificationResult.Error("Failed to parse response: ${e.message}")
        }
    }
}
