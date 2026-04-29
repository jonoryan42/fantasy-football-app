package com.example.fantasyfootballapp.ui.common

import android.app.Activity
import android.util.Log
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
//        clearButton = root.findViewById<View?>(R.id.btnClear)
    )
}

//func for assigning jerseys to players in the activities, depending on their current club
fun jerseyDrawableForClub(club: String?): Int {
    val c = club?.trim()?.lowercase() ?: return R.drawable.bg_jersey_placeholder

    return when {
        c.contains("ballinroad") -> R.drawable.ballinroadjersey
        c.contains("bohemian") -> R.drawable.bohemiansjersey
        c.contains("tycor") -> R.drawable.tycorjersey
        c.contains("ferrybank") -> R.drawable.ferrybankjersey
        c.contains("tramore") -> R.drawable.tramorejersey
        c.contains("villa") -> R.drawable.villajersey
        c.contains("dungarvan") -> R.drawable.dungarvanjersey
        c.contains("waterford") && c.contains("crystal")-> R.drawable.waterfordcrystaljersey
        c.contains("portlaw") -> R.drawable.portlawjersey

        else -> {
            Log.d("JERSEY_DEBUG", "No jersey for club: $club")
            R.drawable.bg_jersey_placeholder
        }
    }
}
