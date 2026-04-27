package com.example.fantasyfootballapp.data

import android.util.Log
import com.example.fantasyfootballapp.config.GameweekConfig
import com.example.fantasyfootballapp.data.mappers.toModel
import com.example.fantasyfootballapp.model.CreateTeamRequest
import com.example.fantasyfootballapp.model.GameweekStat
import com.example.fantasyfootballapp.model.Player
import com.example.fantasyfootballapp.model.RegisterWithTeamRequest
import com.example.fantasyfootballapp.model.UpdateUserTeamRequest
import com.example.fantasyfootballapp.model.UpdateTeamNameRequest
import com.example.fantasyfootballapp.model.User
import com.example.fantasyfootballapp.network.ApiService
import com.example.fantasyfootballapp.network.AuthResponse
import com.example.fantasyfootballapp.network.LoginRequest
import com.example.fantasyfootballapp.network.RegisterRequest
import com.example.fantasyfootballapp.util.RepoResult
import com.example.fantasyfootballapp.network.Fixture
import com.example.fantasyfootballapp.network.LeaderboardTeamDto
import com.example.fantasyfootballapp.network.UserGameweekScoreDto

class FantasyRepository(
    private val api: ApiService,
    private val tokenStore: TokenStore
) {
    //Suspend functions may pause in the background

    //Add new team to the backend
    suspend fun submitTeamToBackend(teamName: String, playerIds: List<Int>) {
        val request = CreateTeamRequest(
            teamName = teamName,
            playerIds = playerIds
        )
        val json = com.google.gson.Gson().toJson(request)
        Log.d("FantasyRepository", "CreateTeamRequest JSON: $json")

        val res = api.createTeam(request)

        if (!res.isSuccessful) {
            val err = res.errorBody()?.string()
            Log.e("FantasyRepository", "Save failed ${res.code()} body=$err")
            throw Exception("HTTP ${res.code()}: ${err ?: "Bad Request"}")
        }
    }

    //Get list of teams for the Leaderboard sorted by points accrued
    suspend fun getLeaderboard(): List<LeaderboardTeamDto> {
//    suspend fun getLeaderboard(): List<LeaderboardEntry> {
        return api.getLeaderboard()
            .sortedByDescending { it.points }
    }

    //Return all players in the collection
    suspend fun fetchPlayersFromBackend(): List<Player> {
        return api.getPlayers()
    }

    suspend fun getMyTeam(): LeaderboardTeamDto? {
        return try {
            api.getMyTeam()
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404) {
                null //first-time user, no team yet
            } else {
                throw e
            }
        }
    }

//    suspend fun updateMyTeamPlayers(playerIds: List<Int>) {
//        val res = api.updateMyTeamPlayers(UpdateTeamPlayersRequest(playerIds))
//        if (!res.isSuccessful) {
//            val err = res.errorBody()?.string()
//            Log.e("FantasyRepository", "UpdateTeam failed ${res.code()} body=$err")
//            throw Exception("HTTP ${res.code()}: ${err ?: "Bad Request"}")
//        }
//    }

    suspend fun updateMyTeamSlots(
        squadPlayerIds: List<Int>,
        slotPlayerIds: Map<String, Int?>,
        formationKey: String?
    ) {
        val res = api.patchMyTeam(
            UpdateUserTeamRequest(
                squadPlayerIds = squadPlayerIds,
                slotPlayerIds = slotPlayerIds,
                formationKey = formationKey
            )
        )

        if (!res.isSuccessful) {
            val err = res.errorBody()?.string()
            throw Exception("HTTP ${res.code()}: ${err ?: "Bad Request"}")
        }
    }

    suspend fun fetchGameweekStatsFromBackend(playerIds: List<Int>): List<GameweekStat> {
        val idsParam = playerIds.joinToString(",")

        return api.getGameweekStats(
            gw = GameweekConfig.CURRENT_GAMEWEEK,
            season = GameweekConfig.CURRENT_SEASON,
            playerIds = idsParam
        )
    }

    suspend fun fetchUpcomingFixtures(team: String, limit: Int = 2): List<Fixture> {
        return api.getUpcomingFixtures(
            team = team,
            season = GameweekConfig.CURRENT_SEASON,
            fromGw = GameweekConfig.CURRENT_GAMEWEEK + 1,
            limit = limit
        )
    }

    suspend fun fetchMyGameweekScore(
        gameweek: Int,
        season: String = "2025"
    ): UserGameweekScoreDto? {
        return try {
            api.getMyGameweekScore(gameweek, season)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchUserGameweekScore(
        gameweek: Int,
        userId: String,
        season: String = "2025"
    ): UserGameweekScoreDto? {
        return try {
            api.getUserGameweekScore(gameweek, userId, season)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getCurrentUser(): User {
        val me = api.getMe()          // MeResponse
        return me.toModel()
    }

    suspend fun updateCurrentUserTeamName(newTeamName: String) {
        val res = api.updateMyTeamName(UpdateTeamNameRequest(newTeamName))
        if (!res.isSuccessful) {
            throw Exception("Failed to update team name: HTTP ${res.code()}")
        }
    }


    suspend fun login(email: String, password: String): User {
        val res = api.login(LoginRequest(email, password))
        tokenStore.saveToken(res.token)
        return res.user.toModel()
    }

    fun logout() {
        tokenStore.clearToken()
    }

    fun getTokenOrNull(): String? = tokenStore.getToken()

    suspend fun register(fname: String, lname: String, email: String, password: String): AuthResponse {
        val body = RegisterRequest(
            fname = fname.trim(),
            lname = lname.trim(),
            email = email.trim(),
            password = password
        )

        val response = api.register(body)

        // store token for future authenticated calls
        tokenStore.saveToken(response.token)

        return response
    }

    suspend fun registerWithTeam(
        fname: String,
        lname: String,
        email: String,
        password: String,
        teamName: String,
        playerIds: List<Int>,
        slotPlayerIds: Map<String, Int?>,
        formationKey: String
    ): AuthResponse {

        val body = RegisterWithTeamRequest(
            fname = fname.trim(),
            lname = lname.trim(),
            email = email.trim(),
            password = password,
            teamName = teamName.trim(),
            playerIds = playerIds,
            slotPlayerIds = slotPlayerIds,
            formationKey = formationKey
        )

        val response = api.registerWithTeam(body)

        //store token for future authenticated calls
        tokenStore.saveToken(response.token)

        return response
    }

    suspend fun registerWithTeamSafe(
        fname: String,
        lname: String,
        email: String,
        password: String,
        teamName: String,
        playerIds: List<Int>,
        slotPlayerIds: Map<String, Int?>,
        formationKey: String
    ): RepoResult<AuthResponse> {
        return try {
            val res = registerWithTeam(
                fname = fname,
                lname = lname,
                email = email,
                password = password,
                teamName = teamName,
                playerIds = playerIds,
                slotPlayerIds = slotPlayerIds,
                formationKey = formationKey
            )
            RepoResult.Success(data = res)
        } catch (e: Exception) {
            RepoResult.Error(message = e.message ?: "Registration failed")
        }
    }

}