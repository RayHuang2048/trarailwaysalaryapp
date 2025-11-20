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
import com.ray.trarailwaysalaryapp.data.TrainTimetableResponse // 導入新的時刻表回應資料類別
import com.ray.trarailwaysalaryapp.data.TrainTimetableDetail // 導入時刻表詳細資料類別
import com.ray.trarailwaysalaryapp.data.StopTime // 導入停靠站資料類別
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat // 用於日期格式化
import java.util.Date // 用於獲取當前日期
import java.util.Locale // 用於地區設定
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

    private val _trainLiveData = MutableLiveData<List<TrainLiveInfo>>()
    val trainLiveData: LiveData<List<TrainLiveInfo>> = _trainLiveData

    // 新增 LiveData 用於向 UI 發送列車時刻表資料列表
    private val _trainTimetableLiveData = MutableLiveData<List<StopTime>>()
    val trainTimetableLiveData: LiveData<List<StopTime>> = _trainTimetableLiveData

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

    /**
     * 負責向 TDX 認證服務發送請求，獲取 Access Token。
     * TDX API 需要 OAuth 2.0 Client Credentials 認證流程。
     * @return 成功獲取則回傳 Access Token 字串，否則回傳 null。
     */
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

    /**
     * 根據列車號碼查詢台鐵列車的即時動態資訊。
     * 如果 Access Token 過期或無效，會嘗試重新獲取並重試查詢。
     * @param trainNo 要查詢的列車號碼。
     */
    fun queryTrainLiveStatus(trainNo: String) {
        _trainLiveData.postValue(emptyList<TrainLiveInfo>())
        _trainTimetableLiveData.postValue(emptyList()) // 清空時刻表數據
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

                    _trainLiveData.postValue(trains ?: emptyList<TrainLiveInfo>())
                    _errorMessage.postValue("")

                    if (!trains.isNullOrEmpty()) {
                        Log.d(TAG, "列車動態查詢成功，找到 ${trains.size} 筆資料。")
                    } else {
                        Log.d(TAG, "未找到列車 ${trainNo} 的即時動態資訊。")
                        _errorMessage.postValue("未找到列車 ${trainNo} 的即時動態資訊。")
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

            // 無論即時動態查詢成功與否，都嘗試查詢時刻表
            queryTrainTimetable(trainNo)
        }
    }

    /**
     * 根據列車號碼查詢台鐵列車的時刻表資訊。
     * @param trainNo 要查詢的列車號碼。
     */
    private suspend fun queryTrainTimetable(trainNo: String) {
        Log.d(TAG, "嘗試查詢列車時刻表。列車號碼: $trainNo")
        var currentToken = accessToken?.access_token

        if (currentToken == null) {
            Log.e(TAG, "無法獲取存取令牌，無法查詢列車時刻表。")
            _trainTimetableLiveData.postValue(emptyList()) // 清空時刻表數據
            return
        }

        try {
            val response = tdxApiService.getTrainTimetable(
                authorization = "Bearer $currentToken",
                trainNo = trainNo,
                top = 30, // Explicitly pass the top parameter
                format = "JSON"
            )

            Log.d(TAG, "查詢列車時刻表回應碼: ${response.code()}")
            Log.d(TAG, "查詢列車時刻表回應訊息: ${response.message()}")

            if (response.isSuccessful) {
                val apiResponse = response.body()
                val timetableDetails = apiResponse?.TrainTimetables

                if (!timetableDetails.isNullOrEmpty()) {
                    // 假設我們只關心找到的第一個列車時刻表（因為通常只有一個匹配的）
                    val stopTimes = timetableDetails[0].StopTimes
                    _trainTimetableLiveData.postValue(stopTimes)
                    Log.d(TAG, "列車時刻表查詢成功，找到 ${stopTimes.size} 個停靠站。")
                } else {
                    _trainTimetableLiveData.postValue(emptyList())
                    Log.d(TAG, "未找到列車 ${trainNo} 的時刻表資訊。")
                }
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMsg = "查詢列車時刻表失敗: HTTP ${response.code()} - ${response.message()} - ${errorBodyString ?: "無錯誤內容"}"
                Log.e(TAG, errorMsg)
                _trainTimetableLiveData.postValue(emptyList())
            }
        } catch (e: Exception) {
            val errorMsg = "查詢列車時刻表時發生網路錯誤: ${e.message}"
            Log.e(TAG, errorMsg, e)
            _trainTimetableLiveData.postValue(emptyList())
        }
    }
}
