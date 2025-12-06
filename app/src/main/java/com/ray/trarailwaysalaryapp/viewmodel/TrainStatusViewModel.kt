package com.ray.trarailwaysalaryapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.ray.trarailwaysalaryapp.api.TdxApiService
import com.ray.trarailwaysalaryapp.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class TrainStatusViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "TrainStatusViewModel"

    private val TDX_CLIENT_ID = "rayhuang2048-0aee86f6-a3c8-4d36"
    private val TDX_CLIENT_SECRET = "7573bc0e-4e64-499e-9d0a-91899ef7b298"

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val _trainLiveData = MutableLiveData<List<TrainLiveInfo>>()
    val trainLiveData: LiveData<List<TrainLiveInfo>> = _trainLiveData

    private val _trainTimetableLiveData = MutableLiveData<List<StopTime>>()
    val trainTimetableLiveData: LiveData<List<StopTime>> = _trainTimetableLiveData

    private val _allStations = MutableLiveData<List<Station>>()
    val allStations: LiveData<List<Station>> = _allStations

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private var accessToken: TDXAccessToken? = null
    private var tokenFetchTime: Long = 0L

    private val tdxApiService: TdxApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://tdx.transportdata.tw/api/basic/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
        tdxApiService = retrofit.create(TdxApiService::class.java)

        viewModelScope.launch(Dispatchers.IO) {
            getAccessToken()
            fetchAllStations()
        }
    }

    private suspend fun getAccessToken(): String? {
        Log.d(TAG, "嘗試獲取 Access Token...")
        val url = "https://tdx.transportdata.tw/auth/realms/TDXConnect/protocol/openid-connect/token"
        val formBody = "grant_type=client_credentials&client_id=$TDX_CLIENT_ID&client_secret=$TDX_CLIENT_SECRET"
            .toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(formBody).build()

        return try {
            val response = httpClient.newCall(request).execute()
            val responseBodyString = response.body?.string()
            if (response.isSuccessful) {
                responseBodyString?.let {
                    val token = gson.fromJson(it, TDXAccessToken::class.java)
                    accessToken = token
                    tokenFetchTime = System.currentTimeMillis()
                    Log.d(TAG, "Access Token 獲取成功！")
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
        _trainLiveData.postValue(emptyList())
        _trainTimetableLiveData.postValue(emptyList())
        _errorMessage.postValue("")

        if (trainNo.isBlank()) {
            _errorMessage.postValue("請輸入有效的列車號碼。")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            var currentToken = accessToken?.access_token
            if (currentToken == null || (tokenFetchTime + ((accessToken?.expires_in ?: 0L) * 1000L)) < System.currentTimeMillis()) {
                Log.d(TAG, "Access Token 不存在或已過期，嘗試重新獲取...")
                currentToken = getAccessToken()
            }

            if (currentToken == null) {
                _errorMessage.postValue("無法獲取存取令牌，請檢查金鑰或網路連線。")
                return@launch
            }

            try {
                val response = tdxApiService.getTrainLiveInfo(
                    authorization = "Bearer $currentToken",
                    trainNo = trainNo,
                    format = "JSON"
                )

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    val trains = apiResponse?.TrainLiveBoards
                    _trainLiveData.postValue(trains ?: emptyList())
                    if (trains.isNullOrEmpty()) {
                        _errorMessage.postValue("未找到列車 ${trainNo} 的即時動態資訊。")
                    }
                } else {
                    val errorBodyString = response.errorBody()?.string()
                    val errorMsg = "查詢列車動態失敗: HTTP ${response.code()} - ${response.message()} - ${errorBodyString ?: "無錯誤內容"}"
                    Log.e(TAG, errorMsg)
                    _errorMessage.postValue(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "查詢列車動態時發生網路錯誤: ${e.message}"
                Log.e(TAG, errorMsg, e)
                _errorMessage.postValue(errorMsg)
            }
            
            queryTrainTimetable(trainNo)
        }
    }

    private suspend fun queryTrainTimetable(trainNo: String) {
        Log.d(TAG, "嘗試查詢列車時刻表。列車號碼: $trainNo")
        val currentToken = accessToken?.access_token

        if (currentToken == null) {
            Log.e(TAG, "無法獲取存取令牌，無法查詢列車時刻表。")
            return
        }

        try {
            val response = tdxApiService.getTrainTimetable(
                authorization = "Bearer $currentToken",
                trainNo = trainNo,
                top = 30,
                format = "JSON"
            )

            if (response.isSuccessful) {
                val apiResponse = response.body()
                val timetableDetails = apiResponse?.TrainTimetables
                if (!timetableDetails.isNullOrEmpty()) {
                    val stopTimes = timetableDetails[0].StopTimes
                    _trainTimetableLiveData.postValue(stopTimes)
                } else {
                    _trainTimetableLiveData.postValue(emptyList())
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

    fun fetchAllStations() {
        viewModelScope.launch(Dispatchers.IO) {
            var currentToken = accessToken?.access_token
            if (currentToken == null || (tokenFetchTime + ((accessToken?.expires_in ?: 0L) * 1000L)) < System.currentTimeMillis()) {
                Log.d(TAG, "Access Token for stations not available or expired, getting new one.")
                currentToken = getAccessToken()
            }

            if (currentToken == null) {
                _errorMessage.postValue("Cannot get access token, unable to fetch station list.")
                return@launch
            }

            try {
                val response = tdxApiService.getAllStations(
                    authorization = "Bearer $currentToken",
                    format = "JSON"
                )

                if (response.isSuccessful) {
                    val stationResponse = response.body()
                    val stations = stationResponse?.stations ?: emptyList()
                    _allStations.postValue(stations)
                    saveStationsToJson(stationResponse)
                } else {
                    val errorBodyString = response.errorBody()?.string()
                    val errorMsg = "Failed to fetch station list: HTTP ${response.code()} - ${response.message()} - ${errorBodyString ?: "No error body"}"
                    Log.e(TAG, errorMsg)
                    _errorMessage.postValue(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Network error when fetching station list: ${e.message}"
                Log.e(TAG, errorMsg, e)
                _errorMessage.postValue(errorMsg)
            }
        }
    }

    private fun saveStationsToJson(stationResponse: StationResponse?) {
        if (stationResponse == null) return
        val context = getApplication<Application>().applicationContext
        val file = File(context.filesDir, "stations.json")
        try {
            file.writeText(gson.toJson(stationResponse))
        } catch (e: Exception) {
            Log.e(TAG, "無法儲存車站資料", e)
        }
    }
}
