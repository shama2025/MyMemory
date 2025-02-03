package com.mashaffer.mymemory.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

    /**
     * Checks if the permission is granted
     */
    fun isPermissionGranted(contex: Context, permission: String):Boolean{
        return ContextCompat.checkSelfPermission(contex, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Prompts the user for permission request
     */
    fun requestPermission(activity: Activity?, permission: String, requestCode:Int){
        ActivityCompat.requestPermissions(activity!!, arrayOf(permission))
    }
