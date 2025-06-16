package com.ray.trarailwaysalaryapp

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import android.util.Log

class SponsorFragment : Fragment(), PurchasesUpdatedListener {

    private val TAG = "SponsorFrag" // Logcat 標籤

    private lateinit var buyAppButton: Button
    private lateinit var btnShareViaLine: Button
    private lateinit var btnContactAuthor: Button // 聯絡作者 Email 按鈕

    private lateinit var billingClient: BillingClient
    private var productDetails: ProductDetails? = null // 用於儲存產品詳情

    // 【重要】請確保這個 PRODUCT_ID 與您在 Google Play Console 中設定的「可重複消費」產品 ID 完全一致。
    private val PRODUCT_ID = "ray_trarailway_salary_app_sponsorship_49ntd_consumable"

    // 【重要】請在這裡定義作者的 Email 地址
    private val AUTHOR_EMAIL = "your.actual.email@example.com" // <--- *** 請務必在這裡修改為您實際的 Email 地址！ ***

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: SponsorFragment 建立。")
        val view = inflater.inflate(R.layout.fragment_sponsor, container, false)

        buyAppButton = view.findViewById(R.id.buyAppButton)
        btnShareViaLine = view.findViewById(R.id.btnShareViaLine)
        btnContactAuthor = view.findViewById(R.id.btnContactAuthor) // 初始化聯絡作者 Email 按鈕

        buyAppButton.setOnClickListener {
            Log.d(TAG, "buyAppButton 被點擊。啟動購買流程...")
            launchPurchaseFlow()
        }

        btnShareViaLine.setOnClickListener {
            Log.d(TAG, "btnShareViaLine 被點擊。啟動 Line 分享流程...")
            shareAppViaLine()
        }

        // --- 聯絡作者 Email 按鈕的點擊事件 ---
        btnContactAuthor.setOnClickListener {
            Log.d(TAG, "btnContactAuthor 被點擊。啟動 Email 應用程式...")
            sendEmailToAuthor()
        }
        // --- 結束聯絡作者 Email 按鈕 ---

        // 初始化 BillingClient
        billingClient = BillingClient.newBuilder(requireContext())
            .setListener(this)
            .enablePendingPurchases()
            .build()

