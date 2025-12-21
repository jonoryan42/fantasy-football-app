package com.example.fantasyfootballapp.model

import com.google.gson.annotations.SerializedName

//For requesting to add teams.

data class CreateTeamRequest(
    val teamName: String,
    val playerIds: List<Int>
)


