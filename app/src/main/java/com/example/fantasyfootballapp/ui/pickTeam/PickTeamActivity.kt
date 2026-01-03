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
import com.example.fantasyfootballapp.model.Player
import com.example.fantasyfootballapp.ui.leaderboard.LeaderboardActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PickTeamActivity : AppCompatActivity() {

//    private lateinit var slotAdapter: SlotAdapter
    private lateinit var starterAdapter: SlotAdapter
    private lateinit var benchAdapter: SlotAdapter

    private val maxPlayers = 15

    private var allPlayers: List<Player> = emptyList()
//    private lateinit var slots: MutableList<PlayerSlot>

    private lateinit var starterSlots: MutableList<PlayerSlot>
    private lateinit var benchSlots: MutableList<PlayerSlot>

    private val totalBudget = 100.0

    private lateinit var txtSelectedCount: TextView
    private lateinit var txtBudgetRemaining: TextView
    private lateinit var btnSaveTeam: Button


    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pick_team)

        val txtTeamNameHeader = findViewById<TextView>(R.id.txtTeamNameHeader)
        txtSelectedCount = findViewById<TextView>(R.id.txtSelectedCount)
//        val recyclerSlots = findViewById<RecyclerView>(R.id.recyclerPlayers)
        val recyclerStarters = findViewById<RecyclerView>(R.id.recyclerStarters)
        val recyclerBench = findViewById<RecyclerView>(R.id.recyclerBench)

        btnSaveTeam = findViewById<Button>(R.id.btnSaveTeam)

        txtBudgetRemaining = findViewById<TextView>(R.id.txtBudgetRemaining)


//        recyclerSlots.layoutManager = LinearLayoutManager(this)
        recyclerStarters.layoutManager = LinearLayoutManager(this)
        recyclerBench.layoutManager = LinearLayoutManager(this)


        // Build the 15-slot distribution
//        slots = buildSlots().toMutableList()
        //Starting 11
        starterSlots = buildStarterSlots().toMutableList()
        //Bench (4 Players)
        benchSlots = buildBenchSlots().toMutableList()


        // Disable interaction until data loads
