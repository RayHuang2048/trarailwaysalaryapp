// app/src/main/java/com/ray/trarailwaysalaryapp/viewmodel/TrainStatusViewModel.kt

package com.ray.trarailwaysalaryapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.ray.trarailwaysalaryapp.data.NameType
import com.ray.trarailwaysalaryapp.data.TDXAccessToken
import com.ray.trarailwaysalaryapp.data.TrainLiveData // 注意：這個資料類別可能需要調整，因為 GeneralTrainInfo 的回應結構可能不同
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class TrainStatusViewModel : ViewModel() {

    private val TAG = "TrainStatusViewModel"

    // =========================================================================
    // !!! VERY IMPORTANT !!!
    // 請將以下 YOUR_TDX_CLIENT_ID 和 YOUR_TDX_CLIENT_SECRET 替換為
    // 您在步驟 1 (取得 TDX API 金鑰) 中從 TDX 平台取得的實際金鑰。
    // =========================================================================
    private val TDX_CLIENT_ID = "rayhuang2048-0aee86f6-a3c8-4d36" // <-- 替換為您的 Client ID
    private val TDX_CLIENT_SECRET = "7573bc0e-4e64-499e-9d0a-91899ef7b298" // <-- 替換為您的 Client Secret

    private val httpClient = OkHttpClient()
    private val gson = Gson()

    private val _trainLiveData = MutableLiveData<List<TrainLiveData>>()
    val trainLiveData: LiveData<List<TrainLiveData>> = _trainLiveData

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private var accessToken: TDXAccessToken? = null

    private suspend fun getAccessToken(): String? {
        Log.d(TAG, "嘗試獲取 Access Token...")
        val url = "https://tdx.transportdata.tw/auth/realms/TDXConnect/protocol/openid-connect/token"
        val formBody = "grant_type=client_credentials&client_id=$TDX_CLIENT_ID&client_secret=$TDX_CLIENT_SECRET"
            .toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            val responseBodyString = response.body?.string()
            Log.d(TAG, "獲取 Access Token 回應碼: ${response.code}")
            Log.d(TAG, "獲取 Access Token 回應內容: $responseBodyString")

            if (response.isSuccessful) {
                responseBodyString?.let { responseBody ->
                    val token = gson.fromJson(responseBody, TDXAccessToken::class.java)
                    accessToken = token
                    Log.d(TAG, "Access Token 獲取成功！")
                    token.access_token
                }
            } else {
                _errorMessage.postValue("獲取令牌失敗: ${response.code} ${response.message} - $responseBodyString")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "獲取令牌時發生網路錯誤", e)
            _errorMessage.postValue("獲取令牌時發生網路錯誤: ${e.message}")
            null
        }
    }

    fun queryTrainLiveStatus(trainNo: String) {
        _trainLiveData.postValue(emptyList())
        _errorMessage.postValue("")

        if (trainNo.isBlank()) {
            _errorMessage.postValue("請輸入有效的列車號碼。")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val token = getAccessToken()
            if (token == null) {
                _errorMessage.postValue("無法獲取存取令牌，請檢查金鑰或網路連線。")
                return@launch
            }

            Log.d(TAG, "使用 Access Token 查詢列車動態...")

            // *** 修正後的 API URL 基礎路徑 (使用 GeneralTrainInfo) ***
            val baseUrl = "https://tdx.transportdata.tw/api/basic/v2/Rail/TRA/GeneralTrainInfo/TrainNo/" // <--- 這裡變更了！
            val httpUrl = (baseUrl + trainNo).toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("\$top", "30") // 添加 $top 參數
                ?.addQueryParameter("\$format", "JSON") // 添加 $format 參數
                ?.build()

            if (httpUrl == null) {
                _errorMessage.postValue("無法構建有效的 API URL。")
                Log.e(TAG, "無法構建有效的 API URL for trainNo: $trainNo")
                return@launch
            }

            val request = Request.Builder()
                .url(httpUrl)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .build()

            try {
                val response = httpClient.newCall(request).execute()
                val responseBodyString = response.body?.string()
                Log.d(TAG, "查詢列車動態回應碼: ${response.code}")
                Log.d(TAG, "查詢列車動態回應內容: $responseBodyString")

                if (response.isSuccessful) {
                    responseBodyString?.let { responseBody ->
                        // 注意：這裡的 TrainLiveData 資料類別可能不完全符合 GeneralTrainInfo 的回應結構。
                        // 如果解析失敗，我們可能需要根據 GeneralTrainInfo 的實際回應來調整 data class。
                        val trains = gson.fromJson(responseBody, Array<TrainLiveData>::class.java).toList()
                        _trainLiveData.postValue(trains)
                        Log.d(TAG, "列車動態查詢成功，找到 ${trains.size} 筆資料。")
                    } ?: _errorMessage.postValue("API 回應內容為空。")
                } else {
                    _errorMessage.postValue("查詢列車動態失敗: HTTP ${response.code} - ${response.message} - $responseBodyString")
                }
            } catch (e: Exception) {
                Log.e(TAG, "查詢列車動態時發生網路錯誤", e)
                _errorMessage.postValue("查詢列車動態時發生網路錯誤: ${e.message}")
            }
        }
    }
}
