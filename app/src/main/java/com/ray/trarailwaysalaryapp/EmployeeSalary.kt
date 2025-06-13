package com.ray.trarailwaysalaryapp

// 從業人員的薪資數據模型
// IMPORTANT: This data class should ONLY contain 'grade' and 'amount'.
// The 'professionalAllowance' is now retrieved from the EmployeePosition enum.
data class EmployeeSalary(
    val grade: Int,             // 薪點 (B欄)
    val amount: Double          // 薪額 (C欄)
)
