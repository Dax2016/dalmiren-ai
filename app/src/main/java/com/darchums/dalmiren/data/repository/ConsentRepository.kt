package com.darchums.dalmiren.data.repository

import com.darchums.dalmiren.data.remote.ConsentApiRequest
import com.darchums.dalmiren.data.remote.ConsentApiResponse
import com.darchums.dalmiren.data.remote.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response

class ConsentRepository {

    suspend fun getCloudProcessingConsent(): ConsentApiResponse =
        withContext(Dispatchers.IO) {
            val response = NetworkClient.apiService.getConsent()
            requireSuccessful(response)
        }

    suspend fun updateCloudProcessingConsent(
        granted: Boolean
    ): ConsentApiResponse = withContext(Dispatchers.IO) {
        val response = NetworkClient.apiService.updateConsent(
            ConsentApiRequest(granted = granted)
        )

        val result = requireSuccessful(response)

        if (result.consent_type != "CLOUD_PROCESSING" ||
            result.granted != granted
        ) {
            throw IllegalStateException(
                "The server did not confirm the requested consent change."
            )
        }

        result
    }

    private fun requireSuccessful(
        response: Response<ConsentApiResponse>
    ): ConsentApiResponse {
        if (!response.isSuccessful) {
            val message = response.errorBody()?.string()
                ?.let(::extractErrorMessage)

            throw IllegalStateException(
                message ?: "Unable to communicate with the privacy service."
            )
        }

        return response.body()
            ?: throw IllegalStateException(
                "The privacy service returned an empty response."
            )
    }

    private fun extractErrorMessage(rawBody: String): String {
        return runCatching {
            val json = JSONObject(rawBody)
            val detail = json.opt("detail")

            if (detail is String) detail
            else "The server rejected the privacy request."
        }.getOrDefault(
            "The server rejected the privacy request."
        )
    }
}