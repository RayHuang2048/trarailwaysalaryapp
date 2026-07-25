package com.ray.trarailwaysalaryapp.data

import com.google.gson.annotations.SerializedName

/**
 * TDX OD（起迄站）時刻表查詢的回應資料模型
 * 根據實際 API 回應結構調整
 */
data class ODTimetableResponse(
    @SerializedName("TrainTimetables") val trainTimetables: List<ODTrainTimetable>?,
    @SerializedName("TrainDate") val trainDate: String?
)

/**
 * OD 查詢的列車時刻表詳情
 * 注意：API 實際回傳的是 TrainInfo 和 StopTimes，而不是 DailyTrainInfo
 */
data class ODTrainTimetable(
    @SerializedName("TrainInfo") val trainInfo: TrainInfoData?,
    @SerializedName("StopTimes") val stopTimes: List<ODStopTime>?,
    var fare: Int? = null // 新增：用於存儲媒合後的票價
)

/**
 * 列車基本資訊
 */
data class TrainInfoData(
    @SerializedName("TrainNo") val trainNo: String?,
    @SerializedName("Direction") val direction: Int?,
    @SerializedName("TrainTypeID") val trainTypeID: String?,
    @SerializedName("TrainTypeCode") val trainTypeCode: String?,
    @SerializedName("TrainTypeName") val trainTypeName: ODNameType?,
    @SerializedName("TripHeadSign") val tripHeadSign: String?,
    @SerializedName("StartingStationID") val startingStationID: String?,
    @SerializedName("StartingStationName") val startingStationName: ODNameType?,
    @SerializedName("EndingStationID") val endingStationID: String?,
    @SerializedName("EndingStationName") val endingStationName: ODNameType?,
    @SerializedName("TripLine") val tripLine: Int?,
    @SerializedName("Note") val note: String?
)

/**
 * TDX V3 ODFare 回應包裝
 */
data class ODFareResponse(
    @SerializedName("ODFares") val odFares: List<ODFare>?
)

/**
 * 票價資料 (V3 結構)
 */
data class ODFare(
    @SerializedName("OriginStationID") val originStationID: String?,
    @SerializedName("DestinationStationID") val destinationStationID: String?,
    @SerializedName("TrainType") val trainType: Int?, // 新增：用於區分車種的整數 ID
    @SerializedName("TravelDistance") val travelDistance: Double?, // 新增：用於判斷最短路徑票價
    @SerializedName("Fares") val fares: List<Fare>?
)

data class Fare(
    @SerializedName("TicketType") val ticketType: Int?,
    @SerializedName("FareClass") val fareClass: Int?,
    @SerializedName("CabinClass") val cabinClass: Int?,
    @SerializedName("Price") val price: Int?
)

/**
 * 車次基本資訊 (中英名稱)
 */
data class ODNameType(
    @SerializedName("Zh_tw") val zhTw: String?,
    @SerializedName("En") val en: String?
)

/**
 * OD 查詢的停靠站時間資訊
 */
data class ODStopTime(
    @SerializedName("StopSequence") val stopSequence: Int?,
    @SerializedName("StationID") val stationID: String?,
    @SerializedName("StationName") val stationName: ODNameType?,
    @SerializedName("ArrivalTime") val arrivalTime: String?,
    @SerializedName("DepartureTime") val departureTime: String?,
    @SerializedName("SuspendedFlag") val suspendedFlag: Int?
)
