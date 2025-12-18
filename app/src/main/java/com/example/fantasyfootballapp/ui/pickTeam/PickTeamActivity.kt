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
    private val maxPlayers = 15

    // Keep the fetched list so we can build the confirm dialog later
    private var allPlayers = emptyList<com.example.fantasyfootballapp.model.Player>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pick_team)

        val txtTeamNameHeader = findViewById<TextView>(R.id.txtTeamNameHeader)
        val txtSelectedCount = findViewById<TextView>(R.id.txtSelectedCount)
        val recyclerPlayers = findViewById<RecyclerView>(R.id.recyclerPlayers)
        val btnSaveTeam = findViewById<Button>(R.id.btnSaveTeam)

        val currentTeam = FantasyRepository.getTeamForUser()
        val selectedIds = currentTeam.playerIds.toMutableSet()

        val currentUser = FantasyRepository.getCurrentUser()
        txtTeamNameHeader.text = currentUser.teamName

        @SuppressLint("SetTextI18n")
        fun updateSelectedCount(count: Int) {
            txtSelectedCount.text = "Selected: $count / $maxPlayers"
        }

        updateSelectedCount(selectedIds.size)

        recyclerPlayers.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            try {
                allPlayers = FantasyRepository.fetchPlayersFromBackend()

                playerAdapter = PlayerAdapter(
                    players = allPlayers,
                    selectedPlayerIds = selectedIds,
                    maxPlayers = maxPlayers
                ) { count ->
                    updateSelectedCount(count)
                }

                recyclerPlayers.adapter = playerAdapter
            } catch (e: Exception) {
                Toast.makeText(
                    this@PickTeamActivity,
                    "Failed to load players",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        btnSaveTeam.setOnClickListener {
            if (!::playerAdapter.isInitialized) {
                Toast.makeText(this, "Players are still loading…", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val finalSelection = playerAdapter.getSelectedPlayerIds().toList()

            if (finalSelection.size != maxPlayers) {
                Toast.makeText(this, "Please pick exactly $maxPlayers players.", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val pickedNames = allPlayers
                .filter { it.id in finalSelection }
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
                    btnSaveTeam.isEnabled = false

                    lifecycleScope.launch {
                        try {
                            FantasyRepository.updateTeam(playerIds = finalSelection)

                            FantasyRepository.submitTeamToBackend(
                                teamName = currentUser.teamName,
                                playerIds = finalSelection
                            )

                            startActivity(
                                Intent(
                                    this@PickTeamActivity,
                                    LeaderboardActivity::class.java
                                )
                            )
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
