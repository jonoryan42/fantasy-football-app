package com.example.fantasyfootballapp.ui.common

import android.view.View
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

object SystemBars {

    fun apply(
        activity: AppCompatActivity,
        @ColorRes colorRes: Int,
        lightIcons: Boolean
    ) {
        val color = ContextCompat.getColor(activity, colorRes)
        activity.window.navigationBarColor = color

        activity.window.decorView.systemUiVisibility =
            if (lightIcons) {
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                0
            }
    }
}