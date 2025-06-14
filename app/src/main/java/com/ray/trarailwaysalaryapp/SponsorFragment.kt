package com.ray.trarailwaysalaryapp

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
import com.android.billingclient.api.ConsumeParams // 新增：用於消費購買的類別
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import android.util.Log

class SponsorFragment : Fragment(), PurchasesUpdatedListener {

    private val TAG = "SponsorFrag" // Logcat 標籤

    private lateinit var buyAppButton: Button
    private lateinit var billingClient: BillingClient
    private var productDetails: ProductDetails? = null // 用於儲存產品詳情

    // 【重要】請確保這個 PRODUCT_ID 與您在 Google Play Console 中設定的「可重複消費」產品 ID 完全一致。
    // 如果您還沒有設定，請在 Play Console 中建立一個新的「受管理的產品」，並將其設定為「可重複消費」。
    private val PRODUCT_ID = "ray_trarailway_salary_app_sponsorship_49ntd_consumable" // 範例：為了清楚標示為可消費，加上 "_consumable" 後綴

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: SponsorFragment 建立。")
        val view = inflater.inflate(R.layout.fragment_sponsor, container, false)

        buyAppButton = view.findViewById(R.id.buyAppButton)
        buyAppButton.setOnClickListener {
            Log.d(TAG, "buyAppButton 被點擊。啟動購買流程...")
            launchPurchaseFlow()
        }

        // 初始化 BillingClient
        billingClient = BillingClient.newBuilder(requireContext())
            .setListener(this) // 設定購買更新監聽器
            .enablePendingPurchases() // 啟用待處理購買 (例如，Google Play 點數、電信帳單)
            .build()