//        recyclerSlots.isEnabled = false
        recyclerStarters.isEnabled = false
        recyclerBench.isEnabled = false
        btnSaveTeam.isEnabled = false

        updateHeader()

        // We'll set this after loading on IO
        var currentUserTeamName: String = ""

        lifecycleScope.launch {
            try {
                // ✅ do repo/network/disk work on IO
                val (user, team, players) = withContext(Dispatchers.IO) {
                    val u = FantasyRepository.getCurrentUser()
                    val t = FantasyRepository.getTeamForUser()
                    val p = FantasyRepository.fetchPlayersFromBackend()
                    Triple(u, t, p)
                }

                val currentUser = user
                val currentTeam = team
                allPlayers = players

                currentUserTeamName = currentUser.teamName
                txtTeamNameHeader.text = currentUserTeamName

                // Prefill selection
                // prefillSlotsFromTeam(currentTeam.playerIds)

                // Create adapters
                starterAdapter = SlotAdapter(
                    slots = starterSlots,
                    getPlayerById = { id -> allPlayers.firstOrNull { it.id == id } },
                    onSlotClicked = { slot ->
                        showPickerForSlot(slot) {
                            updateHeader()
                            starterAdapter.notifyDataSetChanged()
                            benchAdapter.notifyDataSetChanged()
                        }
                    },
                    onSlotCleared = { slot ->
                        slot.playerId = null
                        updateHeader()
                        starterAdapter.notifyDataSetChanged()
                        benchAdapter.notifyDataSetChanged()
                    }
                )

                benchAdapter = SlotAdapter(
                    slots = benchSlots,
                    getPlayerById = { id -> allPlayers.firstOrNull { it.id == id } },
                    onSlotClicked = { slot ->
                        showPickerForSlot(slot) {
                            updateHeader()
                            starterAdapter.notifyDataSetChanged()
                            benchAdapter.notifyDataSetChanged()
                        }
                    },
                    onSlotCleared = { slot ->
                        slot.playerId = null
                        updateHeader()
                        starterAdapter.notifyDataSetChanged()
                        benchAdapter.notifyDataSetChanged()
                    }
                )

                recyclerStarters.adapter = starterAdapter
                recyclerBench.adapter = benchAdapter

                // ✅ re-enable now that everything is ready
                recyclerStarters.isEnabled = true
                recyclerBench.isEnabled = true

                updateHeader()

            } catch (e: Exception) {
                Toast.makeText(
                    this@PickTeamActivity,
                    "Failed to load: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } // ✅ closes launch

        btnSaveTeam.setOnClickListener {
            val finalIds = (starterSlots + benchSlots).mapNotNull { it.playerId }

            if (finalIds.size != maxPlayers) {
                Toast.makeText(this, "Please fill all 15 slots.",
                    Toast.LENGTH_SHORT).show()
                //Don't allow confirm
                return@setOnClickListener
            }

            // ✅ Budget check (totalBudget should be 100.0)
            val spent = finalIds
                .mapNotNull { id -> allPlayers.firstOrNull { it.id == id } }
                .sumOf { it.price }

            if (spent > totalBudget) {
                Toast.makeText(
                    this,
                    "Over budget by €%.1fm. Remove/replace a player.".format(spent - totalBudget),
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val pickedNames = finalIds
                //Check for players
                .mapNotNull { id -> allPlayers.firstOrNull { it.id == id } }
                .joinToString("\n") { "• ${it.name} (${it.position}) — €%.1fm".format(it.price) }

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_team_title))
                .setMessage(
                    getString(
                        R.string.confirm_team_message,
                        currentUserTeamName,
                        finalIds.size,
                        pickedNames
                    )
                )
                .setNegativeButton("Back", null)
                .setPositiveButton("Confirm") { _, _ ->
                    btnSaveTeam.isEnabled = false

                    lifecycleScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                FantasyRepository.updateTeam(playerIds = finalIds)
                                FantasyRepository.submitTeamToBackend(
                                    teamName = currentUserTeamName,
                                    playerIds = finalIds
                                )
                            }

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

    private fun buildStarterSlots(): List<PlayerSlot> {
        val positions = listOf(
            "GK",
            "DEF","DEF","DEF","DEF",
            "MID","MID","MID","MID",
            "STR","STR"
        )
        return positions.mapIndexed { i, pos -> PlayerSlot(index = i, position = pos) }
    }

    private fun buildBenchSlots(): List<PlayerSlot> {
        val positions = listOf("GK", "DEF", "MID", "STR")
        return positions.mapIndexed { i, pos -> PlayerSlot(index = 100 + i, position = pos) }
    }

//    private fun prefillSlotsFromTeam(existingIds: List<Int>) {
//        // best-effort: fill slots in order by matching position
//        // (avoids putting a DEF into a GK slot)
//        // if you already have a saved team that matches distribution, it will slot nicely.
//        // any "extra"/mismatched players are ignored.
//        // NOTE: we can't match position here until players are loaded, so we just place in order for now.
//        // If you want perfect prefill by position, we can do it after fetchPlayersFromBackend().
//        existingIds.take(maxPlayers).forEachIndexed { index, id ->
//            if (index < slots.size) slots[index].playerId = id
//        }
//    }

    @SuppressLint("SetTextI18n")
    private fun updateHeader() {
        val allSlots = starterSlots + benchSlots
        val filled = allSlots.count { it.playerId != null }

        val spent = allSlots
            .mapNotNull { it.playerId }
            .mapNotNull { id -> allPlayers.firstOrNull { it.id == id } }
            .sumOf { it.price }

        val remaining = totalBudget - spent

        txtSelectedCount.text = getString(R.string.filled_0_15, filled)
        txtBudgetRemaining.text = "Budget: €%.1fm".format(remaining)

        val overBudget = remaining < 0.0

        //Validation for confirming the team
        btnSaveTeam.isEnabled = (filled == maxPlayers) && !overBudget
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showPickerForSlot(slot: PlayerSlot, onDone: () -> Unit) {
        // combine both lists so "already picked" works across starters+bench
        val allSlots = starterSlots + benchSlots
        val alreadyPicked = allSlots.mapNotNull { it.playerId }.toSet()

        val candidates = allPlayers
            .filter { it.position == slot.position }
            .filter { it.id !in alreadyPicked || it.id == slot.playerId }

        if (candidates.isEmpty()) {
            Toast.makeText(this, "No available ${slot.position} players", Toast.LENGTH_SHORT).show()
            return
        }

        val items = candidates
            .map { "${it.name} • ${it.club} • €%.1fm • ${it.points} pts".format(it.price) }
            .toTypedArray()
        //what index the current selected player in the slot is
        val currentIndex = slot.playerId?.let { id -> candidates.indexOfFirst { it.id == id } } ?: -1
        val previousId = slot.playerId

        AlertDialog.Builder(this)
            .setTitle("Pick ${slot.position}")
            .setSingleChoiceItems(items, currentIndex) { dialog, which ->
                val chosen = candidates[which]
                slot.playerId = chosen.id

                //how much is spent after choosing player
                val allSlotsNow = starterSlots + benchSlots
                val spentNow = allSlotsNow
                    .mapNotNull { it.playerId }
                    .mapNotNull { id -> allPlayers.firstOrNull { it.id == id } }
                    .sumOf { it.price }

                if (spentNow > totalBudget) {
                    slot.playerId = previousId
                    Toast.makeText(
                        this,
                        "Over budget. Pick a cheaper ${slot.position}.",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                    // still update UI to reflect revert
                    updateHeader()
                    starterAdapter.notifyDataSetChanged()
                    benchAdapter.notifyDataSetChanged()
                    return@setSingleChoiceItems
                }

                dialog.dismiss()
                onDone()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


}
