package com.ray.trarailwaysalaryapp

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class AccidentReportingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accident_reporting) // 設定新的布局檔案

        // 設定ActionBar標題
        supportActionBar?.title = "台鐵事故通報平台" // 標題可以根據實際顯示內容調整

        val webView: WebView = findViewById(R.id.webView_accident_report)
        webView.settings.javaScriptEnabled = true // 啟用 JavaScript
        webView.webViewClient = WebViewClient() // 確保在應用程式內部開啟連結，而不是外部瀏覽器

        // 加載台鐵事故通報平台的網址
        // 已更新為您提供的新網址
        val url = "https://www.railway.gov.tw/tra-tip-web/tip/tip007/tip711/blockList"
        webView.loadUrl(url)
    }
}