        connectToGooglePlayBilling() // 連接到 Google Play Billing 服務

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: SponsorFragment 視圖已建立。")
    }

    // 連接到 Google Play Billing 服務
    private fun connectToGooglePlayBilling() {
        Log.d(TAG, "connectToGooglePlayBilling: 正在啟動連線...")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "onBillingSetupFinished: Billing 服務連線成功。")
                    queryProductDetails() // 查詢產品詳情
                    // 處理可重複消費產品時，通常會查詢現有購買，以便消費任何上次會話中未消費的購買。
                    queryPurchases()
                } else {
                    Log.e(TAG, "onBillingSetupFinished: Billing 服務連線失敗: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                    Toast.makeText(requireContext(), "Google Play Billing 服務連線失敗: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
                    activity?.runOnUiThread { // 確保在主執行緒更新 UI
                        buyAppButton.isEnabled = false // 連線失敗，禁用購買按鈕
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "onBillingServiceDisconnected: Billing 服務斷開。正在嘗試重新連線...")
                Toast.makeText(requireContext(), "Google Play Billing 服務斷開，嘗試重新連線...", Toast.LENGTH_SHORT).show()
                // 為了簡單起見，這裡直接重新連線。在實際應用中，可以考慮加入延遲或重試次數限制 (例如：指數退避算法)。
                connectToGooglePlayBilling()
            }
        })
    }

    // 查詢產品詳情 (可重複消費產品也屬於 INAPP 類型)
    private fun queryProductDetails() {
        Log.d(TAG, "queryProductDetails: 正在查詢產品 ID: $PRODUCT_ID 的詳情。")
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP) // 可重複消費產品也是 INAPP 類型
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productsDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productsDetailsList.isNotEmpty()) {
                productDetails = productsDetailsList[0] // 儲存產品詳情
                Log.i(TAG, "queryProductDetails: 產品詳情獲取成功。價格: ${productDetails?.oneTimePurchaseOfferDetails?.formattedPrice}")
                activity?.runOnUiThread { // 確保在主執行緒更新 UI
                    buyAppButton.text = "贊助應用程式 (${productDetails?.oneTimePurchaseOfferDetails?.formattedPrice})"
                    buyAppButton.isEnabled = true // 啟用購買按鈕
                }
            } else {
                Log.e(TAG, "queryProductDetails: 無法獲取產品資訊: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                Toast.makeText(requireContext(), "無法獲取產品資訊: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
                activity?.runOnUiThread { // 確保在主執行緒更新 UI
                    buyAppButton.isEnabled = false // 禁用購買按鈕
                }
            }
        }
    }

    // 啟動購買流程
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

    // 處理購買結果的回調
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

    // 處理單筆購買
    private fun handlePurchase(purchase: Purchase) {
        Log.d(TAG, "handlePurchase: 購買狀態: ${purchase.purchaseState}，訂單 ID: ${purchase.orderId}")
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            Toast.makeText(requireContext(), "感謝您的購買！", Toast.LENGTH_LONG).show()
            Log.i(TAG, "handlePurchase: 購買成功！訂單 ID: ${purchase.orderId}")

            // 【針對可重複消費產品】: 必須先確認 (Acknowledge) 再消費 (Consume)
            if (!purchase.isAcknowledged) {
                Log.d(TAG, "handlePurchase: 購買尚未確認，正在確認中。")
                acknowledgePurchase(purchase) // 這將在確認後觸發消費
            } else {
                Log.d(TAG, "handlePurchase: 購買已確認，繼續消費。")
                // 如果已經確認但尚未消費，則消費它
                consumePurchase(purchase)
            }
            // 對於可重複消費的產品，按鈕通常在成功購買和消費後保持啟用，以便使用者可以再次購買。
            activity?.runOnUiThread {
                buyAppButton.text = "贊助應用程式 (${productDetails?.oneTimePurchaseOfferDetails?.formattedPrice})" // 重置按鈕文字
                buyAppButton.isEnabled = true // 保持按鈕啟用
            }

        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "handlePurchase: 購買正在待處理中，訂單 ID: ${purchase.orderId}")
            Toast.makeText(requireContext(), "購買正在處理中，請稍候...", Toast.LENGTH_LONG).show()
            activity?.runOnUiThread { // 在購買待處理時保持按鈕禁用
                buyAppButton.isEnabled = false
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
            Log.w(TAG, "handlePurchase: 購買處於未指定狀態，訂單 ID: ${purchase.orderId}")
            Toast.makeText(requireContext(), "購買狀態不明", Toast.LENGTH_LONG).show()
            activity?.runOnUiThread { // 重新啟用按鈕或處理錯誤狀態
                buyAppButton.isEnabled = true
            }
        }
    }

    // 確認購買 (所有一次性購買 (包括可重複消費) 都必須確認)
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(TAG, "acknowledgePurchase: 購買已成功確認，訂單 ID: ${purchase.orderId}")
                // 確認後，消費購買以使其再次可用
                consumePurchase(purchase)
            } else {
                Log.e(TAG, "acknowledgePurchase: 購買確認失敗: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                Toast.makeText(requireContext(), "購買確認失敗: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 【新增】: 消費購買以使其可以被重複購買
    private fun consumePurchase(purchase: Purchase) {
        Log.d(TAG, "consumePurchase: 正在消費購買: ${purchase.purchaseToken}")
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams) { billingResult, purchaseToken ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(TAG, "consumePurchase: 購買成功消費，代幣: $purchaseToken")
                Toast.makeText(requireContext(), "贊助已成功！感謝！", Toast.LENGTH_SHORT).show()
                // 更新 UI 或授予權益 (對於簡單的贊助，通常不需要持久的狀態變更)
                activity?.runOnUiThread {
                    buyAppButton.text = "贊助應用程式 (${productDetails?.oneTimePurchaseOfferDetails?.formattedPrice})" // 重置按鈕文字
                    buyAppButton.isEnabled = true // 保持按鈕啟用
                }
            } else {
                Log.e(TAG, "consumePurchase: 消費購買失敗: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                Toast.makeText(requireContext(), "贊助消費失敗: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 查詢使用者歷史購買 (在應用程式啟動時檢查未消費的購買)
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
                        // 對於可重複消費產品，如果已購買但未消費，我們需要消費它。
                        // 如果已正確消費，它通常不會出現在 queryPurchases 的 INAPP 結果中。
                        if (!purchase.isAcknowledged) {
                            Log.d(TAG, "queryPurchases: 找到未確認的購買: ${purchase.orderId}，正在確認並消費。")
                            acknowledgePurchase(purchase) // 這將在確認後觸發消費
                        } else {
                            // 這部分代碼可能在可重複消費產品已正確消費後不會被執行，
                            // 因為已消費的 INAPP 項目不會出現在 queryPurchases 的結果中。
                            // 但為了健壯性保留。
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
        // 在 Fragment 恢復時再次查詢購買是一個好習慣，以捕捉應用程式在後台時完成的任何變更或待處理購買。
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
}