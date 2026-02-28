package com.example.fantasyfootballapp.data

import android.util.Log
import com.example.fantasyfootballapp.data.mappers.toModel
import com.example.fantasyfootballapp.model.CreateTeamRequest
import com.example.fantasyfootballapp.model.GameweekStat
import com.example.fantasyfootballapp.model.LeaderboardEntry
import com.example.fantasyfootballapp.model.Player
import com.example.fantasyfootballapp.model.RegisterWithTeamRequest
import com.example.fantasyfootballapp.model.UpdateLeaderboardTeamRequest
import com.example.fantasyfootballapp.model.UpdateTeamNameRequest
import com.example.fantasyfootballapp.model.User
import com.example.fantasyfootballapp.network.ApiService
import com.example.fantasyfootballapp.network.AuthResponse
import com.example.fantasyfootballapp.network.LoginRequest
import com.example.fantasyfootballapp.network.RegisterRequest
import com.example.fantasyfootballapp.util.RepoResult
import com.example.fantasyfootballapp.model.UpdateTeamPlayersRequest
import com.example.fantasyfootballapp.network.LeaderboardTeamDto
import okhttp3.internal.format

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
    suspend fun getLeaderboard(): List<LeaderboardEntry> {
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
        slotPlayerIds: Map<String, Int?>,
        formationKey: String?) {
        val res = api.patchMyTeam(UpdateLeaderboardTeamRequest(
            slotPlayerIds = slotPlayerIds,
            formationKey = formationKey
        ))
        if (!res.isSuccessful) {
            val err = res.errorBody()?.string()
            throw Exception("HTTP ${res.code()}: ${err ?: "Bad Request"}")
        }
    }

    suspend fun fetchGameweekStatsFromBackend(gw: Int, playerIds: List<Int>): List<GameweekStat> {
        val idsParam = playerIds.joinToString(",")  // "1,2,3,4"
        return api.getGameweekStats(
            gw = gw,
            season = "2025",
            playerIds = idsParam
        )
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
        tokenStore.saveToken(res.token)      // ✅ save it
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

    suspend fun registerSafe(
        fname: String,
        lname: String,
        email: String,
        password: String
    ): RepoResult<AuthResponse> {
        return try {
            val res = register(fname, lname, email, password)
            RepoResult.Success(res)
        } catch (e: Exception) {
            RepoResult.Error(e.message ?: "Registration failed")
        }
    }

    suspend fun registerWithTeam(
        fname: String,
        lname: String,
        email: String,
        password: String,
        teamName: String,
        playerIds: List<Int>
    ): AuthResponse {

        val body = RegisterWithTeamRequest(
            fname = fname.trim(),
            lname = lname.trim(),
            email = email.trim(),
            password = password,
            teamName = teamName.trim(),
            playerIds = playerIds
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
        playerIds: List<Int>
    ): RepoResult<AuthResponse> {
        return try {
            val res = registerWithTeam(fname, lname, email, password, teamName, playerIds)
            RepoResult.Success(data = res)
        } catch (e: Exception) {
            RepoResult.Error(message = e.message ?: "Registration failed")
        }
    }

}