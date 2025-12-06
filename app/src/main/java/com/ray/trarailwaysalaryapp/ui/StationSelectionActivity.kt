package com.ray.trarailwaysalaryapp.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import com.ray.trarailwaysalaryapp.R
import com.ray.trarailwaysalaryapp.data.Station
import com.ray.trarailwaysalaryapp.viewmodel.StationSelectionViewModel

class StationSelectionActivity : AppCompatActivity() {

    private val viewModel: StationSelectionViewModel by viewModels()
    private lateinit var stationAdapter: StationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_station_selection)

        val searchView = findViewById<SearchView>(R.id.search_view_stations)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view_stations)

        stationAdapter = StationAdapter { station ->
            val resultIntent = Intent()
            resultIntent.putExtra("selected_station", station)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
        recyclerView.adapter = stationAdapter

        viewModel.stations.observe(this) { stations ->
            stationAdapter.submitList(stations)
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = viewModel.stations.value?.filter {
                    it.stationName.zhTw.contains(newText ?: "", ignoreCase = true) ||
                    it.stationName.en.contains(newText ?: "", ignoreCase = true)
                }
                stationAdapter.submitList(filteredList)
                return true
            }
        })
    }

    private class StationAdapter(private val onClick: (Station) -> Unit) :
        RecyclerView.Adapter<StationAdapter.StationViewHolder>() {

        private var stations: List<Station> = emptyList()

        fun submitList(newStations: List<Station>?) {
            stations = newStations ?: emptyList()
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return StationViewHolder(view)
        }

        override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
            val station = stations[position]
            holder.bind(station)
            holder.itemView.setOnClickListener { onClick(station) }
        }

        override fun getItemCount() = stations.size

        class StationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textView: TextView = itemView.findViewById(android.R.id.text1)

            fun bind(station: Station) {
                textView.text = "${station.stationName.zhTw} (${station.stationName.en})"
            }
        }
    }
}
