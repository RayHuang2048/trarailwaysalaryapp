package com.ray.trarailwaysalaryapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.math.BigDecimal
import android.view.View
import android.webkit.WebView // 新增：引入 WebView 類別
import android.webkit.WebViewClient // 新增：引入 WebViewClient 類別

class MainActivity : AppCompatActivity() {

    private lateinit var salaryManager: SalaryManager

    // UI elements declaration
    private lateinit var personnelTypeSpinner: Spinner
    private lateinit var officerPositionSpinner: Spinner
    private lateinit var operatingPositionSpinner: Spinner
    private lateinit var employeePositionSpinner: Spinner
    private lateinit var gradeEditText: EditText
    private lateinit var shiftTypeSpinner: Spinner
    // AB班輸入欄位
    private lateinit var replaceThreeShiftDaysEditText: EditText // 替三班一天天數
    private lateinit var holidayOvertimeDaysEditText: EditText // AB班 例假日國定假日加班天數
    // 三班制輸入欄位
    private lateinit var dayShiftDaysEditText: EditText
    private lateinit var nightShiftDaysEditText: EditText
    private lateinit var restDayOvertimeDaysEditText: EditText // 休息日出勤天數
    private lateinit var nationalHolidayAttendanceDaysEditText: EditText // 國定假日出勤天數
    private lateinit var nationalHolidayEveNightShiftDaysEditText: EditText // 國定假日休班前一天接夜班天數
    private lateinit var nightShiftAllowancePerDayEditText: EditText
    private lateinit var calculateButton: Button
    private lateinit var resultTextView: TextView

    // 新增：WebView 宣告
    private lateinit var webViewStatus: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        salaryManager = SalaryManager(this)

        // Initialize UI elements by finding their IDs from the layout file
        personnelTypeSpinner = findViewById(R.id.personnelTypeSpinner)
        officerPositionSpinner = findViewById(R.id.officerPositionSpinner)
        operatingPositionSpinner = findViewById(R.id.operatingPositionSpinner)
        employeePositionSpinner = findViewById(R.id.employeePositionSpinner)
        gradeEditText = findViewById(R.id.gradeEditText)
        shiftTypeSpinner = findViewById(R.id.shiftTypeSpinner)
        // AB班輸入欄位初始化
        replaceThreeShiftDaysEditText = findViewById(R.id.replaceThreeShiftDaysEditText)
        holidayOvertimeDaysEditText = findViewById(R.id.holidayOvertimeDaysEditText)

        dayShiftDaysEditText = findViewById(R.id.dayShiftDaysEditText)
        nightShiftDaysEditText = findViewById(R.id.nightShiftDaysEditText)
        restDayOvertimeDaysEditText = findViewById(R.id.restDayOvertimeDaysEditText)
        nationalHolidayAttendanceDaysEditText = findViewById(R.id.nationalHolidayAttendanceDaysEditText)
        nationalHolidayEveNightShiftDaysEditText = findViewById(R.id.nationalHolidayEveNightShiftDaysEditText)
        nightShiftAllowancePerDayEditText = findViewById(R.id.nightShiftAllowancePerDayEditText)
        calculateButton = findViewById(R.id.calculateButton)
        resultTextView = findViewById(R.id.resultTextView)

        // 新增：WebView 初始化和設定
        webViewStatus = findViewById(R.id.webViewStatus)
        webViewStatus.webViewClient = WebViewClient() // 確保在應用程式內打開連結，而不是跳轉到外部瀏覽器
        webViewStatus.settings.javaScriptEnabled = true // 啟用 JavaScript，因為許多網頁內容是透過 JS 載入的
        // 載入台鐵營運狀態網頁
        webViewStatus.loadUrl("https://www.railway.gov.tw/tra-tip-web/tip/tip007/tip711/blockList")


        // Setup Spinners
        setupPersonnelTypeSpinner()
        setupOfficerPositionSpinner()
        setupOperatingPositionSpinner()
        setupEmployeePositionSpinner()
        setupShiftTypeSpinner()

