package com.ray.trarailwaysalaryapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log // 確保導入 Log

class StatusDisplayFragment : Fragment() {

    private val TAG = "StatusDisplayFrag" // 為 Logcat 訊息添加標籤
    private lateinit var webViewStatus: WebView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: StatusDisplayFragment created.") // Fragment 創建時的日誌
        val view = inflater.inflate(R.layout.fragment_status_display, container, false)

        webViewStatus = view.findViewById(R.id.webViewStatus)
        webViewStatus.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "onPageStarted: WebView started loading URL: $url") // 頁面開始載入時的日誌
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "onPageFinished: WebView finished loading URL: $url") // 頁面載入完成時的日誌
            }

            override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                super.onReceivedError(view, request, error)
                // 檢查錯誤碼和描述，用於偵錯
                val errorMessage = "Error loading page: ${error?.description} (Code: ${error?.errorCode})"
                Log.e(TAG, "onReceivedError: $errorMessage") // 載入錯誤時的日誌
                // 在這裡可以顯示一個錯誤訊息給使用者，例如一個 TextView
                // view?.loadData("<html><body><h1>網頁載入失敗</h1><p>$errorMessage</p></body></html>", "text/html", "utf-8")
            }
        }

        webViewStatus.settings.javaScriptEnabled = true
        webViewStatus.settings.domStorageEnabled = true
        webViewStatus.settings.setSupportZoom(true)
        webViewStatus.settings.builtInZoomControls = true
        webViewStatus.settings.displayZoomControls = false

        val urlToLoad = "https://www.railway.gov.tw/tra-tip-web/tip/tip007/tip711/blockList"
        Log.d(TAG, "Attempting to load URL: $urlToLoad") // 準備加載 URL 的日誌
        webViewStatus.loadUrl(urlToLoad)

        return view
    }

    // 確保 WebView 在生命週期中得到適當管理
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: StatusDisplayFragment resumed.")
        webViewStatus.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: StatusDisplayFragment paused.")
        webViewStatus.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: StatusDisplayFragment view destroyed.")
        // 銷毀 WebView 以防止記憶體洩漏
        // 移除所有子視圖
        (webViewStatus.parent as? ViewGroup)?.removeView(webViewStatus)
        webViewStatus.destroy()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: StatusDisplayFragment destroyed.")
    }
}