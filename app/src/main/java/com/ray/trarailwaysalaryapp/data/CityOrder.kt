package com.ray.trarailwaysalaryapp.data

/**
 * 台灣台鐵縣市順序
 */
object CityOrder {
    // 使用者指定的順序
    private val cityOrderList = listOf(
        "基隆市",
        "新北市",
        "臺北市", "台北市",  // 支援兩種寫法
        "桃園市",
        "新竹縣",
        "新竹市",
        "苗栗縣",
        "臺中市", "台中市",  // 支援兩種寫法
        "彰化縣",
        "南投縣",
        "雲林縣",
        "嘉義縣",
        "嘉義市",
        "臺南市", "台南市",  // 支援兩種寫法
        "高雄市",
        "屏東縣",
        "臺東縣", "台東縣",  // 支援兩種寫法
        "花蓮縣",
        "宜蘭縣"
    )

    // 用於顯示的順序（每個城市只出現一次）
    private val displayOrderList = listOf(
        "基隆市",
        "新北市",
        "臺北市",
        "桃園市",
        "新竹縣",
        "新竹市",
        "苗栗縣",
        "臺中市",
        "彰化縣",
        "南投縣",
        "雲林縣",
        "嘉義縣",
        "嘉義市",
        "臺南市",
        "高雄市",
        "屏東縣",
        "臺東縣",
        "花蓮縣",
        "宜蘭縣"
    )

    /**
     * 取得城市的排序索引，未知城市排在最後
     */
    fun getOrder(city: String): Int {
        // 將「台」轉換為「臺」進行比較
        val normalizedCity = city.replace("台", "臺")
        val index = displayOrderList.indexOf(normalizedCity)
        if (index >= 0) return index
        
        // 如果還是找不到，直接搜尋原始列表
        val originalIndex = cityOrderList.indexOf(city)
        return if (originalIndex >= 0) originalIndex else Int.MAX_VALUE
    }

    /**
     * 依照指定順序排序城市列表
     */
    fun sortCities(cities: List<String>): List<String> {
        return cities.sortedBy { getOrder(it) }
    }
}
