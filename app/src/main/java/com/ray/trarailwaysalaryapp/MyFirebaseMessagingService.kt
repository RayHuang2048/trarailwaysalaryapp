package com.ray.trarailwaysalaryapp // *** 務必確認這裡與你的應用程式套件名稱一致！ ***

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

// 引入你的 MainActivity (用於點擊通知後跳轉)
import com.ray.trarailwaysalaryapp.MainActivity // 確認導入你的主活動

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // 定義通知通道的相關資訊
    private val CHANNEL_ID = "accident_report_channel" // 通知通道的唯一 ID
    private val CHANNEL_NAME = "事故通報通知" // 使用者可見的通道名稱
    private val CHANNEL_DESCRIPTION = "用於接收新的事故通報或更新通知" // 使用者可見的通道描述

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // 檢查訊息是否包含通知酬載 (notification payload)
        // 這是 Firebase Console 中「通知」頁面發送的訊息類型，或 FCM "notification" 字段。
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message Notification Title: ${notification.title}")
            Log.d(TAG, "Message Notification Body: ${notification.body}")
            // 收到通知酬載時，調用發送通知的方法
            // 我們將通知標題、內容和數據酬載一同傳遞
            sendNotification(notification.title, notification.body, remoteMessage.data)
        }

        // 檢查訊息是否包含資料酬載 (data payload)
        // 這是 FCM "data" 字段，通常用於發送自定義數據，即使應用程式在前台也能收到。
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            // 如果只有數據酬載，並且沒有通知酬載（這是常見的後台靜默通知或自定義通知），
            // 我們也需要手動創建通知。
            // 這裡假設如果沒有 notification 酬載，則從 data 酬載中獲取 title 和 body。
            if (remoteMessage.notification == null) {
                sendNotification(
                    remoteMessage.data["title"], // 假設數據酬載中有 'title' 字段
                    remoteMessage.data["body"],  // 假設數據酬載中有 'body' 字段
                    remoteMessage.data
                )
            }
            // 你可以在這裡處理自定義數據，例如根據 `data["type"]` 決定跳轉到哪個頁面
            val reportId = remoteMessage.data["reportId"]
            val type = remoteMessage.data["type"]
            Log.d(TAG, "Received custom data: Report ID=$reportId, Type=$type")
            // 根據這些數據執行相應的邏輯 (例如：跳轉到事故詳情頁面)
            // 注意：跳轉邏輯通常在點擊通知的 PendingIntent 中實現
        }
    }

    // 當設備令牌刷新時會呼叫此方法。
    // 這是一個很重要的回調，因為令牌可能會在 App 安裝後變化。
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        // 每當令牌刷新或 App 首次安裝時，都需要將新的令牌發送到你的 Firestore 中。
        // 我們調用之前在 MainActivity 中定義的 FirebaseTokenManager.saveDeviceTokenToFirestore()
        FirebaseTokenManager.saveDeviceTokenToFirestore()
    }

    /**
     * 創建並顯示系統通知。
     * @param title 通知標題。
     * @param body 通知內容。
     * @param data 附加到通知的自定義數據，當點擊通知時可以傳遞給目標 Activity。
     */
    private fun sendNotification(title: String?, body: String?, data: Map<String, String>?) {
        // 當用戶點擊通知時，要啟動的 Activity
        val intent = Intent(this, MainActivity::class.java).apply {
            // 清除之前的 Activity 堆棧，確保新的 Activity 是應用程式的根 Activity
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // 將自定義數據附加到 Intent，以便 MainActivity 可以獲取並處理
            data?.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        // PendingIntent 用於包裝 Intent，以便系統可以在未來執行它
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, // Request code，多個通知可以使用不同的 ID
            intent,
            // FLAG_ONE_SHOT: 這個 PendingIntent 只能使用一次。
            // FLAG_IMMUTABLE: 從 Android 12 (API 31) 開始，這是必需的，PendingIntent 是不可變的。
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // 構建通知
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // *** 替換為你自己的通知小圖標！通常是白色透明背景的圖標 ***
            .setContentTitle(title ?: "新通報") // 如果沒有標題，則使用預設值
            .setContentText(body ?: "您有一條新的事故通報。") // 如果沒有內容，則使用預設值
            .setAutoCancel(true) // 點擊通知後自動移除
            .setContentIntent(pendingIntent) // 設置點擊行為
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 設置通知優先級

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 對於 Android 8.0 (API level 26) 及更高版本，需要建立通知通道
        // 通知通道讓使用者可以對不同類型的通知進行更細粒度的控制
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // 高重要性會彈出 Head-up 通知
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 發送通知。0 是通知的 ID，如果你有多個通知，可以使用不同的 ID 來區分它們，
        // 或者重複使用同一個 ID 以更新之前的通知。
        notificationManager.notify(0, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}