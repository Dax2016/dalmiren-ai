package com.darchums.dalmiren.ui.screens.companion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darchums.dalmiren.domain.model.ChatMessage
import com.darchums.dalmiren.domain.model.MessageSender
import com.darchums.dalmiren.domain.repository.ParentingAssistantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

class CompanionViewModel(
    private val assistantRepository: ParentingAssistantRepository
) : ViewModel() {

    private var companionQuestions: List<String> = emptyList()

    private fun greeting(
        text: String = "Hello! I am your DALMIREN AI parenting companion. How can I help you understand and guide your child today?"
    ) = ChatMessage(
        id = UUID.randomUUID().toString(),
        sender = MessageSender.ASSISTANT,
        text = text,
        suggestedPrompts = companionQuestions
    )

    private val _messages = MutableStateFlow(listOf(greeting()))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            val module = assistantRepository.getGuidanceModules()
                .firstOrNull { it.id.equals("COMPANION", ignoreCase = true) }

            companionQuestions = module?.suggestedQuestions.orEmpty()

            val current = _messages.value
            if (current.size == 1 && current.first().sender == MessageSender.ASSISTANT) {
                _messages.value = listOf(greeting())
            }
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isLoading.value) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.USER,
            text = userText.trim()
        )

        _messages.value = _messages.value + userMsg
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            assistantRepository.sendCompanionQuery(userText)
                .catch { ex ->
                    _isLoading.value = false
                    _errorMessage.value =
                        "Unable to complete request. Please try again: ${ex.localizedMessage}"
                }
                .collect { reply ->
                    _isLoading.value = false
                    _messages.value = _messages.value + reply
                }
        }
    }

    fun clearChat() {
        _messages.value = listOf(
            greeting("Chat history cleared. What parenting topic would you like to explore now?")
        )
        _errorMessage.value = null
    }
}
