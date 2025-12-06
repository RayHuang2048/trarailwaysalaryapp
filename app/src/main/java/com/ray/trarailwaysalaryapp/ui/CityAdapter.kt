package com.ray.trarailwaysalaryapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ray.trarailwaysalaryapp.R

/**
 * 城市選擇的 RecyclerView Adapter
 */
class CityAdapter(
    private var cities: List<Pair<String, Int>>, // Pair<城市名稱, 車站數量>
    private val onCityClickListener: (String) -> Unit
) : RecyclerView.Adapter<CityAdapter.CityViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_city, parent, false)
        return CityViewHolder(view)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        holder.bind(cities[position])
    }

    override fun getItemCount(): Int = cities.size

    fun updateCities(newCities: List<Pair<String, Int>>) {
        cities = newCities
        notifyDataSetChanged()
    }

    inner class CityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cityNameTextView: TextView = itemView.findViewById(R.id.textview_city_name)
        private val stationCountTextView: TextView = itemView.findViewById(R.id.textview_station_count)

        fun bind(cityInfo: Pair<String, Int>) {
            cityNameTextView.text = cityInfo.first
            stationCountTextView.text = "${cityInfo.second} 個車站"
            itemView.setOnClickListener {
                onCityClickListener(cityInfo.first)
            }
        }
    }
}
