package com.example.fantasyfootballapp.ui.viewTeam

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.data.TokenStore
import com.example.fantasyfootballapp.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//Old code, used in early stages (List recycler view of player slots)
class ViewTeamListActivity : AppCompatActivity() {

    private val repo by lazy {
        val tokenStore = TokenStore(applicationContext) // adjust import/package if needed
        FantasyRepository(ApiClient.service, tokenStore)
    }

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_team_list)

        val txtTeamName = findViewById<TextView>(R.id.txtTeamNameHeader)
        val txtTeamPoints = findViewById<TextView>(R.id.txtTeamPoints)
        val recyclerStarters = findViewById<RecyclerView>(R.id.recyclerStarters)
        val recyclerBench = findViewById<RecyclerView>(R.id.recyclerBench)

        recyclerStarters.layoutManager = LinearLayoutManager(this)
        recyclerBench.layoutManager = LinearLayoutManager(this)

        val teamName = intent.getStringExtra(EXTRA_TEAM_NAME) ?: "Team"
        val playerIds = intent.getIntegerArrayListExtra(EXTRA_PLAYER_IDS)?.toList() ?: emptyList()

        txtTeamName.text = teamName

        val starterIds = playerIds.take(11)
        val benchIds = playerIds.drop(11).take(4)

        //Team total points
        val points = intent.getIntExtra(ViewTeamListActivity.EXTRA_TEAM_POINTS, 0)
        txtTeamPoints.text = "$points pts"

        lifecycleScope.launch {
            try {
                val allPlayers = withContext(Dispatchers.IO) {
                    repo.fetchPlayersFromBackend()
                }

                val byId = allPlayers.associateBy { it.id }

                val starters = starterIds.mapNotNull { byId[it] }
                val bench = benchIds.mapNotNull { byId[it] }

                val teamIds = (starterIds + benchIds)
                val stats = withContext(Dispatchers.IO) {
                    repo.fetchGameweekStatsFromBackend(playerIds = teamIds)
                }

                val pointsMap = stats.associate { it.playerId to it.points }

//                starters.forEach { p -> p.gwPoints = pointsMap[p.id] ?: 0 }
//                bench.forEach { p -> p.gwPoints = pointsMap[p.id] ?: 0 }

                recyclerStarters.adapter = ViewTeamListPlayerAdapter(starters)
                recyclerBench.adapter = ViewTeamListPlayerAdapter(bench)

            } catch (e: Exception) {
                Toast.makeText(this@ViewTeamListActivity,
                    "Failed to load team", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val EXTRA_TEAM_NAME = "extra_team_name"
        const val EXTRA_PLAYER_IDS = "extra_player_ids"
        const val EXTRA_TEAM_POINTS = "extra_team_points"
    }
}
