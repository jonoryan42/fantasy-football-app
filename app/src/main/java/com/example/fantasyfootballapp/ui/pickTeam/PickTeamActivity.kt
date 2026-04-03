package com.example.fantasyfootballapp.ui.pickTeam

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.example.fantasyfootballapp.model.Formation
import com.example.fantasyfootballapp.model.LineupManager
import com.example.fantasyfootballapp.model.LineupState
import com.example.fantasyfootballapp.model.Player
import com.example.fantasyfootballapp.model.Position
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.fantasyfootballapp.ui.common.AppBottomNav
import com.example.fantasyfootballapp.ui.common.SystemBars

class PickTeamActivity : AppCompatActivity() {

    private var teamName: String = "My Team"

    private lateinit var pitchOverlay: View

    private lateinit var slotViews: Map<RosterSlotKey, PlayerSlotView>

    private val selectedBySlot = mutableMapOf<RosterSlotKey, Int?>()

    private val initialBySlot = mutableMapOf<RosterSlotKey, Int?>()

    private var allPlayers: List<Player> = emptyList()

    private var hasSavedTeam: Boolean = false

    private val SLOT_ORDER = RosterSlotKey.SLOT_ORDER

    private lateinit var lineupManager: LineupManager
    private var lineupState = LineupState()          // keep latest state

    private var playerById: Map<Int, Player> = emptyMap()

    private var validBenchIndexes: Set<Int> = emptySet()
    private var currentFormation = Formation.F442

    //Sub state
    private var subModeActive: Boolean = false
    private var subSourceSlot: RosterSlotKey? = null

    private var subSourceBenchIndex: Int? = null
    private var validStarterSlots: Set<RosterSlotKey> = emptySet()

    //Relating to positional rows
    private val defRow = listOf(
        RosterSlotKey.DEF1,
        RosterSlotKey.DEF2,
        RosterSlotKey.DEF3,
        RosterSlotKey.DEF4,
        RosterSlotKey.DEF5
    )

    private val midRow = listOf(
        RosterSlotKey.MID1,
        RosterSlotKey.MID2,
        RosterSlotKey.MID3,
        RosterSlotKey.MID4,
        RosterSlotKey.MID5
    )

    private val strRow = listOf(
        RosterSlotKey.STR1,
        RosterSlotKey.STR2,
        RosterSlotKey.STR3
    )

    private val defFractions = mapOf(
        3 to listOf(0.24f, 0.42f, 0.60f),
        5 to listOf(0.12f, 0.30f, 0.48f, 0.66f, 0.84f)
    )

    private val midFractions = mapOf(
        3 to listOf(0.32f, 0.50f, 0.68f),
        5 to listOf(0.14f, 0.32f, 0.50f, 0.68f, 0.86f)
    )

    private val strFractions = mapOf(
        3 to listOf(0.495f, 0.675f, 0.855f)
    )

    private val repo by lazy {
        val tokenStore = TokenStore(applicationContext)
        FantasyRepository(ApiClient.service, tokenStore)
    }

