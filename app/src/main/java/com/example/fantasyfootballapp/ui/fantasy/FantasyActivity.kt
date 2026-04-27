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
import androidx.lifecycle.lifecycleScope
import com.example.fantasyfootballapp.config.GameweekConfig
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.data.TokenStore
import com.example.fantasyfootballapp.network.ApiClient
import com.example.fantasyfootballapp.ui.common.AppBottomNav
import com.example.fantasyfootballapp.ui.common.SystemBars
import com.example.fantasyfootballapp.ui.main.MainActivity
import kotlinx.coroutines.launch

class FantasyActivity : AppCompatActivity() {

    private val repo by lazy {
        val tokenStore = TokenStore(applicationContext)
        FantasyRepository(ApiClient.service, tokenStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fantasy)

        SystemBars.apply(this, R.color.screen_light_bg, lightIcons = true)

        val tvTeamName = findViewById<TextView>(R.id.tvTeamName)
        val tvGameweekLabel = findViewById<TextView>(R.id.tvGameweekLabel)
        val tvGameweekPoints = findViewById<TextView>(R.id.tvGameweekPoints)
        val tvTotalPoints = findViewById<TextView>(R.id.tvTotalPoints)
        val tvDeadline = findViewById<TextView>(R.id.tvDeadline)
        val btnPickTeam = findViewById<MaterialButton>(R.id.btnPickTeam)
        val btnTransfers = findViewById<MaterialButton>(R.id.btnTransfers)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        loadFantasySummary(tvTeamName, tvGameweekLabel, tvGameweekPoints, tvTotalPoints, tvDeadline)

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

    private fun loadFantasySummary(
        tvTeamName: TextView,
        tvGameweekLabel: TextView,
        tvGameweekPoints: TextView,
        tvTotalPoints: TextView,
        tvDeadline: TextView
    ) {
        lifecycleScope.launch {

            val score = repo.fetchMyGameweekScore(
                gameweek = GameweekConfig.CURRENT_GAMEWEEK,
                season = GameweekConfig.CURRENT_SEASON
            )

            val points = score?.points ?: 0
            val gameweek = score?.gameweek ?: 0

            val team = repo.getMyTeam()


            tvTeamName.text = team?.teamName
            tvGameweekLabel.text = "Gameweek $gameweek"
            tvGameweekPoints.text = points.toString()
            tvTotalPoints.text = points.toString()
            tvDeadline.text = "Tue 2 June, 18:00"
        }
    }
}