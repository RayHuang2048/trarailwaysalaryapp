package com.ray.trarailwaysalaryapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.math.BigDecimal
import org.apache.poi.xssf.usermodel.XSSFWorkbook // For .xlsx files
import java.io.IOException
import com.ray.trarailwaysalaryapp.SalaryManager

class MainActivity : AppCompatActivity() {

    // 定義 Excel 檔案名
    private val EXCEL_FILE_OFFICER = "職員薪額及專業加給表.xlsx"
    private val EXCEL_FILE_OPERATOR = "營運人員薪給表.xlsx"
    private val EXCEL_FILE_EMPLOYEE = "從業人員待遇表(備查本).xlsx"

    // 薪資計算相關的 UI 元素
    private lateinit var etGrade: EditText
    private lateinit var spinnerPersonnelType: Spinner

    // 職員新增的職位選擇 UI 元素
    private lateinit var tvSelectOfficerPosition: TextView
    private lateinit var spinnerOfficerPosition: Spinner

    private lateinit var tvSelectOperatingPosition: TextView
    private lateinit var spinnerOperatingPosition: Spinner
    private lateinit var tvSelectEmployeePosition: TextView
    private lateinit var spinnerEmployeePosition: Spinner

    private lateinit var spinnerShiftType: Spinner
    private lateinit var tvDayShiftDaysLabel: TextView
    private lateinit var etDayShiftDaysThreeShift: EditText
    private lateinit var tvNightShiftDaysLabel: TextView
    private lateinit var etNightShiftDaysThreeShift: EditText
    private lateinit var tvNightShiftAllowanceLabel: TextView
    private lateinit var etNightShiftAllowancePerDay: EditText
    private lateinit var etOvertimeHoursAB133: EditText
    private lateinit var etOvertimeHoursAB166: EditText
    private lateinit var etHolidayHoursAB: EditText
    private lateinit var etOvertimeHoursThreeShift134: EditText
    private lateinit var etOvertimeHoursThreeShift167: EditText
    private lateinit var tvOvertimeThreeShift134Label: TextView
    private lateinit var tvOvertimeThreeShift167Label: TextView
    private lateinit var tvOvertimeAb133Label: TextView
    private lateinit var tvOvertimeAb166Label: TextView
    private lateinit var tvHolidayAbLabel: TextView


    private lateinit var btnCalculate: Button
    private lateinit var tvResult: TextView

    // 新增用於顯示 Excel 數據的 UI 元素 (三個按鈕，三個TextView)
    private lateinit var btnLoadOfficerExcel: Button
    private lateinit var tvOfficerExcelData: TextView
    private lateinit var btnLoadOperatorExcel: Button
    private lateinit var tvOperatorExcelData: TextView
    private lateinit var btnLoadEmployeeExcel: Button
    private lateinit var tvEmployeeExcelData: TextView

    // 薪資管理器實例
    private lateinit var salaryManager: SalaryManager

    // 台鐵事故通報平台按鈕
    private lateinit var btnOpenAccidentReport: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化薪資計算相關的 UI 元素
        etGrade = findViewById(R.id.et_grade)
        spinnerPersonnelType = findViewById(R.id.spinner_personnel_type)

        // 職員職位初始化
        tvSelectOfficerPosition = findViewById(R.id.tv_select_officer_position)
        spinnerOfficerPosition = findViewById(R.id.spinner_officer_position)


        tvSelectOperatingPosition = findViewById(R.id.tv_select_operating_position)
        spinnerOperatingPosition = findViewById(R.id.spinner_operating_position)
        tvSelectEmployeePosition = findViewById(R.id.tv_select_employee_position)
        spinnerEmployeePosition = findViewById(R.id.spinner_employee_position)

        spinnerShiftType = findViewById(R.id.spinner_shift_type)

        tvDayShiftDaysLabel = findViewById(R.id.tv_day_shift_days_label)
        etDayShiftDaysThreeShift = findViewById(R.id.et_day_shift_days_three_shift)
        tvNightShiftDaysLabel = findViewById(R.id.tv_night_shift_days_label)
        etNightShiftDaysThreeShift = findViewById(R.id.et_night_shift_days_three_shift)
        tvNightShiftAllowanceLabel = findViewById(R.id.tv_night_shift_allowance_label)
        etNightShiftAllowancePerDay = findViewById(R.id.et_night_shift_allowance_per_day)

