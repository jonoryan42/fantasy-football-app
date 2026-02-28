package com.example.fantasyfootballapp.model

enum class RosterSlotKey(val position: Position) {
    GK1(Position.GK), GK2(Position.GK),

    DEF1(Position.DEF), DEF2(Position.DEF),
    DEF3(Position.DEF), DEF4(Position.DEF), DEF5(Position.DEF),

    MID1(Position.MID), MID2(Position.MID),
    MID3(Position.MID), MID4(Position.MID), MID5(Position.MID),

    STR1(Position.STR), STR2(Position.STR), STR3(Position.STR),


    //Bench
    BENCH1(Position.BENCH), BENCH2(Position.BENCH), BENCH3(Position.BENCH), BENCH4(Position.BENCH);

    fun isBench(): Boolean = name.startsWith("BENCH")

    fun benchIndexOrNull(): Int? = when (this) {
        BENCH1 -> 0
        BENCH2 -> 1
        BENCH3 -> 2
        BENCH4 -> 3
        else -> null
    }

    companion object {
        // Transfers: 15 pitch slots
        val PITCH_ORDER: List<RosterSlotKey> = listOf(
            GK1, GK2,
            DEF1, DEF2, DEF3, DEF4, DEF5,
            MID1, MID2, MID3, MID4, MID5,
            STR1, STR2, STR3
        )

        // Pick Team: has bench
        val SLOT_ORDER: List<RosterSlotKey> = PITCH_ORDER + listOf(BENCH1, BENCH2, BENCH3, BENCH4)

        fun benchKey(index: Int): RosterSlotKey = when (index) {
            0 -> BENCH1
            1 -> BENCH2
            2 -> BENCH3
            3 -> BENCH4
            else -> throw IllegalArgumentException("Bench index must be 0..3")
        }
    }
}