        connectToGooglePlayBilling()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: SponsorFragment 視圖已建立。")
    }

    private fun connectToGooglePlayBilling() {
        Log.d(TAG, "connectToGooglePlayBilling: 正在啟動連線...")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "onBillingSetupFinished: Billing 服務連線成功。")
                    queryProductDetails()
                    queryPurchases()
                } else {
                    Log.e(TAG, "onBillingSetupFinished: Billing 服務連線失敗: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                    Toast.makeText(requireContext(), "Google Play Billing 服務連線失敗: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
                    activity?.runOnUiThread {
                        buyAppButton.isEnabled = false
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "onBillingServiceDisconnected: Billing 服務斷開。正在嘗試重新連線...")
                Toast.makeText(requireContext(), "Google Play Billing 服務斷開，嘗試重新連線...", Toast.LENGTH_SHORT).show()
                connectToGooglePlayBilling()
            }
        })
    }

    private fun queryProductDetails() {
        Log.d(TAG, "queryProductDetails: 正在查詢產品 ID: $PRODUCT_ID 的詳情。")
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productsDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productsDetailsList.isNotEmpty()) {
                productDetails = productsDetailsList[0]
                Log.i(TAG, "queryProductDetails: 產品詳情獲取成功。價格: ${productDetails?.oneTimePurchaseOfferDetails?.formattedPrice}")
                activity?.runOnUiThread {
                    buyAppButton.text = "贊助應用程式 (${productDetails?.oneTimePurchaseOfferDetails?.formattedPrice})"
                    buyAppButton.isEnabled = true
                }
            } else {
                Log.e(TAG, "queryProductDetails: 無法獲取產品資訊: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                Toast.makeText(requireContext(), "無法獲取產品資訊: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
                activity?.runOnUiThread {
                    buyAppButton.isEnabled = false
                }
            }
        }
    }

    private fun launchPurchaseFlow() {
        productDetails?.let {
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(it)
                    .build()
            )
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            Log.d(TAG, "launchPurchaseFlow: 正在啟動產品: ${it.productId} 的購買流程。")
            billingClient.launchBillingFlow(requireActivity(), billingFlowParams)
        } ?: run {
            Log.w(TAG, "launchPurchaseFlow: 產品詳情尚未載入，無法啟動購買。")
            Toast.makeText(requireContext(), "產品資訊尚未加載，請稍候", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        Log.d(TAG, "onPurchasesUpdated: 結果碼: ${billingResult.responseCode}, 除錯訊息: ${billingResult.debugMessage}")
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                Log.d(TAG, "onPurchasesUpdated: 正在處理購買: ${purchase.orderId}")
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "onPurchasesUpdated: 購買被使用者取消。")
            Toast.makeText(requireContext(), "購買已取消", Toast.LENGTH_SHORT).show()
        } else {
            Log.e(TAG, "onPurchasesUpdated: 購買發生錯誤: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            Toast.makeText(requireContext(), "購買發生錯誤: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        Log.d(TAG, "handlePurchase: 購買狀態: ${purchase.purchaseState}，訂單 ID: ${purchase.orderId}")
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            Toast.makeText(requireContext(), "感謝您的購買！", Toast.LENGTH_LONG).show()
            Log.i(TAG, "handlePurchase: 購買成功！訂單 ID: ${purchase.orderId}")

            if (!purchase.isAcknowledged) {
                Log.d(TAG, "handlePurchase: 購買尚未確認，正在確認中。")
                acknowledgePurchase(purchase)
            } else {
                Log.d(TAG, "handlePurchase: 購買已確認，繼續消費。")
                consumePurchase(purchase)
            }
            activity?.runOnUiThread {
                buyAppButton.text = "贊助應用程式 (${productDetails?.oneTimePurchaseOfferDetails?.formattedPrice})"
                buyAppButton.isEnabled = true
            }

        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "handlePurchase: 購買正在待處理中，訂單 ID: ${purchase.orderId}")
            Toast.makeText(requireContext(), "購買正在處理中，請稍候...", Toast.LENGTH_LONG).show()
            activity?.runOnUiThread {
                buyAppButton.isEnabled = false
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
            Log.w(TAG, "handlePurchase: 購買處於未指定狀態，訂單 ID: ${purchase.orderId}")
            Toast.makeText(requireContext(), "購買狀態不明", Toast.LENGTH_LONG).show()
            activity?.runOnUiThread {
                buyAppButton.isEnabled = true
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(TAG, "acknowledgePurchase: 購買已成功確認，訂單 ID: ${purchase.orderId}")
                consumePurchase(purchase)
            } else {
                Log.e(TAG, "acknowledgePurchase: 購買確認失敗: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                Toast.makeText(requireContext(), "購買確認失敗: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun consumePurchase(purchase: Purchase) {
        Log.d(TAG, "consumePurchase: 正在消費購買: ${purchase.purchaseToken}")
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams) { billingResult, purchaseToken ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(TAG, "consumePurchase: 購買成功消費，代幣: $purchaseToken")
                Toast.makeText(requireContext(), "贊助已成功！感謝！", Toast.LENGTH_SHORT).show()
                activity?.runOnUiThread {
                    buyAppButton.text = "贊助應用程式 (${productDetails?.oneTimePurchaseOfferDetails?.formattedPrice})"
                    buyAppButton.isEnabled = true
                }
            } else {
                Log.e(TAG, "consumePurchase: 消費購買失敗: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                Toast.makeText(requireContext(), "贊助消費失敗: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun queryPurchases() {
        Log.d(TAG, "queryPurchases: 正在查詢使用者的 INAPP 歷史購買。")
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "queryPurchases: INAPP 歷史購買查詢成功。找到 ${purchaseList.size} 筆購買。")
                for (purchase in purchaseList) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        Log.d(TAG, "queryPurchases: 找到已購買項目: ${purchase.orderId}。已確認: ${purchase.isAcknowledged}")
                        if (!purchase.isAcknowledged) {
                            Log.d(TAG, "queryPurchases: 找到未確認的購買: ${purchase.orderId}，正在確認並消費。")
                            acknowledgePurchase(purchase)
                        } else {
                            Log.d(TAG, "queryPurchases: 找到已確認的購買: ${purchase.orderId}。正在嘗試消費以防萬一。")
                            consumePurchase(purchase)
                        }
                    }
                }
            } else {
                Log.e(TAG, "queryPurchases: 查詢購買歷史失敗: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                Toast.makeText(requireContext(), "查詢購買歷史失敗: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: SponsorFragment 恢復。")
        if (billingClient.isReady) {
            queryPurchases()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: SponsorFragment 暫停。")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: SponsorFragment 視圖銷毀。如果已連線，則斷開 BillingClient 連線。")
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: SponsorFragment 銷毀。")
    }

    // 透過 Line 分享您的 App 的功能 (已更新，使用通用 Chooser)
    private fun shareAppViaLine() {
        val appName = getString(R.string.app_name) // 從 strings.xml 獲取你的應用程式名稱
        // 【重要】請將 "com.ray.trarailwaysalaryapp" 替換為你應用程式的實際 package name
        val appStoreLink = "https://play.google.com/store/apps/details?id=com.ray.trarailwaysalaryapp"

        val shareMessage = "快來試試這個超棒的 App：$appName！\n它能幫助你計算薪資、查看營運狀態，還有更多實用功能！\n下載連結：$appStoreLink"

        // 創建一個 Intent 用於分享文本
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareMessage)
        }

        // 我們不直接指定 Line 的包名來啟動，而是讓系統彈出 Chooser
        // 這樣可以兼容更多情況，並且讓用戶選擇他們想用的分享應用程式
        if (shareIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(Intent.createChooser(shareIntent, "透過以下應用程式分享您的台鐵薪資計算器 App"))
        } else {
            Toast.makeText(context, "您的設備上沒有任何應用程式可以處理分享請求。", Toast.LENGTH_LONG).show()
        }
    }

    // 檢查指定 package 是否已安裝的輔助函數 (此函數在此版本中不再直接用於 Line 分享，但仍可保留)
    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            requireContext().packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    // Email 聯絡功能 (已更新，使用 ACTION_SEND)
    private fun sendEmailToAuthor() {
        val subject = "關於您的台鐵薪資App的意見回饋"
        val body = "您好，\n\n我透過台鐵薪資App的贊助頁面與您聯絡。\n\n[請在這裡輸入您的訊息]\n\n"

        // 使用 Intent.ACTION_SEND
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain" // Explicitly set the MIME type
            putExtra(Intent.EXTRA_EMAIL, arrayOf(AUTHOR_EMAIL)) // 收件人 Email
            putExtra(Intent.EXTRA_SUBJECT, subject) // 郵件主旨
            putExtra(Intent.EXTRA_TEXT, body) // 郵件內容
            // If you want to include attachments, you would use Intent.EXTRA_STREAM here
        }

        // 檢查是否有應用程式可以處理這個 Intent
        if (emailIntent.resolveActivity(requireActivity().packageManager) != null) {
            // 使用 Intent.createChooser 讓使用者選擇 Email 應用程式 (即使有預設)
            startActivity(Intent.createChooser(emailIntent, "選擇 Email 應用程式"))
        } else {
            // 如果沒有應用程式可以處理，則顯示錯誤訊息
            Toast.makeText(requireContext(), "您的設備上沒有安裝 Email 應用程式或無法處理此請求。", Toast.LENGTH_SHORT).show()
        }
    }
}