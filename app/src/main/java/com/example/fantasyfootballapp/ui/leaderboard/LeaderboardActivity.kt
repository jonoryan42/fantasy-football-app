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

//    private lateinit var txtHelloUser: TextView
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

//        txtHelloUser = findViewById(R.id.txtHelloUser)
        btnLogout = findViewById(R.id.btnLogout)
        btnTransfers = findViewById(R.id.btnTransfers)
        btnPickTeam = findViewById(R.id.btnPickTeam)


        val recyclerLeaderboard = findViewById<RecyclerView>(R.id.recyclerLeaderboard)
        recyclerLeaderboard.layoutManager = LinearLayoutManager(this)

        leaderboardAdapter = LeaderboardAdapter(mutableListOf()) { team ->
            val intent = Intent(this, ViewTeamActivity::class.java).apply {
                putExtra(ViewTeamActivity.EXTRA_TEAM, team)
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

        //Show Hello [User]
//        lifecycleScope.launch {
//            try {
//                val user = repo.getCurrentUser()
//                Log.d("LEADERBOARD", "fname='${user.fname}' len=${user.fname.length}")
//
//                txtHelloUser.text = "Hello ${user.fname.ifBlank { "there" }}"
//                txtHelloUser.visibility = View.VISIBLE
//
//            } catch (e: Exception) {
//                Log.e("LEADERBOARD", "getCurrentUser failed", e)
//                txtHelloUser.visibility = View.GONE
//                // TEMP so you can see it during testing:
//                 Toast.makeText(this@LeaderboardActivity, "Not logged in (${e.message})", Toast.LENGTH_SHORT).show()
//            }
//        }


    }
}
