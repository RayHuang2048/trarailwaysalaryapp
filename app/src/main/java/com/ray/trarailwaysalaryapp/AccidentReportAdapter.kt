package com.ray.trarailwaysalaryapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AccidentReportAdapter(private val reports: MutableList<AccidentReport>) :
    RecyclerView.Adapter<AccidentReportAdapter.AccidentReportViewHolder>() {

    class AccidentReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewReportId: TextView = itemView.findViewById(R.id.textViewReportId)
        val textViewReporterName: TextView = itemView.findViewById(R.id.textViewReporterName)
        val textViewDateTime: TextView = itemView.findViewById(R.id.textViewDateTime)
        val textViewLocation: TextView = itemView.findViewById(R.id.textViewLocation)
        val textViewDescription: TextView = itemView.findViewById(R.id.textViewDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccidentReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_accident_report, parent, false)
        return AccidentReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccidentReportViewHolder, position: Int) {
        val report = reports[position]
        holder.textViewReportId.text = "回報 ID: #${report.id.substring(0, 8)}" // 顯示 ID 的前8位
        holder.textViewReporterName.text = "回報人: ${report.reporterName.ifEmpty { "匿名" }}" // 如果沒有輸入姓名，顯示匿名
        holder.textViewDateTime.text = "時間: ${report.dateTime}"
        holder.textViewLocation.text = "地點: ${report.location}"
        holder.textViewDescription.text = "描述: ${report.description}"
    }

    override fun getItemCount(): Int = reports.size

    // 新增事故回報的方法
    fun addReport(report: AccidentReport) {
        reports.add(0, report) // 新增到列表頂部
        notifyItemInserted(0) // 通知 RecyclerView 有新項目插入到位置 0
    }
}