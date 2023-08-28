package com.example.bluetoothdemo

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.bluetoothdemo.databinding.ActivityMainBinding

private lateinit var btPrint: BtPrint
private lateinit var dialog: ProgressDialog

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

        fun dismissDialog() {
            if (dialog.isShowing) dialog.dismiss()
        }

        fun beginPrint() {
            btPrint.socketConnect { result ->
                if (result["success"] == false) {

                    this@MainActivity.runOnUiThread {

                        binding.activityReprintPrintInfo.text = result["text"].toString()
                        binding.activityReprintPrintSwitch.isChecked = false

                        dismissDialog()
                        Toast.makeText(this, "Printer connection error...", Toast.LENGTH_SHORT)
                            .show()

                        // TODO: Pooling?

                    }
                } else {
                    btPrint.doPrint(stringToPrint, true)
                }
            }
            binding.printReceipt.setOnClickListener {
                beginPrint()
            }
        }
    }
}