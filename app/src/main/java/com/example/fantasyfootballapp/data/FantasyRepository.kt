package com.example.fantasyfootballapp.data

import com.example.fantasyfootballapp.model.LeaderboardEntry
import com.example.fantasyfootballapp.model.Player
import com.example.fantasyfootballapp.model.Team
import com.example.fantasyfootballapp.model.User

object FantasyRepository {

    // Fake player list for now
    private val players = listOf(
        Player("p1", "John Murphy", "GK", "Tramore AFC", 5, 32),
        Player("p2", "Alan Kelly", "DEF", "Villa FC", 6, 27),
        Player("p3", "Mark Doyle", "MID", "Bohemians", 7, 41),
        Player("p4", "Sean Byrne", "STR", "Waterford FC", 8, 50),
        Player("p5", "David Walsh", "DEF", "Tramore AFC", 5, 19),
        Player("p6", "Eoin Kelly", "MID", "Villa FC", 6, 23),
        Player("p7", "Kevin O'Brien", "STR", "Tramore AFC", 9, 37),
        Player("p8", "Liam Fitzgerald", "GK", "Villa FC", 4, 29),
        Player("p9", "Shane Power", "DEF", "Waterford FC", 5, 22),
        Player("p10", "Cian Browne", "MID", "Tramore AFC", 6, 31),
        Player("p11", "Jamie Ahern", "STR", "Villa FC", 7, 44),
        Player("p12", "Paul Furlong", "GK", "Bohemians", 4, 18),
        Player("p13", "Harry Dunne", "DEF", "Athlone Town", 5, 25),
        Player("p14", "Ronan McGrath", "MID", "Shamrock Rovers", 7, 39),
        Player("p15", "Darragh Foley", "STR", "Dundalk FC", 8, 48),
        Player("p16", "Brian Stack", "DEF", "Cork City", 6, 28),
        Player("p17", "Adam Keane", "MID", "Shelbourne", 6, 33),
        Player("p18", "Luke Byrne", "STR", "Galway United", 7, 41),
        Player("p19", "Daniel Kennedy", "DEF", "Wexford FC", 5, 20),
        Player("p20", "Owen Hayes", "MID", "Finn Harps", 6, 29)

        // add more later if you want
    )

    private val users = mutableListOf(
        User("u1", "demoUser", "Ryan Rovers"),
        User("u2", "emma", "Emma XI"),
        User("u3", "liam", "Liam FC")
    )

    // Start with some dummy teams
    private val teams: MutableMap<String, Team> = mutableMapOf(
        "u1" to Team("u1", mutableListOf("p1", "p2", "p3")),
        "u2" to Team("u2", mutableListOf("p2", "p4")),
        "u3" to Team("u3", mutableListOf("p1", "p5", "p6"))
    )

    private const val CURRENT_USER_ID = "u1"

    fun getCurrentUser(): User = users.first { it.id == CURRENT_USER_ID }

    fun getAllPlayers(): List<Player> = players

    fun getTeamForUser(userId: String = CURRENT_USER_ID): Team =
        teams[userId] ?: Team(userId, mutableListOf())

    fun updateTeam(userId: String = CURRENT_USER_ID, playerIds: List<String>) {
        teams[userId] = Team(userId, playerIds.toMutableList())
    }

    fun getLeaderboard(): List<LeaderboardEntry> {
        return users.map { user ->
            val team = getTeamForUser(user.id)
            val totalPoints = team.playerIds
                .mapNotNull { id -> players.find { it.id == id } }
                .sumOf { it.points }
            LeaderboardEntry(user, totalPoints)
        }.sortedByDescending { it.totalPoints }
    }

    fun updateCurrentUserTeamName(newTeamName: String) {
        val index = users.indexOfFirst { it.id == CURRENT_USER_ID }
        if (index != -1) {
            val currentUser = users[index]
            users[index] = currentUser.copy(teamName = newTeamName)
        }
    }

}