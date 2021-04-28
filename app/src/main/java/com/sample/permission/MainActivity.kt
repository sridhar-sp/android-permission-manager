package com.sample.permission

import android.Manifest
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var permissionManager: PermissionManager

    private lateinit var resultTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultTextView = findViewById(R.id.permissionResultTextView)

        findViewById<Button>(R.id.requestPermissionButton).setOnClickListener { requestPermission() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun requestPermission() {

        permissionManager = PermissionManagerImpl.fromActivity(this)

        permissionManager.requestPermissions(
                getReadSmsPermission(),
                getReadContactsPermission(),
                getWriteExternalStoragePermission()
        ) { isAllPermissionGranted, permissionResult ->
            resultTextView.text = "Is All permission granted $isAllPermissionGranted\nResult\n$permissionResult"
        }
    }

    private fun getReadSmsPermission(): Permission {
        return Permission(Manifest.permission.READ_SMS,
                getString(R.string.text_permission_required),
                getString(R.string.text_read_sms_rationale),
                getString(R.string.text_ok), getString(R.string.text_cancel))
    }

    private fun getReadContactsPermission(): Permission {
        return Permission(Manifest.permission.READ_CONTACTS,
                getString(R.string.text_permission_required),
                getString(R.string.text_read_contacts_rationale),
                getString(R.string.text_ok), getString(R.string.text_cancel))
    }

    private fun getWriteExternalStoragePermission(): Permission {
        return Permission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                getString(R.string.text_permission_required),
                getString(R.string.text_write_external_storage_rationale),
                getString(R.string.text_ok), getString(R.string.text_cancel))
    }

}