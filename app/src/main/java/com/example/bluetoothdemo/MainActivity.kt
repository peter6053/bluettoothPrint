package com.example.bluetoothdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.bluetoothdemo.databinding.ActivityMainBinding

private lateinit var btPrint: BtPrint

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)

        btPrint = BtPrint(printSwitch, printLoading, activity_reprint_printInfo, transactionButton)
    }
}