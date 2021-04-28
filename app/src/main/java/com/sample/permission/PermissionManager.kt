package com.sample.permission

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

/**
 * Interface for handling Android Runtime Permission.
 */
internal interface PermissionManager {

    /**
     * Request one or more [permission] and receive a single callback at [resultCallback].
     *
     * @param permission One or more permission request.
     * @param resultCallback Callback to get the status of [permission] request.
     */
    fun requestPermissions(
        vararg permission: Permission,
        resultCallback: (isAllPermissionGranted: Boolean, result: List<PermissionResult>) -> Unit
    )


    /**
     * Should wire up this method from [AppCompatActivity.onRequestPermissionsResult] or
     * [Fragment.onRequestPermissionsResult]
     */
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)

}

/**
 * Permission request model.
 *
 * @param permission Permission string to request. Constants defined in the [android.Manifest.permission].
 * @param rationaleTitle Title of the dialog box to show when the user denied the permission previously.
 * @param rationaleMessage Body of the dialog box to show when the user denied the permission previously.
 * @param posBtnText Positive button text of the dialog.
 * @param negBtnText Negative button text of the dialog.
 */
internal data class Permission(
    val permission: String,
    val rationaleTitle: String,
    val rationaleMessage: String,
    val posBtnText: String,
    val negBtnText: String
)

internal enum class PermissionState {
    PERMISSION_GRANTED,
    PERMISSION_DENIED,
    PERMISSION_DENIED_COMPLETELY//User either denied 2 times in Android 11 or pressed 'Never ask again' in older version
}

internal data class PermissionResult(val permission: String, val permissionState: PermissionState)
