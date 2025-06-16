package com.ray.trarailwaysalaryapp // 確保這裡是你應用程式的實際套件名稱

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts // 用於請求權限
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment // 導航組件
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth // 如果你需要 Firebase 驗證來獲取使用者 ID
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

// 首先，我們定義一個 Companion Object 來處理 Firebase 令牌相關的邏輯。
// 這樣做可以讓代碼更整潔，並且可以在其他地方重複使用。
object FirebaseTokenManager {

    private const val TAG = "FirebaseTokenManager"

    // 這個函數用於獲取 FCM 設備令牌並將其保存到 Firestore
    fun saveDeviceTokenToFirestore() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d(TAG, "Current FCM Token: $token")

            // 你需要一個方式來識別使用者或設備。
            // 這裡假設你使用 Firebase Authentication 來獲取使用者 ID。
            // 如果你的應用程式沒有使用者登入功能，你可以使用一個設備的唯一標識符，
            // 或者將所有令牌儲存在一個固定的文檔中（不建議，管理複雜）。
            val userId = FirebaseAuth.getInstance().currentUser?.uid // 獲取當前登入使用者的 ID

            if (userId != null && token != null) {
                val db = FirebaseFirestore.getInstance()
                // 將令牌儲存在 'users' 集合中對應使用者 ID 的文檔裡。
                // 'deviceTokens' 是一個陣列，使用 FieldValue.arrayUnion 可以避免重複添加相同的令牌。
                db.collection("users").document(userId)
                    .update("deviceTokens", FieldValue.arrayUnion(token))
                    .addOnSuccessListener { Log.d(TAG, "Device token successfully updated for user: $userId") }
                    .addOnFailureListener { e -> Log.w(TAG, "Error updating device token for user: $userId", e) }
            } else {
                Log.d(TAG, "User not logged in or token is null. Cannot save token.")
                // 如果沒有使用者 ID，但你仍想發送通知給所有安裝了 App 的設備，
                // 你可以考慮創建一個 'all_device_tokens' 集合，
                // 然後將每個令牌作為一個單獨的文檔存儲，文檔 ID 可以是令牌本身。
                // 範例 (如果不需要關聯使用者):
                // if (token != null) {
                //     FirebaseFirestore.getInstance().collection("all_device_tokens").document(token)
                //         .set(mapOf("timestamp" to FieldValue.serverTimestamp()))
                //         .addOnSuccessListener { Log.d(TAG, "Device token saved without user ID: $token") }
                //         .addOnFailureListener { e -> Log.w(TAG, "Error saving device token without user ID", e) }
                // }
            }
        }
    }
}


class MainActivity : AppCompatActivity() {

    // 這是用於處理通知權限請求的 launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 權限已授予
            Log.d("NotificationPermission", "POST_NOTIFICATIONS permission granted")
        } else {
            // 權限被拒絕
            Log.w("NotificationPermission", "POST_NOTIFICATIONS permission denied")
            // 你可以在這裡顯示一個解釋，或者引導使用者到設定去手動開啟
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- 處理 FCM 令牌和通知權限的邏輯 ---

        // 在應用程式啟動時，呼叫儲存設備令牌的函數
        // 這會確保即使令牌刷新，最新的令牌也能被上傳到 Firestore
        FirebaseTokenManager.saveDeviceTokenToFirestore()

        // 處理 Android 13 (API 33) 及更高版本的通知權限請求
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 檢查是否已經有通知權限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // 權限已授予，不需要做任何事情
                Log.d("NotificationPermission", "POST_NOTIFICATIONS permission already granted.")
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // 如果App之前被拒絕過一次，且可以解釋為什麼需要這個權限時，會進入這個分支。
                // 在這裡可以顯示一個自定義的對話框，解釋為什麼需要通知權限，
                // 然後再請求權限。
                Log.d("NotificationPermission", "Should show rationale for POST_NOTIFICATIONS permission.")
                // 為了簡潔，這裡直接請求權限。如果你需要更好的用戶體驗，可以彈出對話框。
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // 首次請求權限，或者使用者勾選了「不再提醒」並拒絕了權限。
                // 直接請求權限。
                Log.d("NotificationPermission", "Requesting POST_NOTIFICATIONS permission.")
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // --- 你的 Bottom Navigation 邏輯 ---

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // 關鍵修改在這裡：
        // 正確的方式是先取得 NavHostFragment 實例，再從它獲取 NavController。
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 將 BottomNavigationView 與 NavController 綁定
        bottomNavigationView.setupWithNavController(navController)
    }
}