        calculateButton.setOnClickListener {
            calculateSalary()
        }
    }

    private fun setupPersonnelTypeSpinner() {
        val personnelTypes = PersonnelType.values().map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, personnelTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        personnelTypeSpinner.adapter = adapter
    }

    private fun setupOfficerPositionSpinner() {
        val officerPositions = OfficerPosition.values().map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, officerPositions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        officerPositionSpinner.adapter = adapter
    }

    private fun setupOperatingPositionSpinner() {
        val operatingPositions = OperatingPosition.values().map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, operatingPositions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        operatingPositionSpinner.adapter = adapter
    }

    private fun setupEmployeePositionSpinner() {
        val employeePositions = EmployeePosition.values().map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, employeePositions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        employeePositionSpinner.adapter = adapter
    }

    private fun setupShiftTypeSpinner() {
        val shiftTypes = ShiftType.values().map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, shiftTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        shiftTypeSpinner.adapter = adapter
    }

    private fun calculateSalary() {
        val personnelType = PersonnelType.values()[personnelTypeSpinner.selectedItemPosition]
        val grade = gradeEditText.text.toString().toIntOrNull() ?: 0

        val officerPosition = if (personnelType == PersonnelType.OFFICER) {
            OfficerPosition.values()[officerPositionSpinner.selectedItemPosition]
        } else {
            null
        }

        val operatingPosition = if (personnelType == PersonnelType.OPERATOR) {
            OperatingPosition.values()[operatingPositionSpinner.selectedItemPosition]
        } else {
            null
        }

        val employeePosition = if (personnelType == PersonnelType.EMPLOYEE) {
            EmployeePosition.values()[employeePositionSpinner.selectedItemPosition]
        } else {
            null
        }

        val shiftType = ShiftType.values()[shiftTypeSpinner.selectedItemPosition]

        // 從輸入欄位獲取值
        val replaceThreeShiftDays = replaceThreeShiftDaysEditText.text.toString().toDoubleOrNull() ?: 0.0
        val holidayOvertimeDays = holidayOvertimeDaysEditText.text.toString().toDoubleOrNull() ?: 0.0

        val dayShiftDays = dayShiftDaysEditText.text.toString().toIntOrNull() ?: 0
        val nightShiftDays = nightShiftDaysEditText.text.toString().toIntOrNull() ?: 0
        val restDayOvertimeDays = restDayOvertimeDaysEditText.text.toString().toDoubleOrNull() ?: 0.0
        val nationalHolidayAttendanceDays = nationalHolidayAttendanceDaysEditText.text.toString().toDoubleOrNull() ?: 0.0
        val nationalHolidayEveNightShiftDays = nationalHolidayEveNightShiftDaysEditText.text.toString().toDoubleOrNull() ?: 0.0
        val nightShiftAllowancePerDay = nightShiftAllowancePerDayEditText.text.toString().toBigDecimalOrNull() ?: BigDecimal.ZERO

        val salaryDetails = salaryManager.getSalary(
            personnelType,
            grade,
            officerPosition,
            operatingPosition,
            employeePosition,
            shiftType,
            replaceThreeShiftDays,
            holidayOvertimeDays,
            dayShiftDays,
            nightShiftDays,
            restDayOvertimeDays,
            nationalHolidayAttendanceDays,
            nationalHolidayEveNightShiftDays,
            nightShiftAllowancePerDay
        )

        displayResults(salaryDetails)
    }

    private fun displayResults(salaryDetails: Map<String, Any>?) {
        if (salaryDetails != null) {
            val amount = salaryDetails["amount"] as Double
            val professionalAllowance = salaryDetails["professionalAllowance"] as Double
            val additionalProfessionalAllowance = salaryDetails["additionalProfessionalAllowance"] as Double
            val managerialAllowance = salaryDetails["managerialAllowance"] as Double
            val dutySalaryAllowance = salaryDetails["dutySalaryAllowance"] as Double
            val talentRetentionAllowance = salaryDetails["talentRetentionAllowance"] as Double
            val hourlyWage = salaryDetails["hourlyWage"] as Double
            val dailyWage = salaryDetails["dailyWage"] as Double

            val totalOvertimePayAB = salaryDetails["totalOvertimePayAB"] as Double
            val replaceThreeShiftOvertimePay = salaryDetails["replaceThreeShiftOvertimePay"] as Double
            val abClassHolidayOvertimePay = salaryDetails["holidayOvertimePay"] as Double

            val totalOvertimePayThreeShift = salaryDetails["totalOvertimePayThreeShift"] as Double
            val calculatedThreeShiftOvertimePay = salaryDetails["calculatedThreeShiftOvertimePay"] as Double
            val restDayOvertimePay = salaryDetails["restDayOvertimePay"] as Double
            val nationalHolidayOvertimePay = salaryDetails["nationalHolidayOvertimePay"] as Double
            val nationalHolidayEveNightShiftPay = salaryDetails["nationalHolidayEveNightShiftPay"] as Double
            val totalNightShiftAllowance = salaryDetails["totalNightShiftAllowance"] as Double

            val monthlyBaseSalary = salaryDetails["monthlyBaseSalary"] as Double
            val totalMonthlyOvertimePay = salaryDetails["totalMonthlyOvertimePay"] as Double
            val totalMonthlySalary = salaryDetails["totalMonthlySalary"] as Double


            val resultText = """
                基本薪/月支數額: ${"%.2f".format(amount)}
                專業加給: ${"%.2f".format(professionalAllowance)}
                專業加給增支數額: ${"%.2f".format(additionalProfessionalAllowance)}
                主管加給: ${"%.2f".format(managerialAllowance)}
                職務薪津貼: ${"%.2f".format(dutySalaryAllowance)}
                留才職務津貼: ${"%.2f".format(talentRetentionAllowance)}
                時薪: ${"%.2f".format(hourlyWage)}
                日薪: ${"%.2f".format(dailyWage)}
                
                AB班總加班費: ${"%.2f".format(totalOvertimePayAB)}
                替三班一天加班費: ${"%.2f".format(replaceThreeShiftOvertimePay)}
                AB班 例假日/國定假日加班費: ${"%.2f".format(abClassHolidayOvertimePay)}
                
                三班制總加班費: ${"%.2f".format(totalOvertimePayThreeShift)}
                三班制排班加班費(1.2hr*1.33x): ${"%.2f".format(calculatedThreeShiftOvertimePay)}
                休息日加班費: ${"%.2f".format(restDayOvertimePay)}
                國定假日出勤加班費: ${"%.2f".format(nationalHolidayOvertimePay)}
                國定假日休班前一天接夜班加班費: ${"%.2f".format(nationalHolidayEveNightShiftPay)}
                夜班津貼總額: ${"%.2f".format(totalNightShiftAllowance)}

                ---
                本月基本薪資 (含津貼): ${"%.2f".format(monthlyBaseSalary)}
                **本月總加班費:** ${"%.2f".format(totalMonthlyOvertimePay)}
                **本月總薪資 (基本薪+加班費+夜班津貼):** ${"%.2f".format(totalMonthlySalary)}
            """.trimIndent()
            resultTextView.text = resultText
        } else {
            resultTextView.text = "薪資計算失敗，請檢查輸入。"
        }
    }
}