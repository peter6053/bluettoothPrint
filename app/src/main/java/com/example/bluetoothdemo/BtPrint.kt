package com.example.bluetoothdemo

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.thread


class BtPrint (

    private var printSwitch: Switch,
    private var printLoading: ProgressBar,
    private var printInfo: TextView,
    private var printButton: Button

): AppCompatActivity() {


    // Define the caller context and activity (tips on optimization will be much appreciated :)

    private val context = printSwitch.context
    private val activity = context as Activity
    private val sharedPrefs = context.getSharedPreferences(context.packageName + ".META", Context.MODE_PRIVATE)


    // Other initializer
    private var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private var printers = ArrayList<BluetoothDevice>()


    private lateinit var printer: BluetoothDevice
    private lateinit var socket: BluetoothSocket
    private val MY_PERMISSIONS_REQUEST_BLUETOOTH = 1


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.d("sdk12uuuuuuuu4rr", "refreshPrinters: ${Build.VERSION.SDK_INT}")

        // btPrint.onRequPermissionsResult(requestCode, permissions as Array<String>, grantResults)
    }










    init {


        // Set Views to initial value.

        preCheckStart()


        // Only proceed if there's a record in sharedPrefs...

        if (sharedPrefs.getString("lastPrinter", "") != "") {

            preCheck()

        } else {


            // ...to skip (second) auto connection attempt if no printers active...

            printInfo.text = "Not going to print"
            preCheckDone()

        }


        // ...but of course proceed if switched manually.

        printSwitch.setOnClickListener {

            if (printSwitch.isChecked) {

                preCheck()

            } else {

                printInfo.text = "Not going to print"

            }

        }

    }

    private fun preCheck() {
        preCheckStart()

        val bluetoothPermission = Manifest.permission.BLUETOOTH
        Log.d("sdk124rr", "refreshPrinters: ${Build.VERSION.SDK_INT}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(context, bluetoothPermission)
            != PackageManager.PERMISSION_GRANTED)
        {
            Log.d("sdk124hjhgr", "refreshPrinters: ${Build.VERSION.SDK_INT}")

            if (ContextCompat.checkSelfPermission(context, bluetoothPermission)
                != PackageManager.PERMISSION_GRANTED) {


                // Permission not granted, show a message to the user
                printInfo.text = "Please grant Bluetooth permission to use this app."
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                    Log.d("sdk124ryytr", "refreshPrinters: ${Build.VERSION.SDK_INT}")
                    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.BLUETOOTH), MY_PERMISSIONS_REQUEST_BLUETOOTH)
                    preCheckDone()
                }
            }


            printSwitch.isChecked = false
            preCheckDone()

            // Stop pre-check until permission is granted

        }

        // Bluetooth permission granted, check if Bluetooth is available and enabled
        if (bluetoothAdapter == null) {
            printInfo.text = "This thing has no Bluetooth"
            printSwitch.isChecked = false
            preCheckDone()
        } else if (!bluetoothAdapter.isEnabled) {
            printInfo.text = "Bluetooth is inactive"
            printSwitch.isChecked = false
            preCheckDone()
        } else {
            // Bluetooth is available and enabled, continue with pre-check
            refreshPrinters()

            if (printers.size > 0) {


                // Loop the printers to crosscheck with sharedPrefs, and also to prepare arrays for printer selection
                // dialog if needed.

                val pNames = Array(printers.size) { "" }
                val pAddrs = Array(printers.size) { "" }

                var deviceFound = false

                for (i in 0 until printers.size) {

                    pNames[i] = printers[i].name
                    pAddrs[i] = printers[i].address // How to do this "correctly" in Kotlin? :D


                    // Printer available in sharedPrefs, attempt connection and break.

                    if (printers[i].address == sharedPrefs.getString("lastPrinter", "")) {

                        deviceFound = true
                        printer = printers[i]
                        testConnection()
                        break

                    }

                }


                // If it gets here

                if (!deviceFound) {


                    // Show printer selection dialog

                    val builder = AlertDialog.Builder(context)
                    builder.setTitle("Select printer")
                        .setItems(
                            pNames
                        ) { _, which ->


                            // On selected, save and rerun preCheck()

                            sharedPrefs.edit().putString("lastPrinter", pAddrs[which]).apply()
                            preCheck()

                        }
                    builder.create()
                    builder.setCancelable(false)
                    builder.show()

                }

            } else {


                // No printers

                printInfo.text = "Please pair a printer"
                printSwitch.isChecked = false
                preCheckDone()

            }

        }

    }


    private fun refreshPrinters() {
        // Check for Bluetooth permission
        Log.d("sdk", "refreshPrinters: ${Build.VERSION.SDK_INT}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

            val   MY_PERMISSIONS_REQUEST_BLUETOOTH_CONNECT = 1
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted, request it
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    MY_PERMISSIONS_REQUEST_BLUETOOTH_CONNECT
                )
                Log.d("test", "refreshPrinters: ")

            }

        }

        else {
            Log.d("testelse", "refreshPrinters: ")
            // Permission already granted, continue with Bluetooth device discovery
            // Clean up first, but I found out that sometimes the device must be reset, and/or Clear Data in
            // Bluetooth Share app, to really refresh the paired devices list, after we Forget/Add a bluetooth device.
            // This probably a device issue (or just me lacking experience).

            printers.clear()
            // Filter the paired devices for printers
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() // this is my attempt to "really refresh" the list
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
            if (pairedDevices != null) {
                for (i in pairedDevices) {
                    if (i.bluetoothClass.deviceClass.toString() == "1664") { // Identifier (or something) for printers.
                        // Now we have a list of printers
                        printers.add(i)
                    } else {
                        printers.add(i)
                    }
                }
            }
        }
    }



    private fun testConnection() {
        // Check for Bluetooth permission
        Log.d("sdk123", "refreshPrinters: ${Build.VERSION.SDK_INT}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_SCAN),
                    MY_PERMISSIONS_REQUEST_BLUETOOTH)

            }
        }
        // Make sure discovery isn't running
        /* if (bluetoothAdapter?.isDiscovering == true) {
              bluetoothAdapter?.cancelDiscovery()
          }*/

        // Socket connect will freeze the UI, so we're doing this in another thread and get the callback
        printInfo.text = printer.name
        printInfo.append("... ")

        socketConnect { result ->
            // Other threads can't touch UI without runOnUiThread()
            activity.runOnUiThread {
                printInfo.append(result["text"].toString())

                // Connection failed, delete from sharedPrefs
                if (result["success"] == false) {
                    sharedPrefs.edit().putString("lastPrinter", "").apply()
                    printSwitch.isChecked = false
                } else {
                    printSwitch.isChecked = true
                }
                preCheckDone()
            }

            // Done checking. From here we assume the printer is active, nearby, and is ready for real printing.
            // We close the socket to allow other devices to use the printer although it might require some more coding
            // to imitate some kind of pooling service (no such service in my two test/cheap printers here).
            socket.close()
        }
    }

    fun socketConnect(callback: (HashMap<String, Any>) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted, request it
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.BLUETOOTH), MY_PERMISSIONS_REQUEST_BLUETOOTH)
                return
            }
        }

        // Check for Bluetooth permission


        // Make sure socket is closed
        if (::socket.isInitialized) socket.close()

        // Google for explanation on this :D
        try {
            socket = printer.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
        } catch (e: Exception) {
            activity.runOnUiThread {
                Toast.makeText(context, "Printer not connected!", Toast.LENGTH_SHORT).show()
            }
            return
        }

        thread(start = true) {

            val result = HashMap<String, Any>()

            try {
                socket.connect()
                result["success"] = true
                result["text"] = "connected."
            } catch (e: IOException) {
                result["success"] = false
                result["text"] = "Could not connect to Printer"
            } catch (e: SecurityException) {
                result["success"] = false
                result["text"] = "Bluetooth permission denied"
            }

            callback(result)

        }
    }


    private fun preCheckStart() {
        printLoading.visibility = View.VISIBLE
        printButton.isClickable = false
        printSwitch.alpha = .25f
    }

    private fun preCheckDone() {
        printLoading.visibility = View.INVISIBLE
        printButton.isClickable = true
        printSwitch.alpha = 1f

    }


    /*    fun doPrint(stringToPrint: String, keepSocket: Boolean = false) {
            // ESC/POS default format

          try{
                socket.outputStream.write(byteArrayOf(27, 33, 0))
           }catch (e : Exception){
                showMessage("Printing errortest...")
           }


            // Print your string

           try{
                socket.outputStream.write(stringToPrint.toByteArray())
            }catch (e : Exception){
                showMessage("Printing errooner...")
           }


            if (!keepSocket) {

                socket.close()
            }

        }*/
    fun doPrint(stringToPrint: String, keepSocket: Boolean = false) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                    // Permission not granted, request it
                    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.BLUETOOTH), MY_PERMISSIONS_REQUEST_BLUETOOTH)
                    return
                }
            }
            if (!socket.isConnected) {
                // Open the socket if it's not already open
                socket = printer.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                socket.connect()
            }

            // ESC/POS default format
            socket.outputStream.write(byteArrayOf(27, 33, 0))

            // Print your string
            socket.outputStream.write(stringToPrint.toByteArray())

            // Flush the output stream to check for errors
            socket.outputStream.flush()

            // Check for any errors by catching and logging any exceptions thrown
            try {
                //  Thread.sleep(500) // Wait for 500 milliseconds to give the printer time to respond
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            val response = ByteArray(1024)
            val inputStream = socket.inputStream
            if (inputStream.available() > 0) {
                inputStream.read(response)
                val errorResponse = String(response).trim { it <= ' ' }
                if (errorResponse.isNotEmpty()) {
                    // showMessage("Printing error: $errorResponse")
                }
            }
        } catch (e : Exception) {
            // Log.d("TAG", "doPrint: ${e.message}")
            //showMessage("Printing error: ${e.message}")
        } finally {
            // Close the socket if keepSocket is false
            if (!keepSocket) {
                socket.close()
            }
        }
    }



    fun showMessage(message : String,keepSocket: Boolean = false){
        activity.runOnUiThread{
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
        if (!keepSocket) {
            socket.close()
        }
    }

    fun printBitmap(bitmap: Bitmap,keepSocket: Boolean = false){
        // ESC/POS default format

        try{
            // Thread.sleep(5000)
            socket.outputStream.write(byteArrayOf(27, 33, 0))
        }catch ( e : Exception){
            showMessage("Printing error...")
            Log.d("exempto=ion", "printBitmap: $e")
        }

        // Print your string

        try{
            socket.outputStream.write(DecodeBitmap.decodeBitmap(bitmap))
        }catch ( e : Exception){
            showMessage("Printing error...")
        }


        if (!keepSocket) {
            // Thread.sleep(500)
            socket.close()
        }
    }

}