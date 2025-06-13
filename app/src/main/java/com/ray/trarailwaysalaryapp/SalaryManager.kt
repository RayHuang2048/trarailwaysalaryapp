---
package com.ray.trarailwaysalaryapp

import android.content.Context
import java.math.BigDecimal
import org.apache.poi.xssf.usermodel.XSSFWorkbook // For .xlsx files
import java.io.IOException
import android.util.Log // 引入 Log 用於調試

// 人員類型列舉
enum class PersonnelType(val displayName: String) {
    OFFICER("職員"),
    OPERATOR("營運人員"),
    EMPLOYEE("從業人員")
}

// 職員的細分職位列舉 (更新：新增 professionalAllowanceIncrease 屬性)
enum class OfficerPosition(val displayName: String, val professionalAllowanceIncrease: Double) {
    STATION_MASTER("站長", 5820.0), // 專業加給增支數額
    DIRECTOR("主任", 5250.0),
    DEPUTY_TRAIN_CONDUCTOR("副座列車長", 5020.0),
    TRAFFIC_STAFF_GRADE("行車人員員級", 5020.0), // 行車人員員級與副座列車長相同
    TRAFFIC_STAFF_JUNIOR_GRADE("行車人員佐級", 4780.0),
    STATION_ATTENDANT("站務員", 4440.0),
    ASSISTANT_STATION_ATTENDANT("助理站務員", 4220.0)
}

// 營運人員職位列舉 (包含職務加給屬性)
enum class OperatingPosition(val displayName: String, val dutyAllowance: Double) {
    OPERATOR("營運員", 4220.0),
    SERVICE_ATTENDANT("服務員", 3640.0),
    ENGINE_DRIVER("司機員", 0.0),
    TRAIN_CONDUCTOR("列車長", 0.0),
    SHIFT_LEADER("領班", 0.0),
    STATION_ATTENDANT("站務員", 0.0),
    OTHER("其他營運人員", 0.0)
}

// 從業人員職位列舉 (更新：新增職務加給屬性)
enum class EmployeePosition(val displayName: String, val dutyAllowance: Double) {
    SERVICE_ATTENDANT("服務員", 3530.0), // 從業人員 - 服務員
    ASSISTANT_STATION_ATTENDANT("助理站務員", 4090.0), // 從業人員 - 助理站務員
    MAINTENANCE_STAFF("維修人員", 0.0),
    CLEANING_STAFF("清潔人員", 0.0),
    TICKET_SELLER("售票員", 0.0),
    OTHER("其他從業人員", 0.0)
}

// 班別類型列舉
enum class ShiftType(val displayName: String) {
    AB_SHIFT("AB班"),
    THREE_SHIFT("三班制")
}

class SalaryManager(private val context: Context) {

    // 定義 Excel 檔案名
    private val EXCEL_FILE_OFFICER = "職員薪額及專業加給表.xlsx"
    private val EXCEL_FILE_OPERATOR = "營運人員薪給表.xlsx"
    private val EXCEL_FILE_EMPLOYEE = "從業人員待遇表(備查本).xlsx"

    // 儲存從 Excel 載入的薪資數據 (Map of Maps，以便查詢)
    // 薪點 -> (薪額, 專業加給)
    private val officerSalaryData = mutableMapOf<Int, Map<String, Double>>()
    private val operatorSalaryData = mutableMapOf<Int, Map<String, Double>>()
    private val employeeSalaryData = mutableMapOf<Int, Map<String, Double>>()

    init {
        loadSalaryData()
    }

