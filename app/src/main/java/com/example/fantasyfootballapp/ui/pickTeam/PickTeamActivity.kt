package com.example.fantasyfootballapp.ui.pickTeam

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.ui.leaderboard.LeaderboardActivity

class PickTeamActivity : AppCompatActivity() {

    private lateinit var playerAdapter: PlayerAdapter
    private val maxPlayers = 11

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pick_team)

        // Team Name
        val txtTeamNameHeader = findViewById<TextView>(R.id.txtTeamNameHeader)

        // Get views from layout
        val txtSelectedCount = findViewById<TextView>(R.id.txtSelectedCount)
        val recyclerPlayers = findViewById<RecyclerView>(R.id.recyclerPlayers)
        val btnSaveTeam = findViewById<Button>(R.id.btnSaveTeam)

        // Load data from repository
        val allPlayers = FantasyRepository.getAllPlayers()
        val currentTeam = FantasyRepository.getTeamForUser()
        val selectedIds = currentTeam.playerIds.toMutableSet()

        val currentUser = FantasyRepository.getCurrentUser()
        txtTeamNameHeader.text = currentUser.teamName

        // Helper to update the "Selected: X / 11" text
        @SuppressLint("SetTextI18n")
        fun Int.updateSelectedCount() {
            txtSelectedCount.text = "Selected: $this / $maxPlayers"
        }

        // Initial count
        selectedIds.size.updateSelectedCount()

        // Create adapter
        playerAdapter = PlayerAdapter(
            players = allPlayers,
            selectedPlayerIds = selectedIds,
            maxPlayers = maxPlayers
        ) { count ->
            // This lambda is called whenever selection changes
            count.updateSelectedCount()
        }

        // RecyclerView setup
        recyclerPlayers.layoutManager = LinearLayoutManager(this)
        recyclerPlayers.adapter = playerAdapter

        // Save button logic
        btnSaveTeam.setOnClickListener {
            val finalSelection = playerAdapter.getSelectedPlayerIds().toList()
            FantasyRepository.updateTeam(playerIds = finalSelection)

            val intent = Intent(this, LeaderboardActivity::class.java)
            startActivity(intent)
        }
    }
}
