package com.ray.trarailwaysalaryapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.Timestamp

// AccidentReportAdapter 用於將事故報告數據顯示在 RecyclerView 中
class AccidentReportAdapter(
    private val isAdmin: Boolean, // 標示目前用戶是否為管理員，用於控制管理員操作按鈕的顯示
    private val listener: OnAdminActionListener? = null // 管理員動作的回調介面
) : ListAdapter<AccidentReport, AccidentReportAdapter.AccidentReportViewHolder>(AccidentReportDiffCallback()) {

    // 延遲初始化 FirebaseAuth 實例，確保在需要時才創建
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // 當 RecyclerView 需要新的 ViewHolder 時調用此方法
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccidentReportViewHolder {
        // 從佈局檔案 item_accident_report.xml 膨脹視圖
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_accident_report, parent, false)
        return AccidentReportViewHolder(view)
    }

    // 當 RecyclerView 需要將數據綁定到指定位置的 ViewHolder 時調用此方法
    override fun onBindViewHolder(holder: AccidentReportViewHolder, position: Int) {
        // 獲取當前位置的 AccidentReport 物件
        val report = getItem(position)
        // 將數據綁定到 ViewHolder
        holder.bind(report, isAdmin, listener, auth.currentUser?.uid)
    }

    // AccidentReportViewHolder 類別，持有列表項目的視圖元素
    class AccidentReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 從 XML 佈局中找到對應的 TextViews
        private val textViewReportId: TextView = itemView.findViewById(R.id.textViewReportId)
        private val textViewReporterName: TextView = itemView.findViewById(R.id.textViewReporterName)
        private val textViewDateTime: TextView = itemView.findViewById(R.id.textViewDateTime)
        private val textViewLocation: TextView = itemView.findViewById(R.id.textViewLocation)
        private val textViewDescription: TextView = itemView.findViewById(R.id.textViewDescription)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)

        // 從 XML 佈局中找到對應的 ImageViews 和 ImageButtons
        private val ivPinnedIcon: ImageView = itemView.findViewById(R.id.ivPinnedIcon) // 置頂圖示
        private val btnPin: ImageButton = itemView.findViewById(R.id.btnPin) // 置頂按鈕
        private val btnUnpin: ImageButton = itemView.findViewById(R.id.btnUnpin) // 取消置頂按鈕
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete) // 刪除按鈕

        private val reportImageView: ImageView = itemView.findViewById(R.id.reportImageView) // 報告圖片

        // 綁定數據到視圖元素的方法
        fun bind(
            report: AccidentReport, // 要顯示的事故報告數據
            isAdmin: Boolean,       // 是否為管理員
            listener: OnAdminActionListener?, // 管理員動作監聽器
            currentUserId: String?  // 當前登入用戶的 ID
        ) {
            // 設置報告的基本信息文本
            textViewReportId.text = "回報 ID: #${report.id.ifEmpty { "N/A" }}" // 如果 ID 為空則顯示 N/A
            // 如果 reporterName 為空字串則顯示 "匿名"
            textViewReporterName.text = "回報人: ${report.reporterName.ifEmpty { "匿名" }}"
            textViewLocation.text = "地點: ${report.location.ifEmpty { "N/A" }}"
            textViewDescription.text = "描述: ${report.description.ifEmpty { "N/A" }}"

            // 顯示使用者輸入的事故發生時間 (這裡假設 dateTime 欄位是使用者輸入的或系統預設的)
            textViewDateTime.text = "事故發生時間: ${report.dateTime.ifEmpty { "N/A" }}"

            // 格式化並顯示 Firestore 伺服器時間戳
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
            // 將 Firestore Timestamp 轉換為 Date，然後格式化為字串
            val formattedTimestamp = report.timestamp?.toDate()?.let { sdf.format(it) } ?: "N/A"
            tvTimestamp.text = "系統回報時間: $formattedTimestamp"

            // 處理圖片載入和顯示
            if (report.imageUrl.isNotEmpty()) {
                // 使用 Glide 載入圖片，設置佔位符和錯誤圖片
                Glide.with(itemView.context)
                    .load(report.imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder) // 圖片載入前的佔位符
                    .error(R.drawable.ic_image_error) // 圖片載入失敗時的錯誤圖片
                    .into(reportImageView)
                reportImageView.visibility = View.VISIBLE // 圖片可見
            } else {
                reportImageView.visibility = View.GONE // 圖片不可見
            }

            // 處理置頂圖示 (ivPinnedIcon) 的顯示狀態
            // 根據 report.pinned 的布林值來決定圖示的顯示或隱藏
            ivPinnedIcon.visibility = if (report.pinned) View.VISIBLE else View.GONE

            // 管理員操作按鈕的邏輯：只有管理員才能看到這些按鈕
            if (isAdmin) {
                btnDelete.visibility = View.VISIBLE // 刪除按鈕始終可見
                // 置頂按鈕在未置頂時可見，取消置頂按鈕在已置頂時可見
                btnPin.visibility = if (!report.pinned) View.VISIBLE else View.GONE
                btnUnpin.visibility = if (report.pinned) View.VISIBLE else View.GONE

                // 設置刪除按鈕的點擊事件監聽器
                btnDelete.setOnClickListener {
                    report.id.let { id -> listener?.onDeleteClick(id) } // 調用監聽器中的 onDeleteClick 方法
                }

                // 設置釘選按鈕的點擊事件監聽器
                btnPin.setOnClickListener {
                    listener?.onPinClick(report) // 調用監聽器中的 onPinClick 方法
                }

                // 設置取消釘選按鈕的點擊事件監聽器
                btnUnpin.setOnClickListener {
                    listener?.onUnpinClick(report) // 調用監聽器中的 onUnpinClick 方法
                }
            } else {
                // 非管理員狀態下，所有管理員操作按鈕均隱藏
                btnDelete.visibility = View.GONE
                btnPin.visibility = View.GONE
                btnUnpin.visibility = View.GONE
            }
        }
    }

    // DiffUtil.ItemCallback 用於優化 RecyclerView 的更新性能
    // 它幫助 ListAdapter 判斷列表中的項目是新增、移除還是更新
    class AccidentReportDiffCallback : DiffUtil.ItemCallback<AccidentReport>() {
        // 判斷兩個項目是否代表同一個邏輯實體 (通常比較 ID)
        override fun areItemsTheSame(oldItem: AccidentReport, newItem: AccidentReport): Boolean {
            return oldItem.id == newItem.id
        }

        // 判斷兩個項目（已確定是同一個邏輯實體）的內容是否相同 (通常比較所有內容)
        override fun areContentsTheSame(oldItem: AccidentReport, newItem: AccidentReport): Boolean {
            return oldItem == newItem // 對於 data class，可以直接比較整個物件
        }
    }
}
