package com.example.fantasyfootballapp.ui.pickTeam

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.ui.leaderboard.LeaderboardActivity
import kotlinx.coroutines.launch

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

            // Enforce 11 (optional, but usually what you want)
            if (finalSelection.size != maxPlayers) {
                Toast.makeText(this, "Please pick exactly $maxPlayers players.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Build a simple summary for the confirmation dialog
            val pickedNames = allPlayers
                .filter { finalSelection.contains(it.id) }
                .joinToString(separator = "\n") { "• ${it.name} (${it.position})" }

            AlertDialog.Builder(this)
                .setTitle("Confirm Team")
                .setMessage(
                    "Team: ${currentUser.teamName}\n\n" +
                            "Players (${finalSelection.size}):\n$pickedNames\n\n" +
                            "Save this team and continue?"
                )
                .setNegativeButton("Back", null)
                .setPositiveButton("Confirm") { _, _ ->
                    // Disable button to avoid double taps
                    btnSaveTeam.isEnabled = false

                    lifecycleScope.launch {
                        try {
                            // 1) Update locally (so UI/data model stays consistent)
                            FantasyRepository.updateTeam(playerIds = finalSelection)

                            // 2) POST to MongoDB via your backend API
                            FantasyRepository.submitTeamToBackend(
                                teamName = currentUser.teamName,
                                playerIds = finalSelection
                            )

                            // 3) Go to leaderboard
                            startActivity(Intent(this@PickTeamActivity, LeaderboardActivity::class.java))
                            finish()
                        } catch (e: Exception) {
                            btnSaveTeam.isEnabled = true
                            Toast.makeText(
                                this@PickTeamActivity,
                                "Failed to save team: ${e.message ?: "Unknown error"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                .show()
        }
    }
}
