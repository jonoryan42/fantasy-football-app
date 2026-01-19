package com.example.fantasyfootballapp.model

data class GameweekStat(
    val playerId: Int,
    val season: String,
    val gameweek: Int,
    val points: Int,
    val minutes: Int? = null,
    val goals: Int? = null,
    val assists: Int? = null,
    val cleansheet: Boolean? = null,
    val yellows: Int? = null,
    val reds: Int? = null
)
