package com.ray.trarailwaysalaryapp.data

import com.google.gson.annotations.SerializedName

/**
 * TDX API 查詢結果的通用包裝模型。
 * 大部分 TDX API 的回應會將實際的資料列表包裝在一個 JSON 物件中。
 * 這個類別是一個泛型類別，`T` 代表實際資料列表中的元素類型。
 *
 * 範例：TDX LiveBoard API 回傳的 JSON 結構如下：
 * {
 * "UpdateTime": "2025-07-25T07:36:24+08:00",
 * "UpdateInterval": 30,
 * "SrcUpdateTime": "2025-07-25T07:36:00+08:00",
 * "SrcUpdateInterval": 60,
 * "AuthorityCode": "TRA",
 * "TrainLiveBoards": [ // <--- 這裡的鍵名必須與 @SerializedName 完全匹配
 * {
 * "TrainNo": "110",
 * "TrainTypeID": "110M",
 * "TrainTypeCode": "11",
 * "TrainTypeName": { "Zh_tw": "自強(3000)", "En": "Tze-Chiang Ltd. Express(3000)" },
 * "StationID": "4170",
 * "StationName": { "Zh_tw": "善化", "En": "Shanhua" },
 * "TrainStationStatus": 2,
 * "DelayTime": 1,
 * "UpdateTime": "2025-07-25T07:34:10+08:00"
 * }
 * ]
 * }
 */
data class TdxApiResponse<T>( // <--- 這裡必須是泛型 <T>
    // 這個屬性名 (TrainLiveBoards) 現在與 @SerializedName 的值完全一致，
    // 並且與 TDX API 回傳的 JSON response root key 完全匹配。
    @SerializedName("TrainLiveBoards") val TrainLiveBoards: List<T>? // <--- 修正：屬性名也改為 TrainLiveBoards
)

/**
 * 台鐵列車即時動態資訊的核心資料模型。
 * 這個類別的屬性名必須與 TDX API (TrainLiveBoard) 回傳的 JSON 鍵名完全一致（大小寫敏感）。
 * 誤點資訊、列車位置等動態數據會在此模型中。
 */
data class TrainLiveInfo(
    @SerializedName("TrainNo") val TrainNo: String, // 列車號碼 (例如: "110")
    @SerializedName("TrainTypeID") val TrainTypeID: String, // 列車車種代碼 (例如: "110M")
    @SerializedName("TrainTypeCode") val TrainTypeCode: String, // 列車車種代碼 (例如: "11")
    @SerializedName("TrainTypeName") val TrainTypeName: StationName, // 列車車種名稱 (例如: "自強(3000)", "區間車")
    @SerializedName("StationID") val StationID: String?, // 目前所在的車站代碼 (可能為 null，表示列車正在站間行駛)
    @SerializedName("StationName") val StationName: StationName?, // 目前所在的車站名稱 (可能為 null)
    @SerializedName("TrainStationStatus") val TrainStationStatus: Int?, // 列車車站狀態 (例如 2: 離站)
    @SerializedName("DelayTime") val DelayTime: Int, // 誤點時間 (分鐘，正值表示誤點，0 表示準點或提早)
    @SerializedName("UpdateTime") val UpdateTime: String // 資料更新時間 (ISO 8601 格式，例如 "2025-07-25T07:34:10+08:00")
)
