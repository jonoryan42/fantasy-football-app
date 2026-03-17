package com.example.fantasyfootballapp.model

data class LeagueRow(
    val position: Int,
    val team: String,
    val played: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val points: Int
)