        etOvertimeHoursAB133 = findViewById(R.id.et_overtime_hours_ab_133)
        etOvertimeHoursAB166 = findViewById(R.id.et_overtime_hours_ab_166)
        etHolidayHoursAB = findViewById(R.id.et_holiday_hours_ab)
        etOvertimeHoursThreeShift134 = findViewById(R.id.et_overtime_hours_three_shift_134)
        etOvertimeHoursThreeShift167 = findViewById(R.id.et_overtime_hours_three_shift_167)
        tvOvertimeThreeShift134Label = findViewById(R.id.tv_overtime_three_shift_134_label)
        tvOvertimeThreeShift167Label = findViewById(R.id.tv_overtime_three_shift_167_label)
        tvOvertimeAb133Label = findViewById(R.id.tv_overtime_ab_133_label)
        tvOvertimeAb166Label = findViewById(R.id.tv_overtime_ab_166_label)
        tvHolidayAbLabel = findViewById(R.id.tv_holiday_ab_label)

        btnCalculate = findViewById(R.id.btn_calculate)
        tvResult = findViewById(R.id.tv_result)

        // 初始化 Excel 讀取相關的 UI 元素
        btnLoadOfficerExcel = findViewById(R.id.btn_load_officer_excel)
        tvOfficerExcelData = findViewById(R.id.tv_officer_excel_data)
        btnLoadOperatorExcel = findViewById(R.id.btn_load_operator_excel)
        tvOperatorExcelData = findViewById(R.id.tv_operator_excel_data)
        btnLoadEmployeeExcel = findViewById(R.id.btn_load_employee_excel)
        tvEmployeeExcelData = findViewById(R.id.tv_employee_excel_data)


        // 初始化薪資管理器
        salaryManager = SalaryManager(this)

        // 設定下拉選單
        setupSpinners()

        // 設定計算按鈕的點擊事件
        btnCalculate.setOnClickListener {
            calculateAndDisplaySalary()
        }

        // 設定載入 Excel 資料按鈕的點擊事件
        btnLoadOfficerExcel.setOnClickListener {
            readExcelFileFromAssets(EXCEL_FILE_OFFICER, tvOfficerExcelData)
        }
        btnLoadOperatorExcel.setOnClickListener {
            readExcelFileFromAssets(EXCEL_FILE_OPERATOR, tvOperatorExcelData)
        }
        btnLoadEmployeeExcel.setOnClickListener {
            readExcelFileFromAssets(EXCEL_FILE_EMPLOYEE, tvEmployeeExcelData)
        }


        // 處理職員職位、營運人員職位和從業人員職位下拉選單的可見性（根據人員類型）
        spinnerPersonnelType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedPersonnelType = PersonnelType.entries[position]
                // 隱藏所有職位相關的 UI 元素
                tvSelectOfficerPosition.visibility = View.GONE
                spinnerOfficerPosition.visibility = View.GONE
                tvSelectOperatingPosition.visibility = View.GONE
                spinnerOperatingPosition.visibility = View.GONE
                tvSelectEmployeePosition.visibility = View.GONE
                spinnerEmployeePosition.visibility = View.GONE

