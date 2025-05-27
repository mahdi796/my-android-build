package com.example.infoviewer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var infoTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        infoTextView = findViewById(R.id.infoTextView)

        if (checkPermissions()) {
            displayInfo()
        } else {
            requestPermissions()
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_CALL_LOG
            ),
            100
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 100 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            displayInfo()
        } else {
            infoTextView.text = "دسترسی لازم داده نشد."
        }
    }

    private fun displayInfo() {
        val contactInfo = getContacts()
        val callLogs = getCallLogs()
        val batteryStatus = getBatteryStatus()

        val result = "📋 اطلاعات دستگاه:\n\n$contactInfo\n$callLogs\n$batteryStatus"
        infoTextView.text = result
    }

    private fun getContacts(): String {
        val contacts = StringBuilder("👥 مخاطبین:\n")
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null, null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val phone = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                contacts.append("$name : $phone\n")
            }
        }
        return contacts.toString()
    }

    private fun getCallLogs(): String {
        val calls = StringBuilder("\n📞 تماس‌ها:\n")
        val cursor: Cursor? = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null, null, null, CallLog.Calls.DATE + " DESC"
        )
        cursor?.use {
            var count = 0
            while (it.moveToNext() && count < 5) {
                val number = it.getString(it.getColumnIndex(CallLog.Calls.NUMBER))
                val type = it.getInt(it.getColumnIndex(CallLog.Calls.TYPE))
                val duration = it.getString(it.getColumnIndex(CallLog.Calls.DURATION))
                val dir = when (type) {
                    CallLog.Calls.OUTGOING_TYPE -> "خروجی"
                    CallLog.Calls.INCOMING_TYPE -> "ورودی"
                    CallLog.Calls.MISSED_TYPE -> "بی‌پاسخ"
                    else -> "دیگر"
                }
                calls.append("$dir: $number، مدت: $duration ثانیه\n")
                count++
            }
        }
        return calls.toString()
    }

    private fun getBatteryStatus(): String {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } else {
            -1
        }
        return "\n🔋 شارژ باتری: $batteryLevel%"
    }
}