    /**
     * 從 Assets 目錄載入 Excel 薪資數據到內存。
     */
    private fun loadSalaryData() {
        try {
            // --- 讀取職員薪資表 (職員薪額及專業加給表.xlsx) ---
            context.assets.open(EXCEL_FILE_OFFICER).use { inputStream ->
                val workbook = XSSFWorkbook(inputStream)
                // 假設工作表名為 "Table1"
                val sheet = workbook.getSheet("Table1") ?: workbook.getSheetAt(0) // 嘗試按名稱獲取，若無則取第一個工作表

                if (sheet == null) {
                    Log.e("SalaryManager", "職員薪資表: 未找到工作表 'Table1' 或第一個工作表為空。")
                    return@use // 跳過此檔案的處理
                }

                val startRow = 4 // Excel 行號 - 1 (因為 POI 是從 0 開始計數，第5列是索引4)
                val endRow = 49 // Excel 行號 - 1 (第50列是索引49)

                val gradeCol = 3 // D 欄是索引 3
                val amountCol = 4 // E 欄是索引 4
                val professionalAllowanceCol = 5 // F 欄是索引 5

                for (i in startRow..endRow) {
                    val row = sheet.getRow(i)
                    if (row == null) {
                        Log.d("SalaryManager", "職員薪資表: 第 $i 行是空的，跳過。")
                        continue
                    }

                    // 讀取薪點 (D 欄)
                    val gradeCell = row.getCell(gradeCol)
                    val grade = try {
                        gradeCell?.numericCellValue?.toInt()
                    } catch (e: Exception) {
                        Log.w("SalaryManager", "職員薪資表: 無法解析第 $i 行 D 欄的薪點：${gradeCell?.stringCellValue ?: "空"}")
                        null
                    }

                    // 讀取薪額 (E 欄)
                    val amountCell = row.getCell(amountCol)
                    val amount = try {
                        amountCell?.numericCellValue
                    } catch (e: Exception) {
                        Log.w("SalaryManager", "職員薪資表: 無法解析第 $i 行 E 欄的薪額：${amountCell?.stringCellValue ?: "空"}")
                        null
                    }

                    // 讀取專業加給 (F 欄)
                    val professionalAllowanceCell = row.getCell(professionalAllowanceCol)
                    val professionalAllowance = try {
                        professionalAllowanceCell?.numericCellValue
                    } catch (e: Exception) {
                        Log.w("SalaryManager", "職員薪資表: 無法解析第 $i 行 F 欄的專業加給：${professionalAllowanceCell?.stringCellValue ?: "空"}")
                        null
                    }

                    if (grade != null && amount != null && professionalAllowance != null) {
                        officerSalaryData[grade] = mapOf(
                            "amount" to amount,
                            "" to professionalAllowance
                        )
                        Log.d("SalaryManager", "載入職員薪資: 薪點=$grade, 薪額=$amount, 專業加給=$professionalAllowance")
                    } else {
                        Log.w("SalaryManager", "職員薪資表: 第 $i 行數據不完整，跳過。薪點:$grade, 薪額:$amount, 專業加給:$professionalAllowance")
                    }
                }
                Log.d("SalaryManager", "職員薪資數據載入完成，共 ${officerSalaryData.size} 筆。")
            }

            // --- 讀取營運人員薪給表 (營運人員薪給表.xlsx) ---
            context.assets.open(EXCEL_FILE_OPERATOR).use { inputStream ->
                val workbook = XSSFWorkbook(inputStream)
                val sheet = workbook.getSheetAt(0) // 假設在第一個工作表
                // TODO: 解析營運人員數據並存入 operatorSalaryData
                Log.d("SalaryManager", "營運人員薪資表待實作載入邏輯。")
            }

            // --- 讀取從業人員待遇表 (從業人員待遇表(備查本).xlsx) ---
            context.assets.open(EXCEL_FILE_EMPLOYEE).use { inputStream ->
                val workbook = XSSFWorkbook(inputStream)
                val sheet = workbook.getSheetAt(0) // 假設在第一個工作表
                // TODO: 解析從業人員數據並存入 employeeSalaryData
                Log.d("SalaryManager", "從業人員待遇表待實作載入邏輯。")
            }

        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("SalaryManager", "讀取 Excel 檔案失敗: ${e.message}")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SalaryManager", "解析 Excel 檔案時發生錯誤: ${e.message}")
        }
    }


