package com.example.fantasyfootballapp.network

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val fname: String,
    val lname: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthUserDto(
    val _id: String,
    val fname: String,
    val lname: String,
    val email: String
)

data class AuthResponse(
    val token: String,
    val user: AuthUserDto
)

data class MeResponse(
    @SerializedName("_id")
    val id: String,
    val email: String,
    val teamName: String?,
    val createdAt: String
)


data class PlayerDto(
    val id: Int,
    val name: String,
    val teamId: String? = null,
    val position: String,
    val goals: Int? = 0,
    val assists: Int? = 0,
    val yellows: Int? = 0,
    val reds: Int? = 0,
    val cleansheets: Int? = 0,
    val owngoals: Int? = 0,
    val points: Int? = 0
)

//data class LeaderboardCreateRequest(
//    val teamName: String,
//    val playerIds: List<Int>
//)

data class LeaderboardTeamDto(
    val _id: String,
    val userId: String,
    val teamName: String,
    val playerIds: List<Int>,
    val createdAt: String,
    val points: Int
)