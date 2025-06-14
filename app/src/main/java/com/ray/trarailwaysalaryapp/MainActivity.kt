package com.ray.trarailwaysalaryapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment // 確保導入這個
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // 關鍵修改在這裡：
        // 我們不能直接用 findNavController(R.id.nav_host_fragment)
        // 因為它可能在 NavHostFragment 準備好之前就被調用了。
        // 正確的方式是先取得 NavHostFragment 實例，再從它獲取 NavController。
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 將 BottomNavigationView 與 NavController 綁定
        bottomNavigationView.setupWithNavController(navController)
    }
}