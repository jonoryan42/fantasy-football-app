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
            "442" to F442,
            "433" to F433,
            "451" to F451,
            "532" to F532,
            "523" to F523,
            "541" to F541,
            "343" to F343,
            "352" to F352
        )

        fun fromKey(key: String?): Formation {
            val normalized = key
                ?.trim()
                ?.uppercase()
                ?.removePrefix("F")
            return all[normalized] ?: F442
        }

        fun keyOf(formation: Formation): String =
            all.entries.firstOrNull { it.value == formation }?.key ?: "442"
    }
}