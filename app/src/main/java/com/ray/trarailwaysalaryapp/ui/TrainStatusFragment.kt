// app/src/main/java/com/ray/trarailwaysalaryapp/ui/TrainStatusFragment.kt

package com.ray.trarailwaysalaryapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast // 引入 Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ray.trarailwaysalaryapp.R // 確保 R 檔案正確引入
import com.ray.trarailwaysalaryapp.viewmodel.TrainStatusViewModel // 引入 ViewModel

class TrainStatusFragment : Fragment() {

    private lateinit var viewModel: TrainStatusViewModel
    private lateinit var trainNumberEditText: EditText
    private lateinit var queryButton: Button
    private lateinit var resultDisplayTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 載入佈局檔案
        val view = inflater.inflate(R.layout.fragment_train_status, container, false)

        // 初始化 UI 元件
        trainNumberEditText = view.findViewById(R.id.trainNumberEditText)
        queryButton = view.findViewById(R.id.queryButton)
        resultDisplayTextView = view.findViewById(R.id.resultDisplayTextView)

        // 初始化 ViewModel。ViewModel 應該由 Fragment 或 Activity 管理其生命週期。
        viewModel = ViewModelProvider(this).get(TrainStatusViewModel::class.java)

        // 觀察列車動態資料的變化。當 ViewModel 的 _trainLiveData 更新時，這裡會收到通知。
        viewModel.trainLiveData.observe(viewLifecycleOwner) { trains ->
            if (trains.isNotEmpty()) {
                val resultText = StringBuilder()
                trains.forEach { train ->
                    resultText.append("車次: ${train.TrainNo} (${train.TrainTypeName.Zh_tw})\n")
                    resultText.append("起訖站: ${train.StartingStationName.Zh_tw} - ${train.EndingStationName.Zh_tw}\n")
                    resultText.append("目前: ${train.StationName?.Zh_tw ?: "行駛中"}") // 如果 StationName 為空，則顯示 "行駛中"
                    if (train.DelayTime > 0) {
                        resultText.append(" 晚點 ${train.DelayTime} 分鐘\n")
                    } else {
                        resultText.append(" 準點\n")
                    }
                    // TDX 的 UpdateTime 格式為 ISO 8601，我們只取時間部分 (例如 "2025-07-02T10:30:00+08:00" 取 "10:30")
                    val timePart = if (train.UpdateTime.length >= 16) train.UpdateTime.substring(11, 16) else train.UpdateTime
                    resultText.append("更新時間: $timePart\n\n")
                }
                resultDisplayTextView.text = resultText.toString()
            } else {
                resultDisplayTextView.text = "未找到列車動態資訊，請檢查列車號碼或稍後再試。"
            }
        }

        // 觀察錯誤訊息。當 ViewModel 發生錯誤時，這裡會收到通知並顯示 Toast。
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotBlank()) {
                Toast.makeText(context, "錯誤: $message", Toast.LENGTH_LONG).show()
                resultDisplayTextView.text = "查詢失敗。請檢查網路或輸入。"
            }
        }

        // 設置查詢按鈕的點擊事件監聽器
        queryButton.setOnClickListener {
            val trainNo = trainNumberEditText.text.toString().trim() // 獲取輸入的列車號碼並去除空格
            if (trainNo.isNotBlank()) {
                resultDisplayTextView.text = "正在查詢列車動態，請稍候..."
                viewModel.queryTrainLiveStatus(trainNo) // 呼叫 ViewModel 中的查詢函數
            } else {
                resultDisplayTextView.text = "請輸入列車號碼以進行查詢。"
                Toast.makeText(context, "請輸入列車號碼！", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}
