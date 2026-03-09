package com.example.fantasyfootballapp.model

data class RegisterWithTeamRequest(
    val fname: String,
    val lname: String,
    val email: String,
    val password: String,
    val teamName: String,
    val playerIds: List<Int>,
    val slotPlayerIds: Map<String, Int?>? = null,
    val formationKey: String? = null
)