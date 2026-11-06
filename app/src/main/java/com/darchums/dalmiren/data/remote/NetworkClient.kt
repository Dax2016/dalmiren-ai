package com.darchums.dalmiren.data.remote

import android.content.Context
import com.darchums.dalmiren.data.repository.SecureTokenStorage
import com.darchums.dalmiren.data.repository.StoredAuthTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Route
import okhttp3.Response as OkHttpResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    @Volatile
    private var baseUrl: String = "http://10.0.2.2:8000/"

    @Volatile
    private var secureTokenStorage: SecureTokenStorage? = null

    @Volatile
    private var retrofitService: DalMirenApiService? = null

    @Volatile
    private var refreshRetrofitService: AuthRefreshApiService? = null

    private val refreshLock = Any()

    fun initialize(context: Context) {
        synchronized(this) {
            if (secureTokenStorage == null) {
                secureTokenStorage = SecureTokenStorage(
                    context.applicationContext
                )
            }
        }
    }

    fun setCustomBaseUrl(url: String) {
        require(url.startsWith("http://") || url.startsWith("https://")) {
            "API base URL must start with http:// or https://"
        }

        val normalized = if (url.endsWith("/")) url else "$url/"

        synchronized(this) {
            if (baseUrl != normalized) {
                baseUrl = normalized
                retrofitService = null
                refreshRetrofitService = null
            }
        }
    }

    fun getBaseUrl(): String = baseUrl

    private val refreshHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val authenticatedHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val originalRequest = chain.request()

                if (isPublicAuthEndpoint(originalRequest)) {
                    chain.proceed(originalRequest)
                } else {
                    val storage = secureTokenStorage

                    val tokens = storage?.let {
                        runBlocking(Dispatchers.IO) {
                            it.readTokens()
                        }
                    }

                    val authenticatedRequest = if (tokens != null) {
                        originalRequest.newBuilder()
                            .header(
                                "Authorization",
                                "${tokens.tokenType} ${tokens.accessToken}"
                            )
                            .build()
                    } else {
                        originalRequest
                    }

                    chain.proceed(authenticatedRequest)
                }
            }
            .authenticator(TokenAuthenticator)
            .build()
    }

    private fun isPublicAuthEndpoint(request: Request): Boolean {
        return when (request.url.encodedPath) {
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh" -> true

            else -> false
        }
    }

    private val refreshApiService: AuthRefreshApiService
        get() {
            refreshRetrofitService?.let { return it }

            return synchronized(this) {
                refreshRetrofitService ?: Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(refreshHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(AuthRefreshApiService::class.java)
                    .also { refreshRetrofitService = it }
            }
        }

    val apiService: DalMirenApiService
        get() {
            check(secureTokenStorage != null) {
                "NetworkClient.initialize(context) must be called before using the API."
            }

            retrofitService?.let { return it }

            return synchronized(this) {
                retrofitService ?: Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(authenticatedHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(DalMirenApiService::class.java)
                    .also { retrofitService = it }
            }
        }

    private object TokenAuthenticator : Authenticator {

        override fun authenticate(
            route: Route?,
            response: OkHttpResponse
        ): Request? {

            if (responseCount(response) >= 2) {
                return null
            }

            val storage = secureTokenStorage ?: return null

            val failedAuthorization =
                response.request.header("Authorization") ?: return null

            return synchronized(refreshLock) {
                try {
                    runBlocking(Dispatchers.IO) {
                        val currentTokens = storage.readTokens()
                            ?: return@runBlocking null

                        val currentAuthorization =
                            "${currentTokens.tokenType} ${currentTokens.accessToken}"

                        // Another request may already have refreshed the session.
                        if (currentAuthorization != failedAuthorization) {
                            return@runBlocking response.request.newBuilder()
                                .header("Authorization", currentAuthorization)
                                .build()
                        }

                        val refreshResponse = refreshApiService
                            .refresh(
                                RefreshApiRequest(currentTokens.refreshToken)
                            )
                            .execute()

                        if (refreshResponse.isSuccessful) {
                            val refreshed = refreshResponse.body()
                                ?: return@runBlocking null

                            val updatedTokens = StoredAuthTokens(
                                accessToken = refreshed.access_token,
                                refreshToken = refreshed.refresh_token,
                                expiresInSeconds = refreshed.expires_in,
                                tokenType = refreshed.token_type
                            )

                            storage.saveTokens(updatedTokens)

                            response.request.newBuilder()
                                .header(
                                    "Authorization",
                                    "${updatedTokens.tokenType} ${updatedTokens.accessToken}"
                                )
                                .build()
                        } else {
                            // A rejected refresh token requires a new sign-in.
                            if (refreshResponse.code() == 401) {
                                storage.clearTokens()
                            }

                            null
                        }
                    }
                } catch (_: Exception) {
                    // Network or secure-storage failures must not trigger cloud
                    // credential fallback or silently destroy a valid session.
                    null
                }
            }
        }

        private fun responseCount(response: OkHttpResponse): Int {
            var count = 1
            var prior = response.priorResponse

            while (prior != null) {
                count++
                prior = prior.priorResponse
            }

            return count
        }
    }
}
