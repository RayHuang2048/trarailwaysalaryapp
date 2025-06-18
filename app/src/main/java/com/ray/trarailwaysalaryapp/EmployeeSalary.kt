package com.ray.trarailwaysalaryapp

import java.math.BigDecimal // 確保有導入 BigDecimal

data class EmployeeSalary(
    val grade: Int,
    val amount: BigDecimal // <-- 這裡必須是 BigDecimal
)