package com.darchums.dalmiren.ui.navigation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.darchums.dalmiren.data.remote.AuthUserResponse
import com.darchums.dalmiren.data.repository.AuthRepository
import com.darchums.dalmiren.data.repository.SecureTokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthSessionState(
    val user: AuthUserResponse? = null,
    val isCheckingSession: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isAuthenticated: Boolean
        get() = user != null
}

class AuthSessionViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthSessionState())
    val state: StateFlow<AuthSessionState> = _state.asStateFlow()

    init {
        restoreSession()
    }

    fun restoreSession() {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isCheckingSession = true,
                    error = null
                )
            }

            try {
                val user = authRepository.restoreSession()

                _state.update {
                    it.copy(
                        user = user,
                        isCheckingSession = false,
                        error = null
                    )
                }
            } catch (exception: Exception) {
                _state.update {
                    it.copy(
                        user = null,
                        isCheckingSession = false,
                        error = exception.message
                            ?: "Unable to verify your session. Check your connection and retry."
                    )
                }
            }
        }
    }

    fun login(email: String, password: String) {
        authenticate {
            authRepository.login(email, password)
        }
    }

    fun register(
        email: String,
        password: String,
        fullName: String?
    ) {
        authenticate {
            authRepository.register(email, password, fullName)
        }
    }

    private fun authenticate(
        operation: suspend () -> AuthUserResponse
    ) {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }

            try {
                val user = operation()

                _state.update {
                    it.copy(
                        user = user,
                        isCheckingSession = false,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (exception: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = exception.message
                            ?: "Authentication failed. Please try again."
                    )
                }
            }
        }
    }

    fun logout() {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }

            try {
                authRepository.logout()
            } catch (exception: Exception) {
                _state.update {
                    it.copy(
                        error = exception.message
                            ?: "Sign-out could not be confirmed by the server."
                    )
                }
            } finally {
                // Return to the signed-out UI after the repository's
                // local token-clearing attempt.
                _state.update {
                    it.copy(
                        user = null,
                        isCheckingSession = false,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

class AuthSessionViewModelFactory(
    context: Context
) : ViewModelProvider.Factory {

    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthSessionViewModel::class.java)) {
            val repository = AuthRepository(
                SecureTokenStorage(appContext)
            )

            return AuthSessionViewModel(repository) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
