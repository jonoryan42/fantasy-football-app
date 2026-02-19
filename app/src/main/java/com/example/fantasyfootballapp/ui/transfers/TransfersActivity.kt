package com.example.fantasyfootballapp.ui.transfers

import android.annotation.SuppressLint
import android.os.Bundle
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
import com.example.fantasyfootballapp.ui.common.PlayerSlotView
import com.example.fantasyfootballapp.ui.common.bindPlayerSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.putAll
import kotlin.text.clear
import kotlin.text.get

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


    private val repo by lazy {
        val tokenStore = TokenStore(applicationContext)
        FantasyRepository(ApiClient.service, tokenStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfers)

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

    private fun loadPlayers() {
        lifecycleScope.launch {
            try {
                val players = withContext(Dispatchers.IO) {
                    repo.fetchPlayersFromBackend()
                }
                allPlayers = players
                snapshotInitialTeam()
                renderAll()
                updateHeader()
            } catch (e: Exception) {
                Toast.makeText(
                    this@TransfersActivity,
                    "Failed to load: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
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
        chipDeadline.text = "Tue 2 Dec\nDeadline"
    }

    //Just show players last name
    private fun lastName(fullName: String): String {
        val parts = fullName.trim().split(Regex("\\s+"))
        return parts.lastOrNull().orEmpty()
    }

    private fun snapshotInitialTeam() {
        initialBySlot.clear()
        initialBySlot.putAll(selectedBySlot)
    }

    private fun transfersUsed(): Int =
        slotViews.keys.count { key -> selectedBySlot[key] != initialBySlot[key] }

    private fun penaltyPoints(): Int {
        val extra = (transfersUsed() - freeTransfers).coerceAtLeast(0)
        return extra * 5
    }

    private fun remainingBudget(): Double = totalBudget - spentNow()

}




