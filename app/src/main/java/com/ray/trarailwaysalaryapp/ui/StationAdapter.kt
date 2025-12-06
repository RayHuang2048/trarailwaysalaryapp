package com.ray.trarailwaysalaryapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ray.trarailwaysalaryapp.R
import com.ray.trarailwaysalaryapp.data.Station

class StationAdapter(
    private var stations: List<Station>,
    private val onStationClickListener: (Station) -> Unit
) : RecyclerView.Adapter<StationAdapter.StationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_station, parent, false)
        return StationViewHolder(view)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        holder.bind(stations[position])
    }

    override fun getItemCount(): Int = stations.size

    fun updateStations(newStations: List<Station>) {
        stations = newStations
        notifyDataSetChanged()
    }

    inner class StationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val stationNameTextView: TextView = itemView.findViewById(R.id.textview_station_name)
        private val stationCityTextView: TextView = itemView.findViewById(R.id.textview_station_city)

        fun bind(station: Station) {
            stationNameTextView.text = station.stationName.zhTw
            stationCityTextView.text = station.locationCity
            itemView.setOnClickListener {
                onStationClickListener(station)
            }
        }
    }
}
