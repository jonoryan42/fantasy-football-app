package com.example.fantasyfootballapp.model

data class LeaderboardEntry(
    val _id: String? = null,
    val teamName: String,
    val playerIds: List<Int> = emptyList(),
    val points: Int = 0,
    val createdAt: String? = null
)