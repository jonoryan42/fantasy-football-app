package com.example.fantasyfootballapp.data.mappers

import com.example.fantasyfootballapp.model.LeaderboardEntry
import com.example.fantasyfootballapp.network.LeaderboardTeamDto

fun LeaderboardTeamDto.toModel(): LeaderboardEntry =
    LeaderboardEntry(
        _id = _id,
        userId = userId,
        teamName = teamName,
        playerIds = playerIds,
        points = points,
        createdAt = createdAt
    )