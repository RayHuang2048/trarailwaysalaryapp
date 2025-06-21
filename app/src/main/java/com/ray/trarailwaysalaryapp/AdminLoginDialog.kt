package com.ray.trarailwaysalaryapp // 確保這個 package 名稱與您的專案一致

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText
import androidx.lifecycle.lifecycleScope // 導入 lifecycleScope
import kotlinx.coroutines.launch // 導入 launch

class AdminLoginDialog : DialogFragment() {

    private lateinit var editTextAdminEmail: TextInputEditText // 電子郵件輸入框
    private lateinit var editTextAdminPassword: TextInputEditText
    private lateinit var buttonConfirm: Button
    private lateinit var buttonCancel: Button

    // 定義一個介面來將登入結果回傳給 Fragment
    interface LoginResultListener {
        fun onLoginSuccess()
        fun onLoginFailure(message: String)
    }

    var loginResultListener: LoginResultListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 載入對話框的佈局
        val view = inflater.inflate(R.layout.dialog_admin_login, container, false)

        // 初始化 UI 元素
        editTextAdminEmail = view.findViewById(R.id.editTextAdminEmail) // 初始化電子郵件輸入框
        editTextAdminPassword = view.findViewById(R.id.editTextAdminPassword)
        buttonConfirm = view.findViewById(R.id.buttonConfirm)
        buttonCancel = view.findViewById(R.id.buttonCancel)

        // 設定「確定」按鈕的點擊事件
        buttonConfirm.setOnClickListener {
            val email = editTextAdminEmail.text.toString().trim() // 獲取電子郵件輸入
            val password = editTextAdminPassword.text.toString().trim() // 獲取密碼輸入

            if (email.isNotEmpty() && password.isNotEmpty()) {
                lifecycleScope.launch {
                    // 調用 AdminManager 進行登入驗證
                    // 請確保 AdminManager 類別存在並且其 loginAdmin 函數接受兩個 String 參數 (email, password)
                    val success = AdminManager.loginAdmin(email, password)
                    if (success) {
                        loginResultListener?.onLoginSuccess() // 登入成功回呼
                        dismiss() // 關閉對話框
                    } else {
                        loginResultListener?.onLoginFailure("密碼或電子郵件不正確") // 登入失敗回呼
                        Toast.makeText(context, "密碼或電子郵件不正確，請重試", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "請輸入電子郵件和密碼", Toast.LENGTH_SHORT).show()
            }
        }

        // 設定「取消」按鈕的點擊事件
        buttonCancel.setOnClickListener {
            dismiss() // 關閉對話框
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        // 設置對話框的寬度為 match_parent，高度為 wrap_content
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
