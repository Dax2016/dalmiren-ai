package com.darchums.dalmiren.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class ChatApiRequest(
    val message: String,
    val conversation_id: String? = null,
    val child_id: String? = null,
    val module: String = "COMPANION"
)

data class ChatApiResponse(
    val response_id: String? = null,
    val conversation_id: String,
    val message: String,
    val response_summary: String? = "",
    val empathy_statement: String? = "",
    val understanding: String? = "",
    val possible_contributing_factors: List<String>? = emptyList(),
    val developmental_context: String? = "",
    val recommendations: List<String>? = emptyList(),
    val practical_steps: List<String>? = emptyList(),
    val communication_scripts: List<String>? = emptyList(),
    val positive_discipline_guidance: String? = "",
    val what_to_avoid: List<String>? = emptyList(),
    val action_plan: List<String>? = emptyList(),
    val reflection_questions: List<String>? = emptyList(),
    val follow_up_question: String? = "",
    val professional_support_guidance: String? = null,
    val safety_flags: List<String>? = emptyList(),
    val provider: String,
    val model: String,
    val routing_reason: String,
    val is_fallback: Boolean,
    val latency_ms: Double? = null
)

data class BehaviourApiRequest(
    val child_id: String? = null,
    val age_group: String = "EARLY_CHILDHOOD",
    val description: String,
    val triggers: String = "",
    val context: String = "",
    val observations: String = ""
)

data class GuidanceApiResponse(
    val title: String,
    val category: String,
    val hypotheses: List<String>,
    val emotional_needs: List<String>,
    val calm_response: String,
    val next_observations: List<String>,
    val practical_tip: String,
    val disclaimer: String
)


data class GuidanceModuleApi(
    val id: String = "",
    val name: String = "",
    val role: String = "",
    val mission: String = "",
    val prompt_version: String = "1.0.0",
    val suggested_questions: List<String> = emptyList()
)

data class GuidanceModulesApiResponse(
    val brand: String = "",
    val prompt_registry_version: String = "",
    val total_modules: Int = 0,
    val modules: List<GuidanceModuleApi> = emptyList()
)
data class HealthApiResponse(
    val status: String,
    val brand: String,
    val tagline: String,
    val providers: Map<String, String>,
    val routing_policy: String
)


data class RegisterApiRequest(
    val email: String,
    val password: String,
    val full_name: String? = null
)

data class LoginApiRequest(
    val email: String,
    val password: String
)

data class RefreshApiRequest(
    val refresh_token: String
)

data class LogoutApiRequest(
    val refresh_token: String
)

data class AuthUserResponse(
    val id: String,
    val email: String,
    val full_name: String? = null
)

data class AuthTokenResponse(
    val token_type: String = "bearer",
    val access_token: String,
    val refresh_token: String,
    val expires_in: Int,
    val user: AuthUserResponse? = null
)

data class ConsentApiRequest(
    val consent_type: String = "CLOUD_PROCESSING",
    val granted: Boolean
)

data class ConsentApiResponse(
    val user_id: String,
    val consent_type: String,
    val granted: Boolean,
    val status: String? = null
)

interface DalMirenApiService {
    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterApiRequest
    ): Response<AuthTokenResponse>

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginApiRequest
    ): Response<AuthTokenResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(
        @Body request: RefreshApiRequest
    ): Response<AuthTokenResponse>

    @POST("api/auth/logout")
    suspend fun logout(
        @Body request: LogoutApiRequest
    ): Response<Map<String, String>>

    @GET("api/auth/me")
    suspend fun getCurrentUser(): Response<AuthUserResponse>

    @POST("api/chat")
    suspend fun chat(@Body request: ChatApiRequest): Response<ChatApiResponse>

    @POST("api/guidance/behaviour")
    suspend fun analyzeBehaviour(@Body request: BehaviourApiRequest): Response<GuidanceApiResponse>

    @POST("api/guidance/emotional")
    suspend fun getEmotionalGuidance(@Body request: BehaviourApiRequest): Response<GuidanceApiResponse>

    @POST("api/guidance/communication")
    suspend fun getCommunicationScript(@Body request: BehaviourApiRequest): Response<GuidanceApiResponse>

    @POST("api/guidance/discipline")
    suspend fun getPositiveDisciplineStrategy(@Body request: BehaviourApiRequest): Response<GuidanceApiResponse>

    @POST("api/guidance/learning")
    suspend fun getLearningSupportStrategy(@Body request: BehaviourApiRequest): Response<GuidanceApiResponse>

    @GET("api/guidance/modules")
    suspend fun getGuidanceModules(): Response<GuidanceModulesApiResponse>
    @GET("api/consent")
    suspend fun getConsent(
        @Query("consent_type") consentType: String = "CLOUD_PROCESSING"
    ): Response<ConsentApiResponse>

    @POST("api/consent")
    suspend fun updateConsent(
        @Body request: ConsentApiRequest
    ): Response<ConsentApiResponse>

    @GET("api/health")
    suspend fun checkHealth(): Response<HealthApiResponse>
}
