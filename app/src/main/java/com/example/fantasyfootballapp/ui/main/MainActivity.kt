package com.example.fantasyfootballapp.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.ui.leaderboard.LeaderboardActivity
import com.example.fantasyfootballapp.ui.pickTeam.PickTeamActivity
import com.example.fantasyfootballapp.ui.teamSetup.CreateTeamActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Create Team
        val btnStartDemo = findViewById<Button>(R.id.btnStartDemo)
        btnStartDemo.setOnClickListener {
            val intent = Intent(this, CreateTeamActivity::class.java)
            startActivity(intent)
        }

        //View Leaderboard
        val btnViewLeaderboard = findViewById<Button>(R.id.btnViewLeaderboard)

        btnViewLeaderboard.setOnClickListener {
            startActivity(Intent(this, LeaderboardActivity::class.java))
        }

    }
}