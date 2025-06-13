package com.ray.trarailwaysalaryapp

import android.content.Context
import android.util.Log
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

class ExcelReader(private val context: Context) {

    private val TAG = "ExcelReader"

    fun readOfficerSalaries(
        fileName: String,
        sheetName: String,
        startRowIndex: Int,
        endRowIndex: Int,
        gradeColIndex: Int,
        amountColIndex: Int,
        professionalAllowanceColIndex: Int,
        dutySalaryColIndex: Int = -1
        // talentRetentionAllowanceColIndex: Int? = null // 移除此行，因為留才津貼是固定值
    ): MutableList<OfficerSalary> {
        val officerSalaries = mutableListOf<OfficerSalary>()
        var inputStream: InputStream? = null

        try {
            Log.d(TAG, "嘗試打開職員薪資檔案: $fileName 從 assets 中讀取。")
            inputStream = context.assets.open(fileName)
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheet(sheetName)
            if (sheet == null) {
                Log.e(TAG, "錯誤：在職員薪資檔案 '$fileName' 中找不到工作表 '$sheetName'。")
                workbook.close()
                return officerSalaries
            }

            for (rowIndex in startRowIndex..endRowIndex) {
                val row: Row? = sheet.getRow(rowIndex)
                if (row == null) {
                    Log.w(TAG, "職員薪資檔案 '$fileName' 在索引 $rowIndex 處跳過空行。")
                    continue
                }

                val gradeCell = row.getCell(gradeColIndex)
                val grade = getCellValueAsInt(gradeCell)

                val amountCell = row.getCell(amountColIndex)
                val amount = getCellValueAsDouble(amountCell)

                val professionalAllowanceCell = row.getCell(professionalAllowanceColIndex)
                val professionalAllowance = getCellValueAsDouble(professionalAllowanceCell) ?: 0.0

                val dutySalary = if (dutySalaryColIndex != -1) {
                    val dutySalaryCell = row.getCell(dutySalaryColIndex)
                    getCellValueAsDouble(dutySalaryCell)
                } else {
                    null
                }

                // 不再從 Excel 讀取 talentRetentionAllowance

                if (grade == null || amount == null) {
                    Log.w(TAG, "職員薪資檔案 '$fileName' 在行 $rowIndex 跳過：薪點或薪額為空或無效。")
                    continue
                }

                val officerSalary = OfficerSalary(
                    grade = grade,
                    amount = amount,
                    professionalAllowance = professionalAllowance,
                    dutySalary = dutySalary
                    // 不再傳遞 talentRetentionAllowance
                )
                officerSalaries.add(officerSalary)
            }
            workbook.close()
        } catch (e: Exception) {
            Log.e(TAG, "讀取職員薪資 Excel 檔案時發生錯誤: $fileName", e)
        } finally {
            inputStream?.close()
        }
        return officerSalaries
    }

    fun readOperatorSalaries(
        fileName: String,
        sheetName: String,
        startRowIndex: Int,
        endRowIndex: Int,
        gradeColIndex: Int,
        amountColIndex: Int
        // talentRetentionAllowanceColIndex: Int? = null // 移除此行，因為留才津貼是固定值
    ): MutableList<OperatorSalary> {
        val operatorSalaries = mutableListOf<OperatorSalary>()
        var inputStream: InputStream? = null

        try {
            Log.d(TAG, "嘗試打開營運人員薪資檔案: $fileName 從 assets 中讀取。")
            inputStream = context.assets.open(fileName)
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheet(sheetName) ?: run {
                Log.e(TAG, "錯誤：在營運人員薪資檔案 '$fileName' 中找不到工作表 '$sheetName'。")
                workbook.close()
                return operatorSalaries
            }

            for (rowIndex in startRowIndex..endRowIndex) {
                val row: Row? = sheet.getRow(rowIndex)
                if (row == null) {
                    Log.w(TAG, "營運人員薪資檔案 '$fileName' 在索引 $rowIndex 處跳過空行。")
                    continue
                }

                val gradeCell = row.getCell(gradeColIndex)
                val grade = getCellValueAsInt(gradeCell)

                val amountCell = row.getCell(amountColIndex)
                val amount = getCellValueAsDouble(amountCell)

                // 不再從 Excel 讀取 talentRetentionAllowance

                if (grade == null || amount == null) {
                    Log.w(TAG, "營運人員薪資檔案 '$fileName' 在行 $rowIndex 跳過：薪點或月支數額為空或無效。")
                    continue
                }

                val operatorSalary = OperatorSalary(
                    grade = grade,
                    amount = amount
                    // 不再傳遞 talentRetentionAllowance
                )
                operatorSalaries.add(operatorSalary)
            }
            workbook.close()
        } catch (e: Exception) {
            Log.e(TAG, "讀取營運人員 Excel 檔案時發生錯誤: $fileName", e)
        } finally {
            inputStream?.close()
        }
        return operatorSalaries
    }


    fun readEmployeeSalaries(
        fileName: String,
        sheetName: String,
        startRowIndex: Int,
        endRowIndex: Int,
        gradeColIndex: Int,
        amountColIndex: Int
    ): MutableList<EmployeeSalary> {
        val employeeSalaries = mutableListOf<EmployeeSalary>()
        var inputStream: InputStream? = null

        try {
            Log.d(TAG, "嘗試打開從業人員薪資檔案: $fileName 從 assets 中讀取。")
            inputStream = context.assets.open(fileName)
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheet(sheetName) ?: run {
                Log.e(TAG, "錯誤：在從業人員薪資檔案 '$fileName' 中找不到工作表 '$sheetName'。")
                workbook.close()
                return employeeSalaries
            }

            for (rowIndex in startRowIndex..endRowIndex) {
                val row: Row? = sheet.getRow(rowIndex)
                if (row == null) {
                    Log.w(TAG, "從業人員薪資檔案 '$fileName' 在索引 $rowIndex 處跳過空行。")
                    continue
                }

                val gradeCell = row.getCell(gradeColIndex)
                val grade = getCellValueAsInt(gradeCell)

                val amountCell = row.getCell(amountColIndex)
                val amount = getCellValueAsDouble(amountCell)

                if (grade == null || amount == null) {
                    Log.w(TAG, "從業人員薪資檔案 '$fileName' 在行 $rowIndex 跳過：薪點或薪額為空或無效。")
                    continue
                }

                val employeeSalary = EmployeeSalary(
                    grade = grade,
                    amount = amount
                )
                employeeSalaries.add(employeeSalary)
            }
            workbook.close()
        } catch (e: Exception) {
            Log.e(TAG, "讀取從業人員 Excel 檔案時發生錯誤: $fileName", e)
        } finally {
            inputStream?.close()
        }
        return employeeSalaries
    }


    private fun getCellValueAsInt(cell: Cell?): Int? {
        return when (cell?.cellType) {
            CellType.NUMERIC -> cell.numericCellValue.toInt()
            CellType.STRING -> cell.stringCellValue.trim().toIntOrNull()
            else -> null
        }
    }

    private fun getCellValueAsDouble(cell: Cell?): Double? {
        return when (cell?.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> {
                cell.stringCellValue.trim().replace(",", "").toDoubleOrNull()
            }
            else -> null
        }
    }
}
