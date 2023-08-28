package com.example.bluetoothdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.bluetoothdemo.databinding.ActivityMainBinding

private lateinit var btPrint: BtPrint

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)

        btPrint = BtPrint(
            binding.activityReprintPrintSwitch,
            binding.activityReprintPrintLoading,
            binding.activityReprintPrintInfo,
            binding.printReceipt
        )

        val stringToPrint = "print success" +
                          "you are a winner"

        fun beginPrint() {
            if (result["success"] == false) {

                this@MainActivity.runOnUiThread {

                    binding.activityReprintPrintInfo.text = result["text"].toString()
                    binding.activityReprintPrintSwitch.isChecked = false

                    dismissDialog()
                    Toast.makeText(this, "Printer connection error...", Toast.LENGTH_SHORT).show()

                    // TODO: Pooling?

                }

            } else {
                btPrint.doPrint(stringToPrint, true)
            }
        }
        binding.printReceipt.setOnClickListener {
            btPrint.doPrint(stringToPrint, true)
        }
    }
}