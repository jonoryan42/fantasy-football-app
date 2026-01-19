package com.example.fantasyfootballapp.ui.pickTeam

data class PlayerSlot(
    val index: Int,          // 0..14
    val position: String,    // "GK", "DEF", "MID", "STR"
    var playerId: Int? = null
)
