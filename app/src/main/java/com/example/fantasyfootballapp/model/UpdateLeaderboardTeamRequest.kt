package com.example.fantasyfootballapp.model

data class UpdateLeaderboardTeamRequest(
    val squadPlayerIds: List<Int>? = null,
    val slotPlayerIds: Map<String, Int?>? = null,
    val formationKey: String? = null
)