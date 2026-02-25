package com.example.fantasyfootballapp.ui.pickTeam

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
import com.example.fantasyfootballapp.model.RegistrationDraft
import com.example.fantasyfootballapp.model.RosterSlotKey
import com.example.fantasyfootballapp.navigation.NavKeys
import com.example.fantasyfootballapp.network.ApiClient
import com.example.fantasyfootballapp.network.LeaderboardTeamDto
import com.example.fantasyfootballapp.network.isUnauthorized
import com.example.fantasyfootballapp.ui.common.PlayerSlotView
import com.example.fantasyfootballapp.ui.common.bindPlayerSlot
import com.example.fantasyfootballapp.ui.leaderboard.LeaderboardActivity
import com.example.fantasyfootballapp.util.RepoResult
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PickTeamActivity : AppCompatActivity() {

    private var teamName: String = "My Team"

    private lateinit var slotViews: Map<RosterSlotKey, PlayerSlotView>

    private val selectedBySlot = mutableMapOf<RosterSlotKey, Int?>()

    private val initialBySlot = mutableMapOf<RosterSlotKey, Int?>()

    private var allPlayers: List<Player> = emptyList()

    private var hasSavedTeam: Boolean = false

    private val SLOT_ORDER = RosterSlotKey.entries.toList()

    private val repo by lazy {
        val tokenStore = TokenStore(applicationContext)
        FantasyRepository(ApiClient.service, tokenStore)
    }

    private val incomingPlayerIds: List<Int>? by lazy {
        intent.getIntegerArrayListExtra(NavKeys.PLAYER_IDS)?.toList()
    }

    private val onboardingDraft: RegistrationDraft? by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NavKeys.REG_DRAFT, RegistrationDraft::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NavKeys.REG_DRAFT)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pick_team)

        teamName = intent.getStringExtra("teamName") ?: "My Team"

        //Top Toolbar
        val toolbar =
            findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbarPickTeam)
        setSupportActionBar(toolbar)

        //remove ActionBar title text entirely
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.title = ""

        //Back arrow behaviour
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

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

        loadPlayers()
        renderAll()
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

            // 1) Load all players (public endpoint)
            try {
                allPlayers = withContext(Dispatchers.IO) { repo.fetchPlayersFromBackend() }
            } catch (e: Exception) {
                Toast.makeText(
                    this@PickTeamActivity,
                    "Failed to load players: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            // 1.5) If Transfers sent us a squad, apply it immediately (skip getMyTeam)
            val incomingIds = intent.getIntegerArrayListExtra(NavKeys.PLAYER_IDS)?.toList()
            if (!incomingIds.isNullOrEmpty() && incomingIds.size == SLOT_ORDER.size) {

                // treat this as the "current team" on entry
                hasSavedTeam = true

                SLOT_ORDER.forEachIndexed { index, key ->
                    val id = incomingIds[index]
                    selectedBySlot[key] = id
                    initialBySlot[key] = id // so "No changes" works in Pick Team
                }

                renderAll()
                return@launch
            }

            // 2) Optionally load saved team (auth endpoint) – do NOT toast 401
            val isOnboarding = onboardingDraft != null
            val token = repo.getTokenOrNull()

            if (!isOnboarding && !token.isNullOrBlank()) {
                try {
                    val team = withContext(Dispatchers.IO) { repo.getMyTeam() }
                    if (team != null) populateUiWithSavedTeam(team)
                } catch (e: Exception) {
                    if (!isUnauthorized(e)) {
                        Toast.makeText(
                            this@PickTeamActivity,
                            "Failed to load saved team: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            // 3) Render UI regardless
            renderAll()
        }
    }

    //Save team for new user
    private fun finalizeRegistrationAndSaveTeam(playerIds: List<Int>) {
        val draft = onboardingDraft ?: run {
            Toast.makeText(this, "Missing registration details.", Toast.LENGTH_LONG).show()
            return
        }

        val teamName = draft.teamName?.trim().orEmpty()
        if (teamName.isBlank()) {
            Toast.makeText(this, "Missing team name. Please go back.", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {

            val result = repo.registerWithTeamSafe(
                fname = draft.firstName,
                lname = draft.lastName,
                email = draft.email,
                password = draft.password,
                teamName = teamName,
                playerIds = playerIds
            )

            when (result) {
                is RepoResult.Success -> {
                    Toast.makeText(this@PickTeamActivity, "Team saved!", Toast.LENGTH_SHORT).show()
                    goToLeaderboard() // or Home
                }

                is RepoResult.Error -> {
                    // btnSave.isEnabled = true
                    Toast.makeText(this@PickTeamActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    //Saving team for logged in user
    private fun saveTeamFromTransfersUi(playerIds: List<Int>) {
        lifecycleScope.launch {
            try {
                // Normal path: existing user updates their team
                repo.updateMyTeamPlayers(playerIds)

                Toast.makeText(this@PickTeamActivity, "Transfers saved!", Toast.LENGTH_SHORT).show()
                goToLeaderboard()

            } catch (e: Exception) {
                // If they somehow don't have a team yet, create one.
                // We'll fetch the teamName from current user (or fallback).
                try {
                    val user = repo.getCurrentUser()
                    val teamName =
                        user.teamName?.trim().takeUnless { it.isNullOrBlank() } ?: "My Team"

                    repo.submitTeamToBackend(teamName, playerIds)

                    Toast.makeText(this@PickTeamActivity, "Team created!", Toast.LENGTH_SHORT)
                        .show()
                    goToLeaderboard()

                } catch (createErr: Exception) {
                    Toast.makeText(
                        this@PickTeamActivity,
                        createErr.message ?: "Save failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun onConfirm(playerIds: List<Int>) {
        if (onboardingDraft != null) {
            finalizeRegistrationAndSaveTeam(playerIds)
        } else {
            saveTeamFromTransfersUi(playerIds)
        }
    }

    private fun confirmAndSave() {

        val playerIds = build15PlayerIds()
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
                onConfirm(playerIds)  // pass ids so you don't recompute
            }
            .show()
    }

    private fun goToLeaderboard() {
        val intent = Intent(this, LeaderboardActivity::class.java)
        // optional: prevents coming back to Transfers with back button
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
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
    }

    //Just show players last name
    private fun lastName(fullName: String): String {
        val parts = fullName.trim().split(Regex("\\s+"))
        return parts.lastOrNull().orEmpty()
    }

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

    private fun build15PlayerIds(): List<Int> {
        val ids = SLOT_ORDER.mapNotNull { selectedBySlot[it] }
        if (ids.size != SLOT_ORDER.size) {
            Toast.makeText(this, "Team not loaded yet.", Toast.LENGTH_SHORT).show()
            return emptyList()
        }
        return ids
    }

    private fun renderAll() = slotViews.keys.forEach { renderSlot(it) }

}
