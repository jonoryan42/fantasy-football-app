package com.example.fantasyfootballapp.model

/**
 * Pure domain logic for turning a squad selection into:
 * - starters placed into active slots for a formation
 * - remaining players placed onto the bench (4 spots)
 *
 * Player IDs are Ints (your DB/player table IDs).
 */
class LineupManager(
    private val defKeys: List<RosterSlotKey> = listOf(RosterSlotKey.DEF1, RosterSlotKey.DEF2, RosterSlotKey.DEF3, RosterSlotKey.DEF4, RosterSlotKey.DEF5),
    private val midKeys: List<RosterSlotKey> = listOf(RosterSlotKey.MID1, RosterSlotKey.MID2, RosterSlotKey.MID3, RosterSlotKey.MID4, RosterSlotKey.MID5),
    private val fwdKeys: List<RosterSlotKey> = listOf(RosterSlotKey.STR1, RosterSlotKey.STR2, RosterSlotKey.STR3),
) {

    private val gk1 = RosterSlotKey.GK1
    private val gk2 = RosterSlotKey.GK2

    /** Slots that are active on the pitch for a given formation. */
    fun activeStarterKeys(formation: Formation): List<RosterSlotKey> {
        return when (formation) {
            Formation.F442 -> listOf(
                gk1,

                // Back 4: LB, LCB, RCB, RB
                RosterSlotKey.DEF1, RosterSlotKey.DEF2,
                RosterSlotKey.DEF4, RosterSlotKey.DEF5,

                // Mid 4: LM, LCM, RCM, RM
                RosterSlotKey.MID1, RosterSlotKey.MID2,
                RosterSlotKey.MID4, RosterSlotKey.MID5,

                // Front 2: LS, RS
                RosterSlotKey.STR1, RosterSlotKey.STR3
            )

            Formation.F433 -> listOf(
                gk1,

                // Back 4
                RosterSlotKey.DEF1, RosterSlotKey.DEF2,
                RosterSlotKey.DEF4, RosterSlotKey.DEF5,

                // Mid 3: LCM, CM, RCM
                RosterSlotKey.MID2, RosterSlotKey.MID3, RosterSlotKey.MID4,

                // Front 3: LW, ST, RW
                RosterSlotKey.STR1, RosterSlotKey.STR2, RosterSlotKey.STR3
            )

            Formation.F451 -> listOf(
                gk1,

                // Back 4
                RosterSlotKey.DEF1, RosterSlotKey.DEF2,
                RosterSlotKey.DEF4, RosterSlotKey.DEF5,

                // Mid 5: LM, LCM, CM, RCM, RM
                RosterSlotKey.MID1, RosterSlotKey.MID2, RosterSlotKey.MID3,
                RosterSlotKey.MID4, RosterSlotKey.MID5,

                // Lone striker
                RosterSlotKey.STR2
            )

            Formation.F532 -> listOf(
                gk1,

                // Back 5: LWB, LCB, CB, RCB, RWB
                RosterSlotKey.DEF1, RosterSlotKey.DEF2, RosterSlotKey.DEF3,
                RosterSlotKey.DEF4, RosterSlotKey.DEF5,

                // Mid 3
                RosterSlotKey.MID2, RosterSlotKey.MID3, RosterSlotKey.MID4,

                // Front 2
                RosterSlotKey.STR1, RosterSlotKey.STR3
            )

            Formation.F523 -> listOf(
                gk1,

                // Back 5
                RosterSlotKey.DEF1, RosterSlotKey.DEF2, RosterSlotKey.DEF3,
                RosterSlotKey.DEF4, RosterSlotKey.DEF5,

                // Mid 2
                RosterSlotKey.MID2, RosterSlotKey.MID4,

                // Front 3
                RosterSlotKey.STR1, RosterSlotKey.STR2, RosterSlotKey.STR3
            )

            Formation.F541 -> listOf(
                gk1,

                // Back 5
                RosterSlotKey.DEF1, RosterSlotKey.DEF2, RosterSlotKey.DEF3,
                RosterSlotKey.DEF4, RosterSlotKey.DEF5,

                // Mid 4
                RosterSlotKey.MID1, RosterSlotKey.MID2,
                RosterSlotKey.MID4, RosterSlotKey.MID5,

                // Lone striker
                RosterSlotKey.STR2
            )

            Formation.F343 -> listOf(
                gk1,

                // Back 3: LCB, CB, RCB
                RosterSlotKey.DEF2, RosterSlotKey.DEF3, RosterSlotKey.DEF4,

                // Mid 4: LM, LCM, RCM, RM
                RosterSlotKey.MID1, RosterSlotKey.MID2,
                RosterSlotKey.MID4, RosterSlotKey.MID5,

                // Front 3
                RosterSlotKey.STR1, RosterSlotKey.STR2, RosterSlotKey.STR3
            )

            Formation.F352 -> listOf(
                gk1,

                // Back 3
                RosterSlotKey.DEF2, RosterSlotKey.DEF3, RosterSlotKey.DEF4,

                // Mid 5
                RosterSlotKey.MID1, RosterSlotKey.MID2, RosterSlotKey.MID3,
                RosterSlotKey.MID4, RosterSlotKey.MID5,

                // Front 2
                RosterSlotKey.STR1, RosterSlotKey.STR3
            )

            else -> listOf(gk1)
        }
    }

    /** Slots that are NOT used on the pitch for a given formation (extras). */
    fun inactivePitchKeys(formation: Formation): List<RosterSlotKey> {
        val active = activeStarterKeys(formation).toSet()

        val allPitch = buildList {
            add(gk1); add(gk2)
            addAll(defKeys); addAll(midKeys); addAll(fwdKeys)
        }

        return allPitch.filter { it !in active }
    }

    data class FormationCounts(val def: Int, val mid: Int, val fwd: Int)

    private fun countOutfieldPositions(
        starterIds: List<Int>,
        playerById: Map<Int, Player>
    ): Pair<Int, FormationCounts>? {
        var gk = 0
        var def = 0
        var mid = 0
        var fwd = 0

        for (id in starterIds) {
            val p = playerById[id] ?: return null
            when (p.position) {
                Position.GK -> gk++
                Position.DEF -> def++
                Position.MID -> mid++
                Position.STR -> fwd++
                else -> {} // ignore BENCH (shouldn't happen for XI anyway)
            }
        }

        return gk to FormationCounts(def, mid, fwd)
    }

    fun detectFormationFromStarters(
        state: LineupState,
        currentFormation: Formation,
        playerById: Map<Int, Player>
    ): Formation? {

        // The XI are whatever is currently in the active starter slots
        val starterSlots = activeStarterKeys(currentFormation)
        val starterIds = starterSlots.mapNotNull { state.starters[it] }

        if (starterIds.size != 11) return null

        val counted = countOutfieldPositions(starterIds, playerById) ?: return null
        val gk = counted.first
        val counts = counted.second

        if (gk != 1) return null

        // Match to one of your supported formations
        return Formation.all.values.firstOrNull {
            it.def == counts.def && it.mid == counts.mid && it.fwd == counts.fwd
        }
    }

    /**
     * Apply a formation by redistributing players:
     * - Fill active starters first (keeping players that already sit in an active slot when possible)
     * - Put remaining players onto bench (4 spots)
     *
     * Important: GK2 is treated as a "bench candidate" in your rules (default 442).
     */
    fun applyFormation(
        state: LineupState,
        from: Formation,
        to: Formation
    ): LineupState {

        val oldActive = activeStarterKeys(from)
        val newActive = activeStarterKeys(to)

        // Canonical pitch order (15 pitch placeholders)
        val allPitchKeys = buildList {
            add(gk1); add(gk2)
            addAll(defKeys)
            addAll(midKeys)
            addAll(fwdKeys)
        }

        // 1) Preserve current STARTING XI (11) from old active keys in a stable order
        val starterIds = oldActive.mapNotNull { key -> state.starters[key] }

        // Safety: if something is missing, top up from the remaining pitch ids (shouldn’t happen normally)
        val used = starterIds.toMutableSet()
        val remainingPitchIds = allPitchKeys
            .mapNotNull { state.starters[it] }
            .filter { used.add(it) } // adds + filters uniques

        val starters11 = (starterIds + remainingPitchIds).take(11)

        // 2) Bench stays exactly the same 4 (don’t “promote” bench players on formation change)
        val bench4 = state.bench.take(4).toMutableList().apply {
            while (size < 4) add(null)
        }

        // 3) Build final starters map (all pitch keys exist, inactive forced null)
        val finalStarters = mutableMapOf<RosterSlotKey, Int?>().apply {
            allPitchKeys.forEach { this[it] = null }
        }

        // 4) Assign the 11 starters into the NEW active slots (stable order)
        newActive.forEachIndexed { index, key ->
            finalStarters[key] = starters11.getOrNull(index)
        }

        return LineupState(
            starters = finalStarters,
            bench = bench4
        )
    }

    fun applyFormationByPosition(
        state: LineupState,
        from: Formation,
        to: Formation,
        playerById: Map<Int, Player>
    ): LineupState {

        val oldActive = activeStarterKeys(from)
        val newActive = activeStarterKeys(to)

        // Canonical pitch order (15 pitch placeholders)
        val allPitchKeys = buildList {
            add(gk1); add(gk2)
            addAll(defKeys)
            addAll(midKeys)
            addAll(fwdKeys)
        }

        // Take current XI (from old active keys)
        val xiIds = oldActive.mapNotNull { state.starters[it] }
        if (xiIds.size != 11) return applyFormation(state, from, to) // fallback

        // Split XI by actual player position
        val gks = mutableListOf<Int>()
        val defs = mutableListOf<Int>()
        val mids = mutableListOf<Int>()
        val fwds = mutableListOf<Int>()
        val unknown = mutableListOf<Int>()

        for (id in xiIds) {
            val p = playerById[id]
            when (p?.position) {
                Position.GK  -> gks.add(id)
                Position.DEF -> defs.add(id)
                Position.MID -> mids.add(id)
                Position.STR -> fwds.add(id)
                else         -> unknown.add(id)
            }
        }

        // If GK logic is weird, fall back
        if (gks.size != 1) return applyFormation(state, from, to)

        // Bench stays exactly the same
        val bench4 = sortBenchIds(state.bench.take(4), playerById)

        // Prepare final starters map (all pitch keys exist; inactive forced null)
        val finalStarters = mutableMapOf<RosterSlotKey, Int?>().apply {
            allPitchKeys.forEach { this[it] = null }
        }

        // Figure out which keys are DEF/MID/FWD in the new formation
        val newDefKeys = newActive.filter { it in defKeys }
        val newMidKeys = newActive.filter { it in midKeys }
        val newFwdKeys = newActive.filter { it in fwdKeys }

        // Put players into their "rightful" groups first
        finalStarters[gk1] = gks.firstOrNull()

        // Helper to fill a list of keys from a list of ids, returning leftovers
        fun fill(keys: List<RosterSlotKey>, ids: MutableList<Int>): MutableList<Int> {
            keys.forEachIndexed { idx, key ->
                finalStarters[key] = ids.getOrNull(idx)
            }
            return ids.drop(keys.size).toMutableList()
        }

        var leftoverDefs = fill(newDefKeys, defs)
        var leftoverMids = fill(newMidKeys, mids)
        var leftoverFwds = fill(newFwdKeys, fwds)

        // Any extras (or unknown) can be used to fill gaps in a stable order
        val leftovers = mutableListOf<Int>().apply {
            addAll(leftoverDefs)
            addAll(leftoverMids)
            addAll(leftoverFwds)
            addAll(unknown)
        }

        // Fill any still-null *active* slots (rare, but safe)
        newActive.forEach { key ->
            if (finalStarters[key] == null) {
                finalStarters[key] = leftovers.removeFirstOrNull()
            }
        }

        return LineupState(
            starters = finalStarters,
            bench = bench4
        )
    }

    private fun <T> MutableList<T>.removeFirstOrNull(): T? =
        if (isEmpty()) null else removeAt(0)

    /**
     * Swap a bench player into a starter slot, and the starter goes to that bench spot.
     * This DOES NOT change formation by itself (we can add auto-formation later).
     */
    fun swapBenchWithStarter(
        state: LineupState,
        benchIndex: Int,
        starterSlot: RosterSlotKey
    ): LineupState {
        require(benchIndex in 0..3) { "benchIndex must be 0..3" }

        val benchPid = state.bench.getOrNull(benchIndex)
        val starterPid = state.starters[starterSlot]

        // No-op if bench is empty
        if (benchPid == null) return state

        val newStarters = state.starters.toMutableMap()
        val newBench = state.bench.toMutableList()

        newStarters[starterSlot] = benchPid
        newBench[benchIndex] = starterPid // could become null, that's fine

        return LineupState(newStarters, newBench)
    }

    //Minimum starters in each row (1 Striker, 2 Mids, 3 Defs)
    private fun keepsMinimumStarterCountsAfterSwap(
        state: LineupState,
        benchIndex: Int,
        starterSlot: RosterSlotKey,
        currentFormation: Formation,
        playerById: Map<Int, Player>
    ): Boolean {
        val benchId = state.bench.getOrNull(benchIndex) ?: return false
        val starterId = state.starters[starterSlot] ?: return false

        // Simulate the swap
        val startersAfter = state.starters.toMutableMap()
        startersAfter[starterSlot] = benchId

        var defCount = 0
        var midCount = 0
        var strCount = 0
        var gkCount = 0

        activeStarterKeys(currentFormation).forEach { slot ->
            val id = startersAfter[slot] ?: return@forEach
            val player = playerById[id] ?: return@forEach

            when (player.position) {
                Position.GK -> gkCount++
                Position.DEF -> defCount++
                Position.MID -> midCount++
                Position.STR -> strCount++
                Position.BENCH -> {}            }
        }

        return gkCount == 1 &&
                defCount >= 3 &&
                midCount >= 2 &&
                strCount >= 1
    }

    //Rules for substitutions
    fun validBenchIndexesForSub(
        state: LineupState,
        starterSlot: RosterSlotKey,
        currentFormation: Formation,
        playerById: Map<Int, Player>
    ): Set<Int> {

        val starterId = state.starters[starterSlot] ?: return emptySet()
        val starter = playerById[starterId] ?: return emptySet()

        val valid = mutableSetOf<Int>()

        for (i in 0 until state.bench.size) {
            val benchId = state.bench[i] ?: continue
            val benchPlayer = playerById[benchId] ?: continue

            // Rule 1: GK can only swap with GK
            val starterIsGK = starter.position == Position.GK
            val benchIsGK = benchPlayer.position == Position.GK
            if (starterIsGK != benchIsGK) continue

            // Rule 2: must still satisfy minimum row counts
            if (!keepsMinimumStarterCountsAfterSwap(
                    state = state,
                    benchIndex = i,
                    starterSlot = starterSlot,
                    currentFormation = currentFormation,
                    playerById = playerById
                )
            ) continue

            valid.add(i)
        }

        return valid
    }

    fun validStarterSlotsForBenchSub(
        state: LineupState,
        benchIndex: Int,
        currentFormation: Formation,
        playerById: Map<Int, Player>
    ): Set<RosterSlotKey> {

        val benchId = state.bench.getOrNull(benchIndex) ?: return emptySet()
        val benchPlayer = playerById[benchId] ?: return emptySet()

        val benchIsGK = benchPlayer.position == Position.GK
        val valid = mutableSetOf<RosterSlotKey>()

        activeStarterKeys(currentFormation).forEach { starterSlot ->
            val starterId = state.starters[starterSlot] ?: return@forEach
            val starterPlayer = playerById[starterId] ?: return@forEach

            val starterIsGK = starterPlayer.position == Position.GK

            // GK only for GK, outfield only for outfield
            if (benchIsGK != starterIsGK) return@forEach

            // New minimum-row rule
            if (!keepsMinimumStarterCountsAfterSwap(
                    state = state,
                    benchIndex = benchIndex,
                    starterSlot = starterSlot,
                    currentFormation = currentFormation,
                    playerById = playerById
                )
            ) {
                return@forEach
            }

            valid.add(starterSlot)
        }

        return valid
    }

    data class SwapResult(
        val state: LineupState,
        val formation: Formation
    )

    fun swapBenchWithStarterAutoFormation(
        state: LineupState,
        benchIndex: Int,
        starterSlot: RosterSlotKey,
        currentFormation: Formation,
        playerById: Map<Int, Player>
    ): SwapResult {

        val swapped = swapBenchWithStarter(state, benchIndex, starterSlot)

        val sortedSwapped = swapped.copy(
            bench = sortBenchIds(swapped.bench, playerById)
        )

        val detected = detectFormationFromStarters(
            state = sortedSwapped,
            currentFormation = currentFormation,
            playerById = playerById
        )

        return if (detected != null && detected != currentFormation) {
            val reformed = applyFormationByPosition(
                state = sortedSwapped,
                from = currentFormation,
                to = detected,
                playerById = playerById
            )
            SwapResult(reformed, detected)
        } else {
            SwapResult(sortedSwapped, currentFormation)
        }
    }

    //Sorting subs from left to right (Gk, DEF, MID, STR)
    fun sortBenchIds(
        bench: List<Int?>,
        playerById: Map<Int, Player>
    ): MutableList<Int?> {
        val nonNull = bench.filterNotNull()

        val sorted = nonNull.sortedWith(
            compareBy<Int> { id ->
                when (playerById[id]?.position) {
                    Position.GK -> 0
                    Position.DEF -> 1
                    Position.MID -> 2
                    Position.STR -> 3
                    else -> 4
                }
            }
        )

        return sorted.toMutableList<Int?>().apply {
            while (size < 4) add(null)
        }
    }
}