package com.ray.trarailwaysalaryapp.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ray.trarailwaysalaryapp.R
import com.ray.trarailwaysalaryapp.viewmodel.TrainStatusViewModel

class TrainStatusFragment : Fragment() {

    private val TAG = "TrainStatusFragment"

    private lateinit var viewModel: TrainStatusViewModel
    private lateinit var trainNoEditText: EditText
    private lateinit var queryButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var errorMessageTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: TrainStatusFragment 建立。")
        val view = inflater.inflate(R.layout.fragment_train_status, container, false)

        // 初始化 ViewModel
        viewModel = ViewModelProvider(this).get(TrainStatusViewModel::class.java)

        // 初始化 UI 元件
        trainNoEditText = view.findViewById(R.id.trainNoEditText)
        queryButton = view.findViewById(R.id.queryButton)
        resultTextView = view.findViewById(R.id.resultTextView)
        errorMessageTextView = view.findViewById(R.id.errorMessageTextView)

        // 設定查詢按鈕的點擊事件
        queryButton.setOnClickListener {
            val trainNo = trainNoEditText.text.toString()
            Log.d(TAG, "查詢按鈕被點擊，列車號碼: $trainNo")
            viewModel.queryTrainLiveStatus(trainNo)
        }

        // 觀察列車動態 LiveData 的變化
        viewModel.trainLiveData.observe(viewLifecycleOwner, Observer { trainList ->
            Log.d(TAG, "trainLiveData 收到更新。列車數量: ${trainList.size}")
            if (trainList.isNotEmpty()) {
                val train = trainList[0] // 假設我們只顯示第一筆列車的資訊
                val statusText = buildString {
                    append("列車號碼: ${train.TrainNo}\n")
                    append("車種: ${train.TrainTypeName.Zh_tw} (${train.TrainTypeName.En})\n")
                    // 修正：顯示列車當前所在車站名稱，而不是 StartingStationName
                    // 因為 TrainLiveInfo 中不再包含 StartingStationName
                    append("目前車站: ${train.StationName?.Zh_tw ?: "站間行駛"}\n") // 使用 ?. 和 ?: 處理可能為 null 的情況
                    append("誤點時間: ${train.DelayTime} 分鐘\n")
                    append("狀態: ")
                    when (train.TrainStationStatus) {
                        0 -> append("正常")
                        1 -> append("進站中")
                        2 -> append("離站中")
                        3 -> append("停靠中")
                        else -> append("未知")
                    }
                    append("\n")
                    append("更新時間: ${train.UpdateTime}\n")
                }
                resultTextView.text = statusText
                errorMessageTextView.text = "" // 清除錯誤訊息
            } else {
                resultTextView.text = "未找到列車動態資訊。"
                // 如果 ViewModel 已經設定了錯誤訊息，這裡就不需要再次設定
                // errorMessageTextView.text = "請輸入有效的列車號碼或稍後再試。"
            }
        })

        // 觀察錯誤訊息 LiveData 的變化
        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { errorMessage ->
            Log.e(TAG, "errorMessage 收到更新: $errorMessage")
            errorMessageTextView.text = errorMessage
            if (errorMessage.isNotBlank()) {
                resultTextView.text = "" // 有錯誤時，清空結果顯示
            }
        })

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: TrainStatusFragment 視圖銷毀。")
    }
}