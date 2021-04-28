package com.sample.permission

import android.app.Activity
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.sample.permission.PermissionManagerImpl.Companion.fromActivity
import com.sample.permission.PermissionManagerImpl.Companion.fromFragment
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.LinkedHashMap

/**
 * PermissionManager class helps to make the life easier working with android M runtime permission.
 *
 * Use factory method [fromActivity] if permission are requested from activity, and [fromFragment] if permission are
 * requested from fragment.
 *
 * [Ref](https://developer.android.com/images/training/permissions/workflow-runtime.svg)
 *
 */
internal class PermissionManagerImpl private constructor(activity: Activity, fragment: Fragment?) :
    PermissionManager {

    companion object {

        fun fromActivity(activity: Activity): PermissionManager {
            return PermissionManagerImpl(activity, null)
        }

        fun fromFragment(activity: Activity, fragment: Fragment): PermissionManager {
            return PermissionManagerImpl(activity, fragment)
        }
    }

    private val permissionRequestMap = LinkedHashMap<Int, Permission>()

    private val pendingPermissionQueue = ArrayDeque<Int>()

    private val grantedPermissionList = mutableListOf<String>()

    private val deniedPermissionList = mutableListOf<String>()

    private val completelyDeniedPermissionList = mutableListOf<String>()

    private lateinit var resultCallback: (isAllPermissionGranted: Boolean, result: List<PermissionResult>) -> Unit

    private val activityRef: WeakReference<Activity> = WeakReference(activity)

    private val fragmentRef: WeakReference<Fragment>? = if (fragment == null) null else WeakReference(fragment)

    override fun requestPermissions(
        vararg permission: Permission,
        resultCallback: (isAllPermissionGranted: Boolean, result: List<PermissionResult>) -> Unit
    ) {
        if (permission.isEmpty())
            throw IllegalArgumentException("Should request at-least one permission")

        this.resultCallback = resultCallback

        permissionRequestMap.clear()
        permission.forEachIndexed { i, p -> permissionRequestMap[i] = p }

        pendingPermissionQueue.clear()
        pendingPermissionQueue.addAll(permissionRequestMap.keys)

        checkPermission(pendingPermissionQueue.peek()!!)
    }

    private fun checkPermission(permissionId: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            onPermissionGranted(permissionId)
            return
        }

        val permission = permissionRequestMap[permissionId]!!.permission

        if (isPermitted(permission)) {
            onPermissionGranted(permissionId)
            return
        }

        if (doWeNeedToExplain(permission)) {
            explainAboutPermission(permissionId) { buttonType ->
                if (buttonType == DialogInterface.BUTTON_POSITIVE)
                    requestPermission(permissionId)
                else
                    onPermissionDenied(permissionId)
            }
        } else {
            requestPermission(permissionId)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (!permissionRequestMap.containsKey(requestCode)) return

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            onPermissionGranted(requestCode)
        else
            handleDeny(requestCode)

    }

    private fun handleDeny(permissionId: Int) {
        if (doWeNeedToExplain(permissionRequestMap[permissionId]!!.permission)) {
            onPermissionDenied(permissionId)
        } else {
            onPermissionDeniedCompletely(permissionId)
        }
    }

    private fun onPermissionGranted(permissionId: Int) {
        grantedPermissionList.add(permissionRequestMap[permissionId]!!.permission)
        checkNextPermission()
    }

    private fun onPermissionDenied(permissionId: Int) {
        deniedPermissionList.add(permissionRequestMap[permissionId]!!.permission)
        checkNextPermission()
    }

    private fun onPermissionDeniedCompletely(permissionId: Int) {
        completelyDeniedPermissionList.add(permissionRequestMap[permissionId]!!.permission)
        checkNextPermission()
    }

    private fun checkNextPermission() {
        pendingPermissionQueue.poll()
        if (!pendingPermissionQueue.isEmpty())
            checkPermission(pendingPermissionQueue.peek()!!)
        else
            dispatchResult()
    }

    private fun dispatchResult() {

        val permissionResults = mutableListOf<PermissionResult>()

        grantedPermissionList.forEach { permission ->
            permissionResults.add(PermissionResult(permission, PermissionState.PERMISSION_GRANTED))
        }
        deniedPermissionList.forEach { permission ->
            permissionResults.add(PermissionResult(permission, PermissionState.PERMISSION_DENIED))
        }
        completelyDeniedPermissionList.forEach { permission ->
            permissionResults.add(PermissionResult(permission, PermissionState.PERMISSION_DENIED_COMPLETELY))
        }

        val isAllPermissionGranted = grantedPermissionList.size == permissionResults.size

        grantedPermissionList.clear()
        deniedPermissionList.clear()
        completelyDeniedPermissionList.clear()

        resultCallback(isAllPermissionGranted, permissionResults)

    }

    private fun explainAboutPermission(permissionId: Int, dialogButtonClickListener: (buttonType: Int) -> Unit) {
        val permission = permissionRequestMap[permissionId]!!

        val alertDialogButtonClickListener = DialogInterface.OnClickListener { dialog, which ->
            dialogButtonClickListener.invoke(which)
        }
        AlertDialog.Builder(activityRef.get()!!).apply {
            setTitle(permission.rationaleTitle)
            setMessage(permission.rationaleMessage)
            setPositiveButton(permission.posBtnText, alertDialogButtonClickListener)
            setNegativeButton(permission.negBtnText, alertDialogButtonClickListener)
        }.show()
    }

    private fun requestPermission(permissionID: Int) {
        val permission = permissionRequestMap[permissionID]!!.permission

        if (null != fragmentRef)
            fragmentRef.get()!!.requestPermissions(arrayOf(permission), permissionID)
        else
            ActivityCompat.requestPermissions(activityRef.get()!!, arrayOf(permission), permissionID)
    }

    private fun isPermitted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activityRef.get()!!, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun doWeNeedToExplain(permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activityRef.get()!!, permission)
    }

}