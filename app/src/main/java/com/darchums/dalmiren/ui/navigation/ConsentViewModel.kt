package com.darchums.dalmiren.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.darchums.dalmiren.data.repository.ConsentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConsentState(
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val cloudProcessingGranted: Boolean = false,
    val error: String? = null
) {
    val canChangeConsent: Boolean
        get() = hasLoaded && !isLoading && !isSaving
}

class ConsentViewModel(
    private val repository: ConsentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ConsentState())
    val state: StateFlow<ConsentState> = _state.asStateFlow()

    init {
        loadConsent()
    }

    fun loadConsent() {
        if (_state.value.isLoading || _state.value.isSaving) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }

            try {
                val result = repository.getCloudProcessingConsent()

                if (result.consent_type != "CLOUD_PROCESSING") {
                    throw IllegalStateException(
                        "The server returned an unexpected consent type."
                    )
                }

                _state.update {
                    it.copy(
                        hasLoaded = true,
                        isLoading = false,
                        cloudProcessingGranted = result.granted,
                        error = null
                    )
                }
            } catch (exception: Exception) {
                _state.update {
                    it.copy(
                        hasLoaded = false,
                        isLoading = false,
                        cloudProcessingGranted = false,
                        error = exception.message
                            ?: "Unable to load your privacy preferences. Please retry."
                    )
                }
            }
        }
    }

    fun setCloudProcessingConsent(granted: Boolean) {
        val current = _state.value

        if (!current.canChangeConsent) return
        if (current.cloudProcessingGranted == granted) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSaving = true,
                    error = null
                )
            }

            try {
                val result = repository.updateCloudProcessingConsent(granted)

                _state.update {
                    it.copy(
                        hasLoaded = true,
                        isSaving = false,
                        cloudProcessingGranted = result.granted,
                        error = null
                    )
                }
            } catch (exception: Exception) {
                // The server may have processed the request even if
                // the response was lost. Re-fetch before displaying status.
                try {
                    val latest = repository.getCloudProcessingConsent()

                    _state.update {
                        it.copy(
                            hasLoaded = true,
                            isSaving = false,
                            cloudProcessingGranted = latest.granted,
                            error = exception.message
                                ?: "The consent change could not be confirmed."
                        )
                    }
                } catch (_: Exception) {
                    _state.update {
                        it.copy(
                            hasLoaded = false,
                            isSaving = false,
                            cloudProcessingGranted = false,
                            error = "Consent status could not be confirmed. Please retry."
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

class ConsentViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConsentViewModel::class.java)) {
            return ConsentViewModel(
                ConsentRepository()
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}