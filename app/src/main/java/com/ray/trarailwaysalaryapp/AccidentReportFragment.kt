package com.ray.trarailwaysalaryapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log

// 導入 Firebase Firestore 相關的類別
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration // 用於管理即時監聽器
import com.google.firebase.firestore.Query // 用於排序查詢

// 導入 Kotlin 協程相關
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Firebase Coroutines 擴展的 await() 函式
import com.google.firebase.firestore.ktx.snapshots // 讓 addSnapshotListener 更方便
import kotlinx.coroutines.tasks.await // for .add().await()

class AccidentReportFragment : Fragment() {

    private val TAG = "AccidentReportFrag"

    private lateinit var editTextReporterName: TextInputEditText
    private lateinit var editTextDateTime: TextInputEditText
    private lateinit var editTextLocation: TextInputEditText
    private lateinit var editTextDescription: TextInputEditText
    private lateinit var buttonSubmitReport: Button
    private lateinit var recyclerViewAccidentReports: RecyclerView
    private lateinit var adapter: AccidentReportAdapter

    // 用於 RecyclerView 的資料列表。這個列表將會被 Firestore 監聽器更新。
    private val accidentReports = mutableListOf<AccidentReport>()

    // Firebase Firestore 實例
    private lateinit var db: FirebaseFirestore
    // 用於儲存 Firestore 即時監聽器的註冊，以便在 Fragment 銷毀時取消監聽，防止記憶體洩漏。
    private var firestoreListener: ListenerRegistration? = null

    // Kotlin 協程相關設定
    private val job = Job() // 用於管理協程的生命週期
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job) // Fragment 的協程範圍

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 在這裡初始化 Firestore 實例，因為它不依賴於 View
        db = FirebaseFirestore.getInstance()
        Log.d(TAG, "onCreate: FirebaseFirestore 實例已初始化。")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: AccidentReportFragment 建立。")
        val view = inflater.inflate(R.layout.fragment_accident_report, container, false)

        // 初始化 UI 元素
        editTextReporterName = view.findViewById(R.id.editTextReporterName)
        editTextDateTime = view.findViewById(R.id.editTextDateTime)
        editTextLocation = view.findViewById(R.id.editTextLocation)
        editTextDescription = view.findViewById(R.id.editTextDescription)
        buttonSubmitReport = view.findViewById(R.id.buttonSubmitReport)
        recyclerViewAccidentReports = view.findViewById(R.id.recyclerViewAccidentReports)

        // 設定 RecyclerView 的 Adapter
        adapter = AccidentReportAdapter(accidentReports)
        recyclerViewAccidentReports.adapter = adapter

        // 設定提交按鈕的點擊監聽器
        buttonSubmitReport.setOnClickListener {
            submitReport()
        }

        // 自動填充當前時間 (方便測試)
        val currentTime = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
        editTextDateTime.setText(currentTime)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: AccidentReportFragment 視圖已建立。")
        // 在 View 建立後設置 Firestore 即時監聽器，開始載入並監聽資料
        setupFirestoreListener()
    }

    private fun submitReport() {
        val reporterName = editTextReporterName.text.toString().trim()
        val dateTime = editTextDateTime.text.toString().trim()
        val location = editTextLocation.text.toString().trim()
        val description = editTextDescription.text.toString().trim()

        // 檢查必要欄位是否填寫
        if (dateTime.isEmpty() || location.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "請填寫事故時間、地點和描述", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "submitReport: 提交失敗 - 必填欄位為空。")
            return
        }

        // 建立 AccidentReport 物件 (ID 仍由 data class 自動生成 UUID)
        val newReport = AccidentReport(
            reporterName = reporterName,
            dateTime = dateTime,
            location = location,
            description = description
        )

        Log.d(TAG, "submitReport: 嘗試上傳回報至 Firestore: $newReport")

        // 【核心變更】: 使用協程將資料上傳至 Firestore
        coroutineScope.launch {
            try {
                // 獲取 "accidentReports" 集合的引用，並使用 add() 方法將 newReport 作為一個新文檔添加
                // .await() 讓這個操作變成 suspendable，可以在協程中等待結果
                val documentRef = db.collection("accidentReports")
                    .add(newReport) // Firebase 會自動為這個文檔生成一個唯一的 ID
                    .await()

                Log.i(TAG, "submitReport: 事故回報上傳成功！文檔 ID: ${documentRef.id}")

                // 在成功上傳後清空輸入欄位，列表的更新會由 Firestore 監聽器自動完成
                activity?.runOnUiThread { // 確保在主執行緒上更新 UI
                    editTextLocation.setText("")
                    editTextDescription.setText("")
                    Toast.makeText(requireContext(), "事故回報成功！", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // 處理上傳失敗的情況，例如網路錯誤或其他 Firebase 錯誤
                Log.e(TAG, "submitReport: 事故回報上傳失敗！", e)
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "事故回報失敗: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 【重要新增】: 設置 Firestore 即時監聽器
    private fun setupFirestoreListener() {
        Log.d(TAG, "setupFirestoreListener: 正在設置 Firestore 即時監聽器。")
        // 監聽 "accidentReports" 集合的變化
        // .orderBy("dateTime", Query.Direction.DESCENDING) 表示按 dateTime 欄位降序排列 (最新在前)
        firestoreListener = db.collection("accidentReports")
            .orderBy("dateTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                // 檢查是否有錯誤發生
                if (e != null) {
                    Log.w(TAG, "Firestore 監聽失敗。", e)
                    Toast.makeText(requireContext(), "無法載入回報: ${e.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                // 如果有快照資料 (即資料庫有變化)
                if (snapshots != null) {
                    val newReports = mutableListOf<AccidentReport>()
                    // 遍歷每個文檔
                    for (doc in snapshots.documents) {
                        // 將每個 Firestore 文檔轉換為 AccidentReport 物件
                        val report = doc.toObject(AccidentReport::class.java)
                        report?.let {
                            // 可選：如果您想將 Firestore 的文檔 ID 也作為 AccidentReport 的一部分
                            // 因為我們在 AccidentReport 內部已經有 UUID 了，所以這裡可以選擇不覆蓋
                            // 如果您希望 Firestore 的文檔 ID 作為主要 ID，可以在 AccidentReport 中新增一個 field 讓 doc.id 傳入
                            newReports.add(it)
                        }
                    }
                    Log.d(TAG, "setupFirestoreListener: 獲取到 ${newReports.size} 筆回報。")

                    // 清空現有資料，並加入從 Firestore 獲取的新資料
                    accidentReports.clear()
                    accidentReports.addAll(newReports)
                    adapter.notifyDataSetChanged() // 通知 Adapter 資料集已更改，RecyclerView 將會更新顯示
                } else {
                    // 如果沒有快照資料 (例如集合是空的)
                    Log.d(TAG, "setupFirestoreListener: 目前沒有事故回報資料。")
                    accidentReports.clear()
                    adapter.notifyDataSetChanged()
                }
            }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: AccidentReportFragment 恢復。")
        // 監聽器已經在 onViewCreated 設置，如果需要重新綁定，可以在這裡重新調用 setupFirestoreListener()
        // 但通常一次設定就足夠，除非您在 onPause 中移除了監聽器
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: AccidentReportFragment 暫停。")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: AccidentReportFragment 視圖銷毀。")
        // 【重要】: 在 View 銷毀時，取消 Firestore 即時監聽器，防止記憶體洩漏
        firestoreListener?.remove()
        firestoreListener = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: AccidentReportFragment 銷毀。取消所有協程。")
        // 在 Fragment 銷毀時，取消所有由這個協程範圍啟動的協程
        job.cancel()
    }
}