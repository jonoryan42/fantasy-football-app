package data

import model.LeaderboardEntry
import model.Player
import model.Team
import model.User

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
        Player("p8", "Liam Fitzgerald", "GK", "Villa FC", 4, 29)
        // add more later if you want
    )

    private val users = listOf(
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
}
