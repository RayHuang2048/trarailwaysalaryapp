package com.ray.trarailwaysalaryapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import java.math.BigDecimal
import android.util.Log // 引入 Log 類別

class SalaryCalculatorFragment : Fragment() {

    // 在 Fragment 中，通常透過 activity 來取得 SalaryManager
    private lateinit var salaryManager: SalaryManager

    // UI elements declaration
    private lateinit var personnelTypeSpinner: Spinner
    private lateinit var officerPositionSpinner: Spinner
    private lateinit var operatingPositionSpinner: Spinner
    private lateinit var employeePositionSpinner: Spinner
    private lateinit var gradeEditText: EditText
    private lateinit var shiftTypeSpinner: Spinner
    private lateinit var replaceThreeShiftDaysEditText: EditText
    private lateinit var holidayOvertimeDaysEditText: EditText
    private lateinit var dayShiftDaysEditText: EditText
    private lateinit var nightShiftDaysEditText: EditText
    private lateinit var restDayOvertimeDaysEditText: EditText
    private lateinit var nationalHolidayAttendanceDaysEditText: EditText
    private lateinit var nationalHolidayEveNightShiftDaysEditText: EditText
    private lateinit var nightShiftAllowancePerDayEditText: EditText
    private lateinit var calculateButton: Button
    private lateinit var resultTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 填充 Fragment 的佈局
        val view = inflater.inflate(R.layout.fragment_salary_calculator, container, false)

        // 初始化 SalaryManager (透過 activity 獲取 context)
        salaryManager = SalaryManager(requireContext())

        // 初始化 UI elements by finding their IDs from the layout file
        // 注意：這裡使用 view.findViewById
        personnelTypeSpinner = view.findViewById(R.id.personnelTypeSpinner)
        officerPositionSpinner = view.findViewById(R.id.officerPositionSpinner)
        operatingPositionSpinner = view.findViewById(R.id.operatingPositionSpinner)
        employeePositionSpinner = view.findViewById(R.id.employeePositionSpinner)
        gradeEditText = view.findViewById(R.id.gradeEditText)
        shiftTypeSpinner = view.findViewById(R.id.shiftTypeSpinner)
        replaceThreeShiftDaysEditText = view.findViewById(R.id.replaceThreeShiftDaysEditText)
        holidayOvertimeDaysEditText = view.findViewById(R.id.holidayOvertimeDaysEditText)
        dayShiftDaysEditText = view.findViewById(R.id.dayShiftDaysEditText)
        nightShiftDaysEditText = view.findViewById(R.id.nightShiftDaysEditText)
        restDayOvertimeDaysEditText = view.findViewById(R.id.restDayOvertimeDaysEditText)
        nationalHolidayAttendanceDaysEditText = view.findViewById(R.id.nationalHolidayAttendanceDaysEditText)
        nationalHolidayEveNightShiftDaysEditText = view.findViewById(R.id.nationalHolidayEveNightShiftDaysEditText)
        nightShiftAllowancePerDayEditText = view.findViewById(R.id.nightShiftAllowancePerDayEditText)
        calculateButton = view.findViewById(R.id.calculateButton)
        resultTextView = view.findViewById(R.id.resultTextView)

        // Setup Spinners
        setupPersonnelTypeSpinner(view) // 傳遞 view 參數
        setupOfficerPositionSpinner(view)
        setupOperatingPositionSpinner(view)
        setupEmployeePositionSpinner(view)
        setupShiftTypeSpinner(view)

        calculateButton.setOnClickListener {
            calculateSalary()
        }

