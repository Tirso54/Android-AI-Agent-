package com.example.agent

import com.example.BuildConfig
import com.example.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AgentPlanner {

    suspend fun planAction(userPrompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey.contains("MY_GEMINI_API_KEY")) {
            return@withContext "Error: Please configure your Gemini API Key."
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    role = "user",
                    parts = listOf(Part(text = userPrompt))
                )
            ),
            generationConfig = GenerationConfig(
                thinkingConfig = ThinkingConfig(thinkingLevel = "high")
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are an AI Agent that controls an Android phone. Respond with the step-by-step actions required to fulfill the user's request. Keep the steps clear and actionable, simulating what accessibility services would do (e.g., '1. Open App X', '2. Click Search', etc.)."))
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = "gemini-3.1-pro-preview",
                apiKey = apiKey,
                request = request
            )
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text != null) {
                text
            } else {
                "No plan generated. The model returned an empty response. (Response size: ${response.candidates?.size})"
            }
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: "No error body"
            "API Error: ${e.code()} - ${e.message()}\nDetails: $errorBody"
        } catch (e: Exception) {
            "Error generating plan: ${e.message}"
        }
    }
    
    suspend fun chat(history: List<Content>, userMessage: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey.contains("MY_GEMINI_API_KEY")) {
            return@withContext "Error: Please configure your Gemini API Key in the AI Studio Settings."
        }
        
        val newContents = history.toMutableList().apply {
            add(Content(role = "user", parts = listOf(Part(text = userMessage))))
        }
        
        val request = GenerateContentRequest(
            contents = newContents,
            systemInstruction = Content(
                parts = listOf(Part(text = "You are an Android AI Agent. You assist the user and explain how you will control their phone to help them."))
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = "gemini-3.5-flash",
                apiKey = apiKey,
                request = request
            )
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text != null) {
                text
            } else {
                "No response from Gemini API. It might have been blocked by safety settings or returned an empty response. (Response size: ${response.candidates?.size})"
            }
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: "No error body"
            "API Error: ${e.code()} - ${e.message()}\nDetails: $errorBody"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
