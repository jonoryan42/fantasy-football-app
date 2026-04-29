package com.example.fantasyfootballapp.ui.leaderboard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.config.GameweekConfig
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.data.TokenStore
import com.example.fantasyfootballapp.network.ApiClient
import com.example.fantasyfootballapp.ui.common.AppBottomNav
import com.example.fantasyfootballapp.ui.common.SystemBars
import com.example.fantasyfootballapp.ui.main.MainActivity
import com.example.fantasyfootballapp.ui.pickTeam.PickTeamActivity
import com.example.fantasyfootballapp.ui.transfers.TransfersActivity
import com.example.fantasyfootballapp.ui.viewTeam.ViewTeamActivity
import com.example.fantasyfootballapp.ui.viewTeam.ViewTeamListActivity
import kotlinx.coroutines.launch

class LeaderboardActivity : AppCompatActivity() {

    private val DEMO_MODE = true

    private lateinit var btnTransfers: Button
    private lateinit var btnPickTeam: Button
    private lateinit var btnLogout: Button

    private lateinit var leaderboardAdapter: LeaderboardAdapter

    private val repo by lazy {
        val tokenStore = TokenStore(applicationContext)
        FantasyRepository(ApiClient.service, tokenStore)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        //Setting the bottom theme to match the nav color
        SystemBars.apply(this, R.color.screen_light_bg, lightIcons = true)

        btnLogout = findViewById(R.id.btnLogout)
        btnTransfers = findViewById(R.id.btnTransfers)
        btnPickTeam = findViewById(R.id.btnPickTeam)


        val recyclerLeaderboard = findViewById<RecyclerView>(R.id.recyclerLeaderboard)
        recyclerLeaderboard.layoutManager = LinearLayoutManager(this)

        val previousGameweek = (GameweekConfig.CURRENT_GAMEWEEK - 1).coerceAtLeast(1)

        leaderboardAdapter = LeaderboardAdapter(mutableListOf()) { team ->
            //If team wasn't initialised for gameweek 1, you can't view their team
            if (team.points <= 0) return@LeaderboardAdapter

            val intent = Intent(this, ViewTeamActivity::class.java).apply {
                putExtra(ViewTeamActivity.EXTRA_USER_ID, team.userId)
                putExtra(ViewTeamActivity.EXTRA_TEAM_NAME, team.teamName)
                putExtra(ViewTeamActivity.EXTRA_GAMEWEEK, previousGameweek)

            }
            startActivity(intent)
        }

        recyclerLeaderboard.adapter = leaderboardAdapter

        btnLogout.setOnClickListener {
            lifecycleScope.launch {
                // Clear auth token
                repo.logout()

                // Go back to MainActivity and clear back stack
                val intent = Intent(this@LeaderboardActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
        }

        btnTransfers.setOnClickListener {
            if (DEMO_MODE) {
                startActivity(Intent(this, TransfersActivity::class.java))
            } else {
                lifecycleScope.launch {
                    val user = try { repo.getCurrentUser() } catch (_: Exception) { null }
                    if (user == null) {
                        Toast.makeText(this@LeaderboardActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    startActivity(Intent(this@LeaderboardActivity, TransfersActivity::class.java))
                }
            }
        }

        btnPickTeam.setOnClickListener {
            if (DEMO_MODE) {
                startActivity(Intent(this, PickTeamActivity::class.java))
            } else {
                lifecycleScope.launch {
                    val user = try { repo.getCurrentUser() } catch (_: Exception) { null }
                    if (user == null) {
                        Toast.makeText(this@LeaderboardActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    startActivity(Intent(this@LeaderboardActivity, PickTeamActivity::class.java))
                }
            }
        }

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)

        AppBottomNav.setup(
            activity = this,
            bottomNav = bottomNav,
            selectedItemId = R.id.nav_leaderboard
        )

        lifecycleScope.launch {
            try {
                val leaderboard = repo.getLeaderboard()
                leaderboardAdapter.setData(leaderboard)
            } catch (e: Exception) {
                Log.e("LEADERBOARD", "Failed to load leaderboard", e)
            }
        }
    }
}
