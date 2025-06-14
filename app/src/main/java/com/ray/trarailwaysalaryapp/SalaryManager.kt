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

// 職員的細分職位列舉 (新增 additionalProfessionalAllowance 和 managerialAllowance 屬性)
enum class OfficerPosition(val displayName: String, val additionalProfessionalAllowance: Double, val managerialAllowance: Double) {
    STATION_MASTER("站長", 5820.0, 5960.0), // 站長主管加給 5960
    DIRECTOR("主任", 5250.0, 3970.0), // 主任主管加給 3970
    VICE_STATION_MASTER("副站長", 0.0, 3970.0), // 副站長，主管加給 3970。專業加給增支數額暫設為0，如果副站長有專業加給增支數額，請提供。
    DEPUTY_TRAIN_CONDUCTOR("副座列車長", 5020.0, 0.0), // 無主管加給
    TRAFFIC_STAFF_GRADE("行車人員員級", 5020.0, 0.0), // 無主管加給
    TRAFFIC_STAFF_JUNIOR_GRADE("行車人員佐級", 4780.0, 0.0), // 無主管加給
    STATION_ATTENDANT("站務員", 4440.0, 0.0), // 無主管加給
    ASSISTANT_STATION_ATTENDANT("助理站務員", 4220.0, 0.0) // 無主管加給
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

// 從業人員職位列舉 (包含職務加給屬性)
enum class EmployeePosition(val displayName: String, val dutyAllowance: Double) {
    SERVICE_ATTENDANT("服務員", 3530.0),
    ASSISTANT_STATION_ATTENDANT("助理站務員", 4090.0),
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
    private val officerSalaryData = mutableMapOf<Int, Map<String, Double>>() // 薪點 -> (薪額, 專業加給)
    private val operatorSalaryData = mutableMapOf<Int, Map<String, Double>>() // 薪點 -> (月支數額)
    private val employeeSalaryData = mutableMapOf<Int, Map<String, Double>>() // 薪點 -> (基本薪)

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
                val sheet = workbook.getSheet("Table1") ?: workbook.getSheetAt(0)

                if (sheet == null) {
                    Log.e("SalaryManager", "職員薪資表: 未找到工作表 'Table1' 或第一個工作表為空。")
                    return@use
                }

                val startRow = 4 // Excel 第5列是索引4
                val endRow = 49 // Excel 第50列是索引49

                val gradeCol = 3 // D 欄是索引 3
                val amountCol = 4 // E 欄是索引 4
                val professionalAllowanceCol = 5 // F 欄是索引 5

                for (i in startRow..endRow) {
                    val row = sheet.getRow(i)
                    if (row == null) {
                        Log.d("SalaryManager", "職員薪資表: 第 ${i + 1} 行是空的，跳過。")
                        continue
                    }

                    val gradeCell = row.getCell(gradeCol)
                    val grade = try { gradeCell?.numericCellValue?.toInt() } catch (e: Exception) { null }

                    val amountCell = row.getCell(amountCol)
                    val amount = try { amountCell?.numericCellValue } catch (e: Exception) { null }

                    val professionalAllowanceCell = row.getCell(professionalAllowanceCol)
                    val professionalAllowance = try { professionalAllowanceCell?.numericCellValue } catch (e: Exception) { null }

                    if (grade != null && amount != null && professionalAllowance != null) {
                        officerSalaryData[grade] = mapOf(
                            "amount" to amount,
                            "professionalAllowance" to professionalAllowance
                        )
                        Log.d("SalaryManager", "載入職員薪資: 薪點=$grade, 薪額=$amount, 專業加給=$professionalAllowance")
                    } else {
                        Log.w("SalaryManager", "職員薪資表: 第 ${i + 1} 行數據不完整，跳過。薪點:$grade, 薪額:$amount, 專業加給:$professionalAllowance")
                    }
                }
                Log.d("SalaryManager", "職員薪資數據載入完成，共 ${officerSalaryData.size} 筆。")
            }

