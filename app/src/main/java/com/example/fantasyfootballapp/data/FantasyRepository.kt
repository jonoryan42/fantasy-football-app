package com.example.fantasyfootballapp.data

import android.util.Log
import com.example.fantasyfootballapp.data.mappers.toModel
import com.example.fantasyfootballapp.model.CreateTeamRequest
import com.example.fantasyfootballapp.model.GameweekStat
import com.example.fantasyfootballapp.model.LeaderboardEntry
import com.example.fantasyfootballapp.model.Player
import com.example.fantasyfootballapp.model.UpdateTeamNameRequest
import com.example.fantasyfootballapp.model.User
//import com.example.fantasyfootballapp.model.Team
//import com.example.fantasyfootballapp.model.User
import com.example.fantasyfootballapp.network.ApiClient
import com.example.fantasyfootballapp.network.ApiService
import com.example.fantasyfootballapp.network.AuthResponse
import com.example.fantasyfootballapp.network.LeaderboardTeamDto
import com.example.fantasyfootballapp.network.LoginRequest
import com.example.fantasyfootballapp.network.RegisterRequest
import com.example.fantasyfootballapp.util.RepoResult
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

//    suspend fun getTeamForUser(): LeaderboardTeamDto? { ... } // or Team model

    //    fun updateTeam(playerIds: List<Int>) {
//        currentTeam = Team(playerIds.toMutableList())
//    }


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

    // Fake player list for now
//    private val players = listOf(
//        Player(1, "John Murphy", "GK", "Tramore AFC", 5, 32),
//        Player(2, "Alan Kelly", "DEF", "Villa FC", 6, 27),
//        Player(3, "Mark Doyle", "MID", "Bohemians", 7, 41),
//        Player(4, "Sean Byrne", "STR", "Waterford FC", 8, 50),
//        Player(5, "David Walsh", "DEF", "Tramore AFC", 5, 19),
//        Player(6, "Eoin Kelly", "MID", "Villa FC", 6, 23),
//        Player(7, "Kevin O'Brien", "STR", "Tramore AFC", 9, 37),
//        Player(8, "Liam Fitzgerald", "GK", "Villa FC", 4, 29),
//        Player(9, "Shane Power", "DEF", "Waterford FC", 5, 22),
//        Player(10, "Cian Browne", "MID", "Tramore AFC", 6, 31),
//        Player(11, "Jamie Ahern", "STR", "Villa FC", 7, 44),
//        Player(12, "Paul Furlong", "GK", "Bohemians", 4, 18),
//        Player(13, "Harry Dunne", "DEF", "Athlone Town", 5, 25),
//        Player(14, "Ronan McGrath", "MID", "Shamrock Rovers", 7, 39),
//        Player(15, "Darragh Foley", "STR", "Dundalk FC", 8, 48),
//        Player(16, "Brian Stack", "DEF", "Cork City", 6, 28),
//        Player(17, "Adam Keane", "MID", "Shelbourne", 6, 33),
//        Player(18, "Luke Byrne", "STR", "Galway United", 7, 41),
//        Player(19, "Daniel Kennedy", "DEF", "Wexford FC", 5, 20),
//        Player(20, "Owen Hayes", "MID", "Finn Harps", 6, 29)
//
//    )

//    private val users = mutableListOf(
//        User(1, "demoUser", "Ryan Rovers"),
//        User(2, "emma", "Emma XI"),
//        User(3, "liam", "Liam FC")
//    )

    // Start with some dummy teams
//    private val teams: MutableMap<String, Team> = mutableMapOf(
//        "u1" to Team(1, mutableListOf("p1", "p2", "p3")),
//        "u2" to Team(2, mutableListOf("p2", "p4")),
//        "u3" to Team(3, mutableListOf("p1", "p5", "p6"))
//    )

//    private val teams: MutableList<Team> = mutableListOf(
//        Team(mutableListOf(1, 2, 3, 4, 5)),
//        Team(mutableListOf(6, 7, 8, 9, 10)),
//        Team(mutableListOf(11, 12, 13, 14, 15))
//    )

//    private const val CURRENT_USER_ID = 1

//    fun getCurrentUser(): User = users.first { it.id == CURRENT_USER_ID }

//    fun getAllPlayers(): List<Player> = players

}