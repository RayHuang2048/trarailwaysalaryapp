package com.ray.trarailwaysalaryapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.ray.trarailwaysalaryapp.api.TdxApiService
import com.ray.trarailwaysalaryapp.data.ODTrainTimetable
import com.ray.trarailwaysalaryapp.data.Station
import com.ray.trarailwaysalaryapp.data.TDXAccessToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TrainTimetableViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "TrainTimetableViewModel"

    private val TDX_CLIENT_ID = "rayhuang2048-0aee86f6-a3c8-4d36"
    private val TDX_CLIENT_SECRET = "7573bc0e-4e64-499e-9d0a-91899ef7b298"

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val tdxApiService: TdxApiService

    private var accessToken: TDXAccessToken? = null
    private var tokenFetchTime: Long = 0L

    // 起程站和到達站
    private val _startStation = MutableLiveData<Station?>()
    val startStation: LiveData<Station?> = _startStation

    private val _arrivalStation = MutableLiveData<Station?>()
    val arrivalStation: LiveData<Station?> = _arrivalStation

    // OD 查詢結果
    private val _odTimetableResults = MutableLiveData<List<ODTrainTimetable>>()
    val odTimetableResults: LiveData<List<ODTrainTimetable>> = _odTimetableResults

    // 錯誤訊息
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // 是否正在查詢
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://tdx.transportdata.tw/api/basic/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
        tdxApiService = retrofit.create(TdxApiService::class.java)
    }

    fun setStartStation(station: Station?) {
        _startStation.value = station
    }

    fun setArrivalStation(station: Station?) {
        _arrivalStation.value = station
    }

    /**
     * 查詢兩站之間的時刻表
     */
    fun queryODTimetable() {
        val origin = _startStation.value
        val destination = _arrivalStation.value

        if (origin == null) {
            _errorMessage.postValue("請選擇起程站")
            return
        }

        if (destination == null) {
            _errorMessage.postValue("請選擇到達站")
            return
        }

        _isLoading.postValue(true)
        _odTimetableResults.postValue(emptyList())
        _errorMessage.postValue("")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 確保有有效的 Access Token
                var currentToken = accessToken?.access_token
                if (currentToken == null || isTokenExpired()) {
                    Log.d(TAG, "Access Token 不存在或已過期，嘗試重新獲取...")
                    currentToken = getAccessToken()
                }

                if (currentToken == null) {
                    _errorMessage.postValue("無法獲取存取令牌，請檢查網路連線。")
                    _isLoading.postValue(false)
                    return@launch
                }

                // 使用今天的日期
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())

                Log.d(TAG, "查詢 OD 時刻表: ${origin.stationID} -> ${destination.stationID}, 日期: $today")

                val response = tdxApiService.getODTimetable(
                    authorization = "Bearer $currentToken",
                    originStationId = origin.stationID,
                    destinationStationId = destination.stationID,
                    trainDate = today,
                    format = "JSON"
                )

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    val timetables = apiResponse?.trainTimetables ?: emptyList()
                    
                    Log.d(TAG, "收到 ${timetables.size} 個班次")
                    
                    // 過濾掉已經過去的班次，並按出發時間排序
                    val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    val filteredTimetables = timetables
                        .filter { timetable ->
                            val stopTimes = timetable.stopTimes
                            if (stopTimes.isNullOrEmpty() || stopTimes.size < 2) return@filter false
                            
                            // 假設第一個是起程站，最後一個是到達站
                            val originStop = stopTimes.first()
                            val departureTime = originStop.departureTime ?: return@filter false
                            
                            departureTime >= currentTime 
                        }
                        .sortedBy { 
                            it.stopTimes?.firstOrNull()?.departureTime ?: "99:99" 
                        }
                    
                    _odTimetableResults.postValue(filteredTimetables)
                    
                    if (filteredTimetables.isEmpty()) {
                        if (timetables.isEmpty()) {
                            _errorMessage.postValue("找不到此路線的班次")
                        } else {
                            _errorMessage.postValue("今日無可搭乘的班次（已過發車時間）")
                        }
                    } else {
                        Log.d(TAG, "找到 ${filteredTimetables.size} 個班次")
                    }
                } else {
                    val errorBodyString = response.errorBody()?.string()
                    val errorMsg = "查詢失敗: HTTP ${response.code()} - ${errorBodyString ?: "無錯誤內容"}"
                    Log.e(TAG, errorMsg)
                    _errorMessage.postValue(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "查詢時發生錯誤: ${e.message}"
                Log.e(TAG, errorMsg, e)
                _errorMessage.postValue(errorMsg)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun isTokenExpired(): Boolean {
        val expiresIn = accessToken?.expires_in ?: 0L
        return (tokenFetchTime + (expiresIn * 1000L)) < System.currentTimeMillis()
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
                null
            }
        } catch (e: Exception) {
            val errorMsg = "獲取令牌時發生網路錯誤: ${e.message}"
            Log.e(TAG, errorMsg, e)
            null
        }
    }
}