            // --- 讀取營運人員薪給表 (營運人員薪給表.xlsx) ---
            context.assets.open(EXCEL_FILE_OPERATOR).use { inputStream ->
                val workbook = XSSFWorkbook(inputStream)
                val sheet = workbook.getSheet("Table1") ?: workbook.getSheetAt(0)

                if (sheet == null) {
                    Log.e("SalaryManager", "營運人員薪資表: 未找到工作表 'Table1' 或第一個工作表為空。")
                    return@use
                }

                val startRow = 3 // Excel 第4列是索引3
                val endRow = 39 // Excel 第40列是索引39

                val gradeCol = 3 // D 欄是索引 3
                val amountCol = 4 // E 欄是索引 4

                for (i in startRow..endRow) {
                    val row = sheet.getRow(i)
                    if (row == null) {
                        Log.d("SalaryManager", "營運人員薪資表: 第 ${i + 1} 行是空的，跳過。")
                        continue
                    }

                    val gradeCell = row.getCell(gradeCol)
                    val grade = try { gradeCell?.numericCellValue?.toInt() } catch (e: Exception) { null }

                    val amountCell = row.getCell(amountCol)
                    val amount = try { amountCell?.numericCellValue } catch (e: Exception) { null }

                    if (grade != null && amount != null) {
                        operatorSalaryData[grade] = mapOf(
                            "amount" to amount
                        )
                        Log.d("SalaryManager", "載入營運人員薪資: 薪點=$grade, 月支數額=$amount")
                    } else {
                        Log.w("SalaryManager", "營運人員薪資表: 第 ${i + 1} 行數據不完整，跳過。薪點:$grade, 月支數額:$amount")
                    }
                }
                Log.d("SalaryManager", "營運人員薪資數據載入完成，共 ${operatorSalaryData.size} 筆。")
            }

