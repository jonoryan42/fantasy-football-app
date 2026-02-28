package com.example.fantasyfootballapp.model

data class Formation(val def: Int, val mid: Int, val fwd: Int) {
    init {
        require(fwd in 1..3) { "This UI supports 1..3 forwards, got $fwd" }
        require(def in 3..5) { "Defenders must be 3..5, got $def" }
        require(mid in 2..5) { "Midfielders must be 2..5, got $mid" }
        require(def + mid + fwd == 10) { "Outfield players must sum to 10" }
    }
    companion object {
        val F442 = Formation(4,4,2)
        val F433 = Formation(4,3,3)
        val F451 = Formation (4,5,1)
        val F532 = Formation(5,3,2)
        val F523 = Formation(5,2,3)
        val F541 = Formation(5,4,1)
        val F343 = Formation(3,4,3)
        val F352 = Formation(3,5,2)

        val all = mapOf(
            "F442" to F442,
            "F433" to F433,
            "F451" to F451,
            "F532" to F532,
            "F523" to F523,
            "F541" to F541,
            "F343" to F343,
            "F352" to F352
        )

        fun fromKey(key: String?): Formation =
            all[key] ?: F442

        fun keyOf(formation: Formation): String =
            all.entries.firstOrNull { it.value == formation }?.key ?: "F442"
    }
}