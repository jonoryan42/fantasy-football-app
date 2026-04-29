@file:Suppress("DEPRECATED_ANNOTATION")

package com.example.fantasyfootballapp.network

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
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
    val fname: String,
    val lname: String,
    val email: String,
    val teamName: String?,
    val createdAt: String
)

@Parcelize
data class LeaderboardTeamDto(
    val _id: String? = null,
    val userId: String,
    val teamName: String,
    val playerIds: List<Int>? = null,
    val squadPlayerIds: List<Int>? = null,
    val slotPlayerIds: Map<String, Int?>? = null,
    val formationKey: String? = null,
    val points: Int = 0,
    val createdAt: String? = null
) : Parcelable

//Two models for Fixtures
data class FixtureEvent(
    val playerName: String? = null,
    val team: String,
    val minute: Int,
    val type: String? = null
)

data class Fixture(
    val season: String,
    val gameweek: Int,
    val homeTeam: String,
    val awayTeam: String,
    val played: Boolean,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val scorers: List<FixtureEvent> = emptyList(),
    val cards: List<FixtureEvent> = emptyList()
)

data class UserGameweekScoreDto(
    val userId: String,
    val teamName: String,
    val season: String,
    val gameweek: Int,
    val points: Int,
    val squadPlayerIds: List<Int> = emptyList(),
    val slotPlayerIds: Map<String, Int?> = emptyMap(),
    val formationKey: String? = null
)