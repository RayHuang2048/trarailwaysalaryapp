package com.ray.trarailwaysalaryapp

import java.util.UUID // 確保有這個 import

// AccidentReport.kt
data class AccidentReport(
    val id: String = UUID.randomUUID().toString(), // 每個回報的唯一 ID
    val reporterName: String = "", // 回報人姓名 (可選)
    val dateTime: String = "",     // 事故發生時間 (格式化後的字串)
    val location: String = "",     // 事故地點
    val description: String = ""   // 事故描述
) {
    // Firebase Firestore 在將文檔反序列化為 Kotlin 物件時，需要一個公共的無參數建構子。
    // 即使 Kotlin data class 通常會自動生成，明確添加能確保兼容性。
    constructor() : this("", "", "", "", "")
}