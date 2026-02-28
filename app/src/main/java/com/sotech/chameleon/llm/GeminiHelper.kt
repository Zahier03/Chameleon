package com.sotech.chameleon.llm

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.sotech.chameleon.data.ImportedModel
import com.sotech.chameleon.data.MessageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiHelper @Inject constructor() {
    private val TAG = "GeminiHelper"

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _currentResponse = MutableStateFlow("")
    val currentResponse: StateFlow<String> = _currentResponse.asStateFlow()

    private var shouldStop = false

    suspend fun generateResponse(
        prompt: String,
        model: ImportedModel,
        images: List<Bitmap> = emptyList(),
        pdfBase64s: List<String> = emptyList(),
        onPartialResult: (String) -> Unit,
        onComplete: (MessageStats?) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            shouldStop = false
            _isGenerating.value = true
            _currentResponse.value = ""

            val startTime = System.currentTimeMillis()
            var firstTokenTime = 0L
            var tokenCount = 0

            val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/${model.modelCode}:generateContent?key=${model.apiKey}"
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.doInput = true
            connection.readTimeout = 60000
            connection.connectTimeout = 30000

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            if (prompt.isNotBlank()) {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            }

                            if (pdfBase64s.isNotEmpty()) {
                                for (pdfBase64 in pdfBase64s) {
                                    put(JSONObject().apply {
                                        put("inlineData", JSONObject().apply {
                                            put("mimeType", "application/pdf")
                                            put("data", pdfBase64)
                                        })
                                    })
                                }
                            }

                            if (images.isNotEmpty()) {
                                for (bitmap in images) {
                                    try {
                                        val base64Image = bitmapToBase64(bitmap)
                                        put(JSONObject().apply {
                                            put("inlineData", JSONObject().apply {
                                                put("mimeType", "image/jpeg")
                                                put("data", base64Image)
                                            })
                                        })
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to encode image", e)
                                    }
                                }
                            }
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", model.temperature.toDouble())
                    put("topK", model.topK)
                    put("topP", model.topP.toDouble())
                    put("maxOutputTokens", model.maxTokens)
                })
            }

            Log.d(TAG, "Sending request to Gemini API: ${model.modelCode} with ${images.size} images and ${pdfBase64s.size} PDFs")

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(requestBody.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseText = reader.readText()
                reader.close()

                Log.d(TAG, "Response received, length: ${responseText.length}")

                if (shouldStop) {
                    _isGenerating.value = false
                    return@withContext
                }

                try {
                    val jsonResponse = JSONObject(responseText)

                    if (jsonResponse.has("candidates")) {
                        val candidates = jsonResponse.getJSONArray("candidates")
                        if (candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)

                            if (candidate.has("content")) {
                                val content = candidate.getJSONObject("content")

                                if (content.has("parts")) {
                                    val parts = content.getJSONArray("parts")
                                    val fullResponse = StringBuilder()

                                    for (i in 0 until parts.length()) {
                                        val part = parts.getJSONObject(i)
                                        if (part.has("text")) {
                                            val text = part.getString("text")
                                            fullResponse.append(text)

                                            if (firstTokenTime == 0L) {
                                                firstTokenTime = System.currentTimeMillis()
                                            }
                                        }
                                    }

                                    val responseString = fullResponse.toString()
                                    tokenCount = responseString.split("\\s+".toRegex()).size

                                    _currentResponse.value = responseString
                                    onPartialResult(responseString)

                                    Log.d(TAG, "Response extracted successfully, tokens: $tokenCount")

                                    val endTime = System.currentTimeMillis()
                                    val totalLatency = (endTime - startTime) / 1000f
                                    val timeToFirstToken = if (firstTokenTime > 0) (firstTokenTime - startTime) / 1000f else totalLatency
                                    val decodeTime = if (firstTokenTime > 0) (endTime - firstTokenTime) / 1000f else totalLatency
                                    val decodeSpeed = if (decodeTime > 0) tokenCount / decodeTime else 0f

                                    val stats = MessageStats(
                                        timeToFirstToken = timeToFirstToken,
                                        prefillSpeed = 0f,
                                        decodeSpeed = decodeSpeed,
                                        totalLatency = totalLatency,
                                        tokenCount = tokenCount,
                                        prefillTokens = 0,
                                        decodeTokens = tokenCount,
                                        accelerator = "Gemini API"
                                    )

                                    _isGenerating.value = false
                                    onComplete(stats)
                                } else {
                                    Log.e(TAG, "No parts in content")
                                    _isGenerating.value = false
                                    onError("Invalid response format: no parts")
                                }
                            } else {
                                Log.e(TAG, "No content in candidate")
                                _isGenerating.value = false
                                onError("Invalid response format: no content")
                            }
                        } else {
                            Log.e(TAG, "No candidates in response")
                            _isGenerating.value = false
                            onError("No response generated")
                        }
                    } else {
                        Log.e(TAG, "No candidates field in response")
                        _isGenerating.value = false
                        onError("Invalid response format")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing response", e)
                    Log.e(TAG, "Response text: $responseText")
                    _isGenerating.value = false
                    _currentResponse.value = ""
                    onError("Failed to parse response: ${e.message}")
                }
            } else {
                val errorStream = connection.errorStream
                if (errorStream != null) {
                    val errorReader = BufferedReader(InputStreamReader(errorStream))
                    val errorResponse = errorReader.readText()
                    errorReader.close()

                    Log.e(TAG, "API Error: $responseCode - $errorResponse")

                    val errorMessage = try {
                        val errorJson = JSONObject(errorResponse)
                        if (errorJson.has("error")) {
                            val error = errorJson.getJSONObject("error")
                            val message = error.optString("message", "Unknown error")

                            when (responseCode) {
                                400 -> "Invalid request: $message"
                                401 -> "Invalid API key. Please check your API key."
                                403 -> "Access forbidden: $message"
                                404 -> "Model not found. Please check the model code."
                                429 -> "Rate limit exceeded. Please wait and try again."
                                503 -> "Service temporarily unavailable. The model is overloaded, please try again in a moment."
                                else -> "$message"
                            }
                        } else {
                            "HTTP Error: $responseCode"
                        }
                    } catch (e: Exception) {
                        when (responseCode) {
                            401 -> "Invalid API key"
                            429 -> "Rate limit exceeded"
                            503 -> "Service temporarily unavailable"
                            else -> "HTTP Error: $responseCode"
                        }
                    }

                    _isGenerating.value = false
                    _currentResponse.value = ""
                    onError(errorMessage)
                } else {
                    _isGenerating.value = false
                    _currentResponse.value = ""
                    onError("HTTP Error: $responseCode - No error details available")
                }
            }

            connection.disconnect()

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Request timeout", e)
            _isGenerating.value = false
            _currentResponse.value = ""
            onError("Request timeout. Please check your internet connection and try again.")
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network error", e)
            _isGenerating.value = false
            _currentResponse.value = ""
            onError("Network error. Please check your internet connection.")
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
            _isGenerating.value = false
            _currentResponse.value = ""
            onError("Failed to generate response: ${e.message ?: "Unknown error"}")
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    fun stopGeneration() {
        shouldStop = true
        _isGenerating.value = false
    }

    fun cleanup() {
        shouldStop = true
        _isGenerating.value = false
        _currentResponse.value = ""
    }

    suspend fun validateApiKey(apiKey: String, modelCode: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/${modelCode}?key=${apiKey}"
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                Pair(true, "API key validated successfully")
            } else {
                val errorStream = connection.errorStream
                val errorReader = BufferedReader(InputStreamReader(errorStream))
                val errorResponse = errorReader.readText()
                errorReader.close()
                connection.disconnect()

                val errorMessage = try {
                    val errorJson = JSONObject(errorResponse)
                    if (errorJson.has("error")) {
                        val error = errorJson.getJSONObject("error")
                        error.optString("message", "Invalid API key or model code")
                    } else {
                        "Invalid API key or model code"
                    }
                } catch (e: Exception) {
                    "Invalid API key or model code"
                }

                Pair(false, errorMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating API key", e)
            Pair(false, "Failed to validate: ${e.message}")
        }
    }
}