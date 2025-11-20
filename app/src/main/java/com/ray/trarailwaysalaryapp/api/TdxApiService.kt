package com.ray.trarailwaysalaryapp.api

import com.ray.trarailwaysalaryapp.data.TdxApiResponse
import com.ray.trarailwaysalaryapp.data.TrainLiveInfo
import com.ray.trarailwaysalaryapp.data.TrainTimetableResponse // 導入新的時刻表回應資料類別
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface TdxApiService {

    /**
     * 獲取台鐵列車的即時動態資訊。
     * @param authorization OAuth 2.0 Access Token (Bearer token)。
     * @param trainNo 列車號碼。
     * @param format 回應資料格式，預設為 JSON。
     * @return 包含 TrainLiveInfo 列表的 Response。
     */
    @GET("v3/Rail/TRA/TrainLiveBoard/TrainNo/{TrainNo}")
    suspend fun getTrainLiveInfo(
        @Header("Authorization") authorization: String,
        @Path("TrainNo") trainNo: String,
        @Query("\$format") format: String = "JSON" // 預設為 JSON 格式
    ): Response<TdxApiResponse<TrainLiveInfo>>

    /**
     * 獲取台鐵列車的一般時刻表資訊。
     * @param authorization OAuth 2.0 Access Token (Bearer token)。
     * @param trainNo 列車號碼。
     * @param format 回應資料格式，預設為 JSON。
     * @return 包含 TrainTimetableDetail 列表的 Response。
     */
    @GET("v3/Rail/TRA/GeneralTrainTimetable/TrainNo/{TrainNo}")
    suspend fun getTrainTimetable(
        @Header("Authorization") authorization: String,
        @Path("TrainNo") trainNo: String,
        @Query("\$top") top: Int = 30,
        @Query("\$format") format: String = "JSON"
    ): Response<TrainTimetableResponse>
}
