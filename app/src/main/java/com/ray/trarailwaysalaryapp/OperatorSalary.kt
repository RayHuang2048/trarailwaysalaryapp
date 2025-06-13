package com.ray.trarailwaysalaryapp

// 營運人員的薪資數據模型
data class OperatorSalary(
    val grade: Int,             // 薪點 (D欄)
    val amount: Double          // 月支數額 (E欄)
    // val talentRetentionAllowance: Double? = null // 移除此行，留才津貼現在是固定值
)
