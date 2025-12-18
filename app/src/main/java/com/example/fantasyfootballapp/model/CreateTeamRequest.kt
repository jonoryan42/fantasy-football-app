package com.example.fantasyfootballapp.model

//For requesting to add teams.

data class CreateTeamRequest(
    val teamName: String,
    val playerIds: List<Int>
)
