package com.example.fantasyfootballapp.ui.common

import android.view.View
import android.widget.ImageView
import android.widget.TextView

//Everything shown in the slots for each player
data class PlayerSlotView(
    val root: View,
    val imgAdd: ImageView,
    val filledGroup: View,
    val imgJersey: ImageView,
    val name: TextView,
    val meta: TextView,
    val clearButton: View? = null
)


