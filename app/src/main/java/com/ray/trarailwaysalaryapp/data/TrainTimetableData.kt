package com.ray.trarailwaysalaryapp.data

import com.google.gson.annotations.SerializedName

// NameType 已經在 TrainLiveInfo.kt 中定義，這裡不需要重複定義。
// 如果您還沒有這個檔案，請確保它存在於 data/ 目錄下。
/*
data class NameType(
    @SerializedName("Zh_tw") val Zh_tw: String, // 中文名稱
    @SerializedName("En") val En: String // 英文名稱
)
*/

/**
 * TDX 列車時刻表 API 的頂層回應模型。
 * 根據 TDX API DailyTrainInfo/TrainTimetable 的回應結構。
 */
data class TrainTimetableResponse(
    @SerializedName("TrainTimetables") val TrainTimetables: List<TrainTimetableDetail>?
)

/**
 * 列車時刻表的詳細資訊，包含列車號碼、日期和停靠站列表。
 */
data class TrainTimetableDetail(
    @SerializedName("TrainNo") val TrainNo: String, // 列車號碼
    @SerializedName("TrainDate") val TrainDate: String, // 列車運行日期 (YYYY-MM-DD)
    @SerializedName("StopTimes") val StopTimes: List<StopTime> // 列車的停靠站時間列表
)

/**
 * 列車停靠站的詳細時間資訊。
 */
data class StopTime(
    @SerializedName("StationID") val StationID: String, // 車站代碼
    @SerializedName("StationName") val StationName: NameType, // 車站名稱 (包含中英文)
    @SerializedName("ArrivalTime") val ArrivalTime: String?, // 預計抵達時間 (HH:mm，可能為 null 如果是起點站)
    @SerializedName("DepartureTime") val DepartureTime: String?, // 預計出發時間 (HH:mm，可能為 null 如果是終點站)
    @SerializedName("Sequence") val Sequence: Int // 停靠站的順序
)
