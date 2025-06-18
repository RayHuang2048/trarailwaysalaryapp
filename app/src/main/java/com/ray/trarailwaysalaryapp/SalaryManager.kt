package com.ray.trarailwaysalaryapp

import android.content.Context
import java.math.BigDecimal
import java.math.RoundingMode
import android.util.Log

// 人員類型列舉
enum class PersonnelType(val displayName: String) {
    OFFICER("職員"),
    OPERATOR("營運人員"),
    EMPLOYEE("從業人員")
}

// 職員的細分職位列舉
enum class OfficerPosition(val displayName: String, val additionalProfessionalAllowance: BigDecimal, val managerialAllowance: BigDecimal) {
    STATION_MASTER("站長", BigDecimal("5820.0"), BigDecimal("5960.0")),
    DIRECTOR("主任", BigDecimal("5250.0"), BigDecimal("3970.0")),
    VICE_STATION_MASTER("副站長", BigDecimal("0.0"), BigDecimal("3970.0")),
    DEPUTY_TRAIN_CONDUCTOR("副座列車長", BigDecimal("5020.0"), BigDecimal("0.0")),
    TRAFFIC_STAFF_GRADE("行車人員員級", BigDecimal("5020.0"), BigDecimal("0.0")),
    TRAFFIC_STAFF_JUNIOR_GRADE("行車人員佐級", BigDecimal("4780.0"), BigDecimal("0.0")),
    STATION_ATTENDANT("站務員", BigDecimal("4440.0"), BigDecimal("0.0")),
    ASSISTANT_STATION_ATTENDANT("助理站務員", BigDecimal("4220.0"), BigDecimal("0.0"))
}

// 營運人員職位列舉 (只保留營運員和服務員)
enum class OperatingPosition(val displayName: String, val dutyAllowance: BigDecimal) {
    OPERATOR("營運員", BigDecimal("4220.0")),
    SERVICE_ATTENDAANT("服務員", BigDecimal("3640.0"))
}

// 從業人員職位列舉 (只保留助理站務員和服務員)
enum class EmployeePosition(val displayName: String, val dutyAllowance: BigDecimal) {
    ASSISTANT_STATION_ATTENDANT("助理站務員", BigDecimal("4090.0")),
    SERVICE_ATTENDANT("服務員", BigDecimal("3530.0"))
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
    private val officerSalaryData = mutableMapOf<Int, Map<String, BigDecimal>>() // 薪點 -> (薪額, 專業加給, 職務薪津貼)
    private val operatorSalaryData = mutableMapOf<Int, Map<String, BigDecimal>>() // 薪點 -> (月支數額)
    private val employeeSalaryData = mutableMapOf<Int, Map<String, BigDecimal>>() // 薪點 -> (基本薪)

    // 夜點費常數的預設值
    private val DEFAULT_NIGHT_SHIFT_ALLOWANCE_PER_DAY = BigDecimal("120.0")

    // 引入 ExcelReader 實例
    private val excelReader = ExcelReader(context)

    init {
        // 在 SalaryManager 實例化時載入薪資數據
        loadSalaryData()
    }

