package com.darchums.dalmiren.data.remote

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthRefreshApiService {

    @POST("api/auth/refresh")
    fun refresh(
        @Body request: RefreshApiRequest
    ): Call<AuthTokenResponse>
}
