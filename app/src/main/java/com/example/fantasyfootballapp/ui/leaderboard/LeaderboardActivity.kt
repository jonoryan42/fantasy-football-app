package com.example.fantasyfootballapp.ui.leaderboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.data.FantasyRepository

class LeaderboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        val recyclerLeaderboard = findViewById<RecyclerView>(R.id.recyclerLeaderboard)

        val leaderboard = FantasyRepository.getLeaderboard()

        val adapter = LeaderboardAdapter(leaderboard)

        recyclerLeaderboard.layoutManager = LinearLayoutManager(this)
        recyclerLeaderboard.adapter = adapter
    }
}
