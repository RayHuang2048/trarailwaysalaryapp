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
    @SerializedName("StopTimes") val stopTimes: List<ODStopTime>?
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
    @SerializedName("StopSequence") val stopSequence: Int?,
    @SerializedName("StationID") val stationID: String?,
    @SerializedName("StationName") val stationName: ODNameType?,
    @SerializedName("ArrivalTime") val arrivalTime: String?,
    @SerializedName("DepartureTime") val departureTime: String?,
    @SerializedName("SuspendedFlag") val suspendedFlag: Int?
)
