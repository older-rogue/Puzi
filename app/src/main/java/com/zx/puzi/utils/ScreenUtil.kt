package com.zx.puzi.utils

import android.app.Activity
import android.content.res.Resources
import android.view.WindowInsets

object ScreenUtil {

    fun getAvailableScreenHeight(activity: Activity): Int {
        val insets = activity.windowManager.currentWindowMetrics.windowInsets
        val statusBarHeight = insets.getInsets(WindowInsets.Type.statusBars()).top
        val navigationBarHeight = insets.getInsets(WindowInsets.Type.navigationBars()).bottom
        return activity.windowManager.currentWindowMetrics.bounds.height() - statusBarHeight - navigationBarHeight
    }

    fun dp2px(dpValue: Float): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
} 