package com.ray.trarailwaysalaryapp.api

import com.ray.trarailwaysalaryapp.data.TDXAccessToken
import com.ray.trarailwaysalaryapp.data.TdxApiResponse
import com.ray.trarailwaysalaryapp.data.TrainLiveInfo
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TdxApiService {

    @FormUrlEncoded
    @POST("auth/realms/TDXConnect/protocol/openid-connect/token")
    suspend fun getAccessToken(
        @Field("grant_type") grantType: String = "client_credentials",
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String
    ): Response<TDXAccessToken>

    /**
     * 查詢台鐵列車的即時動態資訊。
     * 此 API 提供列車的即時位置、誤點時間、到站/離站狀態等。
     *
     * @param authorization 包含 Access Token 的授權頭，格式為 "Bearer [您的Access Token]"。
     * @param trainNo 要查詢的列車號碼 (例如: "110", "123")。
     * @param format 要求回傳資料的格式，固定為 "JSON"。
     * @return 包含列車即時動態資料的 Response 物件，由 TdxApiResponse 包裝。
     */
    // *** 修正：將 @GET 路徑更新為您提供的正確路徑 ***
    @GET("v3/Rail/TRA/TrainLiveBoard/TrainNo/{TrainNo}") // <--- 這裡修改了！
    suspend fun getTrainLiveInfo(
        @Header("Authorization") authorization: String,
        @Path("TrainNo") trainNo: String,
        @Query("\$format") format: String = "JSON"
    ): Response<TdxApiResponse<TrainLiveInfo>>
}