package com.ray.trarailwaysalaryapp.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ray.trarailwaysalaryapp.R
import com.ray.trarailwaysalaryapp.data.StopTime
import com.ray.trarailwaysalaryapp.viewmodel.TrainStatusViewModel

class TrainStatusFragment : Fragment() {

    private val TAG = "TrainStatusFragment"

    private lateinit var viewModel: TrainStatusViewModel
    private lateinit var trainNoEditText: EditText
    private lateinit var queryButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var errorMessageTextView: TextView
    private lateinit var trainRouteComposeView: ComposeView

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
        trainRouteComposeView = view.findViewById(R.id.trainRouteComposeView)

        // 設定查詢按鈕的點擊事件
        queryButton.setOnClickListener {
            val trainNo = trainNoEditText.text.toString()
            Log.d(TAG, "查詢按鈕被點擊，列車號碼: $trainNo")
            viewModel.queryTrainLiveStatus(trainNo) // 這個呼叫現在也會觸發時刻表查詢
        }

        // 觀察列車動態 LiveData 的變化
        viewModel.trainLiveData.observe(viewLifecycleOwner, Observer { trainList ->
            Log.d(TAG, "trainLiveData 收到更新。列車數量: ${trainList.size}")
            // 清空之前的內容，準備顯示新的動態和時刻表
            resultTextView.text = ""
            if (trainList.isNotEmpty()) {
                val train = trainList[0] // 假設我們只顯示第一筆列車的資訊
                val statusText = buildString {
                    append("列車號碼: ${train.TrainNo}\n")
                    append("車種: ${train.TrainTypeName.zhTw} (${train.TrainTypeName.en})\n")
                    append("目前車站: ${train.StationName?.zhTw ?: "站間行駛"}\n")
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

                // 更新 ComposeView
                val stopTimes = viewModel.trainTimetableLiveData.value ?: emptyList()
                updateTrainRouteView(stopTimes, train.StationName?.zhTw)
            } else {
                resultTextView.text = "未找到列車動態資訊。"
                updateTrainRouteView(emptyList(), null)
            }
        })

        // *** 新增：觀察列車時刻表 LiveData 的變化 ***
        viewModel.trainTimetableLiveData.observe(viewLifecycleOwner, Observer { stopTimes ->
            Log.d(TAG, "trainTimetableLiveData 收到更新。停靠站數量: ${stopTimes.size}")
            val currentText = resultTextView.text.toString()
            val timetableSection = StringBuilder()

            if (stopTimes.isNotEmpty()) {
                timetableSection.append("\n--- 列車時刻表 ---\n")
                for (stop in stopTimes) {
                    timetableSection.append("站序: ${stop.Sequence}, ")
                    timetableSection.append("車站: ${stop.StationName.zhTw} (${stop.StationName.en}), ")
                    timetableSection.append("抵達: ${stop.ArrivalTime ?: "N/A"}, ") // 如果為 null 顯示 N/A
                    timetableSection.append("出發: ${stop.DepartureTime ?: "N/A"}\n") // 如果為 null 顯示 N/A
                }
                // 更新 ComposeView
                val train = viewModel.trainLiveData.value?.firstOrNull()
                updateTrainRouteView(stopTimes, train?.StationName?.zhTw)
            } else {
                timetableSection.append("\n--- 未找到列車時刻表資訊 ---")
                updateTrainRouteView(emptyList(), null)
            }
            // 將時刻表資訊追加到現有的列車動態資訊後面
            resultTextView.text = currentText + timetableSection.toString()
        })
        // *** 新增觀察結束 ***


        // 觀察錯誤訊息 LiveData 的變化
        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { errorMessage ->
            Log.e(TAG, "errorMessage 收到更新: $errorMessage")
            errorMessageTextView.text = errorMessage
            if (errorMessage.isNotBlank()) {
                // 如果有錯誤，清空結果顯示，但保留錯誤訊息
                resultTextView.text = ""
                updateTrainRouteView(emptyList(), null)
            }
        })

        return view
    }

    private fun updateTrainRouteView(stops: List<StopTime>, currentStationName: String?) {
        trainRouteComposeView.setContent {
            TrainRouteLine(stops = stops, currentStationName = currentStationName)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: TrainStatusFragment 視圖銷毀。")
    }
}
