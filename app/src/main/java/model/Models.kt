package model

data class Player(
    val id: String,
    val name: String,
    val position: String,      // "GK", "DEF", "MID", "STR"
    val teamName: String,
    val price: Int,
    val points: Int            // current total points
)

data class User(
    val id: String,
    val username: String,
    val teamName: String
)

data class Team(
    val userId: String,
    val playerIds: MutableList<String>
)

data class LeaderboardEntry(
    val user: User,
    val totalPoints: Int
)
