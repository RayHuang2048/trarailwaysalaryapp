// app/src/main/java/com/ray/trarailwaysalaryapp/data/TDXDataClasses.kt

package com.ray.trarailwaysalaryapp.data

// 用於解析 TDX API 回傳的 Access Token
data class TDXAccessToken(
    val access_token: String,
    val expires_in: Int, // Token的有效時間，單位為秒
    val token_type: String
)

// 用於解析列車動態資料中的名稱 (中/英文)
data class NameType(
    val Zh_tw: String, // 中文名稱
    val En: String     // 英文名稱
)

// 用於解析單一列車的即時動態資料 (簡化版，您可以根據實際API回應擴展)
data class TrainLiveData(
    val TrainNo: String,          // 列車號碼 (例如: "110")
    val TrainTypeID: String,      // 列車車種代碼 (例如: "1")
    val TrainTypeName: NameType,  // 列車車種名稱 (例如: "自強號")
    val TrainType: Int,           // 列車種類代號 (例如: 1=自強, 2=莒光...)
    val Direction: Int,           // 順逆行 (0: 順行, 1: 逆行)
    val TripHeadsign: String,     // 行駛方向站名 (例如: "潮州")
    val StartingStationName: NameType, // 起始站名稱 (例如: "七堵")
    val EndingStationName: NameType,   // 終點站名稱 (例如: "潮州")
    val DelayTime: Int,           // 誤點時間 (分鐘)
    val StationID: String?,       // 目前所在車站代碼 (如果列車停靠在站)
    val StationName: NameType?,   // 目前所在車站名稱
    val UpdateTime: String,       // 資料更新時間 (UTC)
    val SrcUpdateTime: String     // 來源更新時間 (UTC)
)