    /**
     * 從 Assets 目錄載入 Excel 薪資數據到內存。
     * 使用 ExcelReader 進行檔案讀取和數據轉換。
     */
    private fun loadSalaryData() {
        try {
            // --- 讀取職員薪資表 (職員薪額及專業加給表.xlsx) ---
            val officers = excelReader.readOfficerSalaries(
                fileName = EXCEL_FILE_OFFICER,
                sheetName = "Table1",
                startRowIndex = 4, // Excel 第5列是索引4
                endRowIndex = 49,  // Excel 第50列是索引49
                gradeColIndex = 3, // D 欄是索引 3
                amountColIndex = 4, // E 欄是索引 4
                professionalAllowanceColIndex = 5, // F 欄是索引 5
                dutySalaryColIndex = -1 // 職員 Excel 表中若無此列，保持 -1
            )
            officers.forEach { officer ->
                officerSalaryData[officer.grade] = mapOf(
                    "amount" to officer.amount,
                    "professionalAllowance" to officer.professionalAllowance,
                    "dutySalary" to (officer.dutySalary ?: BigDecimal.ZERO) // 如果 dutySalary 為 null，則儲存為 BigDecimal.ZERO
                )
            }
            Log.d("SalaryManager", "職員薪資數據載入完成，共 ${officerSalaryData.size} 筆。")

            // --- 讀取營運人員薪給表 (營運人員薪給表.xlsx) ---
            val operators = excelReader.readOperatorSalaries(
                fileName = EXCEL_FILE_OPERATOR,
                sheetName = "Table1",
                startRowIndex = 4, // <-- 已調整為 4 (對應 Excel 第5列)
                endRowIndex = 39,  // Excel 第40列是索引39
                gradeColIndex = 3, // D 欄是索引 3
                amountColIndex = 4 // E 欄是索引 4
            )
            operators.forEach { operator ->
                operatorSalaryData[operator.grade] = mapOf(
                    "amount" to operator.amount
                )
            }
            Log.d("SalaryManager", "營運人員薪資數據載入完成，共 ${operatorSalaryData.size} 筆。")

            // --- 讀取從業人員待遇表 (從業人員待遇表(備查本).xlsx) ---
            val employees = excelReader.readEmployeeSalaries(
                fileName = EXCEL_FILE_EMPLOYEE,
                sheetName = "Table1",
                startRowIndex = 3, // Excel 第4列是索引3
                endRowIndex = 48,  // Excel 第49列是索引48
                gradeColIndex = 1, // B 欄是索引 1
                amountColIndex = 2 // C 欄是索引 2
            )
            employees.forEach { employee ->
                employeeSalaryData[employee.grade] = mapOf(
                    "amount" to employee.amount
                )
            }
            Log.d("SalaryManager", "從業人員薪資數據載入完成，共 ${employeeSalaryData.size} 筆。")

        } catch (e: Exception) {
            // 這裡捕獲任何在載入 Excel 時的錯誤，並清楚地記錄下來
            Log.e("SalaryManager", "載入薪資數據時發生錯誤: ${(e as Throwable).localizedMessage}", e)
        }
    }