        return view
    }

    // 修改 setupSpinner 函數，接收一個 View 參數
    private fun setupPersonnelTypeSpinner(view: View) {
        val personnelTypes = PersonnelType.values().map { it.displayName }
        val adapter = ArrayAdapter(view.context, android.R.layout.simple_spinner_item, personnelTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        personnelTypeSpinner.adapter = adapter
    }

    private fun setupOfficerPositionSpinner(view: View) {
        val officerPositions = OfficerPosition.values().map { it.displayName }
        val adapter = ArrayAdapter(view.context, android.R.layout.simple_spinner_item, officerPositions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        officerPositionSpinner.adapter = adapter
    }

    private fun setupOperatingPositionSpinner(view: View) {
        val operatingPositions = OperatingPosition.values().map { it.displayName }
        val adapter = ArrayAdapter(view.context, android.R.layout.simple_spinner_item, operatingPositions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        operatingPositionSpinner.adapter = adapter
    }

    private fun setupEmployeePositionSpinner(view: View) {
        val employeePositions = EmployeePosition.values().map { it.displayName }
        val adapter = ArrayAdapter(view.context, android.R.layout.simple_spinner_item, employeePositions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        employeePositionSpinner.adapter = adapter
    }

    private fun setupShiftTypeSpinner(view: View) {
        val shiftTypes = ShiftType.values().map { it.displayName }
        val adapter = ArrayAdapter(view.context, android.R.layout.simple_spinner_item, shiftTypes)
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

        // Log the input parameters before calling getSalary
        Log.d("SalaryCalcInputs", "PersonnelType: $personnelType, Grade: $grade, OfficerPosition: $officerPosition, OperatingPosition: $operatingPosition, EmployeePosition: $employeePosition, ShiftType: $shiftType")
        Log.d("SalaryCalcInputs", "ReplaceThreeShiftDays: $replaceThreeShiftDays, HolidayOvertimeDays: $holidayOvertimeDays, DayShiftDays: $dayShiftDays, NightShiftDays: $nightShiftDays")
        Log.d("SalaryCalcInputs", "RestDayOvertimeDays: $restDayOvertimeDays, NationalHolidayAttendanceDays: $nationalHolidayAttendanceDays, NationalHolidayEveNightShiftDays: $nationalHolidayEveNightShiftDays, NightShiftAllowancePerDay: $nightShiftAllowancePerDay")


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
            // Log the entire salaryDetails map to see all values received
            Log.d("SalaryCalc", "Salary Details Map: $salaryDetails")

            // Using safe casts (as?) and providing default values (?: 0.0 or BigDecimal.ZERO)
            // This prevents crashes if a key is missing or the type is incorrect,
            // and defaults to 0.0 if the value is null or missing.
            val amount = salaryDetails["amount"] as? Double ?: 0.0
            val professionalAllowance = salaryDetails["professionalAllowance"] as? Double ?: 0.0
            val additionalProfessionalAllowance = salaryDetails["additionalProfessionalAllowance"] as? Double ?: 0.0
            val managerialAllowance = salaryDetails["managerialAllowance"] as? Double ?: 0.0
            val dutySalaryAllowance = salaryDetails["dutySalaryAllowance"] as? Double ?: 0.0
            val talentRetentionAllowance = salaryDetails["talentRetentionAllowance"] as? Double ?: 0.0
            val hourlyWage = salaryDetails["hourlyWage"] as? Double ?: 0.0
            val dailyWage = salaryDetails["dailyWage"] as? Double ?: 0.0

            val totalOvertimePayAB = salaryDetails["totalOvertimePayAB"] as? Double ?: 0.0
            val replaceThreeShiftOvertimePay = salaryDetails["replaceThreeShiftOvertimePay"] as? Double ?: 0.0
            val abClassHolidayOvertimePay = salaryDetails["holidayOvertimePay"] as? Double ?: 0.0

            val totalOvertimePayThreeShift = salaryDetails["totalOvertimePayThreeShift"] as? Double ?: 0.0
            val calculatedThreeShiftOvertimePay = salaryDetails["calculatedThreeShiftOvertimePay"] as? Double ?: 0.0
            val restDayOvertimePay = salaryDetails["restDayOvertimePay"] as? Double ?: 0.0
            val nationalHolidayOvertimePay = salaryDetails["nationalHolidayOvertimePay"] as? Double ?: 0.0
            val nationalHolidayEveNightShiftPay = salaryDetails["nationalHolidayEveNightShiftPay"] as? Double ?: 0.0
            val totalNightShiftAllowance = salaryDetails["totalNightShiftAllowance"] as? Double ?: 0.0

            val monthlyBaseSalary = salaryDetails["monthlyBaseSalary"] as? Double ?: 0.0
            val totalMonthlyOvertimePay = salaryDetails["totalMonthlyOvertimePay"] as? Double ?: 0.0
            val totalMonthlySalary = salaryDetails["totalMonthlySalary"] as? Double ?: 0.0

            // Log the specific value of totalMonthlySalary and other key totals
            Log.d("SalaryCalc", "Retrieved monthlyBaseSalary: ${"%.2f".format(monthlyBaseSalary)}")
            Log.d("SalaryCalc", "Retrieved totalMonthlyOvertimePay: ${"%.2f".format(totalMonthlyOvertimePay)}")
            Log.d("SalaryCalc", "Retrieved totalNightShiftAllowance: ${"%.2f".format(totalNightShiftAllowance)}")
            Log.d("SalaryCalc", "Retrieved totalMonthlySalary: ${"%.2f".format(totalMonthlySalary)}")


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
                 本月基本薪資 (含津貼): ${"%.2f".format(monthlyBaseSalary)}
                **本月總加班費:** ${"%.2f".format(totalMonthlyOvertimePay)}
                **本月總薪資 (基本薪+加班費+夜班津貼):** ${"%.2f".format(totalMonthlySalary)}
            """.trimIndent()
            resultTextView.text = resultText
        } else {
            // This message indicates that salaryDetails was null.
            Log.e("SalaryCalc", "薪資計算失敗：Salary details map is null!")
            resultTextView.text = "薪資計算失敗，請檢查輸入。"
        }
    }
}