package com.example.fantasyfootballapp.data

import com.example.fantasyfootballapp.model.CreateTeamRequest
import com.example.fantasyfootballapp.model.LeaderboardEntry
import com.example.fantasyfootballapp.model.Player
import com.example.fantasyfootballapp.model.Team
import com.example.fantasyfootballapp.model.User
import com.example.fantasyfootballapp.network.ApiClient
import com.example.fantasyfootballapp.network.ApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object FantasyRepository {

    private const val BASE_URL = "http://10.0.2.2:8080/"

    private var currentTeam: Team = Team(mutableListOf())


    private val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    //Add new team to the backend
    suspend fun submitTeamToBackend(
        teamName: String,
        playerIds: List<Int>
    ) {
        val request = CreateTeamRequest(
            teamName = teamName,
            playerIds = playerIds
        )

        // This POSTs to /api/leaderboard and returns a LeaderboardEntry
        api.createTeam(request)
    }

    //Return all players in the collection
    suspend fun fetchPlayersFromBackend(): List<Player> {
        return api.getPlayers()
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

    private val users = mutableListOf(
        User(1, "demoUser", "Ryan Rovers"),
        User(2, "emma", "Emma XI"),
        User(3, "liam", "Liam FC")
    )

    // Start with some dummy teams
//    private val teams: MutableMap<String, Team> = mutableMapOf(
//        "u1" to Team(1, mutableListOf("p1", "p2", "p3")),
//        "u2" to Team(2, mutableListOf("p2", "p4")),
//        "u3" to Team(3, mutableListOf("p1", "p5", "p6"))
//    )

    private val teams: MutableList<Team> = mutableListOf(
        Team(mutableListOf(1, 2, 3, 4, 5)),
        Team(mutableListOf(6, 7, 8, 9, 10)),
        Team(mutableListOf(11, 12, 13, 14, 15))
    )

    private const val CURRENT_USER_ID = 1

    fun getCurrentUser(): User = users.first { it.id == CURRENT_USER_ID }

//    fun getAllPlayers(): List<Player> = players

    fun getTeamForUser(userId: Int = CURRENT_USER_ID): Team =
        teams[userId]

    fun updateTeam(playerIds: List<Int>) {
        currentTeam = Team(playerIds.toMutableList())
    }

    //Suspend functions may pause in the background
    suspend fun getLeaderboard(): List<LeaderboardEntry> {
        return ApiClient.service
            .getLeaderboard()
            .sortedByDescending { it.points }
    }

    fun updateCurrentUserTeamName(newTeamName: String) {
        val index = users.indexOfFirst { it.id == CURRENT_USER_ID }
        if (index != -1) {
            val currentUser = users[index]
            users[index] = currentUser.copy(teamName = newTeamName)
        }
    }

}