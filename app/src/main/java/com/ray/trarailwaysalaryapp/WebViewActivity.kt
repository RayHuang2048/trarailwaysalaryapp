package com.ray.trarailwaysalaryapp

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class WebViewActivity : AppCompatActivity() {

    private lateinit var webViewStatusFull: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        // 初始化 WebView
        webViewStatusFull = findViewById(R.id.webViewStatusFull)

        // 設定 WebViewClient 確保連結在 App 內部打開
        webViewStatusFull.webViewClient = WebViewClient()

        // 啟用 JavaScript
        webViewStatusFull.settings.javaScriptEnabled = true

        // (可選) 啟用縮放功能
        webViewStatusFull.settings.setSupportZoom(true)
        webViewStatusFull.settings.builtInZoomControls = true
        webViewStatusFull.settings.displayZoomControls = false // 不顯示內建縮放控制鈕

        // 載入網頁
        webViewStatusFull.loadUrl("https://www.railway.gov.tw/tra-tip-web/tip/tip007/tip711/blockList")
    }

    // 處理返回鍵，讓 WebView 返回上一個網頁而不是直接退出 Activity
    override fun onBackPressed() {
        if (webViewStatusFull.canGoBack()) {
            webViewStatusFull.goBack()
        } else {
            super.onBackPressed()
        }
    }
}