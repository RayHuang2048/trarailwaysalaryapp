package com.ray.trarailwaysalaryapp

import java.math.BigDecimal // 確保有導入 BigDecimal

data class OfficerSalary(
    val grade: Int,
    val amount: BigDecimal, // <-- 這裡必須是 BigDecimal
    val professionalAllowance: BigDecimal, // <-- 這裡必須是 BigDecimal
    val dutySalary: BigDecimal? = null // <-- 這裡必須是 BigDecimal?
)