            // --- 讀取從業人員待遇表 (從業人員待遇表(備查本).xlsx) ---
            context.assets.open(EXCEL_FILE_EMPLOYEE).use { inputStream ->
                val workbook = XSSFWorkbook(inputStream)
                val sheet = workbook.getSheet("Table1") ?: workbook.getSheetAt(0)

                if (sheet == null) {
                    Log.e("SalaryManager", "從業人員待遇表: 未找到工作表 'Table1' 或第一個工作表為空。")
                    return@use
                }

                val startRow = 3 // Excel 第4列是索引3
                val endRow = 48 // Excel 第49列是索引48

                val gradeCol = 1 // B 欄是索引 1
                val amountCol = 2 // C 欄是索引 2

                for (i in startRow..endRow) {
                    val row = sheet.getRow(i)
                    if (row == null) {
                        Log.d("SalaryManager", "從業人員待遇表: 第 ${i + 1} 行是空的，跳過。")
                        continue
                    }

                    val gradeCell = row.getCell(gradeCol)
                    val grade = try { gradeCell?.numericCellValue?.toInt() } catch (e: Exception) { null }

                    val amountCell = row.getCell(amountCol)
                    val amount = try { amountCell?.numericCellValue } catch (e: Exception) { null }

                    if (grade != null && amount != null) {
                        employeeSalaryData[grade] = mapOf(
                            "amount" to amount
                        )
                        Log.d("SalaryManager", "載入從業人員薪資: 薪點=$grade, 薪額=$amount")
                    } else {
                        Log.w("SalaryManager", "從業人員待遇表: 第 ${i + 1} 行數據不完整，跳過。薪點:$grade, 薪額:$amount")
                    }
                }
                Log.d("SalaryManager", "從業人員薪資數據載入完成，共 ${employeeSalaryData.size} 筆。")
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
        officerPosition: OfficerPosition?, // 職員職位
        operatingPosition: OperatingPosition?,
        employeePosition: EmployeePosition?,
        shiftType: ShiftType,
        // AB班的加班時數修改為替三班一天天數和例假日國定假日加班天數
        replaceThreeShiftDays: Double, // 替三班一天天數
        holidayOvertimeDays: Double, // AB班例假日/國定假日加班天數 (AB班用)
        // 三班制的加班時數移除，只保留天數相關輸入
        dayShiftDays: Int, // 日班天數
        nightShiftDays: Int, // 夜班天數
        restDayOvertimeDays: Double, // 休息日出勤天數
        nationalHolidayAttendanceDays: Double, // 國定假日出勤天數 (三班制用)
        nationalHolidayEveNightShiftDays: Double, // 國定假日休班前一天接夜班天數
        nightShiftAllowancePerDay: BigDecimal
    ): Map<String, Any>? {

        var amount = 0.0 // 月支數額 / 基本薪
        var professionalAllowance = 0.0 // 專業加給 (僅職員使用，從 Excel 讀取)
        var additionalProfessionalAllowance = 0.0 // 職員專業加給增支數額 (來自 OfficerPosition enum)
        var managerialAllowance = 0.0 // 主管加給
        var dutySalaryAllowance = 0.0 // 職務薪津貼
        var talentRetentionAllowance = 0.0 // 留才職務津貼
        var hourlyWage = 0.0
        var dailyWage = 0.0

        when (personnelType) {
            PersonnelType.OFFICER -> {
                val officerData = officerSalaryData[grade]
                if (officerData != null) {
                    amount = officerData["amount"] ?: 0.0
                    professionalAllowance = officerData["professionalAllowance"] ?: 0.0 // 從 Excel 讀取專業加給
                } else {
                    Log.w("SalaryManager", "職員薪資: 未找到薪點 $grade 的數據，使用預設值。")
                    amount = 32000.0
                    professionalAllowance = 9500.0
                }

                // 從選定的職員職位獲取專業加給增支數額
                additionalProfessionalAllowance = officerPosition?.additionalProfessionalAllowance ?: 0.0
                // 從選定的職員職位獲取主管加給
                managerialAllowance = officerPosition?.managerialAllowance ?: 0.0

                // 統一設定職員的留才津貼為 2000 元
                talentRetentionAllowance = 2000.0

                // 職員的職務薪津貼明確設為 0
                dutySalaryAllowance = 0.0

                // 職員的日薪和時薪計算 (主管加給重新計入)
                val baseForDailyHourly = amount + professionalAllowance + additionalProfessionalAllowance + managerialAllowance
                dailyWage = baseForDailyHourly / 30.0
                hourlyWage = dailyWage / 8.0
            }
            PersonnelType.OPERATOR -> {
                // 營運人員的計算邏輯
                val operatorData = operatorSalaryData[grade]
                if (operatorData != null) {
                    amount = operatorData["amount"] ?: 0.0
                } else {
                    Log.w("SalaryManager", "營運人員薪資: 未找到薪點 $grade 的數據，使用預設值。")
                    amount = 30000.0 // 預設值
                }

                // 營運人員沒有專業加給，明確設為 0
                professionalAllowance = 0.0

                // 營運人員的職務津貼從 OperatingPosition enum 獲取
                dutySalaryAllowance = operatingPosition?.dutyAllowance ?: 0.0

                // 營運人員的留才津貼為 2000 (硬編碼)
                talentRetentionAllowance = 2000.0

                // 營運人員的日薪和時薪計算：(月支數額 + 職務薪) / 30 為日薪，再除以 8 為時薪
                val baseForDailyHourly = amount + dutySalaryAllowance
                dailyWage = baseForDailyHourly / 30.0
                hourlyWage = dailyWage / 8.0
            }
            PersonnelType.EMPLOYEE -> {
                // 從業人員的計算邏輯
                val employeeData = employeeSalaryData[grade]
                if (employeeData != null) {
                    amount = employeeData["amount"] ?: 0.0
                } else {
                    Log.w("SalaryManager", "從業人員薪資: 未找到薪點 $grade 的數據，使用預設值。")
                    amount = 28000.0 // 預設值
                }

                // 從業人員沒有專業加給，明確設為 0
                professionalAllowance = 0.0

                // 從業人員的留才職務津貼為 0 (硬編碼，若有變更請告知)
                talentRetentionAllowance = 0.0

                // 從業人員的職務津貼從 EmployeePosition enum 獲取
                dutySalaryAllowance = employeePosition?.dutyAllowance ?: 0.0

                // 從業人員的日薪和時薪計算：(薪額 + 職務薪) / 30 為日薪，再除以 8 為時薪
                val baseForDailyHourly = amount + dutySalaryAllowance
                dailyWage = baseForDailyHourly / 30.0
                hourlyWage = dailyWage / 8.0
            }
        }

        // 計算加班費
        var totalOvertimePayAB = 0.0
        var replaceThreeShiftOvertimePay = 0.0 // 替三班一天加班費
        var abClassHolidayOvertimePay = 0.0 // AB班 例假日/國定假日加班費

        var totalOvertimePayThreeShift = 0.0
        var calculatedThreeShiftOvertimePay = 0.0 // 三班制基於天數計算的加班費
        var restDayOvertimePay = 0.0 // 休息日加班費
        var nationalHolidayOvertimePay = 0.0 // 國定假日出勤加班費 (三班制用)
        var nationalHolidayEveNightShiftPay = 0.0 // 國定假日休班前一天接夜班加班費
        var totalNightShiftAllowance = BigDecimal.ZERO.toDouble()

        var monthlyBaseSalary = 0.0 // 月基本薪資 (含本薪、專業加給、職務加給、主管加給、留才津貼)
        var totalMonthlySalary = 0.0 // 這個月總薪資
        var totalMonthlyOvertimePay = 0.0 // 這個月總加班費

        when (shiftType) {
            ShiftType.AB_SHIFT -> {
                // 替三班一天加班費計算：每替一天，自動算兩小時1.33倍 + 一小時1.66倍
                val payPerReplaceDay = (hourlyWage * 2 * 1.33) + (hourlyWage * 1 * 1.66)
                replaceThreeShiftOvertimePay = payPerReplaceDay * replaceThreeShiftDays

                // 例假日/國定假日加班費 (AB班)：多一天薪水 (日薪 * 天數 * 1.0)
                abClassHolidayOvertimePay = dailyWage * holidayOvertimeDays * 1.0

                totalOvertimePayAB = replaceThreeShiftOvertimePay + abClassHolidayOvertimePay
                totalMonthlyOvertimePay = totalOvertimePayAB // AB班的總加班費

                monthlyBaseSalary = amount + professionalAllowance + additionalProfessionalAllowance + managerialAllowance + dutySalaryAllowance + talentRetentionAllowance
                totalMonthlySalary = monthlyBaseSalary + totalMonthlyOvertimePay + totalNightShiftAllowance // AB班沒有夜班津貼，但統一加上
            }
            ShiftType.THREE_SHIFT -> {
                // 三班制排班加班費: 日班天數和夜班天數，一天算加班1.2小時，費率1.33倍
                val totalShiftDays = dayShiftDays + nightShiftDays
                calculatedThreeShiftOvertimePay = hourlyWage * totalShiftDays * 1.2 * 1.33

                // 休息日出勤加班費計算：一天算加班11小時，前2小時1.33倍，中6小時1.66倍，後3小時2.66倍
                val payPerRestDayOvertimeHour = (hourlyWage * 2 * 1.33) + (hourlyWage * 6 * 1.66) + (hourlyWage * 3 * 2.66)
                restDayOvertimePay = payPerRestDayOvertimeHour * restDayOvertimeDays

                // 國定假日出勤加班費計算：一天算加班9小時，前8小時1倍，中2小時1.33倍，後1小時1.66倍
                val payPerNationalHolidayAttendanceHour = (hourlyWage * 8 * 1.0) + (hourlyWage * 2 * 1.33) + (hourlyWage * 1 * 1.66)
                nationalHolidayOvertimePay = payPerNationalHolidayAttendanceHour * nationalHolidayAttendanceDays

                // 國定假日休班前一天接夜班加班費：一天算加班8小時，1倍時薪
                nationalHolidayEveNightShiftPay = hourlyWage * 8 * nationalHolidayEveNightShiftDays * 1.0


                totalNightShiftAllowance = nightShiftAllowancePerDay.multiply(nightShiftDays.toBigDecimal()).toDouble()

                totalOvertimePayThreeShift = calculatedThreeShiftOvertimePay + restDayOvertimePay + nationalHolidayOvertimePay + nationalHolidayEveNightShiftPay
                totalMonthlyOvertimePay = totalOvertimePayThreeShift // 三班制的總加班費

                monthlyBaseSalary = amount + professionalAllowance + additionalProfessionalAllowance + managerialAllowance + dutySalaryAllowance + talentRetentionAllowance
                totalMonthlySalary = monthlyBaseSalary + totalMonthlyOvertimePay + totalNightShiftAllowance
            }
        }

        return mapOf(
            "amount" to amount,
            "professionalAllowance" to professionalAllowance,
            "additionalProfessionalAllowance" to additionalProfessionalAllowance,
            "managerialAllowance" to managerialAllowance,
            "dutySalaryAllowance" to dutySalaryAllowance,
            "talentRetentionAllowance" to talentRetentionAllowance,
            "hourlyWage" to hourlyWage,
            "dailyWage" to dailyWage,
            "totalOvertimePayAB" to totalOvertimePayAB,
            "replaceThreeShiftOvertimePay" to replaceThreeShiftOvertimePay, // 顯示替三班一天加班費
            "holidayOvertimePay" to abClassHolidayOvertimePay, // 顯示AB班例假日/國定假日加班費
            "totalOvertimePayThreeShift" to totalOvertimePayThreeShift,
            "calculatedThreeShiftOvertimePay" to calculatedThreeShiftOvertimePay, // 三班制排班加班費
            "restDayOvertimePay" to restDayOvertimePay, // 休息日加班費
            "nationalHolidayOvertimePay" to nationalHolidayOvertimePay, // 國定假日出勤加班費
            "nationalHolidayEveNightShiftPay" to nationalHolidayEveNightShiftPay, // 國定假日休班前一天接夜班加班費
            "totalNightShiftAllowance" to totalNightShiftAllowance,
            "monthlyBaseSalary" to monthlyBaseSalary, // 新增：月基本薪資
            "totalMonthlyOvertimePay" to totalMonthlyOvertimePay, // 新增：這個月總加班費
            "totalMonthlySalary" to totalMonthlySalary // 新增：這個月總薪資
        )
    }
}