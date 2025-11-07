package com.zx.puzi.utils

import android.content.Context
import android.content.pm.PackageManager

/**
 * 获取应用版本号
 */
fun Context.getAppCode(): Int {
    return try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        packageInfo.versionCode
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        Int.MAX_VALUE
    }
}

/**
 * 获取应用版本名称
 */
fun Context.getAppVersion(): String {
    return try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        packageInfo.versionName ?: ""
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        ""
    }
}