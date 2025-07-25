// app/src/main/java/com/ray/trarailwaysalaryapp/data/TDXAccessToken.kt
package com.ray.trarailwaysalaryapp.data

import com.google.gson.annotations.SerializedName

data class TDXAccessToken( // 確保這裡是 TDXAccessToken
    @SerializedName("access_token") val access_token: String,
    @SerializedName("expires_in") val expires_in: Long, // token 有效期，單位為秒
    @SerializedName("token_type") val token_type: String,
    @Transient var fetch_time: Long = System.currentTimeMillis() // 紀錄獲取時間，用於判斷是否過期
)