package com.example.fantasyfootballapp.model

enum class RosterSlotKey(val position: Position) {
    GK1(Position.GK), GK2(Position.GK),

    DEF1(Position.DEF), DEF2(Position.DEF),
    DEF3(Position.DEF), DEF4(Position.DEF), DEF5(Position.DEF),

    MID1(Position.MID), MID2(Position.MID),
    MID3(Position.MID), MID4(Position.MID), MID5(Position.MID),

    STR1(Position.STR), STR2(Position.STR), STR3(Position.STR)
}
