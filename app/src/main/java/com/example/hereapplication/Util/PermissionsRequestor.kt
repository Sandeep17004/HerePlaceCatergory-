package com.example.hereapplication.Util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.hereapplication.MainActivity


class PermissionsRequester(context: MainActivity) {
    private val PERMISSIONS_REQUEST_CODE = 42
    private lateinit var resultListener: ResultListener
    private var activity: Activity = context

    interface ResultListener {
        fun permissionsGranted()
        fun permissionsDenied()
    }

    fun request(resultListener: ResultListener) {
        this.resultListener = resultListener
        val missingPermissions = getPermissionsToRequest()
        if (missingPermissions.isEmpty()) {
            resultListener.permissionsGranted()
        } else {
            ActivityCompat.requestPermissions(
                activity,
                missingPermissions,
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    private fun getPermissionsToRequest(): Array<String> {
        val permissionList: ArrayList<String> = ArrayList()
        try {
            val packageInfo = activity.packageManager.getPackageInfo(
                activity.packageName, PackageManager.GET_PERMISSIONS
            )
            if (packageInfo.requestedPermissions != null) {
                for (permission in packageInfo.requestedPermissions) {
                    if (ContextCompat.checkSelfPermission(
                            activity, permission
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M && permission == Manifest.permission.CHANGE_NETWORK_STATE) {
                            // Exclude CHANGE_NETWORK_STATE as it does not require explicit user approval.
                            // This workaround is needed for devices running Android 6.0.0,
                            // see https://issuetracker.google.com/issues/37067994
                            continue
                        }
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                            (permission == Manifest.permission.ACTIVITY_RECOGNITION || permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        ) {
                            continue
                        }
                        permissionList.add(permission)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return permissionList.toArray(arrayOfNulls(0))
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (grantResults.isEmpty()) {
            // Request was cancelled.
            return
        }
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            var allGranted = true
            for (result in grantResults) {
                allGranted = allGranted and (result == PackageManager.PERMISSION_GRANTED)
            }
            if (allGranted) {
                resultListener.permissionsGranted()
            } else {
                resultListener.permissionsDenied()
            }
        }
    }
}