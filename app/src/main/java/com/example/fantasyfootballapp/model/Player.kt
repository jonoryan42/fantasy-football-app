package com.example.fantasyfootballapp.model

data class Player(
    val id: Int,
    val name: String,
    val club: String,
    val position: Position, // GK, DEF, MID, STR
    val price: Double
)
