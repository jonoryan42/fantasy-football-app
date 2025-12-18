package com.example.fantasyfootballapp.model
data class User(
    val id: Int,
    val username: String,
    val teamName: String
)

data class Team(
    val playerIds: MutableList<Int>
)