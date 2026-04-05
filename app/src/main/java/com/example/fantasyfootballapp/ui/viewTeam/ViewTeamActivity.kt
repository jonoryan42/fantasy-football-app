package com.example.fantasyfootballapp.ui.viewTeam

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.config.GameweekConfig
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.data.TokenStore
import com.example.fantasyfootballapp.model.Formation
import com.example.fantasyfootballapp.model.LineupManager
import com.example.fantasyfootballapp.model.LineupState
import com.example.fantasyfootballapp.model.Player
import com.example.fantasyfootballapp.model.Position
import com.example.fantasyfootballapp.model.RosterSlotKey
import com.example.fantasyfootballapp.navigation.NavKeys
import com.example.fantasyfootballapp.network.ApiClient
import com.example.fantasyfootballapp.network.LeaderboardTeamDto
import com.example.fantasyfootballapp.network.isUnauthorized
import com.example.fantasyfootballapp.ui.common.AppBottomNav
import com.example.fantasyfootballapp.ui.common.PlayerSlotView
import com.example.fantasyfootballapp.ui.common.SystemBars
import com.example.fantasyfootballapp.ui.common.bindPlayerSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.component1
import kotlin.collections.component2

@Suppress("DEPRECATION")
class ViewTeamActivity : AppCompatActivity() {

    //for other User Teams
    companion object {
        const val EXTRA_TEAM = "extra_team"
    }

    private var viewedTeam: LeaderboardTeamDto? = null

    private var teamName: String = "My Team"

    //For gameweek stats
    val gw = GameweekConfig.CURRENT_GAMEWEEK
    val season = GameweekConfig.CURRENT_SEASON

    private lateinit var pitchOverlay: View

    private lateinit var slotViews: Map<RosterSlotKey, PlayerSlotView>

    private val selectedBySlot = mutableMapOf<RosterSlotKey, Int?>()

    private val initialBySlot = mutableMapOf<RosterSlotKey, Int?>()

    private var allPlayers: List<Player> = emptyList()

    private var hasSavedTeam: Boolean = false

    private lateinit var lineupManager: LineupManager
    private var lineupState = LineupState()          // keep latest state

    private var playerById: Map<Int, Player> = emptyMap()

    private var currentFormation = Formation.F442

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_team)

        SystemBars.apply(this, R.color.screen_light_bg, lightIcons = true)

        viewedTeam = intent.getParcelableExtra(EXTRA_TEAM)

        teamName = viewedTeam?.teamName ?: "My Team"

        val toolbarTitle = findViewById<TextView>(R.id.toolbarTitle)
        toolbarTitle.text = teamName

        pitchOverlay = findViewById(R.id.pitchOverlay)

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)

        AppBottomNav.setup(
            activity = this,
            bottomNav = bottomNav,
            selectedItemId = R.id.nav_leaderboard
        )

        lineupManager = LineupManager()

        //Top Toolbar
        val toolbar =
            findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbarViewTeam)
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

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.menu_pick_team, menu)
//        return true
//    }

    private fun loadPlayers() {
        lifecycleScope.launch {
            try {
                allPlayers = withContext(Dispatchers.IO) { repo.fetchPlayersFromBackend() }
                playerById = allPlayers.associateBy { it.id }

                loadViewedTeamFromIntent()

                renderAll()
                renderBenchPosLabels()

            } catch (e: Exception) {
                Toast.makeText(
                    this@ViewTeamActivity,
                    "Failed to load players: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
        }
    }

    private fun loadViewedTeamFromIntent() {
        val team = viewedTeam

        selectedBySlot.clear()

        if (team == null) {
            currentFormation = Formation.F442
            RosterSlotKey.entries.forEach { key ->
                selectedBySlot[key] = null
            }
            renderAll()
            renderBenchPosLabels()
            return
        }

        val slotMap = decodeSlotMap(team)
        val squadIds = getSquadIds(team).orEmpty()
        val hasAnySlots = slotMap.values.any { it != null }

        when {
            hasAnySlots -> {
                loadFromSavedSlots(team, slotMap)
            }

            squadIds.size == 15 -> {
                currentFormation = Formation.F442
                seedSelectedBySlotFromTransferOrder(squadIds)
                force442AndFillBenchFromExtras()
            }

            else -> {
                currentFormation = Formation.F442
                RosterSlotKey.entries.forEach { key ->
                    selectedBySlot[key] = null
                }
                renderAll()
                renderBenchPosLabels()
            }
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
        key.name.startsWith("STR") -> Position.STR   // or Position.STR if that's what you use
        else -> null // bench
    }

    private fun playerPos(id: Int?): Position? =
        allPlayers.firstOrNull { it.id == id }?.position

    private fun buildSlotMapPayload(): Map<String, Int?> {
        return RosterSlotKey.SLOT_ORDER.associate { key ->
            key.name to selectedBySlot[key]
        }
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
        val playerId = selectedBySlot[slot] ?: return
        val player = playerById[playerId] ?: return

        showStatsDialog(player)
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