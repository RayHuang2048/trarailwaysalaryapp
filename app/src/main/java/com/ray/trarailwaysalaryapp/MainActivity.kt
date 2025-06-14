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

class MainActivity : AppCompatActivity() {

    private lateinit var salaryManager: SalaryManager

    // UI elements declaration
    private lateinit var personnelTypeSpinner: Spinner
    private lateinit var officerPositionSpinner: Spinner
    private lateinit var operatingPositionSpinner: Spinner
    private lateinit var employeePositionSpinner: Spinner
    private lateinit var gradeEditText: EditText
    private lateinit var shiftTypeSpinner: Spinner
    private lateinit var overtimeHoursAB133EditText: EditText
    private lateinit var overtimeHoursAB166EditText: EditText
    private lateinit var holidayHoursABEditText: EditText
    private lateinit var overtimeHoursThreeShift134EditText: EditText
    private lateinit var overtimeHoursThreeShift167EditText: EditText
    private lateinit var dayShiftDaysEditText: EditText
    private lateinit var nightShiftDaysEditText: EditText
    private lateinit var nightShiftAllowancePerDayEditText: EditText
    private lateinit var calculateButton: Button
    private lateinit var resultTextView: TextView

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
        overtimeHoursAB133EditText = findViewById(R.id.overtimeHoursAB133EditText)
        overtimeHoursAB166EditText = findViewById(R.id.overtimeHoursAB166EditText)
        holidayHoursABEditText = findViewById(R.id.holidayHoursABEditText)
        overtimeHoursThreeShift134EditText = findViewById(R.id.overtimeHoursThreeShift134EditText)
        overtimeHoursThreeShift167EditText = findViewById(R.id.overtimeHoursThreeShift167EditText)
        dayShiftDaysEditText = findViewById(R.id.dayShiftDaysEditText)
        nightShiftDaysEditText = findViewById(R.id.nightShiftDaysEditText)
        nightShiftAllowancePerDayEditText = findViewById(R.id.nightShiftAllowancePerDayEditText)
        calculateButton = findViewById(R.id.calculateButton)
        resultTextView = findViewById(R.id.resultTextView)

        // Setup Spinners
        setupPersonnelTypeSpinner()
        setupOfficerPositionSpinner() // 需要更新以包含副站長
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
        // 更新此處以包含新增的副站長職位
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

        val overtimeHoursAB133 = overtimeHoursAB133EditText.text.toString().toDoubleOrNull() ?: 0.0
        val overtimeHoursAB166 = overtimeHoursAB166EditText.text.toString().toDoubleOrNull() ?: 0.0
        val holidayHoursAB = holidayHoursABEditText.text.toString().toDoubleOrNull() ?: 0.0
        val overtimeHoursThreeShift134 = overtimeHoursThreeShift134EditText.text.toString().toDoubleOrNull() ?: 0.0
        val overtimeHoursThreeShift167 = overtimeHoursThreeShift167EditText.text.toString().toDoubleOrNull() ?: 0.0
        val dayShiftDays = dayShiftDaysEditText.text.toString().toIntOrNull() ?: 0
        val nightShiftDays = nightShiftDaysEditText.text.toString().toIntOrNull() ?: 0
        val nightShiftAllowancePerDay = nightShiftAllowancePerDayEditText.text.toString().toBigDecimalOrNull() ?: BigDecimal.ZERO

        val salaryDetails = salaryManager.getSalary(
            personnelType,
            grade,
            officerPosition,
            operatingPosition,
            employeePosition,
            shiftType,
            overtimeHoursAB133,
            overtimeHoursAB166,
            holidayHoursAB,
            overtimeHoursThreeShift134,
            overtimeHoursThreeShift167,
            dayShiftDays,
            nightShiftDays,
            nightShiftAllowancePerDay
        )

        displayResults(salaryDetails)
    }

    private fun displayResults(salaryDetails: Map<String, Any>?) {
        if (salaryDetails != null) {
            val amount = salaryDetails["amount"] as Double
            val professionalAllowance = salaryDetails["professionalAllowance"] as Double
            val additionalProfessionalAllowance = salaryDetails["additionalProfessionalAllowance"] as Double
            val managerialAllowance = salaryDetails["managerialAllowance"] as Double // 顯示主管加給
            val dutySalaryAllowance = salaryDetails["dutySalaryAllowance"] as Double
            val talentRetentionAllowance = salaryDetails["talentRetentionAllowance"] as Double
            val hourlyWage = salaryDetails["hourlyWage"] as Double
            val dailyWage = salaryDetails["dailyWage"] as Double
            val totalOvertimePayAB = salaryDetails["totalOvertimePayAB"] as Double
            val totalOvertimePayThreeShift = salaryDetails["totalOvertimePayThreeShift"] as Double
            val totalNightShiftAllowance = salaryDetails["totalNightShiftAllowance"] as Double

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
                三班制總加班費: ${"%.2f".format(totalOvertimePayThreeShift)}
                夜班津貼總額: ${"%.2f".format(totalNightShiftAllowance)}
            """.trimIndent()
            resultTextView.text = resultText
        } else {
            resultTextView.text = "薪資計算失敗，請檢查輸入。"
        }
    }
}