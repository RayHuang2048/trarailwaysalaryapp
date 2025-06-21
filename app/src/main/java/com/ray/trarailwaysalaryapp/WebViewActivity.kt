package com.ray.trarailwaysalaryapp

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback // 導入新的 OnBackPressedCallback

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view) // 假設您的佈局檔案是 activity_web_view.xml

        webView = findViewById(R.id.webViewStatusFull) // 假設您的 WebView ID 是 webView
        webView.webViewClient = WebViewClient() // 確保在同一個應用程式中打開連結
        webView.settings.javaScriptEnabled = true // 啟用 JavaScript

        // 從 Intent 中獲取 URL
        val url = intent.getStringExtra("URL")
        if (url != null) {
            webView.loadUrl(url)
        } else {
            // 如果 URL 為空，顯示錯誤或加載預設頁面
            webView.loadUrl("about:blank") // 加載一個空白頁
            // 或者顯示 Toast 提示錯誤
            // Toast.makeText(this, "無法載入網頁：URL 無效", Toast.LENGTH_LONG).show()
        }

        // --- 處理返回按鈕事件的新的推薦方法 ---
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack() // 如果 WebView 可以返回上一頁，則返回
                } else {
                    isEnabled = false // 如果 WebView 無法返回，則禁用此回呼，讓系統處理
                    onBackPressedDispatcher.onBackPressed() // 讓系統執行預設的返回行為 (關閉 Activity)
                }
            }
        })
        // --- 舊的 onBackPressed() 方法可以移除或註釋掉 ---
    }

    // 舊的 onBackPressed() 方法，現在可以刪除或註釋掉
    // @Deprecated("Deprecated in Java") // Kotlin 自動生成這個註解，可以保留作為參考
    // override fun onBackPressed() {
    //     if (webView.canGoBack()) {
    //         webView.goBack()
    //     } else {
    //         super.onBackPressed()
    //     }
    // }
}
