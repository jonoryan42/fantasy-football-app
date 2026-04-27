package com.example.fantasyfootballapp.network

import com.example.fantasyfootballapp.config.GameweekConfig
import com.example.fantasyfootballapp.model.CreateTeamRequest
import com.example.fantasyfootballapp.model.GameweekStat
import com.example.fantasyfootballapp.model.LeaderboardEntry
import com.example.fantasyfootballapp.model.Player
import com.example.fantasyfootballapp.model.RegisterWithTeamRequest
import com.example.fantasyfootballapp.model.UpdateLeaderboardTeamRequest
import com.example.fantasyfootballapp.model.UpdateTeamNameRequest
import com.example.fantasyfootballapp.model.UpdateTeamPlayersRequest
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

//App Endpoints
interface ApiService {
    @GET("api/leaderboard")
//    suspend fun getLeaderboard(): List<LeaderboardEntry>
    suspend fun getLeaderboard(): List<LeaderboardTeamDto>

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

    @GET("/api/leaderboard/me")
    suspend fun getMyTeam(): LeaderboardTeamDto

    @GET("api/fixtures/upcoming")
    suspend fun getUpcomingFixtures(
        @Query("team") team: String,
        @Query("season") season: String = GameweekConfig.CURRENT_SEASON,
        @Query("fromGw") fromGw: Int = GameweekConfig.CURRENT_GAMEWEEK + 1,
        @Query("limit") limit: Int = 2
    ): List<Fixture>

    @GET("/api/gameweeks/{gameweek}/me")
    suspend fun getMyGameweekScore(
        @Path("gameweek") gameweek: Int,
        @Query("season") season: String = "2025"
    ): UserGameweekScoreDto?

    @GET("/api/gameweeks/{gameweek}/user/{userId}")
    suspend fun getUserGameweekScore(
        @Path("gameweek") gameweek: Int,
        @Path("userId") userId: String,
        @Query("season") season: String = "2025"
    ): UserGameweekScoreDto?

    //POSTS
    @POST("/api/leaderboard") // or whatever your endpoint is
    suspend fun createTeam(@Body request: CreateTeamRequest): Response<LeaderboardTeamDto>


    @POST("/api/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("/api/auth/register-with-team")
    suspend fun registerWithTeam(@Body body: RegisterWithTeamRequest): AuthResponse

    @POST("/api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @PATCH("/api/users/me/teamname")
    suspend fun updateMyTeamName(@Body req: UpdateTeamNameRequest): retrofit2.Response<Unit>

    @PATCH("/api/leaderboard/me")
    suspend fun patchMyTeam(
        @Body body: UpdateLeaderboardTeamRequest
    ): Response<LeaderboardTeamDto>
}