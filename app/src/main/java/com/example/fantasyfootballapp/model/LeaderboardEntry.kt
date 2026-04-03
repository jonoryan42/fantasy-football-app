package com.example.fantasyfootballapp.model

import com.example.fantasyfootballapp.network.LeaderboardTeamDto

data class LeaderboardEntry(
    val _id: String? = null,
    val userId: String,
    val teamName: String,
    val playerIds: List<Int> = emptyList(),
    val points: Int,
    val createdAt: String? = null,
    val teamDto: LeaderboardTeamDto? = null
)