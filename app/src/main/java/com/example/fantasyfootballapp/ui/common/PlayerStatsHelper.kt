package com.example.fantasyfootballapp.ui.common

import com.example.fantasyfootballapp.config.GameweekConfig
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.model.GameweekStat
import com.example.fantasyfootballapp.model.Player
import com.example.fantasyfootballapp.network.Fixture

object PlayerStatsHelper {

    suspend fun loadCurrentGameweekStats(
        repo: FantasyRepository,
        players: Collection<Player>
    ): Map<Int, GameweekStat> {
        val ids = players.map { it.id }.distinct()
        if (ids.isEmpty()) return emptyMap()

        return repo.fetchGameweekStatsFromBackend(ids)
            .associateBy { it.playerId }
    }

    suspend fun loadUpcomingFixturesForTeams(
        repo: FantasyRepository,
        players: Collection<Player>
    ): Map<String, List<Fixture>> {
        val teams = players.map { it.club }.distinct()

        return teams.associateWith { teamName ->
            repo.fetchUpcomingFixtures(teamName)
        }
    }

    fun buildMessage(
        player: Player,
        gwStat: GameweekStat?,
        allStats: List<GameweekStat>,
        upcoming: List<Fixture>
    ): String {
        val gwPoints = gwStat?.points ?: 0
        val gwMinutes = gwStat?.minutes ?: 0
        val gwGoals = gwStat?.goals ?: 0
        val gwAssists = gwStat?.assists ?: 0
        val gwCleanSheet = gwStat?.cleansheet ?: false
        val gwYellows = gwStat?.yellows ?: 0
        val gwReds = gwStat?.reds ?: 0
        val totalPoints = allStats.sumOf { it.points }

        return buildString {
            appendLine("${player.club}")
            appendLine("${player.position}")
            appendLine("€${player.price}m")
            appendLine("Total Points: $totalPoints")
            appendLine()

            appendLine("Gameweek ${GameweekConfig.CURRENT_GAMEWEEK}")
            appendLine("Points: $gwPoints")
            appendLine("Minutes: $gwMinutes")
            appendLine("Goals: $gwGoals")
            appendLine("Assists: $gwAssists")
            appendLine("Clean sheet: ${if (gwCleanSheet) "Yes" else "No"}")
            appendLine("Yellow Cards: $gwYellows")
            appendLine("Red Cards: $gwReds")
            appendLine()

            appendLine("Upcoming Fixtures")
            if (upcoming.isEmpty()) {
                appendLine("No upcoming fixtures")
            } else {
                upcoming.forEach { fixture ->
                    val isHome = fixture.homeTeam == player.club
                    val opponent = if (isHome) fixture.awayTeam else fixture.homeTeam
                    val prefix = if (isHome) "vs" else "@"
                    val tag = if (isHome) "(H)" else "(A)"
                    appendLine("GW${fixture.gameweek}: $prefix $opponent $tag")
                }
            }

            appendLine()

        }
    }
}