    private val onboardingDraft: RegistrationDraft? by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NavKeys.REG_DRAFT, RegistrationDraft::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NavKeys.REG_DRAFT)
        }
    }

    private val isFirstTeamCreate by lazy {
        intent.getBooleanExtra("EXTRA_FIRST_TEAM_CREATE", false)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pick_team)

        SystemBars.apply(this, R.color.screen_light_bg, lightIcons = true)

        teamName = intent.getStringExtra("teamName") ?: "My Team"

        pitchOverlay = findViewById(R.id.pitchOverlay)

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)

        AppBottomNav.setup(
            activity = this,
            bottomNav = bottomNav,
            selectedItemId = R.id.nav_fantasy
        )

        lineupManager = LineupManager()

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

            RosterSlotKey.BENCH1 to bindPlayerSlot(R.id.benchSlot1),
            RosterSlotKey.BENCH2 to bindPlayerSlot(R.id.benchSlot2),
            RosterSlotKey.BENCH3 to bindPlayerSlot(R.id.benchSlot3),
            RosterSlotKey.BENCH4 to bindPlayerSlot(R.id.benchSlot4),
        )

        lineupState = LineupState() //initialise lineup state

        wireSlotClicks()
        loadPlayers()
        renderAll()
        renderBenchPosLabels()
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
                playerById = allPlayers.associateBy { it.id }
            } catch (e: Exception) {
                Toast.makeText(
                    this@PickTeamActivity,
                    "Failed to load players: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            // 1.5) Transfers sends a 15-player squad
            val incomingSquad = intent.getIntegerArrayListExtra(NavKeys.PLAYER_IDS)?.toList()
            val hasIncomingSquad = !incomingSquad.isNullOrEmpty() && incomingSquad.size == 15

            if (hasIncomingSquad && isFirstTeamCreate) {
                seedSelectedBySlotFromTransferOrder(incomingSquad)
                hasSavedTeam = false

                //convert Transfer-style 15 into PickTeam 4-4-2 + bench
                force442AndFillBenchFromExtras()
                return@launch
            }


            // 2) load saved team if exists
            val isOnboarding = onboardingDraft != null
            val token = repo.getTokenOrNull()

            if (!isOnboarding && !token.isNullOrBlank()) {
                try {
                    val team = withContext(Dispatchers.IO) { repo.getMyTeam() }

                    if (team != null) {
                        val slotMap = decodeSlotMap(team) // Map<RosterSlotKey, Int?>
                        val hasAnySlots = slotMap.values.any { it != null }

                        Log.d("PickTeam", "team = $team")
                        Log.d("PickTeam", "decoded slotMap = $slotMap")
                        Log.d("PickTeam", "hasAnySlots = $hasAnySlots")

                        if (hasAnySlots) {
                            val savedSlots = decodeSlotMap(team).toMutableMap()
                            val squad = incomingSquad ?: getSquadIds(team)

                            reconcileSlotsWithSquadByPosition(savedSlots, squad)

                            loadFromSavedSlots(team, savedSlots)   // ✅ use reconciled
                        } else {
                            val squad = getSquadIds(team)
                            loadFirstTimeFromSquad(squad)
                        }

                        return@launch // important: avoid renderAll() overriding your lineup render
                    }

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

            // 3) Render UI regardless (empty state)
            renderAll()
            renderBenchPosLabels()
        }
    }
        private fun decodeSlotMap(team: LeaderboardTeamDto): Map<RosterSlotKey, Int?> {
            val raw = team.slotPlayerIds ?: return emptyMap()

            return raw.mapNotNull { (k, v) ->
                val key = runCatching { RosterSlotKey.valueOf(k) }.getOrNull()
                key?.let { it to v }
            }.toMap()
        }

        private fun getSquadIds(team: LeaderboardTeamDto): List<Int>? {
            return team.squadPlayerIds ?: team.playerIds
        }

       private fun seedSelectedBySlotFromTransferOrder(squad: List<Int>?) {
            // PITCH_ORDER must be the 15 transfer-style placeholders (GK1..STR3)
            val pitchOrder = RosterSlotKey.PITCH_ORDER

            selectedBySlot.clear()
            pitchOrder.forEachIndexed { i, key ->
                selectedBySlot[key] = squad?.get(i)
            }

            // bench keys start empty (formation will move extras into BENCH1..4)
            selectedBySlot[RosterSlotKey.BENCH1] = null
            selectedBySlot[RosterSlotKey.BENCH2] = null
            selectedBySlot[RosterSlotKey.BENCH3] = null
            selectedBySlot[RosterSlotKey.BENCH4] = null
        }

    private fun loadFromSavedSlots(team: LeaderboardTeamDto, slotMap: Map<RosterSlotKey, Int?>) {
        lineupState = LineupState.fromSlotMap(slotMap)

        currentFormation = Formation.fromKey(team.formationKey)

        renderLineup(lineupState, currentFormation)

        // hasChanges() is false on entry
        initialBySlot.clear()
        initialBySlot.putAll(selectedBySlot)
        hasSavedTeam = true
    }

    //For Loading the team after new user has selected their team
        private fun loadFirstTimeFromSquad(squad: List<Int>?) {
            if (squad?.size != 15) return

            seedSelectedBySlotFromTransferOrder(squad)
            applyDefault442() // seeds -> applyFormation -> renderLineup
        }

    //Save team for new user
    private fun finalizeRegistrationAndSaveTeam(
        playerIds: List<Int>,
        slotMap: Map<String, Int?>,
        formationKey: String
    ) {
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
                playerIds = playerIds,
                slotPlayerIds = slotMap,
                formationKey = formationKey
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

    private fun onConfirmSlotMap(slotMap: Map<String, Int?>) {
        lifecycleScope.launch {
            try {
                if (onboardingDraft != null) {
                    // If onboarding needs to register + save, do that here or keep your existing flow.
                    // For now, you probably want to call your existing finalizeRegistrationAndSaveTeam
                    // BUT that expects a List<Int> (15). See note below.
                    finalizeRegistrationAndSaveTeam(
                        playerIds = buildCurrentSquadIds(),
                        slotMap = slotMap,
                        formationKey = Formation.keyOf(currentFormation)
                    )
                } else {
                    repo.updateMyTeamSlots(
                        slotMap,
                        formationKey = Formation.keyOf(currentFormation)) // implement in repo to call backend route
                    onSavedSuccessfully()
                    goToLeaderboard()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PickTeamActivity, e.message ?: "Save failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmAndSave() {
        val payload = buildSlotMapPayload()
        val squadIds = buildCurrentSquadIds()

        //Just a safety check
        if (squadIds.size != 15) {
            Toast.makeText(this, "Team not complete", Toast.LENGTH_LONG).show()
            return
        }

        val shouldPrompt =
            hasSavedTeam && startingXiChanged(currentFormation)

        if (!shouldPrompt) {
            onConfirmSlotMap(slotMap = payload)
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm")
            .setMessage("Save these changes to your starting XI?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm") { _, _ ->
                onConfirmSlotMap(slotMap = payload)
            }
            .show()
    }

    private fun onSavedSuccessfully() {
        initialBySlot.clear()
        initialBySlot.putAll(selectedBySlot)
        hasSavedTeam = true
    }
    private fun goToLeaderboard() {
        val intent = Intent(this, LeaderboardActivity::class.java)
        //prevents coming back to Transfers with back button
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun hasChanges(): Boolean =
        RosterSlotKey.SLOT_ORDER.any { key ->
            selectedBySlot[key] != initialBySlot[key]
        }

    private fun populateUiWithSavedTeam(team: LeaderboardTeamDto) {
        val ids = team.playerIds
        if (ids?.size != SLOT_ORDER.size) return

        hasSavedTeam = true

        SLOT_ORDER.forEachIndexed { index, key ->
            val id = ids.get(index)
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

    private fun buildCurrentSquadIds(): List<Int> {
        return RosterSlotKey.SLOT_ORDER
            .mapNotNull { selectedBySlot[it] }
            .distinct()
    }

    private fun slotGroup(key: RosterSlotKey): Position? = when {
        key.name.startsWith("GK") -> Position.GK
        key.name.startsWith("DEF") -> Position.DEF
        key.name.startsWith("MID") -> Position.MID
        key.name.startsWith("STR") -> Position.STR
        else -> null // bench
    }

    private fun playerPos(id: Int?): Position? =
        allPlayers.firstOrNull { it.id == id }?.position

    private fun reconcileSlotsWithSquadByPosition(
        slotMap: MutableMap<RosterSlotKey, Int?>,
        newSquad: List<Int?>?
    ) {
        val squadSet = newSquad?.toSet()

        // 1) Remove transferred-out players
        for (k in RosterSlotKey.SLOT_ORDER) {
            val pid = slotMap[k]
            if (pid != null && squadSet?.contains(pid) != true) slotMap[k] = null
        }

        // 2) Determine missing/new players not yet placed
        val used = slotMap.values.filterNotNull().toMutableSet()
        val missing = (newSquad ?: emptyList())
            .filter { it !in used }
            .toMutableList()

        // 3) Fill empty PITCH slots first by matching position group
        val pitchKeys = RosterSlotKey.PITCH_ORDER
        for (k in pitchKeys) {
            if (slotMap[k] != null) continue
            val need = slotGroup(k)

            val idx = missing.indexOfFirst { id -> playerPos(id) == need }
            if (idx != -1) {
                slotMap[k] = missing.removeAt(idx)
            }
            }

        // 4) Fill bench with whatever remains (or also position-sort if you want)
        val benchKeys = listOf(RosterSlotKey.BENCH1, RosterSlotKey.BENCH2, RosterSlotKey.BENCH3, RosterSlotKey.BENCH4)
        for (k in benchKeys) {
            if (missing.isEmpty()) break
            if (slotMap[k] == null) slotMap[k] = missing.removeAt(0)
        }

        // 5) Any leftovers (shouldn't happen) can fill remaining null pitch slots
        for (k in pitchKeys) {
            if (missing.isEmpty()) break
            if (slotMap[k] == null) slotMap[k] = missing.removeAt(0)
        }
    }

    private fun buildSlotMapPayload(): Map<String, Int?> {
        return RosterSlotKey.SLOT_ORDER.associate { key ->
            key.name to selectedBySlot[key]
        }
    }

    private fun startingXiChanged(formation: Formation): Boolean {
        val keys = lineupManager.activeStarterKeys(formation)
        val before = keys.mapNotNull { initialBySlot[it] }.toSet()
        val now = keys.mapNotNull { selectedBySlot[it] }.toSet()
        return before != now
    }

    private fun renderAll() = slotViews.keys.forEach { renderSlot(it)
    renderBenchPosLabels()
    }

    private fun renderLineup(
        state: LineupState,
        formation: Formation
    ) {
        val activeKeys = lineupManager.activeStarterKeys(formation).toSet()

        // 1) Show/hide pitch slots (bench always visible)
        slotViews.keys.forEach { key ->
            slotViews[key]?.root?.visibility =
                if (key in activeKeys || key.isBench()) View.VISIBLE else View.GONE
        }

        // 2) Update selectedBySlot for starters (includes extras too, but that's ok)
        state.starters.forEach { (key, playerId) ->
            selectedBySlot[key] = playerId
        }

        // 3) Update bench slots
        val benchKeys = listOf(
            RosterSlotKey.BENCH1, RosterSlotKey.BENCH2,
            RosterSlotKey.BENCH3, RosterSlotKey.BENCH4
        )
        benchKeys.forEachIndexed { index, benchKey ->
            selectedBySlot[benchKey] = state.bench.getOrNull(index)
        }

        // 4) Re-render everything
        renderAll()
        renderBenchPosLabels()
        applyPitchSpacing()
    }

    private fun seedStateFromSelectedSlotsInto(state: LineupState) {
        val pitchKeys = listOf(
            RosterSlotKey.GK1, RosterSlotKey.GK2,
            RosterSlotKey.DEF1, RosterSlotKey.DEF2, RosterSlotKey.DEF3, RosterSlotKey.DEF4, RosterSlotKey.DEF5,
            RosterSlotKey.MID1, RosterSlotKey.MID2, RosterSlotKey.MID3, RosterSlotKey.MID4, RosterSlotKey.MID5,
            RosterSlotKey.STR1, RosterSlotKey.STR2, RosterSlotKey.STR3
        )

        // Fill starters from your existing selectedBySlot map
        pitchKeys.forEach { key ->
            state.starters[key] = selectedBySlot[key]
        }

        // Bench: seed from selectedBySlot if present (otherwise null)
        state.bench[0] = selectedBySlot[RosterSlotKey.BENCH1]
        state.bench[1] = selectedBySlot[RosterSlotKey.BENCH2]
        state.bench[2] = selectedBySlot[RosterSlotKey.BENCH3]
        state.bench[3] = selectedBySlot[RosterSlotKey.BENCH4]    }

    private fun applyDefault442() {
        currentFormation = Formation.F442

        // make sure lineupState reflects selectedBySlot
        seedStateFromSelectedSlotsInto(lineupState)

        renderLineup(lineupState, currentFormation)
    }

    private fun force442AndFillBenchFromExtras() {
        currentFormation = Formation.F442

        //1. Capture the players currently in the "right side" + extra GK ----
        val capturedBenchIds = listOf(
            selectedBySlot[RosterSlotKey.GK2],
            selectedBySlot[RosterSlotKey.DEF5],
            selectedBySlot[RosterSlotKey.MID5],
            selectedBySlot[RosterSlotKey.STR3]
        )

        val benchIds = lineupManager.sortBenchIds(capturedBenchIds, playerById)

        // 2. Shift central -> right (and right -> further right) ----
        // DEF shift: CCB -> RCB, RCB -> RB
        val ccb = selectedBySlot[RosterSlotKey.DEF3]
        val rcb = selectedBySlot[RosterSlotKey.DEF4]
        selectedBySlot[RosterSlotKey.DEF4] = ccb
        selectedBySlot[RosterSlotKey.DEF5] = rcb

        // MID shift: CM -> RCM, RCM -> RM
        val cm = selectedBySlot[RosterSlotKey.MID3]
        val rcm = selectedBySlot[RosterSlotKey.MID4]
        selectedBySlot[RosterSlotKey.MID4] = cm
        selectedBySlot[RosterSlotKey.MID5] = rcm

        // STR shift: ST -> RS
        val st = selectedBySlot[RosterSlotKey.STR2]
        selectedBySlot[RosterSlotKey.STR3] = st

        // ---- 3) Clear hidden/unused pitch slots for 4-4-2 ----
        selectedBySlot[RosterSlotKey.GK2] = null
        selectedBySlot[RosterSlotKey.DEF3] = null
        selectedBySlot[RosterSlotKey.MID3] = null
        selectedBySlot[RosterSlotKey.STR2] = null

        // ---- 4) Put captured right-side players onto bench ----
        val benchKeys = listOf(
            RosterSlotKey.BENCH1,
            RosterSlotKey.BENCH2,
            RosterSlotKey.BENCH3,
            RosterSlotKey.BENCH4
        )
        benchKeys.forEachIndexed { i, k -> selectedBySlot[k] = benchIds.getOrNull(i) }

        // ---- 5) Render + baseline ----
        lineupState = LineupState.fromSlotMap(selectedBySlot)
        renderLineup(lineupState, currentFormation)

        initialBySlot.clear()
        initialBySlot.putAll(selectedBySlot)
    }

    //Clicking on Players
    private fun wireSlotClicks() {
        slotViews.forEach { (slotKey, slotView) ->
            // Use whichever is your actual clickable view.
            // From your renderLineup screenshot, you have slotViews[key]?.root
            slotView.root.setOnClickListener { onSlotClicked(slotKey) }
        }
    }

    private fun onSlotClicked(slot: RosterSlotKey) {
        if (subModeActive) {
            handleClickDuringSubMode(slot)
            return
        }

        val playerId = selectedBySlot[slot] ?: return
        val player = playerById[playerId] ?: return

        showPlayerDialog(slot, player)
    }

    private fun showPlayerDialog(slot: RosterSlotKey, player: Player) {
        AlertDialog.Builder(this)
            .setTitle(player.name)
            .setItems(arrayOf("View Stats", "Substitution")) { _, which ->
                when (which) {
                    0 -> showStatsDialog(player)
                    1 -> {
                        if (slot.isBench()) {
                            beginBenchSubstitution(slot)
                        } else {
                            beginStarterSubstitution(slot)
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showStatsDialog(player: Player) {
        val msg = buildString {
            appendLine("Club: ${player.club}")
            appendLine("Position: ${player.position}")
            appendLine("Price: ${player.price}")
            appendLine("Points: ${player.points}")
            appendLine("Goals: ${player.goals}  Assists: ${player.assists}")
            appendLine("Clean sheets: ${player.cleansheets}")
            appendLine("Yellows: ${player.yellows}  Reds: ${player.reds}")
        }

        AlertDialog.Builder(this)
            .setTitle(player.name)
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

    //Making subs
    private fun beginStarterSubstitution(starterSlot: RosterSlotKey) {
        if (!isStarterSlot(starterSlot)) return

        subModeActive = true
        subSourceSlot = starterSlot

        validBenchIndexes = lineupManager.validBenchIndexesForSub(
            state = lineupState,
            starterSlot = starterSlot,
            currentFormation = currentFormation,
            playerById = playerById
        )

        applyBenchHighlighting(validBenchIndexes)
    }

    private fun applyBenchHighlighting(valid: Set<Int>) {
        val benchKeys = listOf(
            RosterSlotKey.BENCH1,
            RosterSlotKey.BENCH2,
            RosterSlotKey.BENCH3,
            RosterSlotKey.BENCH4
        )

        benchKeys.forEachIndexed { idx, key ->
            val view = slotViews[key] ?: return@forEachIndexed
            val enabled = idx in valid

            view.root.apply {
                alpha = if (enabled) 1f else 0.35f
                isEnabled = enabled
            }
        }
    }

    //Subbing from the bench
    private fun beginBenchSubstitution(benchSlot: RosterSlotKey) {
        if (!benchSlot.isBench()) return

        val benchIndex = when (benchSlot) {
            RosterSlotKey.BENCH1 -> 0
            RosterSlotKey.BENCH2 -> 1
            RosterSlotKey.BENCH3 -> 2
            RosterSlotKey.BENCH4 -> 3
            else -> return
        }

        subModeActive = true
        subSourceSlot = null
        subSourceBenchIndex = benchIndex
        validBenchIndexes = emptySet()

        validStarterSlots = lineupManager.validStarterSlotsForBenchSub(
            state = lineupState,
            benchIndex = benchIndex,
            currentFormation = currentFormation,
            playerById = playerById
        )

        applyStarterHighlighting(validStarterSlots)
    }

    private fun applyStarterHighlighting(valid: Set<RosterSlotKey>) {
        val activeKeys = lineupManager.activeStarterKeys(currentFormation).toSet()

        activeKeys.forEach { key ->
            val slotView = slotViews[key] ?: return@forEach
            val enabled = key in valid

            slotView.root.alpha = if (enabled) 1f else 0.35f
            slotView.root.isEnabled = enabled
        }
    }

    private fun endSubMode() {
        subModeActive = false
        subSourceSlot = null
        subSourceBenchIndex = null
        validBenchIndexes = emptySet()
        validStarterSlots = emptySet()

        slotViews.forEach { (_, slotView) ->
            slotView.root.alpha = 1f
            slotView.root.isEnabled = true
        }
    }

    private fun handleClickDuringSubMode(clickedSlot: RosterSlotKey) {
        // Case 1: starter selected first, now waiting for bench target
        val starterSource = subSourceSlot
        if (starterSource != null) {
            if (!clickedSlot.isBench()) {
                endSubMode()
                return
            }

            val benchIndex = when (clickedSlot) {
                RosterSlotKey.BENCH1 -> 0
                RosterSlotKey.BENCH2 -> 1
                RosterSlotKey.BENCH3 -> 2
                RosterSlotKey.BENCH4 -> 3
                else -> return
            }

            if (benchIndex !in validBenchIndexes) return

            val result = lineupManager.swapBenchWithStarterAutoFormation(
                state = lineupState,
                benchIndex = benchIndex,
                starterSlot = starterSource,
                currentFormation = currentFormation,
                playerById = playerById
            )

            lineupState = result.state
            currentFormation = result.formation

            endSubMode()
            renderLineup(lineupState, currentFormation)
            return
        }

        // Case 2: bench selected first, now waiting for starter target
        val benchSource = subSourceBenchIndex
        if (benchSource != null) {
            if (clickedSlot !in validStarterSlots) {
                endSubMode()
                return
            }

            val result = lineupManager.swapBenchWithStarterAutoFormation(
                state = lineupState,
                benchIndex = benchSource,
                starterSlot = clickedSlot,
                currentFormation = currentFormation,
                playerById = playerById
            )

            lineupState = result.state
            currentFormation = result.formation

            endSubMode()
            renderLineup(lineupState, currentFormation)
        }
    }

    private fun isStarterSlot(key: RosterSlotKey): Boolean =
        !key.isBench() && (key in lineupManager.activeStarterKeys(currentFormation))

    //Bench Position labels
    private fun renderBenchPosLabels() {
        val labels = listOf(
            findViewById<TextView>(R.id.benchRole1),
            findViewById(R.id.benchRole2),
            findViewById(R.id.benchRole3),
            findViewById(R.id.benchRole4)
        )

        val benchSlots = listOf(
            RosterSlotKey.BENCH1,
            RosterSlotKey.BENCH2,
            RosterSlotKey.BENCH3,
            RosterSlotKey.BENCH4
        )

        benchSlots.forEachIndexed { i, slot ->
            val id = selectedBySlot[slot]
            val player = id?.let { playerById[it] }
            labels[i].text = shortPos(player?.position)
        }
    }

    private fun shortPos(pos: Position?): String {
        return when (pos) {
            Position.GK -> "GK"
            Position.DEF -> "DEF"
            Position.MID -> "MID"
            Position.STR -> "STR"
            else -> ""
        }
    }

    //Positional Rows logic
    @SuppressLint("UseKtx")
    private fun applyRowSpacing(
        rowKeys: List<RosterSlotKey>,
        fractionsMap: Map<Int, List<Float>>
    ) {
        //Array of visible slots
        val visibleViews = rowKeys
            .mapNotNull { key -> slotViews[key]?.root?.takeIf { it.visibility == View.VISIBLE } }

        if (visibleViews.isEmpty()) return

        val fractions = fractionsMap[visibleViews.size] ?: return
        val parent = visibleViews.first().parent as? View ?: return
        val parentWidth = parent.width.toFloat()
        if (parentWidth <= 0f) return

        visibleViews.forEachIndexed { index, view ->
            val targetCenterX = parentWidth * fractions[index]
            val currentCenterX = view.x + (view.width / 2f)
            view.translationX = targetCenterX - currentCenterX
        }
    }

    //For setting slot positions back to default
    private fun resetPitchTranslations() {
        val allPitchKeys = listOf(
            RosterSlotKey.GK1,
            RosterSlotKey.DEF1, RosterSlotKey.DEF2, RosterSlotKey.DEF3, RosterSlotKey.DEF4, RosterSlotKey.DEF5,
            RosterSlotKey.MID1, RosterSlotKey.MID2, RosterSlotKey.MID3, RosterSlotKey.MID4, RosterSlotKey.MID5,
            RosterSlotKey.STR1, RosterSlotKey.STR2, RosterSlotKey.STR3
        )

        allPitchKeys.forEach { key ->
            slotViews[key]?.root?.translationX = 0f
            slotViews[key]?.root?.translationY = 0f        }
    }

    private fun applyPitchSpacing() {
        resetPitchTranslations()

        // wait until views have been laid out with correct visibility
        pitchOverlay.post {
            applyRowSpacing(defRow, defFractions)
            applyRowSpacing(midRow, midFractions)
            applyRowSpacing(strRow, strFractions)

            applyFiveRowShape(midRow)
            applyFiveRowShape(defRow)

            applyThreeRowShape(defRow)
            applyThreeRowShape(midRow)
            applyThreeRowShape(strRow)
        }
    }

    @SuppressLint("UseKtx")
    private fun applyFiveRowShape(rowKeys: List<RosterSlotKey>) {

        val visibleViews = rowKeys
            .mapNotNull { key -> slotViews[key]?.root?.takeIf { it.visibility == View.VISIBLE } }

        if (visibleViews.size != 5) return

        // dip the 3rd and 4th players slightly
        visibleViews[3].translationY = 21f
        visibleViews[4].translationY = 19f
    }

    @SuppressLint("UseKtx")
    private fun applyThreeRowShape(rowKeys: List<RosterSlotKey>) {
        val visibleViews = rowKeys
            .mapNotNull { key -> slotViews[key]?.root?.takeIf { it.visibility == View.VISIBLE } }

        if (visibleViews.size != 3) return

        // lower the right-most player only
        visibleViews[2].translationY = 30f
    }
}
