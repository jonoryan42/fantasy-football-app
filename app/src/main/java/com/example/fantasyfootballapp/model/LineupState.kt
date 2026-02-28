package com.example.fantasyfootballapp.model

data class LineupState(
    val starters: MutableMap<RosterSlotKey, Int?> = mutableMapOf(),
    val bench: MutableList<Int?> = MutableList(4) { null }
) {

    companion object {

        fun fromSlotMap(slotMap: Map<RosterSlotKey, Int?>): LineupState {
            val starters = mutableMapOf<RosterSlotKey, Int?>()

            val pitchKeys = listOf(
                RosterSlotKey.GK1, RosterSlotKey.GK2,
                RosterSlotKey.DEF1, RosterSlotKey.DEF2, RosterSlotKey.DEF3, RosterSlotKey.DEF4, RosterSlotKey.DEF5,
                RosterSlotKey.MID1, RosterSlotKey.MID2, RosterSlotKey.MID3, RosterSlotKey.MID4, RosterSlotKey.MID5,
                RosterSlotKey.STR1, RosterSlotKey.STR2, RosterSlotKey.STR3
            )

            for (k in pitchKeys) {
                starters[k] = slotMap[k]
            }

            val bench = mutableListOf(
                slotMap[RosterSlotKey.BENCH1],
                slotMap[RosterSlotKey.BENCH2],
                slotMap[RosterSlotKey.BENCH3],
                slotMap[RosterSlotKey.BENCH4],
            )

            return LineupState(starters, bench)
        }
    }
}