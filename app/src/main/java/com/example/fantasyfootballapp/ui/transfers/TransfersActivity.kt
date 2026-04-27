package com.example.fantasyfootballapp.ui.transfers

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
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.data.TokenStore
import com.example.fantasyfootballapp.model.GameweekStat
import com.example.fantasyfootballapp.model.Player
import com.example.fantasyfootballapp.model.Position
import com.example.fantasyfootballapp.model.RegistrationDraft
import com.example.fantasyfootballapp.model.RosterSlotKey
import com.example.fantasyfootballapp.navigation.NavKeys
import com.example.fantasyfootballapp.network.ApiClient
import com.example.fantasyfootballapp.network.Fixture
import com.example.fantasyfootballapp.network.LeaderboardTeamDto
import com.example.fantasyfootballapp.network.isUnauthorized
import com.example.fantasyfootballapp.ui.common.AppBottomNav
import com.example.fantasyfootballapp.ui.common.PlayerSlotView
import com.example.fantasyfootballapp.ui.common.PlayerStatsHelper
import com.example.fantasyfootballapp.ui.common.SystemBars
import com.example.fantasyfootballapp.ui.common.bindPlayerSlot
import com.example.fantasyfootballapp.ui.leaderboard.LeaderboardActivity
import com.example.fantasyfootballapp.ui.pickTeam.PickTeamActivity
import com.example.fantasyfootballapp.util.RepoResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransfersActivity : AppCompatActivity() {

    private var teamName: String = "My Team"

    private lateinit var slotViews: Map<RosterSlotKey, PlayerSlotView>
    private val selectedBySlot = mutableMapOf<RosterSlotKey, Int?>()

    private val initialBySlot = mutableMapOf<RosterSlotKey, Int?>()

    private var allPlayers: List<Player> = emptyList()
    private val totalBudget = 100.0

    private lateinit var chipBudget: TextView
    private lateinit var chipPoints: TextView
    private lateinit var chipTransfers: TextView
    private lateinit var chipDeadline: TextView

    private var freeTransfers = 15 // later this comes from backend per gameweek

    private var hasSavedTeam: Boolean = false

    private var gwStatsByPlayerId: Map<Int, GameweekStat> = emptyMap()
    private var upcomingFixturesByTeam: Map<String, List<Fixture>> = emptyMap()

    private var playerById: Map<Int, Player> = emptyMap()


//    private val SLOT_ORDER = RosterSlotKey.PITCH_ORDER

    private val repo by lazy {
        val tokenStore = TokenStore(applicationContext)
        FantasyRepository(ApiClient.service, tokenStore)
    }

    //For collecting user data and saving after team selection
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
        setContentView(R.layout.activity_transfers)

        SystemBars.apply(this, R.color.screen_light_bg, lightIcons = true)

        teamName = intent.getStringExtra("teamName") ?: "My Team"

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

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)

        AppBottomNav.setup(
            activity = this,
            bottomNav = bottomNav,
            selectedItemId = R.id.nav_fantasy
        )
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
            // 1) Load all players (public endpoint) – if this fails, show a toast
            try {
                allPlayers = withContext(Dispatchers.IO) { repo.fetchPlayersFromBackend() }
                playerById = allPlayers.associateBy { it.id }

                gwStatsByPlayerId = withContext(Dispatchers.IO) {
                    PlayerStatsHelper.loadCurrentGameweekStats(
                        repo = repo,
                        players = allPlayers
                    )
                }

                upcomingFixturesByTeam = withContext(Dispatchers.IO) {
                    PlayerStatsHelper.loadUpcomingFixturesForTeams(
                        repo = repo,
                        players = allPlayers
                    )
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@TransfersActivity,
                    "Failed to load players: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
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
                    // If token is invalid/expired, ignore (your interceptor can clear it)
                    if (!isUnauthorized(e)) {
                        Toast.makeText(
                            this@TransfersActivity,
                            "Failed to load saved team: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            // 3) Render UI regardless
            renderAll()
            updateHeader()
        }
    }

    private fun showPickerForSlot(slot: RosterSlotKey) {
        val requiredPos = slot.position

        val alreadyPicked = selectedBySlot.values.filterNotNull().toSet()
        val currentId = selectedBySlot[slot]

        val candidates = allPlayers
            .filter { it.position == requiredPos }
            .filter { it.id !in alreadyPicked || it.id == currentId }
            .sortedByDescending { it.price }

        if (candidates.isEmpty()) {
            Toast.makeText(this, "No available $requiredPos players", Toast.LENGTH_SHORT).show()
            return
        }

        val items = candidates
            .map { player ->
                val points = gwStatsByPlayerId[player.id]?.points ?: 0
                "${player.name} - ${player.club} - €%.1fm - $points pts".format(player.price)
            }
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

    private fun goToPickTeam(playerIds: List<Int>) {
        val intent = Intent(this, PickTeamActivity::class.java)

        // existing 15-player list
        intent.putIntegerArrayListExtra(
            NavKeys.PLAYER_IDS,
            ArrayList(playerIds)
        )

        // NEW: pass exact transfer slots too
        val transferSlotMap = HashMap<String, Int>()

        selectedBySlot.forEach { (slot, playerId) ->
            if (playerId != null) {
                transferSlotMap[slot.name] = playerId
            }
        }

        intent.putExtra(NavKeys.TRANSFER_SLOT_MAP, transferSlotMap)

        onboardingDraft?.let { draft ->
            intent.putExtra(NavKeys.REG_DRAFT, draft)
            intent.putExtra(NavKeys.EXTRA_FIRST_TEAM_CREATE, !hasSavedTeam)
        }

        intent.putExtra("teamName", teamName)

        startActivity(intent)
    }

    private fun confirmAndSave() {
        //stop immediately if not complete
        val playerIds = build15PlayerIdsOrNull()

        // Optional: if nothing changed, don't prompt
        if (hasSavedTeam && !hasChanges()) {
            Toast.makeText(this, "No changes pending.", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm")
            .setMessage("Confirm these changes to your team?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm") { _, _ ->
                goToPickTeam(playerIds)  // pass ids so you don't recompute
            }
            .show()
    }

    private fun hasChanges(): Boolean =
        RosterSlotKey.entries.any { key -> selectedBySlot[key] != initialBySlot[key] }

    private fun populateUiWithSavedTeam(team: LeaderboardTeamDto) {
        val rawIds = team.squadPlayerIds ?: team.playerIds ?: return
        if (rawIds.size != RosterSlotKey.PITCH_ORDER.size) return

        val playersById = allPlayers.associateBy { it.id }

        val gks  = rawIds.filter { playersById[it]?.position == Position.GK }
        val defs = rawIds.filter { playersById[it]?.position == Position.DEF }
        val mids = rawIds.filter { playersById[it]?.position == Position.MID }
        val strs = rawIds.filter { playersById[it]?.position == Position.STR }

        if (gks.size != 2 || defs.size != 5 || mids.size != 5 || strs.size != 3) {
            return
        }

        val orderedIds = gks + defs + mids + strs

        hasSavedTeam = true
        selectedBySlot.clear()
        initialBySlot.clear()

        RosterSlotKey.PITCH_ORDER.forEachIndexed { index, key ->
            val id = orderedIds[index]
            selectedBySlot[key] = id
            initialBySlot[key] = id
        }

        renderAll()
        updateHeader()
    }

    private fun build15PlayerIdsOrNull(): List<Int> {
        val ids = RosterSlotKey.PITCH_ORDER.mapNotNull { selectedBySlot[it] }
        if (ids.size != RosterSlotKey.PITCH_ORDER.size) return emptyList()
        return ids
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




