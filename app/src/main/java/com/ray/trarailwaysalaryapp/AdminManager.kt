package com.ray.trarailwaysalaryapp

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await // 導入這個 extension function

object AdminManager { // 使用 object 實現單例模式

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_IS_ADMIN_LOGGED_IN = "is_admin_logged_in"
    private const val TAG = "AdminManager"

    private lateinit var auth: FirebaseAuth // Firebase Authentication 實例
    private lateinit var sharedPrefs: SharedPreferences

    // 使用 MutableStateFlow 來觀察管理員登入狀態的變化
    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    // 初始化方法，在應用程式啟動時呼叫一次
    fun initialize(context: Context) {
        auth = FirebaseAuth.getInstance()
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 首次初始化時，檢查 SharedPrefs 中的狀態並更新 Flow
        // 這確保應用程式重啟後，如果之前是登入狀態，可以恢復
        val storedLoginStatus = sharedPrefs.getBoolean(KEY_IS_ADMIN_LOGGED_IN, false)
        // 額外檢查 Firebase 本身的登入狀態，保持兩者一致
        _isAdminLoggedIn.value = storedLoginStatus && (auth.currentUser != null)
        Log.d(TAG, "Initialized. Admin logged in: ${_isAdminLoggedIn.value}")
    }

    /**
     * 嘗試以管理員帳戶登入。
     * @return 成功登入時返回 true，否則返回 false。
     */
    suspend fun loginAdmin(email: String, password: String): Boolean {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val success = result.user != null
            if (success) {
                // 將管理員登入狀態存入 SharedPreferences
                sharedPrefs.edit().putBoolean(KEY_IS_ADMIN_LOGGED_IN, true).apply()
                _isAdminLoggedIn.value = true // 更新狀態 Flow
                Log.d(TAG, "Admin login successful for $email")
            } else {
                Log.w(TAG, "Admin login failed for $email: User is null.")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Admin login failed: ${e.message}", e)
            _isAdminLoggedIn.value = false // 確保失敗時更新狀態
            false
        }
    }

    /**
     * 登出管理員帳戶。
     */
    fun logoutAdmin() {
        auth.signOut() // Firebase 登出
        sharedPrefs.edit().putBoolean(KEY_IS_ADMIN_LOGGED_IN, false).apply() // 清除 SharedPreferences 狀態
        _isAdminLoggedIn.value = false // 更新狀態 Flow
        Log.d(TAG, "Admin logged out.")
    }

    // 提供一個判斷是否是管理員的快捷方法，基於 Flow 的最新值
    fun isAdmin(): Boolean {
        return _isAdminLoggedIn.value
    }
}