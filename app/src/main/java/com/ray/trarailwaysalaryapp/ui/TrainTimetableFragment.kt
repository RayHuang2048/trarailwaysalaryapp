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

class TrainTimetableFragment : Fragment(R.layout.fragment_train_timetable) {

    private val viewModel: TrainTimetableViewModel by activityViewModels()

    private lateinit var buttonStartStation: Button
    private lateinit var textStartStation: TextView
    private lateinit var buttonArrivalStation: Button
    private lateinit var textArrivalStation: TextView
    private lateinit var buttonQueryTimetable: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var odTimetableAdapter: ODTimetableAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonStartStation = view.findViewById(R.id.button_start_station)
        textStartStation = view.findViewById(R.id.text_start_station)
        buttonArrivalStation = view.findViewById(R.id.button_arrival_station)
        textArrivalStation = view.findViewById(R.id.text_arrival_station)
        buttonQueryTimetable = view.findViewById(R.id.button_query_timetable)
        recyclerView = view.findViewById(R.id.recycler_view_timetable)
        progressBar = view.findViewById(R.id.progress_bar)

        // 初始化 RecyclerView
        odTimetableAdapter = ODTimetableAdapter(emptyList())
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = odTimetableAdapter
        }

        // 起程站按鈕
        buttonStartStation.setOnClickListener {
            StationSelectionDialogFragment.newInstance("start").show(parentFragmentManager, StationSelectionDialogFragment.FRAGMENT_TAG)
        }

        // 到達站按鈕
        buttonArrivalStation.setOnClickListener {
            StationSelectionDialogFragment.newInstance("arrival").show(parentFragmentManager, StationSelectionDialogFragment.FRAGMENT_TAG)
        }

        // 查詢按鈕
        buttonQueryTimetable.setOnClickListener {
            viewModel.queryODTimetable()
        }

        // 觀察起程站
        viewModel.startStation.observe(viewLifecycleOwner) {
            textStartStation.text = it?.stationName?.zhTw ?: "尚未選擇"
        }

        // 觀察到達站
        viewModel.arrivalStation.observe(viewLifecycleOwner) {
            textArrivalStation.text = it?.stationName?.zhTw ?: "尚未選擇"
        }

        // 觀察查詢結果
        viewModel.odTimetableResults.observe(viewLifecycleOwner) { results ->
            odTimetableAdapter.updateTimetables(results)
        }

        // 觀察載入狀態
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            buttonQueryTimetable.isEnabled = !isLoading
        }

        // 觀察錯誤訊息
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
