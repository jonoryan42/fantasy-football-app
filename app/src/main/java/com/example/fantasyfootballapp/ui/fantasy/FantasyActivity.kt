package com.example.fantasyfootballapp.ui.fantasy

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.ui.leaderboard.LeaderboardActivity
import com.example.fantasyfootballapp.ui.league.LeagueActivity
import com.example.fantasyfootballapp.ui.pickTeam.PickTeamActivity
import com.example.fantasyfootballapp.ui.transfers.TransfersActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import android.widget.TextView
import com.example.fantasyfootballapp.ui.common.AppBottomNav
import com.example.fantasyfootballapp.ui.common.SystemBars
import com.example.fantasyfootballapp.ui.main.MainActivity

class FantasyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fantasy)

        SystemBars.apply(this, R.color.screen_light_bg, lightIcons = true)

        val tvGameweekPoints = findViewById<TextView>(R.id.tvGameweekPoints)
        val tvTotalPoints = findViewById<TextView>(R.id.tvTotalPoints)
        val tvDeadline = findViewById<TextView>(R.id.tvDeadline)
        val btnPickTeam = findViewById<MaterialButton>(R.id.btnPickTeam)
        val btnTransfers = findViewById<MaterialButton>(R.id.btnTransfers)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Temporary placeholder values
        tvGameweekPoints.text = "52"
        tvTotalPoints.text = "418"
        tvDeadline.text = "Tue 2 Dec, 18:00"

        btnPickTeam.setOnClickListener {
            startActivity(Intent(this, PickTeamActivity::class.java))
        }

        btnTransfers.setOnClickListener {
            startActivity(Intent(this, TransfersActivity::class.java))
        }

        //Bottom nav taken from AppBottomNav file
        AppBottomNav.setup(
            activity = this,
            bottomNav = bottomNav,
            selectedItemId = R.id.nav_fantasy
        )
    }
}