                when (selectedPersonnelType) {
                    PersonnelType.OFFICER -> {
                        tvSelectOfficerPosition.visibility = View.VISIBLE
                        spinnerOfficerPosition.visibility = View.VISIBLE
                    }
                    PersonnelType.OPERATOR -> {
                        tvSelectOperatingPosition.visibility = View.VISIBLE
                        spinnerOperatingPosition.visibility = View.VISIBLE
                    }
                    PersonnelType.EMPLOYEE -> {
                        tvSelectEmployeePosition.visibility = View.VISIBLE
                        spinnerEmployeePosition.visibility = View.VISIBLE
                    }
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // 不執行任何操作
            }
        }

        // 根據班別類型調整加班時數輸入框的可見性
        spinnerShiftType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedShiftType = ShiftType.entries[position]
                val isThreeShift = (selectedShiftType == ShiftType.THREE_SHIFT)

                etOvertimeHoursAB133.visibility = if (!isThreeShift) View.VISIBLE else View.GONE
                tvOvertimeAb133Label.visibility = if (!isThreeShift) View.VISIBLE else View.GONE
                etOvertimeHoursAB166.visibility = if (!isThreeShift) View.VISIBLE else View.GONE
                tvOvertimeAb166Label.visibility = if (!isThreeShift) View.VISIBLE else View.GONE
                etHolidayHoursAB.visibility = if (!isThreeShift) View.VISIBLE else View.GONE
                tvHolidayAbLabel.visibility = if (!isThreeShift) View.VISIBLE else View.GONE

                etDayShiftDaysThreeShift.visibility = if (isThreeShift) View.VISIBLE else View.GONE
                tvDayShiftDaysLabel.visibility = if (isThreeShift) View.VISIBLE else View.GONE
                etNightShiftDaysThreeShift.visibility = if (isThreeShift) View.VISIBLE else View.GONE
                tvNightShiftDaysLabel.visibility = if (isThreeShift) View.VISIBLE else View.GONE
                etNightShiftAllowancePerDay.visibility = if (isThreeShift) View.VISIBLE else View.GONE
                tvNightShiftAllowanceLabel.visibility = if (isThreeShift) View.VISIBLE else View.GONE

                etOvertimeHoursThreeShift134.visibility = if (isThreeShift) View.VISIBLE else View.GONE
                tvOvertimeThreeShift134Label.visibility = if (isThreeShift) View.VISIBLE else View.GONE
                etOvertimeHoursThreeShift167.visibility = if (isThreeShift) View.VISIBLE else View.GONE
                tvOvertimeThreeShift167Label.visibility = if (isThreeShift) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // 不執行任何操作
            }
        }

        btnOpenAccidentReport = findViewById(R.id.btn_open_accident_report)
        btnOpenAccidentReport.setOnClickListener {
            val intent = Intent(this, AccidentReportingActivity::class.java)
            startActivity(intent)
        }
    }

    // 設定所有下拉選單的輔助函數
    private fun setupSpinners() {
        val personnelTypeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            PersonnelType.entries.map { it.displayName }
        )
        personnelTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPersonnelType.adapter = personnelTypeAdapter

        // 新增職員職位下拉選單
        val officerPositionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            OfficerPosition.entries.map { it.displayName }
        )
        officerPositionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOfficerPosition.adapter = officerPositionAdapter


        val operatingPositionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            OperatingPosition.entries.map { it.displayName }
        )
        operatingPositionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOperatingPosition.adapter = operatingPositionAdapter

        val employeePositionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            EmployeePosition.entries.map { it.displayName }
        )
        employeePositionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEmployeePosition.adapter = employeePositionAdapter

        val shiftTypeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            ShiftType.entries.map { it.displayName }
        )
        shiftTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerShiftType.adapter = shiftTypeAdapter
    }

    // 計算並顯示薪資的函數
    private fun calculateAndDisplaySalary() {
        val gradeString = etGrade.text.toString()
        if (gradeString.isBlank()) {
            Toast.makeText(this, "請輸入薪點/薪級", Toast.LENGTH_SHORT).show()
            etGrade.error = "薪點/薪級不可為空"
            return
        }
        val grade = gradeString.toIntOrNull()
        if (grade == null) {
            Toast.makeText(this, "薪點/薪級必須為數字", Toast.LENGTH_SHORT).show()
            etGrade.error = "請輸入有效數字"
            return
        }

        val selectedPersonnelType = PersonnelType.entries[spinnerPersonnelType.selectedItemPosition]
        // 根據選擇的人員類型獲取對應的職位
        val selectedOfficerPosition = if (selectedPersonnelType == PersonnelType.OFFICER) {
            OfficerPosition.entries[spinnerOfficerPosition.selectedItemPosition]
        } else {
            null
        }
        val selectedOperatingPosition = if (selectedPersonnelType == PersonnelType.OPERATOR) {
            OperatingPosition.entries[spinnerOperatingPosition.selectedItemPosition]
        } else {
            null
        }
        val selectedEmployeePosition = if (selectedPersonnelType == PersonnelType.EMPLOYEE) {
            EmployeePosition.entries[spinnerEmployeePosition.selectedItemPosition]
        } else {
            null
        }

        val selectedShiftType = ShiftType.entries[spinnerShiftType.selectedItemPosition]

        val dayShiftDays = etDayShiftDaysThreeShift.text.toString().toIntOrNull() ?: 0
        val nightShiftDays = etNightShiftDaysThreeShift.text.toString().toIntOrNull() ?: 0
        val nightShiftAllowancePerDay = etNightShiftAllowancePerDay.text.toString().toBigDecimalOrNull() ?: BigDecimal.ZERO

        val overtimeHoursAB133 = etOvertimeHoursAB133.text.toString().toDoubleOrNull() ?: 0.0
        val overtimeHoursAB166 = etOvertimeHoursAB166.text.toString().toDoubleOrNull() ?: 0.0
        val holidayHoursAB = etHolidayHoursAB.text.toString().toDoubleOrNull() ?: 0.0
        val overtimeHoursThreeShift134_input = etOvertimeHoursThreeShift134.text.toString().toDoubleOrNull() ?: 0.0
        val overtimeHoursThreeShift167_input = etOvertimeHoursThreeShift167.text.toString().toDoubleOrNull() ?: 0.0

        val salaryDetails = salaryManager.getSalary(
            selectedPersonnelType,
            grade,
            selectedOfficerPosition, // 傳入職員細分職位
            selectedOperatingPosition,
            selectedEmployeePosition,
            selectedShiftType,
            overtimeHoursAB133,
            overtimeHoursAB166,
            holidayHoursAB,
            overtimeHoursThreeShift134_input,
            overtimeHoursThreeShift167_input,
            dayShiftDays,
            nightShiftDays,
            nightShiftAllowancePerDay
        )

        if (salaryDetails != null) {
            val amount = salaryDetails["amount"] as? Double ?: 0.0
            val professionalAllowance = salaryDetails["professionalAllowance"] as? Double ?: 0.0
            val dutySalaryAllowance = salaryDetails["dutySalaryAllowance"] as? Double ?: 0.0
            val talentRetentionAllowance = salaryDetails["talentRetentionAllowance"] as? Double ?: 0.0
            val hourlyWage = salaryDetails["hourlyWage"] as? Double ?: 0.0
            val dailyWage = salaryDetails["dailyWage"] as? Double ?: 0.0

            val totalOvertimePayAB = salaryDetails["totalOvertimePayAB"] as? Double ?: 0.0
            val overtimePayAB133 = salaryDetails["overtimePayAB133"] as? Double ?: 0.0
            val overtimePayAB166 = salaryDetails["overtimePayAB166"] as? Double ?: 0.0
            val holidayOvertimePayAB = salaryDetails["holidayOvertimePayAB"] as? Double ?: 0.0

            val totalOvertimePayThreeShift = salaryDetails["totalOvertimePayThreeShift"] as? Double ?: 0.0
            val fixedThreeShiftOvertimePay = salaryDetails["fixedThreeShiftOvertimePay"] as? Double ?: 0.0
            val totalNightShiftAllowance = salaryDetails["totalNightShiftAllowance"] as? Double ?: 0.0

            val overtimePayThreeShift134_val = salaryDetails["overtimePayThreeShift134"] as? Double ?: 0.0
            val overtimePayThreeShift167_val = salaryDetails["overtimePayThreeShift167"] as? Double ?: 0.0


            var totalSalary: Double
            val baseSalary: Double = when (selectedPersonnelType) {
                PersonnelType.OFFICER -> amount + professionalAllowance + dutySalaryAllowance
                PersonnelType.OPERATOR -> amount + professionalAllowance + dutySalaryAllowance // 營運人員現在也包含職務加給
                PersonnelType.EMPLOYEE -> amount + professionalAllowance
            }

            totalSalary = baseSalary
            if (selectedPersonnelType == PersonnelType.OFFICER || selectedPersonnelType == PersonnelType.OPERATOR) {
                totalSalary += talentRetentionAllowance
            }

            when (selectedShiftType) {
                ShiftType.AB_SHIFT -> {
                    totalSalary += totalOvertimePayAB
                }
                ShiftType.THREE_SHIFT -> {
                    totalSalary += fixedThreeShiftOvertimePay
                    totalSalary += totalNightShiftAllowance
                    totalSalary += overtimePayThreeShift134_val + overtimePayThreeShift167_val
                }
            }


            val resultText = StringBuilder()
            resultText.append("--- 薪資計算結果 ---\n")
            resultText.append("人員類型: ${selectedPersonnelType.displayName}\n")
            resultText.append("薪點/薪級: $grade\n")
            resultText.append("月支數額/基本薪: ${String.format("%.2f", amount)}\n")

            if (selectedPersonnelType == PersonnelType.OFFICER) {
                resultText.append("職員職位: ${selectedOfficerPosition?.displayName ?: "未選擇"}\n") // 顯示職員細分職位
                resultText.append("職員專業加給: ${String.format("%.2f", professionalAllowance)}\n")
                resultText.append("職務薪津貼: ${String.format("%.2f", dutySalaryAllowance)}\n")
            } else if (selectedPersonnelType == PersonnelType.OPERATOR) {
                resultText.append("營運人員職位: ${selectedOperatingPosition?.displayName ?: "未選擇"}\n")
                resultText.append("營運人員專業加給: ${String.format("%.2f", professionalAllowance)}\n")
                if (dutySalaryAllowance > 0) { // 營運人員有職務加給才顯示
                    resultText.append("職務薪津貼: ${String.format("%.2f", dutySalaryAllowance)}\n")
                }
            } else if (selectedPersonnelType == PersonnelType.EMPLOYEE) {
                resultText.append("從業人員職位: ${selectedEmployeePosition?.displayName ?: "未選擇"}\n")
                resultText.append("從業人員專業加給: ${String.format("%.2f", professionalAllowance)}\n")
            }

            if (selectedPersonnelType == PersonnelType.OFFICER || selectedPersonnelType == PersonnelType.OPERATOR) {
                resultText.append("留才職務津貼: ${String.format("%.2f", talentRetentionAllowance)}\n")
            }

            resultText.append("--------------------\n")
            resultText.append("班別: ${selectedShiftType.displayName}\n")
            resultText.append("時薪: ${String.format("%.2f", hourlyWage)}\n")
            resultText.append("日薪: ${String.format("%.2f", dailyWage)}\n")

            when (selectedShiftType) {
                ShiftType.AB_SHIFT -> {
                    resultText.append("AB班 1.33倍加班費: ${String.format("%.2f", overtimePayAB133)}\n")
                    resultText.append("AB班 1.66倍加班費: ${String.format("%.2f", overtimePayAB166)}\n")
                    resultText.append("AB班 例假日/國定假日加班費: ${String.format("%.2f", holidayOvertimePayAB)}\n")
                    resultText.append("AB班總加班費: ${String.format("%.2f", totalOvertimePayAB)}\n")
                }
                ShiftType.THREE_SHIFT -> {
                    resultText.append("日班天數: $dayShiftDays 天\n")
                    resultText.append("夜班天數: $nightShiftDays 天\n")
                    resultText.append("每夜班津貼: ${String.format("%.2f", nightShiftAllowancePerDay)}\n")
                    resultText.append("固定三班制加班費 (每班1.2小時, 1.33倍): ${String.format("%.2f", fixedThreeShiftOvertimePay)}\n")
                    resultText.append("總夜班津貼: ${String.format("%.2f", totalNightShiftAllowance)}\n")
                    if (overtimeHoursThreeShift134_input > 0 || overtimeHoursThreeShift167_input > 0) {
                        resultText.append("三班 1.34倍額外加班費: ${String.format("%.2f", overtimePayThreeShift134_val)}\n")
                        resultText.append("三班 1.67倍額外加班費: ${String.format("%.2f", overtimePayThreeShift167_val)}\n")
                    }
                    resultText.append("三班總加班費 (含固定與額外): ${String.format("%.2f", totalOvertimePayThreeShift)}\n")
                }
            }
            resultText.append("--------------------\n")
            resultText.append("月總薪資估計: ${String.format("%.2f", totalSalary)}\n")

            tvResult.text = resultText.toString()

        } else {
            tvResult.text = "找不到符合條件的薪資數據，請確認輸入。"
        }
    }

    // 重構的函數：從 assets 目錄讀取指定 Excel 檔案並顯示到指定 TextView
    private fun readExcelFileFromAssets(fileName: String, targetTextView: TextView) {
        try {
            assets.open(fileName).use { inputStream ->
                val workbook = XSSFWorkbook(inputStream)
                val sheet = workbook.getSheetAt(0) // 讀取第一個工作表

                val stringBuilder = StringBuilder("--- Excel 資料 ($fileName) ---\n")
                // 迭代每一行
                for (row in sheet) {
                    val rowData = StringBuilder()
                    // 迭代每一單元格
                    for (cell in row) {
                        when (cell.cellType) {
                            org.apache.poi.ss.usermodel.CellType.STRING -> rowData.append(cell.stringCellValue).append("\t")
                            org.apache.poi.ss.usermodel.CellType.NUMERIC -> rowData.append(cell.numericCellValue).append("\t")
                            org.apache.poi.ss.usermodel.CellType.BOOLEAN -> rowData.append(cell.booleanCellValue).append("\t")
                            org.apache.poi.ss.usermodel.CellType.FORMULA -> rowData.append(cell.cachedFormulaResultType).append("\t")
                            org.apache.poi.ss.usermodel.CellType.BLANK -> rowData.append("").append("\t")
                            else -> rowData.append("").append("\t")
                        }
                    }
                    if (rowData.trim().isNotEmpty()) { // 避免顯示空白行
                        stringBuilder.append(rowData.toString().trimEnd()).append("\n")
                    }
                }
                targetTextView.text = stringBuilder.toString()
                workbook.close()
                Toast.makeText(this, "$fileName 載入成功！", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            targetTextView.text = "讀取 Excel 檔案失敗: ${e.message}"
            Toast.makeText(this, "無法讀取資產中的 Excel 檔案: $fileName，請確認檔案是否存在且檔名正確。\n錯誤: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            targetTextView.text = "解析 Excel 檔案失敗: ${e.message}"
            Toast.makeText(this, "解析 Excel 檔案時發生錯誤: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}