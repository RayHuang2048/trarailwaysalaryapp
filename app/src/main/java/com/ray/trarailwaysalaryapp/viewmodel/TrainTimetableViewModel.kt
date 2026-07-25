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

    // 韏瑞?蝡??圈?蝡?
    private val _startStation = MutableLiveData<Station?>()
    val startStation: LiveData<Station?> = _startStation

    private val _arrivalStation = MutableLiveData<Station?>()
    val arrivalStation: LiveData<Station?> = _arrivalStation

    // OD ?亥岷蝯?
    private val _odTimetableResults = MutableLiveData<List<ODTrainTimetable>>()
    val odTimetableResults: LiveData<List<ODTrainTimetable>> = _odTimetableResults

    // ?航炊閮
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // ?臬甇??亥岷
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
     * ?亥岷?拍?銋????餉”
     */
    fun queryODTimetable() {
        val origin = _startStation.value
        val destination = _arrivalStation.value

        if (origin == null) {
            _errorMessage.postValue("Please select origin station")
            return
        }

        if (destination == null) {
            _errorMessage.postValue("Please select destination station")
            return
        }

        _isLoading.postValue(true)
        _odTimetableResults.postValue(emptyList())
        _errorMessage.postValue("")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                var currentToken = accessToken?.access_token
                if (currentToken == null || isTokenExpired()) {
                    Log.d(TAG, "Access Token expired, fetching...")
                    currentToken = getAccessToken()
                }

                if (currentToken == null) {
                    _errorMessage.postValue("Failed to get access token")
                    _isLoading.postValue(false)
                    return@launch
                }

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())

                Log.d(TAG, "Query OD timetable ${origin.stationID} -> ${destination.stationID}, date: $today")

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

                    Log.d(TAG, "Total timetables: ${timetables.size}")

                    val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    val filteredTimetables = timetables
                        .filter { timetable ->
                            val stopTimes = timetable.stopTimes
                            if (stopTimes.isNullOrEmpty() || stopTimes.size < 2) return@filter false

                            val originStop = stopTimes.first()
                            val departureTime = originStop.departureTime ?: return@filter false

                            departureTime >= currentTime
                        }
                        .sortedBy {
                            it.stopTimes?.firstOrNull()?.departureTime ?: "99:99"
                        }

                    if (filteredTimetables.isNotEmpty()) {
                        try {
                            Log.d(TAG, "Query ODFare: ${origin.stationID} -> ${destination.stationID}")
                            val fareResponse = tdxApiService.getODFare(
                                authorization = "Bearer $currentToken",
                                originStationId = origin.stationID,
                                destinationStationId = destination.stationID
                            )

                            if (fareResponse.isSuccessful) {
                                val fareRoot = fareResponse.body()
                                val odFares = fareRoot?.odFares
                                if (!odFares.isNullOrEmpty()) {
                                    val fareDebug = odFares.joinToString(separator = " | ") { od ->
                                        val standard = od.fares?.find {
                                            it.ticketType == 1 && it.fareClass == 1 && it.cabinClass == 1
                                        }?.price
                                        val anyPrice = od.fares?.firstOrNull { it.price != null }?.price
                                        "type=${od.trainType}, dist=${od.travelDistance}, standard=$standard, any=$anyPrice"
                                    }
                                    Log.d(TAG, "ODFare debug: $fareDebug")
                                    Log.d(TAG, "ODFare count: ${odFares.size}")

                                    filteredTimetables.forEach { trainTimetable ->
                                        val trainTypeID = trainTimetable.trainInfo?.trainTypeID ?: ""
                                        val trainTypeName = trainTimetable.trainInfo?.trainTypeName?.zhTw ?: ""
                                        trainTimetable.fare =
                                            findPriceForTrainType(trainTypeID, trainTypeName, odFares)
                                        Log.d(
                                            TAG,
                                            "Train ${trainTimetable.trainInfo?.trainNo} ($trainTypeName, ID: $trainTypeID) fare: ${trainTimetable.fare}"
                                        )
                                    }
                                } else {
                                    Log.w(TAG, "ODFare response empty")
                                }
                            } else {
                                Log.e(TAG, "ODFare request failed: ${fareResponse.code()}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "ODFare request exception: ${e.message}", e)
                        }
                    }

                    _odTimetableResults.postValue(filteredTimetables)

                    if (filteredTimetables.isEmpty()) {
                        if (timetables.isEmpty()) {
                            _errorMessage.postValue("No timetable data")
                        } else {
                            _errorMessage.postValue("No upcoming trains")
                        }
                    } else {
                        Log.d(TAG, "Filtered timetables: ${filteredTimetables.size}")
                    }
                } else {
                    val errorBodyString = response.errorBody()?.string()
                    val errorMsg = "OD timetable failed: HTTP ${response.code()} - ${errorBodyString ?: "no body"}"
                    Log.e(TAG, errorMsg)
                    _errorMessage.postValue(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "OD timetable exception: ${e.message}"
                Log.e(TAG, errorMsg, e)
                _errorMessage.postValue(errorMsg)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * ?寞?頠活蝔桅? ID??蝔梯?蟡典?”嚗??曉???蟡典
     * TDX V3 ODFares 蝯?銝哨?TrainType ?舀?賂?1:憭芷陌?? 2:?格??? 3:?芸撥, 4:??, 5:敺抵?, 6:??? 7:??翰
     */
    private fun findPriceForTrainType(rawTrainTypeID: String, trainTypeName: String, odFares: List<com.ray.trarailwaysalaryapp.data.ODFare>): Int? {
        val trainTypeID = rawTrainTypeID.trim()
        
        // 1. Prefer name-based mapping (more stable than IDs)
        val targetTypes = when {
            trainTypeName.contains("太魯閣") -> listOf(1, 3, 2, 6)
            trainTypeName.contains("普悠瑪") -> listOf(2, 3, 1, 6)
            trainTypeName.contains("自強") -> listOf(3, 1, 2, 6)
            trainTypeName.contains("莒光") -> listOf(4, 5, 2, 3, 6)
            trainTypeName.contains("復興") -> listOf(5, 4, 6)
            trainTypeName.contains("區間") -> listOf(6, 7, 5, 2)

            // Fallback: map by train type ID code
            trainTypeID == "1102" || trainTypeID == "110B" -> listOf(1, 3, 2, 6)
            trainTypeID == "1107" || trainTypeID == "1108" -> listOf(2, 3, 1, 6)
            trainTypeID.startsWith("110") || (trainTypeID.startsWith("111") && trainTypeID != "1110") ->
                listOf(3, 1, 2, 6)
            trainTypeID == "1110" || trainTypeID.startsWith("112") -> listOf(4, 5, 2, 3, 6)
            trainTypeID.startsWith("113") || trainTypeID.startsWith("114") -> listOf(6, 7, 5, 2)
            else -> listOf(6, 7, 5, 3, 4, 1, 2)
        }

        Log.d(TAG, "Match trainTypeID=$trainTypeID name=$trainTypeName order=$targetTypes")
        
        // 2. 靘??摨??曉?? ODFare
        fun extractPrice(odFare: com.ray.trarailwaysalaryapp.data.ODFare): Int? {
            val standard = odFare.fares?.find {
                it.ticketType == 1 && it.fareClass == 1 && it.cabinClass == 1
            }?.price
            return standard ?: odFare.fares?.firstOrNull { it.price != null }?.price
        }

        var bestODFare: com.ray.trarailwaysalaryapp.data.ODFare? = null
        for (type in targetTypes) {
            val matchedWithPrice = odFares.filter { it.trainType == type }
                .sortedBy { it.travelDistance ?: Double.MAX_VALUE }
                .firstOrNull { extractPrice(it) != null }
            if (matchedWithPrice != null) {
                bestODFare = matchedWithPrice
                Log.d(TAG, "Matched trainType=$type (price ok)")
                break
            }
        }

        if (bestODFare == null) {
            for (type in targetTypes) {
                val matched = odFares.filter { it.trainType == type }
                    .sortedBy { it.travelDistance ?: Double.MAX_VALUE }
                    .firstOrNull()
                if (matched != null) {
                    bestODFare = matched
                    Log.d(TAG, "Matched trainType=$type")
                    break
                }
            }
        }        
        // Extra fallback for Tze-Chiang class
        if (bestODFare == null && trainTypeName.contains("自強")) {
            bestODFare = odFares.filter { it.trainType in listOf(1, 2, 3) }
                .sortedBy { it.travelDistance ?: Double.MAX_VALUE }
                .firstOrNull { extractPrice(it) != null }
            if (bestODFare == null) {
                bestODFare = odFares.filter { it.trainType in listOf(1, 2, 3) }
                    .sortedBy { it.travelDistance ?: Double.MAX_VALUE }
                    .firstOrNull()
            }
            if (bestODFare != null) Log.d(TAG, "Fallback matched (Tze-Chiang): ${bestODFare.trainType}")
        }        
        // Final fallback: shortest travel distance
        if (bestODFare == null && odFares.isNotEmpty()) {
            bestODFare = odFares
                .sortedBy { it.travelDistance ?: Double.MAX_VALUE }
                .firstOrNull { extractPrice(it) != null }
                ?: odFares.minByOrNull { it.travelDistance ?: Double.MAX_VALUE }
            Log.d(TAG, "Final fallback trainType=${bestODFare?.trainType}")
        }            
        // 3. 撠撠?蟡典 (?函巨/?犖 TicketType 1, ?桅漣 FareClass 1, 銝?祈?撱?CabinClass 1)
        val price = bestODFare?.let { extractPrice(it) }
        
        if (price == null && bestODFare != null) {
            Log.w(TAG, "Matched trainType=${bestODFare.trainType} but standard fare not found")
            return bestODFare.fares?.firstOrNull { it.price != null }?.price
        }
        
        return price
    }

    private fun isTokenExpired(): Boolean {
        val expiresIn = accessToken?.expires_in ?: 0L
        return (tokenFetchTime + (expiresIn * 1000L)) < System.currentTimeMillis()
    }

    private suspend fun getAccessToken(): String? {
        Log.d(TAG, "Requesting access token...")
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
                    Log.d(TAG, "Access token received")
                    token.access_token
                }
            } else {
                val errorMsg = "Access token failed: ${response.code} ${response.message} - $responseBodyString"
                Log.e(TAG, errorMsg)
                null
            }
        } catch (e: Exception) {
            val errorMsg = "Access token exception: ${e.message}"
            Log.e(TAG, errorMsg, e)
            null
        }
    }
}




