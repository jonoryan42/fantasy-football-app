package com.example.fantasyfootballapp.model

//For making changes to your team
data class UpdateUserTeamRequest(
    val squadPlayerIds: List<Int>? = null,
    val slotPlayerIds: Map<String, Int?>? = null,
    val formationKey: String? = null
)