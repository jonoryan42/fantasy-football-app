package com.example.fantasyfootballapp.network

import com.example.fantasyfootballapp.model.CreateTeamRequest
import com.example.fantasyfootballapp.model.LeaderboardEntry
import com.example.fantasyfootballapp.model.Player
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST

//App Endpoints
interface ApiService {
    @GET("api/leaderboard")
    suspend fun getLeaderboard(): List<LeaderboardEntry>

    @GET("api/players")
    suspend fun getPlayers(): List<Player>

    @POST("api/leaderboard")
    suspend fun createTeam(@Body body: CreateTeamRequest): LeaderboardEntry
}