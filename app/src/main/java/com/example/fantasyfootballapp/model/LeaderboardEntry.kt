package com.example.fantasyfootballapp.model

data class LeaderboardEntry(
    val _id: String? = null,
    val teamName: String,
    val playerIds: List<Int> = emptyList(),
    val points: Int,
    val createdAt: String? = null
)