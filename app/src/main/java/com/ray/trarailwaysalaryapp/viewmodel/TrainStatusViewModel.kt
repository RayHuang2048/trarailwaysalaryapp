package com.ray.trarailwaysalaryapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ray.trarailwaysalaryapp.data.TDXAccessToken
import com.ray.trarailwaysalaryapp.data.TdxApiResponse
import com.ray.trarailwaysalaryapp.data.TrainLiveInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class TrainStatusViewModel : ViewModel() {

    private val TAG = "TrainStatusViewModel"

    private val TDX_CLIENT_ID = "rayhuang2048-0aee86f6-a3c8-4d36"
    private val TDX_CLIENT_SECRET = "7573bc0e-4e64-499e-9d0a-91899ef7b298"

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // LiveData 用於向 UI 發送列車即時動態資料列表
    // 這裡宣告為非空列表，因此 postValue 時必須傳遞非空的 List<TrainLiveInfo>
    private val _trainLiveData = MutableLiveData<List<TrainLiveInfo>>()
    val trainLiveData: LiveData<List<TrainLiveInfo>> = _trainLiveData

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private var accessToken: TDXAccessToken? = null

    private val TDX_AUTH_BASE_URL = "https://tdx.transportdata.tw/"
    private val TDX_DATA_BASE_URL = "https://tdx.transportdata.tw/api/basic/"

    private val tdxApiService: com.ray.trarailwaysalaryapp.api.TdxApiService
    private val authApiService: com.ray.trarailwaysalaryapp.api.TdxApiService

    init {
        val dataRetrofit = Retrofit.Builder()
            .baseUrl(TDX_DATA_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
        tdxApiService = dataRetrofit.create(com.ray.trarailwaysalaryapp.api.TdxApiService::class.java)

        val authRetrofit = Retrofit.Builder()
            .baseUrl(TDX_AUTH_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        authApiService = authRetrofit.create(com.ray.trarailwaysalaryapp.api.TdxApiService::class.java)

        viewModelScope.launch(Dispatchers.IO) {
            getAccessToken()
        }
    }

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
                    _errorMessage.postValue("")
                    token.access_token
                }
            } else {
                val errorMsg = "獲取令牌失敗: ${response.code} ${response.message} - $responseBodyString"
                Log.e(TAG, errorMsg)
                _errorMessage.postValue(errorMsg)
                null
            }
        } catch (e: Exception) {
            val errorMsg = "獲取令牌時發生網路錯誤: ${e.message}"
            Log.e(TAG, errorMsg, e)
            _errorMessage.postValue(errorMsg)
            null
        }
    }

    fun queryTrainLiveStatus(trainNo: String) {
        _trainLiveData.postValue(emptyList<TrainLiveInfo>())
        _errorMessage.postValue("")

        if (trainNo.isBlank()) {
            _errorMessage.postValue("請輸入有效的列車號碼。")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            var currentToken = accessToken?.access_token

            if (currentToken == null || (accessToken?.fetch_time ?: 0L) + (accessToken?.expires_in ?: 0L) * 1000L < System.currentTimeMillis()) {
                Log.d(TAG, "Access Token 不存在或已過期，嘗試重新獲取...")
                currentToken = getAccessToken()
            }

            if (currentToken == null) {
                _errorMessage.postValue("無法獲取存取令牌，請檢查金鑰或網路連線。")
                return@launch
            }

            Log.d(TAG, "使用 Access Token 查詢列車動態...")

            try {
                val response = tdxApiService.getTrainLiveInfo(
                    authorization = "Bearer $currentToken",
                    trainNo = trainNo,
                    format = "JSON"
                )

                Log.d(TAG, "查詢列車動態回應碼: ${response.code()}")
                Log.d(TAG, "查詢列車動態回應訊息: ${response.message()}")


                if (response.isSuccessful) {
                    val apiResponse = response.body()

                    Log.d(TAG, "解析後的 apiResponse: $apiResponse")
                    val trains = apiResponse?.TrainLiveBoards
                    Log.d(TAG, "解析後的 trains 列表: $trains")

                    // 修正：確保 trains 在傳遞給 postValue 之前是非空的
                    // 使用 Elvis 操作符 ?: 來提供一個空列表作為備用值
                    _trainLiveData.postValue(trains ?: emptyList<TrainLiveInfo>()) // <--- 修正這裡
                    _errorMessage.postValue("") // 清除錯誤訊息

                    if (!trains.isNullOrEmpty()) {
                        Log.d(TAG, "列車動態查詢成功，找到 ${trains.size} 筆資料。")
                    } else {
                        Log.d(TAG, "未找到列車 ${trainNo} 的即時動態資訊。")
                        _errorMessage.postValue("未找到列車 ${trainNo} 的即時動態資訊。") // 確保 UI 也顯示此訊息
                    }
                } else {
                    val errorBodyString = response.errorBody()?.string()
                    val errorMsg = "查詢列車動態失敗: HTTP ${response.code()} - ${response.message()} - ${errorBodyString ?: "無錯誤內容"}"
                    Log.e(TAG, errorMsg)
                    _errorMessage.postValue(errorMsg)

                    if (response.code() == 401) {
                        Log.w(TAG, "Access Token 可能過期或無效，嘗試重新獲取並重試。")
                        getAccessToken()
                        if (accessToken != null) {
                            queryTrainLiveStatus(trainNo)
                        } else {
                            _errorMessage.postValue("TDX 認證過期或無效，無法查詢。請檢查憑證。")
                            _trainLiveData.postValue(emptyList<TrainLiveInfo>())
                        }
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "查詢列車動態時發生網路錯誤: ${e.message}"
                Log.e(TAG, errorMsg, e)
                _errorMessage.postValue(errorMsg)
                _trainLiveData.postValue(emptyList<TrainLiveInfo>())
            }
        }
    }
}