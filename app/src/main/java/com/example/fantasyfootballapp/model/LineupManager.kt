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
        return buildList {
            add(gk1)
            addAll(defKeys.take(formation.def))
            addAll(midKeys.take(formation.mid))
            addAll(fwdKeys.take(formation.fwd))
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
}