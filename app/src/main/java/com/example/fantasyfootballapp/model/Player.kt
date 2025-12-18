package com.example.fantasyfootballapp.model

data class Player(
    val id: Int,
    val name: String,
    val club: String,
    val position: String,   // "GK", "DEF", "MID", "STR"
    val goals: Int,
    val assists: Int,
    val cleansheets: Int,
    val yellows: Int,
    val reds: Int,
    val owngoals: Int,
    val points: Int
)
