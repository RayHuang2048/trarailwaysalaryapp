package com.ray.trarailwaysalaryapp

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.Timestamp

data class AccidentReport(
    @DocumentId
    var id: String = "",             // Firestore Document ID

    var reporterName: String = "",   // <--- 修正：改回非空 String，預設為 ""
    var dateTime: String = "",       // 事故發生時間，由系統預設或手動輸入
    var location: String = "",
    var description: String = "",

    @ServerTimestamp // Firestore 將在文件創建/更新時自動設置此值
    var timestamp: Timestamp? = null,

    var title: String = "",
    var severity: String = "",
    var status: String = "",
    var imageUrl: String = "",
    var pinned: Boolean = false
) {
    // 無引數建構子必須與主建構子的參數數量和型別完全匹配
    constructor() : this(
        "",    // id
        "",    // <--- 修正：reporterName 預設為 ""
        "",    // dateTime
        "",    // location
        "",    // description
        null,  // timestamp
        "",    // title
        "",    // severity
        "",    // status
        "",    // imageUrl
        false
    )
}
