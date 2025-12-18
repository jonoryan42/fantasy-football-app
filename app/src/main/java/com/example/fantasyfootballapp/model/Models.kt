package com.example.fantasyfootballapp.model

data class Player(
    val id: Int,
    val name: String,
    val position: String,      // "GK", "DEF", "MID", "STR"
    val teamName: String,
    val price: Int,
    val points: Int            // current total points
)

data class User(
    val id: Int,
    val username: String,
    val teamName: String
)

data class Team(
    val playerIds: MutableList<Int>
)