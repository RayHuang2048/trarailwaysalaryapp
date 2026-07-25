package com.ray.trarailwaysalaryapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ray.trarailwaysalaryapp.R
import com.ray.trarailwaysalaryapp.data.ODTrainTimetable

/**
 * 顯示 OD 時刻表查詢結果的 RecyclerView Adapter
 */
class ODTimetableAdapter(
    private var timetables: List<ODTrainTimetable>
) : RecyclerView.Adapter<ODTimetableAdapter.TimetableViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimetableViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_od_timetable, parent, false)
        return TimetableViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimetableViewHolder, position: Int) {
        holder.bind(timetables[position])
    }

    override fun getItemCount(): Int = timetables.size

    fun updateTimetables(newTimetables: List<ODTrainTimetable>) {
        timetables = newTimetables
        notifyDataSetChanged()
    }

    inner class TimetableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val trainNoTextView: TextView = itemView.findViewById(R.id.textview_train_no)
        private val trainTypeTextView: TextView = itemView.findViewById(R.id.textview_train_type)
        private val departureTimeTextView: TextView = itemView.findViewById(R.id.textview_departure_time)
        private val arrivalTimeTextView: TextView = itemView.findViewById(R.id.textview_arrival_time)
        private val durationTextView: TextView = itemView.findViewById(R.id.textview_duration)
        private val routeTextView: TextView = itemView.findViewById(R.id.textview_route)
        private val priceTextView: TextView = itemView.findViewById(R.id.textview_price)

        fun bind(timetable: ODTrainTimetable) {
            val trainInfo = timetable.trainInfo
            // 假設 stopTimes 包含 [起程站, 到達站]
            val stopTimes = timetable.stopTimes ?: emptyList()
            val originStop = stopTimes.firstOrNull()
            val destStop = if (stopTimes.size >= 2) stopTimes.lastOrNull() else null

            // 車次
            trainNoTextView.text = trainInfo?.trainNo ?: "--"

            // 車種
            val trainType = trainInfo?.trainTypeName?.zhTw ?: "普通"
            trainTypeTextView.text = trainType

            // 出發時間
            val departureTime = originStop?.departureTime ?: "--:--"
            departureTimeTextView.text = departureTime

            // 抵達時間
            // 如果起程和到達站相同，或者只有一個站，顯示到達時間或 --
            val arrivalTime = destStop?.arrivalTime ?: "--:--"
            arrivalTimeTextView.text = arrivalTime

            // 計算行車時間
            val duration = calculateDuration(departureTime, arrivalTime)
            durationTextView.text = duration

            // 起訖站
            val startStation = trainInfo?.startingStationName?.zhTw ?: ""
            val endStation = trainInfo?.endingStationName?.zhTw ?: ""
            routeTextView.text = "$startStation → $endStation"

            // 票價
            if (timetable.fare != null) {
                priceTextView.visibility = View.VISIBLE
                priceTextView.text = "$ ${timetable.fare}"
            } else {
                priceTextView.visibility = View.GONE
            }
        }

        private fun calculateDuration(departure: String, arrival: String): String {
            try {
                val depParts = departure.split(":")
                val arrParts = arrival.split(":")
                
                if (depParts.size == 2 && arrParts.size == 2) {
                    val depMinutes = depParts[0].toInt() * 60 + depParts[1].toInt()
                    val arrMinutes = arrParts[0].toInt() * 60 + arrParts[1].toInt()
                    
                    var diff = arrMinutes - depMinutes
                    if (diff < 0) diff += 24 * 60  // 跨日
                    
                    val hours = diff / 60
                    val minutes = diff % 60
                    
                    return if (hours > 0) {
                        "${hours}時${minutes}分"
                    } else {
                        "${minutes}分"
                    }
                }
            } catch (e: Exception) {
                // 解析失敗
            }
            return "--"
        }
    }
}
