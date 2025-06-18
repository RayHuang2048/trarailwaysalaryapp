package com.ray.trarailwaysalaryapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.math.BigDecimal
import java.math.RoundingMode // 引入 RoundingMode 用於 BigDecimal 顯示格式化

class SalaryCalculatorFragment : Fragment() {

    // 聲明所有的 UI 元素
    private lateinit var personnelTypeSpinner: Spinner
    private lateinit var officerPositionSpinner: Spinner
    private lateinit var operatingPositionSpinner: Spinner
    private lateinit var employeePositionSpinner: Spinner
    private lateinit var gradeEditText: EditText
    private lateinit var shiftTypeSpinner: Spinner

    // AB班相關的 EditText 和它們的 TextView 標籤 (始終顯示)
    private lateinit var tvReplaceThreeShiftDaysLabel: TextView
    private lateinit var replaceThreeShiftDaysEditText: EditText
    private lateinit var tvHolidayOvertimeDaysLabel: TextView
    private lateinit var holidayOvertimeDaysEditText: EditText

    // 三班制相關的 EditText 和它們的 TextView 標籤 (根據班別類型動態顯示)
    private lateinit var tvDayShiftDaysLabel: TextView
    private lateinit var dayShiftDaysEditText: EditText
    private lateinit var tvNightShiftDaysLabel: TextView
    private lateinit var nightShiftDaysEditText: EditText
    private lateinit var tvRestDayOvertimeDaysLabel: TextView
    private lateinit var restDayOvertimeDaysEditText: EditText
    private lateinit var tvNationalHolidayAttendanceDaysLabel: TextView
    private lateinit var nationalHolidayAttendanceDaysEditText: EditText
    private lateinit var tvNationalHolidayEveNightShiftDaysLabel: TextView
    private lateinit var nationalHolidayEveNightShiftDaysEditText: EditText
    private lateinit var tvNightShiftAllowancePerDayLabel: TextView
    private lateinit var nightShiftAllowancePerDayEditText: EditText

    private lateinit var calculateButton: Button
    private lateinit var resultTextView: TextView

