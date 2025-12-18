package com.example.fantasyfootballapp.ui.leaderboard

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.network.ApiClient
import kotlinx.coroutines.launch

class LeaderboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        val recyclerLeaderboard = findViewById<RecyclerView>(R.id.recyclerLeaderboard)
        recyclerLeaderboard.layoutManager = LinearLayoutManager(this)

        val adapter = LeaderboardAdapter(mutableListOf())
        recyclerLeaderboard.adapter = adapter

        //Testing Leaderboard
        lifecycleScope.launch {
            try {
                val leaderboard =
                    FantasyRepository.getLeaderboard() // suspend call inside coroutine
                adapter.setData(leaderboard)

            } catch (e: Exception) {
                Log.e("LEADERBOARD", "Failed to load leaderboard", e)
            }
        }
    }
}
