package com.ray.trarailwaysalaryapp.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ray.trarailwaysalaryapp.R
import com.ray.trarailwaysalaryapp.viewmodel.TrainTimetableViewModel

import androidx.fragment.app.viewModels
import com.ray.trarailwaysalaryapp.data.StopTime
import com.ray.trarailwaysalaryapp.viewmodel.TrainStatusViewModel
import androidx.lifecycle.Observer
import androidx.compose.ui.platform.ComposeView
import android.widget.EditText
import android.util.Log

class TrainTimetableFragment : Fragment(R.layout.fragment_train_timetable) {

    private val TAG = "TrainTimetableFragment"
    private val timetableViewModel: TrainTimetableViewModel by activityViewModels()
    private val statusViewModel: TrainStatusViewModel by viewModels()

    // 時刻表 UI
    private lateinit var buttonStartStation: Button
    private lateinit var textStartStation: TextView
    private lateinit var buttonArrivalStation: Button
    private lateinit var textArrivalStation: TextView
    private lateinit var buttonQueryTimetable: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var odTimetableAdapter: ODTimetableAdapter

    // 列車動態 UI
    private lateinit var trainNoEditText: EditText
    private lateinit var queryStatusButton: Button
    private lateinit var statusResultTextView: TextView
    private lateinit var statusErrorMessageTextView: TextView
    private lateinit var trainRouteComposeView: ComposeView

    private var allStations: List<com.ray.trarailwaysalaryapp.data.Station> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 初始化 UI 元件 ---
        // 時刻表
        buttonStartStation = view.findViewById(R.id.button_start_station)
        textStartStation = view.findViewById(R.id.text_start_station)
        buttonArrivalStation = view.findViewById(R.id.button_arrival_station)
        textArrivalStation = view.findViewById(R.id.text_arrival_station)
        buttonQueryTimetable = view.findViewById(R.id.button_query_timetable)
        recyclerView = view.findViewById(R.id.recycler_view_timetable)
        progressBar = view.findViewById(R.id.progress_bar)

        // 列車動態
        trainNoEditText = view.findViewById(R.id.trainNoEditText)
        queryStatusButton = view.findViewById(R.id.queryStatusButton)
        statusResultTextView = view.findViewById(R.id.statusResultTextView)
        statusErrorMessageTextView = view.findViewById(R.id.statusErrorMessageTextView)
        trainRouteComposeView = view.findViewById(R.id.trainRouteComposeView)

        // --- 初始化 RecyclerView ---
        odTimetableAdapter = ODTimetableAdapter(emptyList())
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = odTimetableAdapter
        }

        // --- 設置監聽器 ---
        
        // 列車動態查詢按鈕
        queryStatusButton.setOnClickListener {
            val trainNo = trainNoEditText.text.toString()
            Log.d(TAG, "查詢列車動態: $trainNo")
            statusViewModel.queryTrainLiveStatus(trainNo)
        }

        // 起程站按鈕
        buttonStartStation.setOnClickListener {
            StationSelectionDialogFragment.newInstance("start").show(parentFragmentManager, StationSelectionDialogFragment.FRAGMENT_TAG)
        }

        // 到達站按鈕
        buttonArrivalStation.setOnClickListener {
            StationSelectionDialogFragment.newInstance("arrival").show(parentFragmentManager, StationSelectionDialogFragment.FRAGMENT_TAG)
        }

        // 時刻表查詢按鈕
        buttonQueryTimetable.setOnClickListener {
            timetableViewModel.queryODTimetable()
        }

        // --- 觀察 LiveData (時刻表) ---
        timetableViewModel.startStation.observe(viewLifecycleOwner) {
            textStartStation.text = it?.stationName?.zhTw ?: "尚未選擇"
        }
        timetableViewModel.arrivalStation.observe(viewLifecycleOwner) {
            textArrivalStation.text = it?.stationName?.zhTw ?: "尚未選擇"
        }
        timetableViewModel.odTimetableResults.observe(viewLifecycleOwner) { results ->
            odTimetableAdapter.updateTimetables(results)
        }
        timetableViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            buttonQueryTimetable.isEnabled = !isLoading
        }
        timetableViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }

        // --- 觀察 LiveData (列車動態) ---
        statusViewModel.allStations.observe(viewLifecycleOwner, Observer { stations ->
            allStations = stations
        })

        statusViewModel.trainLiveData.observe(viewLifecycleOwner, Observer { trainList ->
            statusResultTextView.text = ""
            if (trainList.isNotEmpty()) {
                val train = trainList[0]
                val statusText = buildString {
                    append("列車號碼: ${train.TrainNo}\n")
                    append("車種: ${train.TrainTypeName.zhTw}\n")
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
                    append("\n更新時間: ${train.UpdateTime}")
                }
                statusResultTextView.text = statusText
                statusErrorMessageTextView.text = ""

                val stopTimes = statusViewModel.trainTimetableLiveData.value ?: emptyList()
                updateTrainRouteView(stopTimes, train)
            } else {
                statusResultTextView.text = "未找到列車動態資訊。"
                updateTrainRouteView(emptyList(), null)
            }
        })

        statusViewModel.trainTimetableLiveData.observe(viewLifecycleOwner, Observer { stopTimes ->
            val train = statusViewModel.trainLiveData.value?.firstOrNull()
            updateTrainRouteView(stopTimes, train)
        })

        statusViewModel.errorMessage.observe(viewLifecycleOwner, Observer { errorMessage ->
            statusErrorMessageTextView.text = errorMessage
            if (errorMessage.isNotBlank()) {
                statusResultTextView.text = ""
                updateTrainRouteView(emptyList(), null)
            }
        })
    }

    private fun updateTrainRouteView(stops: List<StopTime>, trainInfo: com.ray.trarailwaysalaryapp.data.TrainLiveInfo?) {
        val position = calculateTrainPosition(stops, trainInfo)
        trainRouteComposeView.setContent {
            TrainRouteLine(stops = stops, currentPosition = position)
        }
    }

    private fun calculateTrainPosition(stops: List<StopTime>, trainInfo: com.ray.trarailwaysalaryapp.data.TrainLiveInfo?): Float? {
        if (trainInfo == null) return null
        val currentID = trainInfo.StationID ?: return null

        val stopIndex = stops.indexOfFirst { it.StationID == currentID }
        if (stopIndex != -1) {
            val status = trainInfo.TrainStationStatus ?: 0
            var pos = stopIndex.toFloat()
            if (status == 1 && stopIndex > 0) pos -= 0.5f
            if (status == 2 && stopIndex < stops.size - 1) pos += 0.5f
            return pos
        }

        if (allStations.isEmpty()) return null
        val currentGlobalIndex = allStations.indexOfFirst { it.stationID == currentID }
        if (currentGlobalIndex == -1) return null

        for (i in 0 until stops.size - 1) {
            val stopA = stops[i]
            val stopB = stops[i+1]
            val globalA = allStations.indexOfFirst { it.stationID == stopA.StationID }
            val globalB = allStations.indexOfFirst { it.stationID == stopB.StationID }

            if (globalA != -1 && globalB != -1) {
                val minIndex = minOf(globalA, globalB)
                val maxIndex = maxOf(globalA, globalB)
                if (currentGlobalIndex > minIndex && currentGlobalIndex < maxIndex) {
                    return i.toFloat() + 0.5f
                }
            }
        }
        return null
    }
}
