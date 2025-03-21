package com.zx.puzi.utils

import android.content.Context
import android.content.pm.PackageManager

fun Context.getAppCode(): Int {
    return try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        packageInfo.versionCode
    } catch (e: PackageManager.NameNotFoundException) {
        Int.MAX_VALUE
    }
}

fun Context.getAppVersion(): String {
    return try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        packageInfo.versionName ?: ""
    } catch (e: PackageManager.NameNotFoundException) {
        ""
    }
}