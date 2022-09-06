package com.smsforwarder

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.telephony.SmsManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.opencsv.CSVReader
import com.smsforwarder.databinding.ActivityMainBinding
import java.io.InputStreamReader


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var rowsMap: MutableList<PhoneNumberMessageMapping> = mutableListOf()

    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkForSmsPermission()
        binding.sendSms.setOnClickListener {
            sendMessage()
        }
        binding.addFile.setOnClickListener {
            chooseFile()
        }
    }

    private fun checkForSmsPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), MY_PERMISSIONS_REQUEST_SEND_SMS)
        }
    }

    private fun chooseFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivityForResult(Intent.createChooser(intent, "Document"), DOCUMENT_CHOOSER_REQUEST_CODE)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun sendMessage() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), MY_PERMISSIONS_REQUEST_SEND_SMS)
        } else {
            try {
                binding.sendSms.isEnabled = false
                sendMessageForIndex(0)
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "SMS failed, please try again later ! ", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun sendMessageForIndex(index: Int) {
        if (index < rowsMap.size) {
            val phoneNumber = rowsMap[index].number
            val message = rowsMap[index].message
            handler.postDelayed({
                if (message.length <= 160) {
                    val smsManager: SmsManager = SmsManager.getDefault()
                    val sentPI = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.getBroadcast(this, 0, Intent(SENT), PendingIntent.FLAG_MUTABLE)
                    } else {
                        PendingIntent.getBroadcast(this, 0, Intent(SENT), 0)
                    }
                    smsManager.sendTextMessage(phoneNumber, null, message, sentPI, null)
                    Toast.makeText(this, "Message Sent $index", Toast.LENGTH_SHORT).show()
                    binding.fileName.text = "Number Of Messages Sent $index"
                }
                sendMessageForIndex(index + 1)
            }, 2000)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_SEND_SMS -> {
                if (permissions[0].equals(Manifest.permission.SEND_SMS, ignoreCase = true) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //do nothing
                } else {
                    Toast.makeText(this, "App need permission to work properly", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DOCUMENT_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            contentResolver.openInputStream(uri!!)?.use { inputStream ->
                val reader = CSVReader(InputStreamReader(inputStream))
                reader.readAll().forEach { row ->
                    rowsMap.add(PhoneNumberMessageMapping(row[0], row[1]))
                }
                binding.sendSms.isEnabled = true
                binding.fileName.text = "Number of contact is: ${rowsMap.size}"
            }
        }
    }

    data class PhoneNumberMessageMapping(val number: String, val message: String)

    companion object {
        private const val MY_PERMISSIONS_REQUEST_SEND_SMS = 1
        private const val DOCUMENT_CHOOSER_REQUEST_CODE = 2
        private const val SENT = "SMS_SENT"
    }

}