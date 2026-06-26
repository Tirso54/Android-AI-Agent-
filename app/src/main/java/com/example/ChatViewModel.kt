package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agent.AgentPlanner
import com.example.api.Content
import com.example.api.Part
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isThinking: Boolean = false
)

class ChatViewModel : ViewModel() {
    private val agentPlanner = AgentPlanner()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()
    
    // Maintain internal history for Gemini API
    private val chatHistory = mutableListOf<Content>()

    init {
        // Initial greeting
        addMessage(ChatMessage(
            id = System.currentTimeMillis().toString(),
            isUser = false,
            text = "Hello! I am your Android AI Agent. I can plan tasks and control your phone. What would you like me to do?"
        ))
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        
        val userMsgId = System.currentTimeMillis().toString()
        addMessage(ChatMessage(id = userMsgId, isUser = true, text = userText))
        
        _isTyping.value = true
        
        viewModelScope.launch {
            // Determine if the user is asking for a complex plan or just chatting
            val isComplexTask = userText.contains("open", ignoreCase = true) || 
                                userText.contains("play", ignoreCase = true) ||
                                userText.contains("search", ignoreCase = true) ||
                                userText.contains("minecraft", ignoreCase = true)
            
            val responseText = if (isComplexTask) {
                // Use High Thinking Planner
                val plan = agentPlanner.planAction(userText)
                "**Executing Plan:**\n$plan\n\n*(Note: Real execution requires Accessibility & MediaProjection enabled)*"
            } else {
                // Regular Chat
                agentPlanner.chat(chatHistory, userText)
            }
            
            val agentMsgId = (System.currentTimeMillis() + 1).toString()
            addMessage(ChatMessage(id = agentMsgId, isUser = false, text = responseText))
            
            // Add to history
            chatHistory.add(Content(role = "user", parts = listOf(Part(text = userText))))
            chatHistory.add(Content(role = "model", parts = listOf(Part(text = responseText))))
            
            _isTyping.value = false
        }
    }

    private fun addMessage(message: ChatMessage) {
        _messages.update { current -> current + message }
    }
}
