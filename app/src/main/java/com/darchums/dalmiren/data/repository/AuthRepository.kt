package com.darchums.dalmiren.data.repository

import com.darchums.dalmiren.data.remote.AuthTokenResponse
import com.darchums.dalmiren.data.remote.AuthUserResponse
import com.darchums.dalmiren.data.remote.LoginApiRequest
import com.darchums.dalmiren.data.remote.LogoutApiRequest
import com.darchums.dalmiren.data.remote.NetworkClient
import com.darchums.dalmiren.data.remote.RegisterApiRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response

class AuthRepository(
    private val tokenStorage: SecureTokenStorage
) {

    suspend fun register(
        email: String,
        password: String,
        fullName: String?
    ): AuthUserResponse = withContext(Dispatchers.IO) {
        val response = NetworkClient.apiService.register(
            RegisterApiRequest(
                email = email.trim(),
                password = password,
                full_name = fullName?.trim()?.takeIf { it.isNotEmpty() }
            )
        )

        completeAuthentication(response)
    }

    suspend fun login(
        email: String,
        password: String
    ): AuthUserResponse = withContext(Dispatchers.IO) {
        val response = NetworkClient.apiService.login(
            LoginApiRequest(
                email = email.trim(),
                password = password
            )
        )

        completeAuthentication(response)
    }

    suspend fun restoreSession(): AuthUserResponse? =
        withContext(Dispatchers.IO) {
            val storedTokens = tokenStorage.readTokens()
                ?: return@withContext null

            val response = NetworkClient.apiService.getCurrentUser()

            if (!response.isSuccessful) {
                if (response.code() == 401) {
                    tokenStorage.clearTokens()
                    return@withContext null
                }

                throw IllegalStateException(
                    response.errorBody()?.string()?.let(::extractErrorMessage)
                        ?: "Unable to verify your session. Please try again."
                )
            }

            response.body()
                ?: throw IllegalStateException(
                    "The server returned an empty user profile."
                )
        }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            // Verify the session first. If the access token expired,
            // the network authenticator refreshes and stores the new tokens.
            val userResponse = NetworkClient.apiService.getCurrentUser()

            if (!userResponse.isSuccessful) {
                throw IllegalStateException(
                    userResponse.errorBody()?.string()?.let(::extractErrorMessage)
                        ?: "Unable to verify the session before signing out."
                )
            }

            // Read the latest refresh token after any automatic rotation.
            val latestTokens = tokenStorage.readTokens()

            if (latestTokens != null) {
                val response = NetworkClient.apiService.logout(
                    LogoutApiRequest(
                        refresh_token = latestTokens.refreshToken
                    )
                )

                if (!response.isSuccessful) {
                    throw IllegalStateException(
                        response.errorBody()?.string()?.let(::extractErrorMessage)
                            ?: "The server could not complete sign-out."
                    )
                }
            }
        } finally {
            // Always end the local session, even if the network is unavailable.
            tokenStorage.clearTokens()
        }
    }

    private suspend fun completeAuthentication(
        response: Response<AuthTokenResponse>
    ): AuthUserResponse {
        if (!response.isSuccessful) {
            throw IllegalStateException(
                response.errorBody()?.string()?.let(::extractErrorMessage)
                    ?: "Authentication failed. Please try again."
            )
        }

        val tokens = response.body()
            ?: throw IllegalStateException(
                "The server returned an empty authentication response."
            )

        tokenStorage.saveTokens(
            StoredAuthTokens(
                accessToken = tokens.access_token,
                refreshToken = tokens.refresh_token,
                expiresInSeconds = tokens.expires_in,
                tokenType = tokens.token_type
            )
        )

        return tokens.user
            ?: throw IllegalStateException(
                "Authentication succeeded, but the user profile was missing."
            )
    }

    private fun extractErrorMessage(rawBody: String): String {
        return runCatching {
            val json = JSONObject(rawBody)
            val detail = json.opt("detail")

            when (detail) {
                is String -> detail
                else -> "The server rejected the request."
            }
        }.getOrDefault("The server rejected the request.")
    }
}