    /**
     * 根據輸入參數計算薪資。
     * 返回一個包含各種薪資組件的 Map。所有金額都以 BigDecimal 形式返回。
     */
    fun getSalary(
        personnelType: PersonnelType,
        grade: Int,
        officerPosition: OfficerPosition?, // 職員職位
        operatingPosition: OperatingPosition?, // 營運人員職位
        employeePosition: EmployeePosition?, // 從業人員職位
        shiftType: ShiftType,
        replaceThreeShiftDays: BigDecimal, // 替三班一天天數 (使用 BigDecimal)
        holidayOvertimeDays: BigDecimal, // AB班例假日/國定假日加班天數 (AB班用, 使用 BigDecimal)
        dayShiftDays: Int, // 日班天數
        nightShiftDays: Int, // 夜班天數
        restDayOvertimeDays: BigDecimal, // 休息日出勤天數 (使用 BigDecimal)
        nationalHolidayAttendanceDays: BigDecimal, // 國定假日出勤天數 (三班制用, 使用 BigDecimal)
        nationalHolidayEveNightShiftDays: BigDecimal, // 國定假日休班前一天接夜班天數 (使用 BigDecimal)
        nightShiftAllowancePerDayInput: BigDecimal // 從外部傳入的每日夜點費
    ): Map<String, BigDecimal>? {

        var amount = BigDecimal.ZERO // 月支數額 / 基本薪
        var professionalAllowance = BigDecimal.ZERO // 專業加給 (僅職員使用，從 Excel 讀取)
        var additionalProfessionalAllowance = BigDecimal.ZERO // 職員專業加給增支數額 (來自 OfficerPosition enum)
        var managerialAllowance = BigDecimal.ZERO // 主管加給
        var dutySalaryAllowance = BigDecimal.ZERO // 職務薪津貼
        var talentRetentionAllowance = BigDecimal.ZERO // 留才職務津貼

        // 確保 nightShiftAllowancePerDayInput 不為零，否則使用預設值
        val actualNightShiftAllowancePerDay = if (nightShiftAllowancePerDayInput.compareTo(BigDecimal.ZERO) == 0) {
            DEFAULT_NIGHT_SHIFT_ALLOWANCE_PER_DAY
        } else {
            nightShiftAllowancePerDayInput
        }
        // 使用 BigDecimal 進行乘法
        val totalNightShiftAllowance = actualNightShiftAllowancePerDay.multiply(nightShiftDays.toBigDecimal())
        Log.d("SalaryManager", "${personnelType.displayName} 夜點費計算: 天數=$nightShiftDays, 每日津貼=$actualNightShiftAllowancePerDay, 總額=$totalNightShiftAllowance}")

        when (personnelType) {
            PersonnelType.OFFICER -> {
                val officerData = officerSalaryData[grade]
                if (officerData != null) {
                    amount = officerData["amount"] ?: BigDecimal.ZERO
                    professionalAllowance = officerData["professionalAllowance"] ?: BigDecimal.ZERO
                    dutySalaryAllowance = officerData["dutySalary"] ?: BigDecimal.ZERO
                } else {
                    // 如果沒有從 Excel 載入數據，警告並使用零，而不是硬編碼的假資料
                    Log.w("SalaryManager", "職員薪資: 未在 Excel 中找到薪點 $grade 的數據，將使用零進行計算。請檢查 Excel 檔案和薪點是否正確。")
                    amount = BigDecimal.ZERO
                    professionalAllowance = BigDecimal.ZERO
                    dutySalaryAllowance = BigDecimal.ZERO
                }

                additionalProfessionalAllowance = officerPosition?.additionalProfessionalAllowance ?: BigDecimal.ZERO
                managerialAllowance = officerPosition?.managerialAllowance ?: BigDecimal.ZERO
                talentRetentionAllowance = BigDecimal("2000.0") // 職員有留才津貼，假設為2000

            }
            PersonnelType.OPERATOR -> {
                val operatorData = operatorSalaryData[grade]
                if (operatorData != null) {
                    amount = operatorData["amount"] ?: BigDecimal.ZERO
                } else {
                    // 如果沒有從 Excel 載入數據，警告並使用零
                    Log.w("SalaryManager", "營運人員薪資: 未在 Excel 中找到薪點 $grade 的數據，將使用零進行計算。請檢查 Excel 檔案和薪點是否正確。")
                    amount = BigDecimal.ZERO
                }

                professionalAllowance = BigDecimal.ZERO // 營運人員無專業加給
                dutySalaryAllowance = operatingPosition?.dutyAllowance ?: BigDecimal.ZERO
                talentRetentionAllowance = BigDecimal("2000.0") // 營運人員有留才津貼，假設為2000

            }
            PersonnelType.EMPLOYEE -> {
                val employeeData = employeeSalaryData[grade]
                if (employeeData != null) {
                    amount = employeeData["amount"] ?: BigDecimal.ZERO
                } else {
                    // 如果沒有從 Excel 載入數據，警告並使用零
                    Log.w("SalaryManager", "從業人員薪資: 未在 Excel 中找到薪點 $grade 的數據，將使用零進行計算。請檢查 Excel 檔案和薪點是否正確。")
                    amount = BigDecimal.ZERO
                }

                professionalAllowance = BigDecimal.ZERO // 從業人員無專業加給
                talentRetentionAllowance = BigDecimal.ZERO // 從業人員通常無留才津貼
                dutySalaryAllowance = employeePosition?.dutyAllowance ?: BigDecimal.ZERO
            }
        }

        // --- 計算基礎月薪 (不含加班費和夜點費，但包含所有固定薪資組件) ---
        val monthlyBaseSalaryComponent = amount.add(professionalAllowance)
            .add(additionalProfessionalAllowance)
            .add(managerialAllowance)
            .add(dutySalaryAllowance)
            .add(talentRetentionAllowance)

        // 計算日薪和時薪 (基於 monthlyBaseSalaryComponent，不包含加班費和夜點費)
        // 假設每月工作30天，每天8小時
        val daysInMonth = BigDecimal("30")
        val hoursInDay = BigDecimal("8")

        // 使用較高的精度進行除法，並四捨五入到小數點後兩位
        // 注意：這裡的 dailyWage 和 hourlyWage 如果 monthlyBaseSalaryComponent 是零，結果也會是零
        val dailyWage = monthlyBaseSalaryComponent.divide(daysInMonth, 2, RoundingMode.HALF_UP)
        val hourlyWage = dailyWage.divide(hoursInDay, 2, RoundingMode.HALF_UP)

        Log.d("SalaryManager", "${personnelType.displayName} 基礎月薪組件=$monthlyBaseSalaryComponent, 日薪=$dailyWage, 時薪=$hourlyWage")


        // 計算加班費的相關變數初始化 (均使用 BigDecimal)
        var totalOvertimePayAB = BigDecimal.ZERO
        var replaceThreeShiftOvertimePay = BigDecimal.ZERO // 替三班一天加班費
        var abClassHolidayOvertimePay = BigDecimal.ZERO // AB班例假日/國定假日加班費

        var totalOvertimePayThreeShift = BigDecimal.ZERO
        var calculatedThreeShiftOvertimePay = BigDecimal.ZERO // 三班制基於排班天數計算的加班費
        var restDayOvertimePay = BigDecimal.ZERO // 休息日加班費
        var nationalHolidayOvertimePay = BigDecimal.ZERO // 國定假日出勤加班費 (三班制用)
        var nationalHolidayEveNightShiftPay = BigDecimal.ZERO // 國定假日休班前一天接夜班加班費

        // 根據班別類型計算加班費
        when (shiftType) {
            ShiftType.AB_SHIFT -> {
                // 替三班一天加班費計算：每替一天，自動算兩小時1.33倍 + 一小時1.66倍
                val payPerReplaceDay = (hourlyWage.multiply(BigDecimal("2")).multiply(BigDecimal("1.33")))
                    .add(hourlyWage.multiply(BigDecimal("1")).multiply(BigDecimal("1.66")))
                    .setScale(2, RoundingMode.HALF_UP)

                replaceThreeShiftOvertimePay = payPerReplaceDay.multiply(replaceThreeShiftDays)

                // 例假日/國定假日加班費 (AB班)：多一天薪水 (日薪 * 天數 * 1.0)
                abClassHolidayOvertimePay = dailyWage.multiply(holidayOvertimeDays)

                totalOvertimePayAB = replaceThreeShiftOvertimePay.add(abClassHolidayOvertimePay)
            }
            ShiftType.THREE_SHIFT -> {
                // 三班制排班加班費: 日班天數和夜班天數，一天算加班1.2小時，費率1.33倍
                val totalShiftDays = dayShiftDays.toBigDecimal().add(nightShiftDays.toBigDecimal())
                calculatedThreeShiftOvertimePay = hourlyWage.multiply(totalShiftDays).multiply(BigDecimal("1.2")).multiply(BigDecimal("1.33"))
                    .setScale(2, RoundingMode.HALF_UP)

                // 休息日出勤加班費計算：一天算加班11小時，前2小時1.33倍，中6小時1.66倍，後3小時2.66倍
                val payPerRestDayOvertimeHour = (hourlyWage.multiply(BigDecimal("2")).multiply(BigDecimal("1.33")))
                    .add(hourlyWage.multiply(BigDecimal("6")).multiply(BigDecimal("1.66")))
                    .add(hourlyWage.multiply(BigDecimal("3")).multiply(BigDecimal("2.66")))
                    .setScale(2, RoundingMode.HALF_UP)

                restDayOvertimePay = payPerRestDayOvertimeHour.multiply(restDayOvertimeDays)

                // 國定假日出勤加班費計算：一天算加班9小時，前8小時1倍，中2小時1.33倍，後1小時1.66倍
                val payPerNationalHolidayAttendanceHour = (hourlyWage.multiply(BigDecimal("8")).multiply(BigDecimal("1.0")))
                    .add(hourlyWage.multiply(BigDecimal("2")).multiply(BigDecimal("1.33")))
                    .add(hourlyWage.multiply(BigDecimal("1")).multiply(BigDecimal("1.66")))
                    .setScale(2, RoundingMode.HALF_UP)

                nationalHolidayOvertimePay = payPerNationalHolidayAttendanceHour.multiply(nationalHolidayAttendanceDays)

                // 國定假日休班前一天接夜班加班費：一天算加班8小時，1倍時薪
                nationalHolidayEveNightShiftPay = hourlyWage.multiply(BigDecimal("8")).multiply(nationalHolidayEveNightShiftDays).multiply(BigDecimal("1.0"))
                    .setScale(2, RoundingMode.HALF_UP)

                totalOvertimePayThreeShift = calculatedThreeShiftOvertimePay
                    .add(restDayOvertimePay)
                    .add(nationalHolidayOvertimePay)
                    .add(nationalHolidayEveNightShiftPay)
            }
        }

        // 計算本月總加班費
        val totalMonthlyOvertimePay = totalOvertimePayAB.add(totalOvertimePayThreeShift)

        // 計算最終總月薪
        val finalMonthlySalary = monthlyBaseSalaryComponent.add(totalMonthlyOvertimePay).add(totalNightShiftAllowance)


        return mapOf(
            "amount" to amount.setScale(2, RoundingMode.HALF_UP),
            "professionalAllowance" to professionalAllowance.setScale(2, RoundingMode.HALF_UP),
            "additionalProfessionalAllowance" to additionalProfessionalAllowance.setScale(2, RoundingMode.HALF_UP),
            "managerialAllowance" to managerialAllowance.setScale(2, RoundingMode.HALF_UP),
            "dutySalaryAllowance" to dutySalaryAllowance.setScale(2, RoundingMode.HALF_UP),
            "talentRetentionAllowance" to talentRetentionAllowance.setScale(2, RoundingMode.HALF_UP),
            "hourlyWage" to hourlyWage.setScale(2, RoundingMode.HALF_UP),
            "dailyWage" to dailyWage.setScale(2, RoundingMode.HALF_UP),
            "totalOvertimePayAB" to totalOvertimePayAB.setScale(2, RoundingMode.HALF_UP),
            "replaceThreeShiftOvertimePay" to replaceThreeShiftOvertimePay.setScale(2, RoundingMode.HALF_UP),
            "abClassHolidayOvertimePay" to abClassHolidayOvertimePay.setScale(2, RoundingMode.HALF_UP),
            "totalOvertimePayThreeShift" to totalOvertimePayThreeShift.setScale(2, RoundingMode.HALF_UP),
            "calculatedThreeShiftOvertimePay" to calculatedThreeShiftOvertimePay.setScale(2, RoundingMode.HALF_UP),
            "restDayOvertimePay" to restDayOvertimePay.setScale(2, RoundingMode.HALF_UP),
            "nationalHolidayOvertimePay" to nationalHolidayOvertimePay.setScale(2, RoundingMode.HALF_UP),
            "nationalHolidayEveNightShiftPay" to nationalHolidayEveNightShiftPay.setScale(2, RoundingMode.HALF_UP),
            "totalNightShiftAllowance" to totalNightShiftAllowance.setScale(2, RoundingMode.HALF_UP),
            "monthlyBaseSalaryComponent" to monthlyBaseSalaryComponent.setScale(2, RoundingMode.HALF_UP),
            "totalMonthlyOvertimePay" to totalMonthlyOvertimePay.setScale(2, RoundingMode.HALF_UP),
            "totalMonthlySalary" to finalMonthlySalary.setScale(2, RoundingMode.HALF_UP)
        )
    }
}