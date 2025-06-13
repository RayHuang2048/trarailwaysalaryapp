package com.ray.trarailwaysalaryapp

// 職員的薪資數據模型
data class OfficerSalary(
    val grade: Int,             // 薪點 (D欄)
    val amount: Double,         // 薪額 (E欄)
    val professionalAllowance: Double, // 專業加給 (F欄)
    val dutySalary: Double? = null // 職務薪 (G欄，如果Excel有這欄的話)
    // val talentRetentionAllowance: Double? = null // 移除此行，留才津貼現在是固定值
)
