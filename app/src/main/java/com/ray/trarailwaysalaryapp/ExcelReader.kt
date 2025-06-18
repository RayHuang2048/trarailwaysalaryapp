package com.ray.trarailwaysalaryapp

import android.content.Context
import android.util.Log
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.math.BigDecimal // 引入 BigDecimal
import java.io.IOException


class ExcelReader(private val context: Context) {

    private val TAG = "ExcelReader"

    /**
     * 讀取職員薪資表。
     * 所有金額數據都轉換為 BigDecimal 以確保精度。
     */
    fun readOfficerSalaries(
        fileName: String,
        sheetName: String,
        startRowIndex: Int,
        endRowIndex: Int,
        gradeColIndex: Int,
        amountColIndex: Int,
        professionalAllowanceColIndex: Int,
        dutySalaryColIndex: Int = -1 // 預設為-1表示該列不存在或不讀取
    ): MutableList<OfficerSalary> {
        val officerSalaries = mutableListOf<OfficerSalary>()
        var inputStream: InputStream? = null

        try {
            Log.d(TAG, "嘗試打開職員薪資檔案: '$fileName'，讀取工作表: '$sheetName'。")
            inputStream = context.assets.open(fileName)
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheet(sheetName)
            if (sheet == null) {
                Log.e(TAG, "錯誤：在職員薪資檔案 '$fileName' 中找不到工作表 '$sheetName'。請檢查工作表名稱是否正確。")
                workbook.close()
                return officerSalaries
            }

            for (rowIndex in startRowIndex..endRowIndex) {
                val row: Row? = sheet.getRow(rowIndex)
                if (row == null) {
                    // Log.w(TAG, "職員薪資檔案 '$fileName' 在索引 $rowIndex 處跳過空行。")
                    continue // 跳過空行
                }

                val gradeCell = row.getCell(gradeColIndex)
                val grade = getCellValueAsInt(gradeCell)

                val amountCell = row.getCell(amountColIndex)
                val amount = getCellValueAsBigDecimal(amountCell)

                val professionalAllowanceCell = row.getCell(professionalAllowanceColIndex)
                val professionalAllowance = getCellValueAsBigDecimal(professionalAllowanceCell) ?: BigDecimal.ZERO

                val dutySalary = if (dutySalaryColIndex != -1) {
                    val dutySalaryCell = row.getCell(dutySalaryColIndex)
                    getCellValueAsBigDecimal(dutySalaryCell)
                } else {
                    null // 如果 dutySalaryColIndex 是 -1，則 dutySalary 為 null
                }

                if (grade == null || amount == null) {
                    Log.w(TAG, "職員薪資檔案 '$fileName' 在行 ${rowIndex + 1} 跳過：薪點($grade)或薪額($amount)為空或無效。")
                    continue
                }

                val officerSalary = OfficerSalary(
                    grade = grade,
                    amount = amount,
                    professionalAllowance = professionalAllowance,
                    dutySalary = dutySalary // 傳遞可能為 null 的 BigDecimal
                )
                officerSalaries.add(officerSalary)
            }
            workbook.close()
            Log.d(TAG, "成功讀取職員薪資檔案 '$fileName'，共 ${officerSalaries.size} 筆記錄。")
        } catch (e: IOException) {
            Log.e(TAG, "讀取職員薪資 Excel 檔案時發生 IO 錯誤: '$fileName': ${e.localizedMessage}", e)
        } catch (e: Exception) {
            Log.e(TAG, "解析職員薪資 Excel 檔案時發生錯誤: '$fileName': ${e.localizedMessage}", e)
        } finally {
            inputStream?.close()
        }
        return officerSalaries
    }

