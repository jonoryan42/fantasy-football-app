package com.example.fantasyfootballapp.ui.transfers

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.data.TokenStore
import com.example.fantasyfootballapp.model.Player
import com.example.fantasyfootballapp.model.RosterSlotKey
import com.example.fantasyfootballapp.network.ApiClient
import com.example.fantasyfootballapp.network.LeaderboardTeamDto
import com.example.fantasyfootballapp.ui.common.PlayerSlotView
import com.example.fantasyfootballapp.ui.common.bindPlayerSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransfersActivity : AppCompatActivity() {

    private lateinit var slotViews: Map<RosterSlotKey, PlayerSlotView>
    private val selectedBySlot = mutableMapOf<RosterSlotKey, Int?>()

    private var allPlayers: List<Player> = emptyList()
    private val totalBudget = 100.0

    private lateinit var chipBudget: TextView
    private lateinit var chipPoints: TextView
    private lateinit var chipTransfers: TextView
    private lateinit var chipDeadline: TextView

    private val initialBySlot = mutableMapOf<RosterSlotKey, Int?>()
    private var freeTransfers = 15 // later this comes from backend per gameweek

    private var hasSavedTeam: Boolean = false

    private val SLOT_ORDER = RosterSlotKey.entries.toList()

    // snapshot of what they started with (from backend)
//    private val originalBySlot: MutableMap<String, String?> = mutableMapOf()

    // current UI selection
//    private val currentBySlot: MutableMap<String, String?> = mutableMapOf()


    private val repo by lazy {
        val tokenStore = TokenStore(applicationContext)
        FantasyRepository(ApiClient.service, tokenStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfers)

        //Top Toolbar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbarPickTeam)
        setSupportActionBar(toolbar)

        //remove ActionBar title text entirely
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.title = ""

//Back arrow behaviour
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        chipBudget = findViewById(R.id.txtBudgetRemaining)
        chipPoints = findViewById(R.id.txtTotalPoints)
        chipTransfers = findViewById(R.id.txtFreeTransfers)
        chipDeadline = findViewById(R.id.txtDeadline)


        slotViews = mapOf(
            RosterSlotKey.GK1 to bindPlayerSlot(R.id.slotGK1),
            RosterSlotKey.GK2 to bindPlayerSlot(R.id.slotGK2),

            RosterSlotKey.DEF1 to bindPlayerSlot(R.id.slotLB),
            RosterSlotKey.DEF2 to bindPlayerSlot(R.id.slotLCB),
            RosterSlotKey.DEF3 to bindPlayerSlot(R.id.slotCCB),
            RosterSlotKey.DEF4 to bindPlayerSlot(R.id.slotRCB),
            RosterSlotKey.DEF5 to bindPlayerSlot(R.id.slotRB),

            RosterSlotKey.MID1 to bindPlayerSlot(R.id.slotLM),
            RosterSlotKey.MID2 to bindPlayerSlot(R.id.slotLCM),
            RosterSlotKey.MID3 to bindPlayerSlot(R.id.slotCM),
            RosterSlotKey.MID4 to bindPlayerSlot(R.id.slotRCM),
            RosterSlotKey.MID5 to bindPlayerSlot(R.id.slotRM),

            RosterSlotKey.STR1 to bindPlayerSlot(R.id.slotLS),
            RosterSlotKey.STR2 to bindPlayerSlot(R.id.slotST),
            RosterSlotKey.STR3 to bindPlayerSlot(R.id.slotRS),
        )

        slotViews.forEach { (key, view) ->
            view.root.setOnClickListener { showPickerForSlot(key) }
            view.clearButton?.setOnClickListener {
                selectedBySlot[key] = null
                renderSlot(key)
                updateHeader()
            }
        }

        loadPlayers()
        renderAll() // shows the + icons / "Tap to pick" initially
        updateHeader()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_pick_team, menu) // your existing file
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_confirm_team -> {
                confirmAndSave()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadPlayers() {
        lifecycleScope.launch {
            try {
                allPlayers = withContext(Dispatchers.IO) { repo.fetchPlayersFromBackend() }

                //once players exist, load saved team
                val team = withContext(Dispatchers.IO) { repo.getMyTeam() }

                if (team != null) {
                    populateUiWithSavedTeam(team)
                } else {
                    //first-time user: leave empty
                }

                renderAll()
                updateHeader()

            } catch (e: Exception) {
                Toast.makeText(this@TransfersActivity, "Failed to load: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showPickerForSlot(slot: RosterSlotKey) {
        val requiredPos = slot.position

        val alreadyPicked = selectedBySlot.values.filterNotNull().toSet()
        val currentId = selectedBySlot[slot]

        val candidates = allPlayers
            .filter { it.position == requiredPos }
            .filter { it.id !in alreadyPicked || it.id == currentId }

        if (candidates.isEmpty()) {
            Toast.makeText(this, "No available $requiredPos players", Toast.LENGTH_SHORT).show()
            return
        }

        val items = candidates
            .map { "${it.name} • ${it.club} • €%.1fm • ${it.points} pts".format(it.price) }
            .toTypedArray()

        val currentIndex = currentId?.let { id -> candidates.indexOfFirst { it.id == id } } ?: -1
        val previousId = currentId

        val builder = AlertDialog.Builder(this)
            .setTitle("Pick $requiredPos")
            .setSingleChoiceItems(items, currentIndex) { dialog, which ->
                val chosen = candidates[which]
                selectedBySlot[slot] = chosen.id

                if (spentNow() > totalBudget) {
                    selectedBySlot[slot] = previousId
                    Toast.makeText(this, "Over budget. Pick a cheaper $requiredPos.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    renderSlot(slot)
                    updateHeader()
                    return@setSingleChoiceItems
                }

                dialog.dismiss()
                renderSlot(slot)
                updateHeader()
            }
            .setNegativeButton("Cancel", null)
        if (currentId != null) {
            builder.setNeutralButton("Clear Slot") { dialog, _ ->
                selectedBySlot[slot] = null
                renderSlot(slot)
                updateHeader()
                dialog.dismiss()
            }
        }

        builder.show()
    }

    private fun saveTeamFromTransfersUi(playerIds: List<Int>) {
        val teamName = "My Team"

        lifecycleScope.launch {
            try {
                val existingTeam = withContext(Dispatchers.IO) { repo.getMyTeam() }
                val created = existingTeam == null

                withContext(Dispatchers.IO) {
                    if (created) repo.submitTeamToBackend(teamName, playerIds)
                    else repo.updateMyTeamPlayers(playerIds)
                }

                hasSavedTeam = true
                SLOT_ORDER.forEach { key -> initialBySlot[key] = selectedBySlot[key] }

                Toast.makeText(
                    this@TransfersActivity,
                    if (created) "Team saved!" else "Transfers saved!",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(this@TransfersActivity, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmAndSave() {
        //stop immediately if not complete
        val playerIds = build15PlayerIdsOrNull()
        if (playerIds == null) {
            Toast.makeText(this, "Please fill all slots before confirming.", Toast.LENGTH_SHORT).show()
            return
        }

        // Optional: if nothing changed, don't prompt
        if (hasSavedTeam && !hasChanges()) {
            Toast.makeText(this, "No changes to save.", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm")
            .setMessage("Save these changes to your team?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm") { _, _ ->
                saveTeamFromTransfersUi(playerIds) // pass ids so you don't recompute
            }
            .show()
    }

    private fun hasChanges(): Boolean =
        RosterSlotKey.entries.any { key -> selectedBySlot[key] != initialBySlot[key] }

    private fun populateUiWithSavedTeam(team: LeaderboardTeamDto) {
        val ids = team.playerIds
        if (ids.size != SLOT_ORDER.size) return

        hasSavedTeam = true

        SLOT_ORDER.forEachIndexed { index, key ->
            val id = ids[index]
            selectedBySlot[key] = id
            initialBySlot[key] = id
            renderSlot(key)
        }

        updateHeader()
    }

    private fun build15PlayerIdsOrNull(): List<Int>? {
        val ids = SLOT_ORDER.map { key -> selectedBySlot[key] }
        return if (ids.any { it == null }) null else ids.filterNotNull()
    }

    private fun spentNow(): Double =
        selectedBySlot.values.filterNotNull()
            .mapNotNull { id -> allPlayers.firstOrNull { it.id == id } }
            .sumOf { it.price }

    private fun renderAll() = slotViews.keys.forEach { renderSlot(it) }

    @SuppressLint("SetTextI18n")
    private fun renderSlot(slot: RosterSlotKey) {
        val view = slotViews[slot] ?: return
        val id = selectedBySlot[slot]

        if (id == null) {
            view.imgAdd.visibility = View.VISIBLE
            view.filledGroup.visibility = View.GONE
            view.name.text = "Tap to pick" // optional
            view.meta.text = ""
            view.clearButton?.visibility = View.GONE
            return
        }

        val p = allPlayers.firstOrNull { it.id == id } ?: return

        view.imgAdd.visibility = View.GONE
        view.filledGroup.visibility = View.VISIBLE

        view.imgJersey.setImageResource(R.drawable.bg_jersey_placeholder)
        view.name.text = lastName(p.name)
        view.meta.text = "${p.club} (A)"

        view.clearButton?.visibility = View.VISIBLE
    }


    @SuppressLint("SetTextI18n")
    private fun updateHeader() {
        chipBudget.text = "Budget: €%.1fm".format(remainingBudget())
        chipPoints.text = "Cost: ${penaltyPoints()} pts"

        val used = transfersUsed()
        val left = (freeTransfers - used).coerceAtLeast(0)
        chipTransfers.text = "$left Free Transfers"

        // keep your static deadline for now
        chipDeadline.text = "Gameweek Deadline: Tue 2 Dec"
    }

    //Just show players last name
    private fun lastName(fullName: String): String {
        val parts = fullName.trim().split(Regex("\\s+"))
        return parts.lastOrNull().orEmpty()
    }

    private fun transfersUsed(): Int =
        slotViews.keys.count { key -> selectedBySlot[key] != initialBySlot[key] }

    private fun penaltyPoints(): Int {
        val extra = (transfersUsed() - freeTransfers).coerceAtLeast(0)
        return extra * 5
    }

    private fun remainingBudget(): Double = totalBudget - spentNow()

}




