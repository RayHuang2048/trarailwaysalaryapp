package com.ray.trarailwaysalaryapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp // 確保這裡有導入 Firebase Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date // 確保這裡有導入 Date
import java.text.SimpleDateFormat // 導入日期格式化
import java.util.Locale // 導入 Locale

import com.ray.trarailwaysalaryapp.AdminLoginDialog // 確保已導入 AdminLoginDialog

// 定義一個介面來處理管理員動作，供 Adapter 回呼 Fragment
interface OnAdminActionListener {
    fun onDeleteClick(reportId: String)
    fun onPinClick(report: AccidentReport)
    fun onUnpinClick(report: AccidentReport)
}

class AccidentReportFragment : Fragment(),
    OnAdminActionListener, // 實作 adapter 的管理員動作介面
    AdminLoginDialog.LoginResultListener { // 實作管理員登入對話框的回呼介面

    private val TAG = "AccidentReportFragment"

    // Firestore 實例
    private lateinit var db: FirebaseFirestore
    // Adapter
    private lateinit var accidentReportAdapter: AccidentReportAdapter
    // RecyclerView 參考
    private lateinit var recyclerViewAccidentReports: RecyclerView

    // UI 元素參考
    private lateinit var editTextReporterName: TextInputEditText
    private lateinit var editTextDateTime: TextInputEditText // 用於顯示使用者輸入的時間日期
    private lateinit var editTextLocation: TextInputEditText
    private lateinit var editTextDescription: TextInputEditText
    private lateinit var buttonSubmitReport: Button
    private lateinit var buttonAdminLogin: Button // 管理員登入/登出按鈕
    private lateinit var tvPageTitle: TextView

    // 只看置頂報告的 Switch
    private lateinit var switchShowPinnedOnly: Switch

    // Firestore 監聽器的註冊，方便在 Fragment 銷毀時取消監聽
    private var firestoreListener: ListenerRegistration? = null

    // 判斷是否只顯示置頂報告的狀態
    private var showPinnedOnly: Boolean = false // 預設不只顯示置頂

    // 判斷是否為管理員的狀態，這裡不需要 lateinit，因為可能在 onCreate 之前就被設置
    private var isAdminUser: Boolean = false


    override fun onAttach(context: Context) {
        super.onAttach(context)
        // 在這裡初始化 AdminManager，確保它在 Fragment 附加到 Context 時就可用
        AdminManager.initialize(context.applicationContext)
        Log.d(TAG, "Fragment attached. AdminManager initialized.")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_accident_report, container, false)
        Log.d(TAG, "onCreateView started.")

        // 初始化 Firebase Firestore
        db = FirebaseFirestore.getInstance()
        Log.d(TAG, "Firebase Firestore initialized.")

        // 初始化 UI 元素
        editTextReporterName = view.findViewById(R.id.editTextReporterName)
        editTextDateTime = view.findViewById(R.id.editTextDateTime)
        editTextLocation = view.findViewById(R.id.editTextLocation)
        editTextDescription = view.findViewById(R.id.editTextDescription)
        buttonSubmitReport = view.findViewById(R.id.buttonSubmitReport)
        buttonAdminLogin = view.findViewById(R.id.buttonAdminLogin) // 連結管理者按鈕
        tvPageTitle = view.findViewById(R.id.tv_page_title)

        // 初始化 Switch
        switchShowPinnedOnly = view.findViewById(R.id.switchShowPinnedOnly)

        recyclerViewAccidentReports = view.findViewById(R.id.recyclerViewAccidentReports)
        recyclerViewAccidentReports.layoutManager = LinearLayoutManager(context)

        // 檢查初始管理員狀態，以便正確初始化 Adapter
        // 這裡假設 AdminManager.isAdmin() 已經可以提供即時狀態
        // 如果 AdminManager 的狀態是異步加載的，您需要調整邏輯
        isAdminUser = AdminManager.isAdmin()

        // 修正 Adapter 初始化：不再傳遞 emptyList()
        accidentReportAdapter = AccidentReportAdapter(isAdmin = isAdminUser, listener = this)
        recyclerViewAccidentReports.adapter = accidentReportAdapter
        Log.d(TAG, "RecyclerView and Adapter initialized with isAdmin: $isAdminUser.")

        // <--- 新增：預設事故發生時間為手機系統時間 ---
        // 在這裡設置 editTextDateTime 的初始值為當前時間
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        editTextDateTime.setText(dateFormat.format(Date()))
        // 由於 XML 中已將其設為不可編輯，這裡只需設定一次即可。
        // --- 預設時間設定結束 ---

        // 設定提交按鈕的點擊事件
        buttonSubmitReport.setOnClickListener {
            Log.d(TAG, "Submit button clicked.")
            submitAccidentReport()
        }

        // 設定管理員登入按鈕的點擊事件
        buttonAdminLogin.setOnClickListener {
            if (AdminManager.isAdmin()) {
                // 如果已是管理員，則執行登出
                AdminManager.logoutAdmin()
                Toast.makeText(context, "管理員已登出", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Admin logout initiated.")
                // UI 更新將由 isAdminLoggedIn Flow 的觀察者處理
            } else {
                // 如果未登入，顯示登入對話框
                showAdminLoginDialog()
                Log.d(TAG, "Admin login dialog shown.")
            }
        }

        // 為 Switch 設定監聽器
        switchShowPinnedOnly.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "Switch 'Show Pinned Only' changed to: $isChecked")
            showPinnedOnly = isChecked
            // 當 Switch 狀態改變時，重新監聽 Firestore 數據以更新列表
            listenForAccidentReports()
        }

        // 觀察 AdminManager 中的管理員登入狀態
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AdminManager.isAdminLoggedIn.collectLatest { isAdmin ->
                    Log.d(TAG, "Admin login status changed: $isAdmin")
                    // 更新 Fragment 內部的 isAdminUser 狀態
                    isAdminUser = isAdmin
                    updateAdminUI(isAdmin)
                }
            }
        }

        // 首次載入時監聽 Firestore 數據
        listenForAccidentReports()
        Log.d(TAG, "onCreateView completed, listenForAccidentReports called for the first time.")

        return view
    }

    override fun onStop() {
        super.onStop()
        // 當 Fragment 不再可見時，取消 Firestore 監聽，避免記憶體洩漏
        firestoreListener?.remove()
        firestoreListener = null
        Log.d(TAG, "Firestore listener removed in onStop.")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called.")
    }

    // 更新管理員相關 UI 的方法
    private fun updateAdminUI(isAdmin: Boolean) {
        Log.d(TAG, "updateAdminUI called with isAdmin: $isAdmin")
        // 更新管理員按鈕文字
        buttonAdminLogin.text = if (isAdmin) "管理員登出" else "管理員登入"

        // 管理員登入時，隱藏「只看置頂」開關，並確保顯示所有報告
        if (isAdmin) {
            switchShowPinnedOnly.visibility = View.GONE
            switchShowPinnedOnly.isChecked = false // 管理員應始終看到所有報告
            showPinnedOnly = false // 更新內部狀態
            Log.d(TAG, "Admin is logged in. 'Show Pinned Only' switch hidden and unchecked.")
        } else {
            switchShowPinnedOnly.visibility = View.VISIBLE // 訪客模式下顯示開關
            Log.d(TAG, "Admin is logged out. 'Show Pinned Only' switch visible.")
        }

        // 由於 isAdmin 狀態可能改變，且 Adapter 的 isAdmin 是 val，
        // 我們需要重新創建 Adapter 實例來更新 isAdmin 狀態。
        // 或者，在 Adapter 中添加一個方法來更新其內部 isAdmin 狀態，
        // 但重新創建 Adapter 並不影響 ListAdapter 的 DiffUtil 機制。
        accidentReportAdapter = AccidentReportAdapter(isAdmin = isAdmin, listener = this)
        recyclerViewAccidentReports.adapter = accidentReportAdapter

        // 重新監聽以應用最新的篩選狀態（如果isAdmin變化導致showPinnedOnly變化）
        // 確保此處的 listenForAccidentReports() 會重新建立查詢
        listenForAccidentReports()

        // 注意：accidentReportAdapter.notifyDataSetChanged() 在 ListAdapter 中通常不需要，
        // 因為 submitList() 會處理 DiffUtil 更新。但如果僅是 UI 元素（如按鈕的可見性）改變，
        // 而數據本身未變，且 Adapter 沒有在 onBindViewHolder 中響應 isAdmin 變化，則可能需要。
        // 在我們的 Adapter 設計中，onBindViewHolder 會檢查 isAdmin。
        // 因此，只要 listenForAccidentReports 重新觸發 submitList，通常就足夠了。
        // accidentReportAdapter.notifyDataSetChanged() // 一般情況下不需要
    }


    // 顯示管理員登入對話框
    private fun showAdminLoginDialog() {
        val dialog = AdminLoginDialog()
        dialog.loginResultListener = this // 設定回呼監聽器為當前 Fragment
        dialog.show(parentFragmentManager, "AdminLoginDialog")
        Log.d(TAG, "AdminLoginDialog shown.")
    }

    // 實作 AdminLoginDialog.LoginResultListener 介面方法 (登入結果回呼)
    override fun onLoginSuccess() {
        Toast.makeText(context, "管理員登入成功！", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Admin login successful callback received.")
        // AdminManager.isAdminLoggedIn Flow 會自動發出新狀態，並由 updateAdminUI 處理 UI 更新
    }

    override fun onLoginFailure(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        Log.w(TAG, "Admin login failed: $message")
    }

    // 提交事故報告到 Cloud Firestore
    private fun submitAccidentReport() {
        // reporterName 現在是非空 String，如果輸入為空或空白，則儲存為 ""
        val reporterName = editTextReporterName.text?.toString().let { if (it.isNullOrBlank()) "" else it.trim() }

        val dateTimeInput = editTextDateTime.text?.toString()?.trim() ?: ""
        val location = editTextLocation.text?.toString().let { if (it.isNullOrBlank()) "" else it.trim() }
        val description = editTextDescription.text?.toString().let { if (it.isNullOrBlank()) "" else it.trim() }

        // 只有 location 和 description 是必填 (因為 reporterName 可空，dateTime 自動填充)
        if (location.isEmpty() || description.isEmpty()) {
            Toast.makeText(context, "請填寫所有必填欄位：事故地點、描述", Toast.LENGTH_LONG).show()
            Log.w(TAG, "Form validation failed: Missing required fields (Location or Description).")
            return
        }

        val report = AccidentReport(
            id = "", // id 屬性通常由 @DocumentId 自動填充，這裡可以傳遞空字串作為預設值
            reporterName = reporterName, // 傳遞非空 String 的 reporterName
            dateTime = dateTimeInput,    // dateTime 仍然是 String
            location = location,
            description = description,
            timestamp = Timestamp(Date()), // 使用當前時間作為系統回報時間
            title = "",
            severity = "",
            status = "",
            imageUrl = "",
            pinned = false // 修正：現在使用 pinned 欄位
        )
        Log.d(TAG, "Attempting to submit report: $report")

        db.collection("accidentReports")
            .add(report)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(context, "事故回報成功！ ID: ${documentReference.id}", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Document added with ID: ${documentReference.id}")
                clearFormFields()
                // 提交成功後，重新預設時間，以防用戶連續提交
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                editTextDateTime.setText(dateFormat.format(Date()))
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding document: ${e.message}", e)
                Toast.makeText(context, "事故回報失敗: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // 清空表單欄位 (除了 DateTime，因為它是預設時間)
    private fun clearFormFields() {
        editTextReporterName.setText("")
        editTextLocation.setText("")
        editTextDescription.setText("")
        Log.d(TAG, "Form fields cleared (except DateTime).")
    }

    private fun listenForAccidentReports() {
        firestoreListener?.remove()
        firestoreListener = null
        Log.d(TAG, "Old Firestore listener removed. Setting up new one with showPinnedOnly = $showPinnedOnly (Admin status: ${AdminManager.isAdmin()})")

        var query: Query = db.collection("accidentReports")

        // 如果不是管理員且只看置頂報告，則新增 where 條件
        // 管理員模式下，即使 showPinnedOnly 為 true，也應顯示所有報告
        if (showPinnedOnly && !AdminManager.isAdmin()) {
            query = query.whereEqualTo("pinned", true) // 修正：查詢使用 "pinned"
            Log.d(TAG, "Querying for pinned reports only.")
        } else {
            Log.d(TAG, "Querying for all reports (or admin mode).")
        }

        // 排序規則：先按 pinned 降序 (true 在前)，再按 timestamp 降序 (最新的在前)
        // 注意：Firestore 需要複合索引 (composite index) 如果您同時使用 where() 和 orderBy() 在不同字段上，或同時在多個字段上使用 orderBy()。
        // 請確保您已在 Firebase Console 中設置了這些複合索引。
        query = query
            .orderBy("pinned", Query.Direction.DESCENDING) // 修正：排序使用 "pinned"
            .orderBy("timestamp", Query.Direction.DESCENDING)

        firestoreListener = query.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed: ${e.message}", e)
                Toast.makeText(context, "載入事故報告失敗: ${e.message}", Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }

            if (snapshots != null) {
                Log.d(TAG, "Firestore snapshots received. Size: ${snapshots.size()}")
                if (!snapshots.isEmpty) {
                    val reports = mutableListOf<AccidentReport>()
                    for (doc in snapshots.documents) {
                        Log.d(TAG, "Processing document ID: ${doc.id}, Data: ${doc.data}")
                        try {
                            val report = doc.toObject(AccidentReport::class.java)
                            report?.let {
                                reports.add(it)
                                // 直接使用 it.reporterName (非空 String)，因為它現在的預設值是 ""
                                // 修正：移除多餘的空安全處理，因為 reporterName 已是非空 String
                                Log.d(TAG, "Converted report (Success): ID=${it.id}, Reporter=${it.reporterName}, Time=${it.dateTime}, Pinned=${it.pinned}, Timestamp=${it.timestamp}")
                            } ?: run {
                                Log.e(TAG, "Failed to convert document to AccidentReport (toObject returned null): Document ID = ${doc.id}, Data = ${doc.data}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error converting document to AccidentReport: ${e.message}. Document ID = ${doc.id}, Data = ${doc.data}", e)
                        }
                    }
                    accidentReportAdapter.submitList(reports)
                    Log.d(TAG, "Reports submitted to adapter. Total: ${reports.size} reports displayed.")
                } else {
                    Log.d(TAG, "Current data: snapshots is empty (no reports found matching criteria).")
                    accidentReportAdapter.submitList(emptyList())
                }
            } else {
                Log.d(TAG, "Current data: snapshots is null (no data from Firestore).")
                accidentReportAdapter.submitList(emptyList())
            }
        }
    }

    override fun onDeleteClick(reportId: String) {
        if (!AdminManager.isAdmin()) {
            Toast.makeText(context, "您沒有權限執行此操作！", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Attempted delete without admin permission for reportId: $reportId")
            return
        }
        Log.d(TAG, "Deleting report with ID: $reportId")
        db.collection("accidentReports").document(reportId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "事故報告已刪除！", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "DocumentSnapshot successfully deleted: $reportId")
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "刪除失敗: ${e.message}", Toast.LENGTH_LONG).show()
                Log.w(TAG, "Error deleting document $reportId: ${e.message}", e)
            }
    }

    override fun onPinClick(report: AccidentReport) {
        if (!AdminManager.isAdmin()) {
            Toast.makeText(context, "您沒有權限執行此操作！", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Attempted pin without admin permission for reportId: ${report.id}")
            return
        }
        Log.d(TAG, "Pinning report with ID: ${report.id}")
        val updatedReport = report.copy(pinned = true) // 修正：現在更新 pinned 欄位
        db.collection("accidentReports").document(updatedReport.id)
            .set(updatedReport)
            .addOnSuccessListener {
                Toast.makeText(context, "事故報告已置頂！", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "DocumentSnapshot successfully pinned: ${updatedReport.id}")
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "置頂失敗: ${e.message}", Toast.LENGTH_LONG).show()
                Log.w(TAG, "Error pinning document ${updatedReport.id}: ${e.message}", e)
            }
    }

    override fun onUnpinClick(report: AccidentReport) {
        if (!AdminManager.isAdmin()) {
            Toast.makeText(context, "您沒有權限執行此操作！", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Attempted unpin without admin permission for reportId: ${report.id}")
            return
        }
        Log.d(TAG, "Unpinning report with ID: ${report.id}")
        val updatedReport = report.copy(pinned = false) // 修正：現在更新 pinned 欄位
        db.collection("accidentReports").document(updatedReport.id)
            .set(updatedReport)
            .addOnSuccessListener {
                Toast.makeText(context, "事故報告已取消置頂！", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "DocumentSnapshot successfully unpinned: ${updatedReport.id}")
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "取消置頂失敗: ${e.message}", Toast.LENGTH_LONG).show()
                Log.w(TAG, "Error unpinning document ${updatedReport.id}: ${e.message}", e)
            }
    }
}