    private lateinit var salaryManager: SalaryManager // 聲明 SalaryManager 實例

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_salary_calculator, container, false)

        // 初始化所有 UI 元素
        personnelTypeSpinner = view.findViewById(R.id.personnelTypeSpinner)
        officerPositionSpinner = view.findViewById(R.id.officerPositionSpinner)
        operatingPositionSpinner = view.findViewById(R.id.operatingPositionSpinner)
        employeePositionSpinner = view.findViewById(R.id.employeePositionSpinner)
        gradeEditText = view.findViewById(R.id.gradeEditText)
        shiftTypeSpinner = view.findViewById(R.id.shiftTypeSpinner)

        // 初始化 AB班相關的 UI 元素 (這些在 XML 中設定為始終顯示)
        tvReplaceThreeShiftDaysLabel = view.findViewById(R.id.tv_replace_three_shift_days_label)
        replaceThreeShiftDaysEditText = view.findViewById(R.id.replaceThreeShiftDaysEditText)
        tvHolidayOvertimeDaysLabel = view.findViewById(R.id.tv_holiday_overtime_days_label)
        holidayOvertimeDaysEditText = view.findViewById(R.id.holidayOvertimeDaysEditText)

        // 初始化三班制相關的 UI 元素 (這些的可見性由 updateShiftInputVisibility 控制)
        tvDayShiftDaysLabel = view.findViewById(R.id.tv_day_shift_days_label)
        dayShiftDaysEditText = view.findViewById(R.id.dayShiftDaysEditText)
        tvNightShiftDaysLabel = view.findViewById(R.id.tv_night_shift_days_label)
        nightShiftDaysEditText = view.findViewById(R.id.nightShiftDaysEditText)
        tvRestDayOvertimeDaysLabel = view.findViewById(R.id.tv_rest_day_overtime_days_label)
        restDayOvertimeDaysEditText = view.findViewById(R.id.restDayOvertimeDaysEditText)
        tvNationalHolidayAttendanceDaysLabel = view.findViewById(R.id.tv_national_holiday_attendance_days_label)
        nationalHolidayAttendanceDaysEditText = view.findViewById(R.id.nationalHolidayAttendanceDaysEditText)
        tvNationalHolidayEveNightShiftDaysLabel = view.findViewById(R.id.tv_national_holiday_eve_night_shift_days_label)
        nationalHolidayEveNightShiftDaysEditText = view.findViewById(R.id.nationalHolidayEveNightShiftDaysEditText)
        tvNightShiftAllowancePerDayLabel = view.findViewById(R.id.tv_night_shift_allowance_per_day_label)
        nightShiftAllowancePerDayEditText = view.findViewById(R.id.nightShiftAllowancePerDayEditText)

        calculateButton = view.findViewById(R.id.calculateButton)
        resultTextView = view.findViewById(R.id.resultTextView)

        // --- 初始設置：所有職位 Spinner 及其標籤都隱藏 ---
        view.findViewById<TextView>(R.id.tv_officer_position_label).visibility = View.GONE
        officerPositionSpinner.visibility = View.GONE
        view.findViewById<TextView>(R.id.tv_operating_position_label).visibility = View.GONE
        operatingPositionSpinner.visibility = View.GONE
        view.findViewById<TextView>(R.id.tv_employee_position_label).visibility = View.GONE
        employeePositionSpinner.visibility = View.GONE

        // 初始化 SalaryManager
        salaryManager = SalaryManager(requireContext())

        // --- 設置人員類型 Spinner 的監聽器 ---
        personnelTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedType = parent?.getItemAtPosition(position).toString()
                updatePositionSpinnersVisibility(selectedType)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // --- 設置班別類型 Spinner 的監聽器 ---
        shiftTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedShiftType = parent?.getItemAtPosition(position).toString()
                updateShiftInputVisibility(selectedShiftType)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // --- 計算按鈕的點擊事件 ---
        calculateButton.setOnClickListener {
            calculateSalary()
        }

        // --- 初始載入時的處理 ---
        updatePositionSpinnersVisibility(personnelTypeSpinner.selectedItem.toString())
        updateShiftInputVisibility(shiftTypeSpinner.selectedItem.toString())

        return view
    }

    // --- 根據人員類型更新職位 Spinner 及對應標籤的可見性 ---
    private fun updatePositionSpinnersVisibility(personnelType: String) {
        view?.findViewById<TextView>(R.id.tv_officer_position_label)?.visibility = View.GONE
        officerPositionSpinner.visibility = View.GONE
        view?.findViewById<TextView>(R.id.tv_operating_position_label)?.visibility = View.GONE
        operatingPositionSpinner.visibility = View.GONE
        view?.findViewById<TextView>(R.id.tv_employee_position_label)?.visibility = View.GONE
        employeePositionSpinner.visibility = View.GONE

        when (personnelType) {
            "職員" -> {
                view?.findViewById<TextView>(R.id.tv_officer_position_label)?.visibility = View.VISIBLE
                officerPositionSpinner.visibility = View.VISIBLE
            }
            "營運人員" -> {
                view?.findViewById<TextView>(R.id.tv_operating_position_label)?.visibility = View.VISIBLE
                operatingPositionSpinner.visibility = View.VISIBLE
            }
            "從業人員" -> {
                view?.findViewById<TextView>(R.id.tv_employee_position_label)?.visibility = View.VISIBLE
                employeePositionSpinner.visibility = View.VISIBLE
            }
        }
    }

    // --- 根據班別類型更新相關輸入框 (及其標籤) 的可見性 ---
    private fun updateShiftInputVisibility(shiftType: String) {
        when (shiftType) {
            "AB班" -> {
                tvDayShiftDaysLabel.visibility = View.GONE
                dayShiftDaysEditText.visibility = View.GONE
                tvNightShiftDaysLabel.visibility = View.GONE
                nightShiftDaysEditText.visibility = View.GONE
                tvRestDayOvertimeDaysLabel.visibility = View.GONE
                restDayOvertimeDaysEditText.visibility = View.GONE
                tvNationalHolidayAttendanceDaysLabel.visibility = View.GONE
                nationalHolidayAttendanceDaysEditText.visibility = View.GONE
                tvNationalHolidayEveNightShiftDaysLabel.visibility = View.GONE
                nationalHolidayEveNightShiftDaysEditText.visibility = View.GONE
                tvNightShiftAllowancePerDayLabel.visibility = View.GONE
                nightShiftAllowancePerDayEditText.visibility = View.GONE
            }
            "三班制" -> {
                tvDayShiftDaysLabel.visibility = View.VISIBLE
                dayShiftDaysEditText.visibility = View.VISIBLE
                tvNightShiftDaysLabel.visibility = View.VISIBLE
                nightShiftDaysEditText.visibility = View.VISIBLE
                tvRestDayOvertimeDaysLabel.visibility = View.VISIBLE
                restDayOvertimeDaysEditText.visibility = View.VISIBLE
                tvNationalHolidayAttendanceDaysLabel.visibility = View.VISIBLE
                nationalHolidayAttendanceDaysEditText.visibility = View.VISIBLE
                tvNationalHolidayEveNightShiftDaysLabel.visibility = View.VISIBLE
                nationalHolidayEveNightShiftDaysEditText.visibility = View.VISIBLE
                tvNightShiftAllowancePerDayLabel.visibility = View.VISIBLE
                nightShiftAllowancePerDayEditText.visibility = View.VISIBLE
            }
            else -> {
                tvDayShiftDaysLabel.visibility = View.GONE
                dayShiftDaysEditText.visibility = View.GONE
                tvNightShiftDaysLabel.visibility = View.GONE
                nightShiftDaysEditText.visibility = View.GONE
                tvRestDayOvertimeDaysLabel.visibility = View.GONE
                restDayOvertimeDaysEditText.visibility = View.GONE
                tvNationalHolidayAttendanceDaysLabel.visibility = View.GONE
                nationalHolidayAttendanceDaysEditText.visibility = View.GONE
                tvNationalHolidayEveNightShiftDaysLabel.visibility = View.GONE
                nationalHolidayEveNightShiftDaysEditText.visibility = View.GONE
                tvNightShiftAllowancePerDayLabel.visibility = View.GONE
                nightShiftAllowancePerDayEditText.visibility = View.GONE
            }
        }
    }

    // --- 實際的薪資計算邏輯 ---
    private fun calculateSalary() {
        // 獲取並轉換人員類型 (為 SalaryManager 準備)
        val selectedPersonnelTypeString = personnelTypeSpinner.selectedItem?.toString() ?: ""
        val personnelType = PersonnelType.values().find { it.displayName == selectedPersonnelTypeString }

        if (personnelType == null) {
            resultTextView.text = "錯誤：請選擇有效的人員類型。"
            return
        }

        // 獲取並轉換職位類型 (為 SalaryManager 準備)
        var officerPosition: OfficerPosition? = null
        var operatingPosition: OperatingPosition? = null
        var employeePosition: EmployeePosition? = null

        when (personnelType) {
            PersonnelType.OFFICER -> {
                val selectedOfficerPosString = officerPositionSpinner.selectedItem?.toString() ?: ""
                officerPosition = OfficerPosition.values().find { it.displayName == selectedOfficerPosString }
                if (officerPosition == null) {
                    resultTextView.text = "錯誤：請選擇有效的職員職位。"
                    return
                }
            }
            PersonnelType.OPERATOR -> {
                val selectedOperatingPosString = operatingPositionSpinner.selectedItem?.toString() ?: ""
                operatingPosition = OperatingPosition.values().find { it.displayName == selectedOperatingPosString }
                if (operatingPosition == null) {
                    resultTextView.text = "錯誤：請選擇有效的營運人員職位。"
                    return
                }
            }
            PersonnelType.EMPLOYEE -> {
                val selectedEmployeePosString = employeePositionSpinner.selectedItem?.toString() ?: ""
                employeePosition = EmployeePosition.values().find { it.displayName == selectedEmployeePosString }
                if (employeePosition == null) {
                    resultTextView.text = "錯誤：請選擇有效的從業人員職位。"
                    return
                }
            }
        }

        // 獲取薪點 (使用 toIntOrNull()避免崩潰，若無法轉換則為0)
        val grade = gradeEditText.text.toString().toIntOrNull() ?: 0
        if (grade == 0) {
            resultTextView.text = "錯誤：請輸入有效的薪點。"
            return
        }

        // 獲取並轉換班別類型 (為 SalaryManager 準備)
        val selectedShiftTypeString = shiftTypeSpinner.selectedItem?.toString() ?: ""
        val shiftType = ShiftType.values().find { it.displayName == selectedShiftTypeString }
        if (shiftType == null) {
            resultTextView.text = "錯誤：請選擇有效的班別類型。"
            return
        }

        // 獲取 AB 班相關天數 (直接讀取為 BigDecimal)
        val replaceThreeShiftDays = replaceThreeShiftDaysEditText.text.toString().toBigDecimalOrNull() ?: BigDecimal.ZERO
        val holidayOvertimeDays = holidayOvertimeDaysEditText.text.toString().toBigDecimalOrNull() ?: BigDecimal.ZERO

        // 獲取三班制相關天數 (ToInt/ToBigDecimal，根據可見性判斷是否讀取，否則預設為0/BigDecimal.ZERO)
        val dayShiftDays = if (dayShiftDaysEditText.visibility == View.VISIBLE) dayShiftDaysEditText.text.toString().toIntOrNull() ?: 0 else 0
        val nightShiftDays = if (nightShiftDaysEditText.visibility == View.VISIBLE) nightShiftDaysEditText.text.toString().toIntOrNull() ?: 0 else 0
        val restDayOvertimeDays = if (restDayOvertimeDaysEditText.visibility == View.VISIBLE) restDayOvertimeDaysEditText.text.toString().toBigDecimalOrNull() ?: BigDecimal.ZERO else BigDecimal.ZERO
        val nationalHolidayAttendanceDays = if (nationalHolidayAttendanceDaysEditText.visibility == View.VISIBLE) nationalHolidayAttendanceDaysEditText.text.toString().toBigDecimalOrNull() ?: BigDecimal.ZERO else BigDecimal.ZERO
        val nationalHolidayEveNightShiftDays = if (nationalHolidayEveNightShiftDaysEditText.visibility == View.VISIBLE) nationalHolidayEveNightShiftDaysEditText.text.toString().toBigDecimalOrNull() ?: BigDecimal.ZERO else BigDecimal.ZERO
        val nightShiftAllowancePerDayInput = if (nightShiftAllowancePerDayEditText.visibility == View.VISIBLE) nightShiftAllowancePerDayEditText.text.toString().toBigDecimalOrNull() ?: BigDecimal.ZERO else BigDecimal.ZERO

        // 呼叫 SalaryManager 進行計算
        val salaryResults = salaryManager.getSalary(
            personnelType = personnelType,
            grade = grade,
            officerPosition = officerPosition,
            operatingPosition = operatingPosition,
            employeePosition = employeePosition,
            shiftType = shiftType,
            replaceThreeShiftDays = replaceThreeShiftDays,
            holidayOvertimeDays = holidayOvertimeDays,
            dayShiftDays = dayShiftDays,
            nightShiftDays = nightShiftDays,
            restDayOvertimeDays = restDayOvertimeDays,
            nationalHolidayAttendanceDays = nationalHolidayAttendanceDays,
            nationalHolidayEveNightShiftDays = nationalHolidayEveNightShiftDays,
            nightShiftAllowancePerDayInput = nightShiftAllowancePerDayInput
        )

        if (salaryResults == null) {
            resultTextView.text = "計算失敗，請檢查輸入。"
            return
        }

        // 從結果 Map 中提取並格式化顯示數據
        val totalMonthlySalary = salaryResults["totalMonthlySalary"] ?: BigDecimal.ZERO
        val monthlyBaseSalaryComponent = salaryResults["monthlyBaseSalaryComponent"] ?: BigDecimal.ZERO
        val totalMonthlyOvertimePay = salaryResults["totalMonthlyOvertimePay"] ?: BigDecimal.ZERO
        val totalNightShiftAllowance = salaryResults["totalNightShiftAllowance"] ?: BigDecimal.ZERO
        val hourlyWage = salaryResults["hourlyWage"] ?: BigDecimal.ZERO
        val dailyWage = salaryResults["dailyWage"] ?: BigDecimal.ZERO
        val amount = salaryResults["amount"] ?: BigDecimal.ZERO // 本薪/月支數額
        val professionalAllowance = salaryResults["professionalAllowance"] ?: BigDecimal.ZERO
        val additionalProfessionalAllowance = salaryResults["additionalProfessionalAllowance"] ?: BigDecimal.ZERO
        val managerialAllowance = salaryResults["managerialAllowance"] ?: BigDecimal.ZERO
        val dutySalaryAllowance = salaryResults["dutySalaryAllowance"] ?: BigDecimal.ZERO
        val talentRetentionAllowance = salaryResults["talentRetentionAllowance"] ?: BigDecimal.ZERO
        val replaceThreeShiftOvertimePay = salaryResults["replaceThreeShiftOvertimePay"] ?: BigDecimal.ZERO
        val abClassHolidayOvertimePay = salaryResults["abClassHolidayOvertimePay"] ?: BigDecimal.ZERO
        val calculatedThreeShiftOvertimePay = salaryResults["calculatedThreeShiftOvertimePay"] ?: BigDecimal.ZERO
        val restDayOvertimePay = salaryResults["restDayOvertimePay"] ?: BigDecimal.ZERO
        val nationalHolidayOvertimePay = salaryResults["nationalHolidayOvertimePay"] ?: BigDecimal.ZERO
        val nationalHolidayEveNightShiftPay = salaryResults["nationalHolidayEveNightShiftPay"] ?: BigDecimal.ZERO


        val resultText = StringBuilder()
        resultText.append("--- 本月薪資總計：${totalMonthlySalary.setScale(2, RoundingMode.HALF_UP)} 元 ---\n\n")
        resultText.append("詳細組成：\n")
        resultText.append("● 基本薪資組成 (本薪/薪額/月支數額 + 各項固定加給): ${monthlyBaseSalaryComponent.setScale(2, RoundingMode.HALF_UP)} 元\n")
        resultText.append("  ↳ 本薪/月支數額: ${amount.setScale(2, RoundingMode.HALF_UP)} 元\n")
        if (professionalAllowance > BigDecimal.ZERO) resultText.append("  ↳ 專業加給: ${professionalAllowance.setScale(2, RoundingMode.HALF_UP)} 元\n")
        if (additionalProfessionalAllowance > BigDecimal.ZERO) resultText.append("  ↳ 專業加給增支數額: ${additionalProfessionalAllowance.setScale(2, RoundingMode.HALF_UP)} 元\n")
        if (managerialAllowance > BigDecimal.ZERO) resultText.append("  ↳ 主管加給: ${managerialAllowance.setScale(2, RoundingMode.HALF_UP)} 元\n")
        if (dutySalaryAllowance > BigDecimal.ZERO) resultText.append("  ↳ 職務薪津貼: ${dutySalaryAllowance.setScale(2, RoundingMode.HALF_UP)} 元\n")
        if (talentRetentionAllowance > BigDecimal.ZERO) resultText.append("  ↳ 留才職務津貼: ${talentRetentionAllowance.setScale(2, RoundingMode.HALF_UP)} 元\n")
        resultText.append("● 總加班費: ${totalMonthlyOvertimePay.setScale(2, RoundingMode.HALF_UP)} 元\n")
        resultText.append("● 夜點費總額: ${totalNightShiftAllowance.setScale(2, RoundingMode.HALF_UP)} 元\n")
        resultText.append("● 計算時薪: ${hourlyWage.setScale(2, RoundingMode.HALF_UP)} 元/小時\n")
        resultText.append("● 計算日薪: ${dailyWage.setScale(2, RoundingMode.HALF_UP)} 元/天\n\n")


        // 顯示加班費細項 (根據班別類型和值)
        resultText.append("加班費細項:\n")
        if (shiftType == ShiftType.AB_SHIFT) {
            if (replaceThreeShiftDays > BigDecimal.ZERO) resultText.append("  ↳ 替三班一天加班費 (${replaceThreeShiftDays} 天): ${replaceThreeShiftOvertimePay.setScale(2, RoundingMode.HALF_UP)} 元\n")
            if (holidayOvertimeDays > BigDecimal.ZERO) resultText.append("  ↳ 例假日/國定假日加班費 (${holidayOvertimeDays} 天): ${abClassHolidayOvertimePay.setScale(2, RoundingMode.HALF_UP)} 元\n")
        } else if (shiftType == ShiftType.THREE_SHIFT) {
            if (dayShiftDays > 0 || nightShiftDays > 0) resultText.append("  ↳ 三班制排班加班費 (日班 ${dayShiftDays} 天, 夜班 ${nightShiftDays} 天): ${calculatedThreeShiftOvertimePay.setScale(2, RoundingMode.HALF_UP)} 元\n")
            if (restDayOvertimeDays > BigDecimal.ZERO) resultText.append("  ↳ 休息日出勤加班費 (${restDayOvertimeDays} 天): ${restDayOvertimePay.setScale(2, RoundingMode.HALF_UP)} 元\n")
            if (nationalHolidayAttendanceDays > BigDecimal.ZERO) resultText.append("  ↳ 國定假日出勤加班費 (${nationalHolidayAttendanceDays} 天): ${nationalHolidayOvertimePay.setScale(2, RoundingMode.HALF_UP)} 元\n")
            if (nationalHolidayEveNightShiftDays > BigDecimal.ZERO) resultText.append("  ↳ 國定假日休班前一天接夜班加班費 (${nationalHolidayEveNightShiftDays} 天): ${nationalHolidayEveNightShiftPay.setScale(2, RoundingMode.HALF_UP)} 元\n")
        }
        if (totalMonthlyOvertimePay == BigDecimal.ZERO) {
            resultText.append("  (無加班)\n")
        }
        resultText.append("\n")


        resultText.append("--- 您選擇的條件 ---\n")
        resultText.append("● 人員類型: ${personnelType.displayName}\n")
        when (personnelType) {
            PersonnelType.OFFICER -> resultText.append("  ↳ 職位: ${officerPosition?.displayName ?: "未選擇"}\n")
            PersonnelType.OPERATOR -> resultText.append("  ↳ 職位: ${operatingPosition?.displayName ?: "未選擇"}\n")
            PersonnelType.EMPLOYEE -> resultText.append("  ↳ 職位: ${employeePosition?.displayName ?: "未選擇"}\n")
        }
        resultText.append("● 薪點: $grade\n")
        resultText.append("● 班別類型: ${shiftType.displayName}\n")

        if (shiftType == ShiftType.AB_SHIFT) {
            resultText.append("  ↳ 替三班一天天數: ${replaceThreeShiftDays.setScale(1, RoundingMode.HALF_UP)}\n")
            resultText.append("  ↳ AB班 例假日/國定假日加班天數: ${holidayOvertimeDays.setScale(1, RoundingMode.HALF_UP)}\n")
        } else if (shiftType == ShiftType.THREE_SHIFT) {
            resultText.append("  ↳ 日班天數: $dayShiftDays\n")
            resultText.append("  ↳ 夜班天數: $nightShiftDays\n")
            resultText.append("  ↳ 休息日出勤天數: ${restDayOvertimeDays.setScale(1, RoundingMode.HALF_UP)}\n")
            resultText.append("  ↳ 國定假日出勤天數: ${nationalHolidayAttendanceDays.setScale(1, RoundingMode.HALF_UP)}\n")
            resultText.append("  ↳ 國定假日休班前一天接夜班天數: ${nationalHolidayEveNightShiftDays.setScale(1, RoundingMode.HALF_UP)}\n")
            resultText.append("  ↳ 夜班津貼每日金額: ${nightShiftAllowancePerDayInput.setScale(2, RoundingMode.HALF_UP)}\n")
        }

        resultTextView.text = resultText.toString()
    }
}