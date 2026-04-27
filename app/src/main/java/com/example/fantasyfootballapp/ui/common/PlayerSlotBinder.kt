package com.example.fantasyfootballapp.ui.common

import android.app.Activity
import android.view.View
import androidx.annotation.IdRes
import com.example.fantasyfootballapp.R

fun Activity.bindPlayerSlot(@IdRes includeId: Int): PlayerSlotView {
    val root = findViewById<View>(includeId)

    return PlayerSlotView(
        root = root,
        imgAdd = root.findViewById(R.id.imgAdd),
        filledGroup = root.findViewById(R.id.filledGroup),
        imgJersey = root.findViewById(R.id.imgJersey),
        name = root.findViewById(R.id.txtPlayerName),
        meta = root.findViewById(R.id.txtPlayerMeta),
//        clearButton = root.findViewById<View?>(R.id.btnClear) // only if it exists
    )
}

fun jerseyDrawableForClub(club: String?): Int {
    return when (club?.trim()?.lowercase()) {
        "ballinroad fc" -> R.drawable.ballinroadjersey
        else -> R.drawable.bg_jersey_placeholder
    }
}
