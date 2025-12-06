package com.ray.trarailwaysalaryapp.data

import com.google.gson.annotations.SerializedName

/**
 * TDX OD（起迄站）時刻表查詢的回應資料模型
 */
data class ODTimetableResponse(
    @SerializedName("TrainTimetables") val trainTimetables: List<ODTrainTimetable>?
)

/**
 * OD 查詢的列車時刻表詳情
 */
data class ODTrainTimetable(
    @SerializedName("TrainDate") val trainDate: String?,        // 列車運行日期
    @SerializedName("DailyTrainInfo") val dailyTrainInfo: DailyTrainInfo?,  // 列車基本資訊
    @SerializedName("OriginStopTime") val originStopTime: ODStopTime?,      // 起程站資訊
    @SerializedName("DestinationStopTime") val destinationStopTime: ODStopTime?  // 到達站資訊
)

/**
 * 列車基本資訊
 */
data class DailyTrainInfo(
    @SerializedName("TrainNo") val trainNo: String,            // 列車號碼
    @SerializedName("Direction") val direction: Int?,          // 行駛方向 (0: 順行, 1: 逆行)
    @SerializedName("TrainTypeName") val trainTypeName: ODNameType?,  // 列車類型名稱
    @SerializedName("TripLine") val tripLine: Int?,            // 山海線 (0: 不經山海線, 1: 山線, 2: 海線)
    @SerializedName("StartingStationName") val startingStationName: ODNameType?, // 列車起點站
    @SerializedName("EndingStationName") val endingStationName: ODNameType?      // 列車終點站
)

/**
 * 站名類型（包含中英文）
 */
data class ODNameType(
    @SerializedName("Zh_tw") val zhTw: String?,
    @SerializedName("En") val en: String?
)

/**
 * OD 查詢的停靠站時間資訊
 */
data class ODStopTime(
    @SerializedName("StationID") val stationID: String,
    @SerializedName("StationName") val stationName: ODNameType?,
    @SerializedName("ArrivalTime") val arrivalTime: String?,   // 抵達時間
    @SerializedName("DepartureTime") val departureTime: String? // 發車時間
)
