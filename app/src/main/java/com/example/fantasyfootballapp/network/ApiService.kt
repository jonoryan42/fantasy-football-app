package com.example.fantasyfootballapp.network

import com.example.fantasyfootballapp.model.CreateTeamRequest
import com.example.fantasyfootballapp.model.LeaderboardEntry
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST

//App Endpoints
interface ApiService {
    @GET("api/leaderboard")
    suspend fun getLeaderboard(): List<LeaderboardEntry>

    @POST("api/leaderboard")
    suspend fun createTeam(@Body body: CreateTeamRequest): LeaderboardEntry
}