package com.example.fantasyfootballapp.network

import com.example.fantasyfootballapp.model.CreateTeamRequest
import com.example.fantasyfootballapp.model.GameweekStat
import com.example.fantasyfootballapp.model.LeaderboardEntry
import com.example.fantasyfootballapp.model.Player
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

//App Endpoints
interface ApiService {
    @GET("api/leaderboard")
    suspend fun getLeaderboard(): List<LeaderboardEntry>

    @GET("api/players")
    suspend fun getPlayers(): List<Player>

    @GET("api/gameweeks/{gw}")
    suspend fun getGameweekStats(
        @Path("gw") gw: Int,
        @Query("season") season: String = "2025",
        @Query("playerIds") playerIds: String
    ): List<GameweekStat>

    //Get User
    @GET("/api/auth/me")
    suspend fun getMe(): MeResponse

    //POSTS
    @POST("/api/leaderboard") // or whatever your endpoint is
    suspend fun createTeam(@Body request: CreateTeamRequest): Response<LeaderboardTeamDto>


    @POST("/api/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("/api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

}