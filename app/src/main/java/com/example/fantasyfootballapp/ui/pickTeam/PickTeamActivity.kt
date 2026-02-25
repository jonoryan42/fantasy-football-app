package com.example.fantasyfootballapp.ui.pickTeam

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.data.TokenStore
import com.example.fantasyfootballapp.model.Player
import com.example.fantasyfootballapp.model.RosterSlotKey
import com.example.fantasyfootballapp.network.ApiClient
import com.example.fantasyfootballapp.ui.common.PlayerSlotView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PickTeamActivity : AppCompatActivity() {

    private var teamName: String = "My Team"

    private lateinit var slotViews: Map<RosterSlotKey, PlayerSlotView>

    private val selectedBySlot = mutableMapOf<RosterSlotKey, Int?>()
    private lateinit var starterAdapter: SlotAdapter
    private lateinit var benchAdapter: SlotAdapter

    private var allPlayers: List<Player> = emptyList()
    private lateinit var starterSlots: MutableList<PlayerSlot>
    private lateinit var benchSlots: MutableList<PlayerSlot>

    private lateinit var recyclerStarters: RecyclerView
    private lateinit var recyclerBench: RecyclerView

    private val repo by lazy {
        val tokenStore = TokenStore(applicationContext)
        FantasyRepository(ApiClient.service, tokenStore)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pick_team)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarPickTeam)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.pick_team)

        // Back arrow behaviour
        toolbar.setNavigationOnClickListener {
            finish()
        }

        recyclerStarters = findViewById(R.id.recyclerStarters)
        recyclerBench = findViewById(R.id.recyclerBench)

        recyclerStarters.layoutManager = LinearLayoutManager(this)
        recyclerBench.layoutManager = LinearLayoutManager(this)

        starterSlots = buildStarterSlots().toMutableList()
        benchSlots = buildBenchSlots().toMutableList()

        recyclerStarters.isEnabled = false
        recyclerBench.isEnabled = false

        updateHeader()

        lifecycleScope.launch {
            try {
                val players = withContext(Dispatchers.IO) {
                    repo.fetchPlayersFromBackend()
                }

                allPlayers = players

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

                recyclerStarters.isEnabled = true
                recyclerBench.isEnabled = true

                updateHeader()

            } catch (e: Exception) {
                Toast.makeText(
                    this@PickTeamActivity,
                    "Failed to load players: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun buildStarterSlots(): List<PlayerSlot> {
        val positions = listOf(
            "GK",
            "DEF", "DEF", "DEF", "DEF",
            "MID", "MID", "MID", "MID",
            "STR", "STR"
        )
        return positions.mapIndexed { i, pos ->
            PlayerSlot(index = i, position = pos)
        }
    }

    private fun buildBenchSlots(): List<PlayerSlot> {
        val positions = listOf("GK", "DEF", "MID", "STR")
        return positions.mapIndexed { i, pos ->
            PlayerSlot(index = 100 + i, position = pos)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateHeader() {
        val allSlots = starterSlots + benchSlots

        val spent = allSlots
            .mapNotNull { it.playerId }
            .mapNotNull { id -> allPlayers.firstOrNull { it.id == id } }
            .sumOf { it.price }


    }

    private fun showPickerForSlot(slot: PlayerSlot, onDone: () -> Unit) {
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
            .map { "${it.name} • ${it.club} • €%.1fm".format(it.price) }
            .toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Pick ${slot.position}")
            .setItems(items) { dialog, which ->
                slot.playerId = candidates[which].id
                updateHeader()
                starterAdapter.notifyDataSetChanged()
                benchAdapter.notifyDataSetChanged()
                dialog.dismiss()
                onDone()
            }
            .show()
    }
}
