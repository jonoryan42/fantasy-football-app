package com.example.fantasyfootballapp.ui.leaderboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.network.ApiClient
import com.example.fantasyfootballapp.ui.viewTeam.ViewTeamActivity
import kotlinx.coroutines.launch

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var leaderboardAdapter: LeaderboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        val recyclerLeaderboard = findViewById<RecyclerView>(R.id.recyclerLeaderboard)
        recyclerLeaderboard.layoutManager = LinearLayoutManager(this)

        leaderboardAdapter = LeaderboardAdapter(mutableListOf()) { team ->
            val intent = Intent(this, ViewTeamActivity::class.java).apply {
                putExtra(ViewTeamActivity.EXTRA_TEAM_NAME, team.teamName)
                putExtra(ViewTeamActivity.EXTRA_TEAM_POINTS, team.points)
                putIntegerArrayListExtra(
                    ViewTeamActivity.EXTRA_PLAYER_IDS,
                    ArrayList(team.playerIds)
                )
            }
            startActivity(intent)
        }

        recyclerLeaderboard.adapter = leaderboardAdapter

        lifecycleScope.launch {
            try {
                val leaderboard = FantasyRepository.getLeaderboard()
                leaderboardAdapter.setData(leaderboard)
            } catch (e: Exception) {
                Log.e("LEADERBOARD", "Failed to load leaderboard", e)
            }
        }
    }
}
