package com.ray.trarailwaysalaryapp.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ray.trarailwaysalaryapp.R
import com.ray.trarailwaysalaryapp.data.CityOrder
import com.ray.trarailwaysalaryapp.data.Station
import com.ray.trarailwaysalaryapp.data.StationCityMapping
import com.ray.trarailwaysalaryapp.viewmodel.TrainStatusViewModel
import com.ray.trarailwaysalaryapp.viewmodel.TrainTimetableViewModel

class StationSelectionDialogFragment : DialogFragment() {

    private val TAG = "StationSelectionDialog"
    
    private val trainStatusViewModel: TrainStatusViewModel by activityViewModels()
    private val trainTimetableViewModel: TrainTimetableViewModel by activityViewModels()
    
    private lateinit var stationAdapter: StationAdapter
    private lateinit var cityAdapter: CityAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var titleTextView: TextView
    private lateinit var backButton: ImageButton
    
    private var allStations: List<Station> = emptyList()
    private var selectedCity: String? = null  // null = 顯示城市列表, 非null = 顯示該城市的車站
    private var stationType: String? = null

    companion object {
        const val FRAGMENT_TAG = "StationSelectionDialog"
        private const val ARG_STATION_TYPE = "station_type"

        fun newInstance(stationType: String): StationSelectionDialogFragment {
            val args = Bundle()
            args.putString(ARG_STATION_TYPE, stationType)
            val fragment = StationSelectionDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_station_selection, container, false)
    }

    override fun onStart() {
        super.onStart()
        // 設定對話框為接近全螢幕大小
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerview_stations)
        searchEditText = view.findViewById(R.id.edittext_search_station)
        titleTextView = view.findViewById(R.id.textview_title)
        backButton = view.findViewById(R.id.button_back)
        stationType = requireArguments().getString(ARG_STATION_TYPE)

        // 初始化車站 Adapter
        stationAdapter = StationAdapter(emptyList()) { station ->
            when (stationType) {
                "start" -> trainTimetableViewModel.setStartStation(station)
                "arrival" -> trainTimetableViewModel.setArrivalStation(station)
            }
            dismiss()
        }

        // 初始化城市 Adapter
        cityAdapter = CityAdapter(emptyList()) { city ->
            showStationsForCity(city)
        }

        recyclerView.layoutManager = LinearLayoutManager(context)

        // 返回按鈕點擊事件
        backButton.setOnClickListener {
            showCityList()
        }

        // 觀察車站資料
        trainStatusViewModel.allStations.observe(viewLifecycleOwner) { stations ->
            allStations = stations
            showCityList()
        }

        // 搜尋功能
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (query.isNotEmpty()) {
                    // 搜尋時直接顯示所有符合的車站
                    showSearchResults(query)
                } else {
                    // 清空搜尋時，根據目前狀態顯示
                    if (selectedCity != null) {
                        showStationsForCity(selectedCity!!)
                    } else {
                        showCityList()
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * 顯示城市列表
     */
    private fun showCityList() {
        selectedCity = null
        titleTextView.text = "選擇縣市"
        backButton.visibility = View.GONE
        searchEditText.hint = "搜尋車站"
        
        Log.d(TAG, "showCityList called, allStations size: ${allStations.size}")
        
        // 使用 StationCityMapping 來分組車站
        val cityStationCount = allStations
            .groupBy { station -> 
                StationCityMapping.getCityForStation(station.stationName.zhTw)
            }
            .map { (city, stations) -> city to stations.size }
            .filter { it.first != "其他" }  // 過濾掉未知的車站
        
        Log.d(TAG, "City count: ${cityStationCount.size}, cities: ${cityStationCount.map { it.first }}")
        
        // 依照指定順序排序
        val sortedCities = cityStationCount.sortedBy { CityOrder.getOrder(it.first) }
        
        cityAdapter.updateCities(sortedCities)
        recyclerView.adapter = cityAdapter
    }

    /**
     * 顯示指定城市的車站列表
     */
    private fun showStationsForCity(city: String) {
        selectedCity = city
        titleTextView.text = city
        backButton.visibility = View.VISIBLE
        searchEditText.hint = "搜尋 $city 的車站"
        
        // 使用 StationCityMapping 來過濾該城市的車站
        val filteredStations = allStations.filter { station ->
            StationCityMapping.getCityForStation(station.stationName.zhTw) == city
        }
        stationAdapter.updateStations(filteredStations)
        recyclerView.adapter = stationAdapter
    }

    /**
     * 顯示搜尋結果
     */
    private fun showSearchResults(query: String) {
        titleTextView.text = "搜尋結果"
        backButton.visibility = View.VISIBLE
        
        val filteredStations = allStations.filter {
            it.stationName.zhTw.contains(query, ignoreCase = true) ||
            it.stationName.en.contains(query, ignoreCase = true) ||
            it.locationCity.contains(query, ignoreCase = true)
        }
        stationAdapter.updateStations(filteredStations)
        recyclerView.adapter = stationAdapter
    }
}