    /**
     * 根據輸入參數計算薪資。
     * 返回一個包含各種薪資組件的 Map。
     */
    fun getSalary(
        personnelType: PersonnelType,
        grade: Int,
        officerPosition: OfficerPosition?,
        operatingPosition: OperatingPosition?,
        employeePosition: EmployeePosition?,
        shiftType: ShiftType,
        overtimeHoursAB133: Double,
        overtimeHoursAB166: Double,
        holidayHoursAB: Double,
        overtimeHoursThreeShift134: Double,
        overtimeHoursThreeShift167: Double,
        dayShiftDays: Int,
        nightShiftDays: Int,
        nightShiftAllowancePerDay: BigDecimal
    ): Map<String, Any>? {

        var amount = 0.0 // 月支數額 / 基本薪
        var professionalAllowance = 0.0 // 專業加給
        var dutySalaryAllowance = 0.0 // 職務薪津貼
        var talentRetentionAllowance = 0.0 // 留才職務津貼
        var hourlyWage = 0.0
        var dailyWage = 0.0

        when (personnelType) {
            PersonnelType.OFFICER -> {
                // 從載入的數據中獲取薪資
                val officerData = officerSalaryData[grade]
                if (officerData != null) {
                    amount = officerData["amount"] ?: 0.0
                    // 專業加給從 Excel 讀取
                    professionalAllowance = officerData["professionalAllowance"] ?: 0.0
                } else {
                    Log.w("SalaryManager", "職員薪資: 未找到薪點 $grade 的數據，使用預設值。")
                    // 如果 Excel 中沒有找到，使用硬編碼預設值 (作為備用)
                    // 注意：這些硬編碼值目前只包含基本薪額和專業加給，沒有包含額外的增支數額
                    when (officerPosition) {
                        OfficerPosition.STATION_MASTER -> { amount = 45000.0; professionalAllowance = 15000.0 }
                        OfficerPosition.DIRECTOR -> { amount = 42000.0; professionalAllowance = 14000.0 }
                        OfficerPosition.DEPUTY_TRAIN_CONDUCTOR -> { amount = 40000.0; professionalAllowance = 13000.0 }
                        OfficerPosition.TRAFFIC_STAFF_GRADE -> { amount = 38000.0; professionalAllowance = 12000.0 }
                        OfficerPosition.TRAFFIC_STAFF_JUNIOR_GRADE -> { amount = 36000.0; professionalAllowance = 11000.0 }
                        OfficerPosition.STATION_ATTENDANT -> { amount = 35000.0; professionalAllowance = 10000.0 }
                        OfficerPosition.ASSISTANT_STATION_ATTENDANT -> { amount = 32000.0; professionalAllowance = 9500.0 }
                        else -> { /* 不處理，保持 0.0 */ }
                    }
                }

                // 根據職員職位，將「專業加給增支數額」加到專業加給中
                professionalAllowance += officerPosition?.professionalAllowanceIncrease ?: 0.0

                // 職務薪津貼和留才職務津貼目前還是硬編碼，需要確認這些是否在 Excel 裡面或者有其他規則
                when (officerPosition) {
                    OfficerPosition.STATION_MASTER -> { dutySalaryAllowance = 8000.0; talentRetentionAllowance = 5000.0 }
                    OfficerPosition.DIRECTOR -> { dutySalaryAllowance = 7000.0; talentRetentionAllowance = 4000.0 }
                    OfficerPosition.DEPUTY_TRAIN_CONDUCTOR -> { dutySalaryAllowance = 6000.0; talentRetentionAllowance = 3000.0 }
                    OfficerPosition.TRAFFIC_STAFF_GRADE -> { dutySalaryAllowance = 5000.0; talentRetentionAllowance = 2500.0 }
                    OfficerPosition.TRAFFIC_STAFF_JUNIOR_GRADE -> { dutySalaryAllowance = 4000.0; talentRetentionAllowance = 2000.0 }
                    OfficerPosition.STATION_ATTENDANT -> { dutySalaryAllowance = 3000.0; talentRetentionAllowance = 1500.0 }
                    OfficerPosition.ASSISTANT_STATION_ATTENDANT -> { dutySalaryAllowance = 2500.0; talentRetentionAllowance = 1200.0 }
                    else -> { /* 保持 0.0 */ }
                }

                // 職員的時薪計算
                hourlyWage = (amount + professionalAllowance + dutySalaryAllowance) / 160.0
                // 職員的日薪計算 (假設每月 20 天工作日)
                dailyWage = (amount + professionalAllowance + dutySalaryAllowance) / 20.0
            }
            PersonnelType.OPERATOR -> {
                // TODO: 這裡未來會從 operatorSalaryData 查詢
                amount = 30000.0 + grade * 100.0 // 假設基本薪隨薪級增加
                professionalAllowance = 8000.0 + grade * 50.0
                talentRetentionAllowance = 1000.0 + grade * 20.0
                dutySalaryAllowance = operatingPosition?.dutyAllowance ?: 0.0

                hourlyWage = (amount + professionalAllowance + dutySalaryAllowance) / 168.0
                dailyWage = (amount + professionalAllowance + dutySalaryAllowance) / 21.0
            }
            PersonnelType.EMPLOYEE -> {
                // TODO: 這裡未來會從 employeeSalaryData 查詢
                amount = 28000.0 + grade * 80.0
                professionalAllowance = 7000.0 + grade * 40.0
                talentRetentionAllowance = 0.0 // 從業人員沒有留才職務津貼
                dutySalaryAllowance = employeePosition?.dutyAllowance ?: 0.0

                hourlyWage = (amount + professionalAllowance + dutySalaryAllowance) / 176.0
                dailyWage = (amount + professionalAllowance + dutySalaryAllowance) / 22.0
            }
        }


        // 計算加班費
        var totalOvertimePayAB = 0.0
        var overtimePayAB133 = 0.0
        var overtimePayAB166 = 0.0
        var holidayOvertimePayAB = 0.0

        var totalOvertimePayThreeShift = 0.0
        var fixedThreeShiftOvertimePay = 0.0
        var totalNightShiftAllowance = BigDecimal.ZERO.toDouble()
        var overtimePayThreeShift134 = 0.0
        var overtimePayThreeShift167 = 0.0

        when (shiftType) {
            ShiftType.AB_SHIFT -> {
                overtimePayAB133 = hourlyWage * overtimeHoursAB133 * 1.33
                overtimePayAB166 = hourlyWage * overtimeHoursAB166 * 1.66
                holidayOvertimePayAB = hourlyWage * holidayHoursAB * 2.0

                totalOvertimePayAB = overtimePayAB133 + overtimePayAB166 + holidayOvertimePayAB
            }
            ShiftType.THREE_SHIFT -> {
                val fixedOvertimeHours = 24.0
                fixedThreeShiftOvertimePay = hourlyWage * fixedOvertimeHours * 1.33

                totalNightShiftAllowance = nightShiftAllowancePerDay.multiply(nightShiftDays.toBigDecimal()).toDouble()

                overtimePayThreeShift134 = hourlyWage * overtimeHoursThreeShift134 * 1.34
                overtimePayThreeShift167 = hourlyWage * overtimeHoursThreeShift167 * 1.67

                totalOvertimePayThreeShift = fixedThreeShiftOvertimePay + overtimePayThreeShift134 + overtimePayThreeShift167
            }
        }

        return mapOf(
            "amount" to amount,
            "professionalAllowance" to professionalAllowance,
            "dutySalaryAllowance" to dutySalaryAllowance,
            "talentRetentionAllowance" to talentRetentionAllowance,
            "hourlyWage" to hourlyWage,
            "dailyWage" to dailyWage,
            "totalOvertimePayAB" to totalOvertimePayAB,
            "overtimePayAB133" to overtimePayAB133,
            "overtimePayAB166" to overtimePayAB166,
            "holidayOvertimePayAB" to holidayOvertimePayAB,
            "totalOvertimePayThreeShift" to totalOvertimePayThreeShift,
            "fixedThreeShiftOvertimePay" to fixedThreeShiftOvertimePay,
            "totalNightShiftAllowance" to totalNightShiftAllowance,
            "overtimePayThreeShift134" to overtimePayThreeShift134,
            "overtimePayThreeShift167" to overtimePayThreeShift167
        )
    }
}