    /**
     * 讀取營運人員薪給表。
     * 所有金額數據都轉換為 BigDecimal 以確保精度。
     */
    fun readOperatorSalaries(
        fileName: String,
        sheetName: String,
        startRowIndex: Int,
        endRowIndex: Int,
        gradeColIndex: Int,
        amountColIndex: Int
    ): MutableList<OperatorSalary> {
        val operatorSalaries = mutableListOf<OperatorSalary>()
        var inputStream: InputStream? = null

        try {
            Log.d(TAG, "嘗試打開營運人員薪資檔案: '$fileName'，讀取工作表: '$sheetName'。")
            inputStream = context.assets.open(fileName)
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheet(sheetName) ?: run {
                Log.e(TAG, "錯誤：在營運人員薪資檔案 '$fileName' 中找不到工作表 '$sheetName'。請檢查工作表名稱是否正確。")
                workbook.close()
                return operatorSalaries
            }

            for (rowIndex in startRowIndex..endRowIndex) {
                val row: Row? = sheet.getRow(rowIndex)
                if (row == null) {
                    // Log.w(TAG, "營運人員薪資檔案 '$fileName' 在索引 $rowIndex 處跳過空行。")
                    continue
                }

                val gradeCell = row.getCell(gradeColIndex)
                val grade = getCellValueAsInt(gradeCell)

                val amountCell = row.getCell(amountColIndex)
                val amount = getCellValueAsBigDecimal(amountCell)

                if (grade == null || amount == null) {
                    Log.w(TAG, "營運人員薪資檔案 '$fileName' 在行 ${rowIndex + 1} 跳過：薪點($grade)或月支數額($amount)為空或無效。")
                    continue
                }

                val operatorSalary = OperatorSalary(
                    grade = grade,
                    amount = amount
                )
                operatorSalaries.add(operatorSalary)
            }
            workbook.close()
            Log.d(TAG, "成功讀取營運人員薪資檔案 '$fileName'，共 ${operatorSalaries.size} 筆記錄。")
        } catch (e: IOException) {
            Log.e(TAG, "讀取營運人員 Excel 檔案時發生 IO 錯誤: '$fileName': ${e.localizedMessage}", e)
        } catch (e: Exception) {
            Log.e(TAG, "解析營運人員 Excel 檔案時發生錯誤: '$fileName': ${e.localizedMessage}", e)
        } finally {
            inputStream?.close()
        }
        return operatorSalaries
    }

    /**
     * 讀取從業人員待遇表。
     * 所有金額數據都轉換為 BigDecimal 以確保精度。
     */
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
            Log.d(TAG, "嘗試打開從業人員薪資檔案: '$fileName'，讀取工作表: '$sheetName'。")
            inputStream = context.assets.open(fileName)
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheet(sheetName) ?: run {
                Log.e(TAG, "錯誤：在從業人員薪資檔案 '$fileName' 中找不到工作表 '$sheetName'。請檢查工作表名稱是否正確。")
                workbook.close()
                return employeeSalaries
            }

            for (rowIndex in startRowIndex..endRowIndex) {
                val row: Row? = sheet.getRow(rowIndex)
                if (row == null) {
                    // Log.w(TAG, "從業人員薪資檔案 '$fileName' 在索引 $rowIndex 處跳過空行。")
                    continue
                }

                val gradeCell = row.getCell(gradeColIndex)
                val grade = getCellValueAsInt(gradeCell)

                val amountCell = row.getCell(amountColIndex)
                val amount = getCellValueAsBigDecimal(amountCell)

                if (grade == null || amount == null) {
                    Log.w(TAG, "從業人員薪資檔案 '$fileName' 在行 ${rowIndex + 1} 跳過：薪點($grade)或薪額($amount)為空或無效。")
                    continue
                }

                val employeeSalary = EmployeeSalary(
                    grade = grade,
                    amount = amount
                )
                employeeSalaries.add(employeeSalary)
            }
            workbook.close()
            Log.d(TAG, "成功讀取從業人員薪資檔案 '$fileName'，共 ${employeeSalaries.size} 筆記錄。")
        } catch (e: IOException) {
            Log.e(TAG, "讀取從業人員 Excel 檔案時發生 IO 錯誤: '$fileName': ${e.localizedMessage}", e)
        } catch (e: Exception) {
            Log.e(TAG, "解析從業人員 Excel 檔案時發生錯誤: '$fileName': ${e.localizedMessage}", e)
        } finally {
            inputStream?.close()
        }
        return employeeSalaries
    }

    /**
     * 安全地從 Cell 讀取 Int 值。
     */
    private fun getCellValueAsInt(cell: Cell?): Int? {
        return when (cell?.cellType) {
            CellType.NUMERIC -> cell.numericCellValue.toInt()
            CellType.STRING -> cell.stringCellValue.trim().toIntOrNull()
            else -> null
        }
    }

    /**
     * 安全地從 Cell 讀取 BigDecimal 值。
     * 處理數值和字串類型，並移除字串中的逗號。
     */
    private fun getCellValueAsBigDecimal(cell: Cell?): BigDecimal? {
        return when (cell?.cellType) {
            CellType.NUMERIC -> BigDecimal(cell.numericCellValue)
            CellType.STRING -> {
                val stringValue = cell.stringCellValue.trim()
                // 移除逗號，然後嘗試轉換為 BigDecimal
                stringValue.replace(",", "").toBigDecimalOrNull()
            }
            CellType.BLANK -> BigDecimal.ZERO // 將空白單元格視為 0
            else -> null // 其他類型返回 null
        }
    }
}