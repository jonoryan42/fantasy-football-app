package com.example.fantasyfootballapp.ui.leaderboard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.example.fantasyfootballapp.ui.main.MainActivity
import com.example.fantasyfootballapp.ui.viewTeam.ViewTeamActivity
import kotlinx.coroutines.launch

class LeaderboardActivity : AppCompatActivity() {

    private val repo by lazy {
        val tokenStore = TokenStore(applicationContext)
        FantasyRepository(ApiClient.service, tokenStore)
    }

    private lateinit var leaderboardAdapter: LeaderboardAdapter

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val txtHelloUser = findViewById<TextView>(R.id.txtHelloUser)


        val recyclerLeaderboard = findViewById<RecyclerView>(R.id.recyclerLeaderboard)
        recyclerLeaderboard.layoutManager = LinearLayoutManager(this)

        leaderboardAdapter = LeaderboardAdapter(mutableListOf()) { team ->
            val intent = Intent(this, ViewTeamActivity::class.java).apply {
                putExtra(ViewTeamActivity.EXTRA_TEAM_NAME, team.teamName)
                putExtra(ViewTeamActivity.EXTRA_TEAM_POINTS, team.points)
                putIntegerArrayListExtra(
                    ViewTeamActivity.EXTRA_PLAYER_IDS,
                    ArrayList(team.playerIds)
                )
            }
            startActivity(intent)
        }

        recyclerLeaderboard.adapter = leaderboardAdapter

        btnLogout.setOnClickListener {
            lifecycleScope.launch {
                // Clear auth token
                repo.logout()
                // OR tokenStore.clear() depending on your implementation

                // Go back to MainActivity and clear back stack
                val intent = Intent(this@LeaderboardActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
        }

        lifecycleScope.launch {
            try {
                val leaderboard = repo.getLeaderboard()
                leaderboardAdapter.setData(leaderboard)
            } catch (e: Exception) {
                Log.e("LEADERBOARD", "Failed to load leaderboard", e)
            }
        }

        //Show Hello [User]
        lifecycleScope.launch {
            try {
                val user = repo.getCurrentUser()
                Log.d("LEADERBOARD", "fname='${user.fname}' len=${user.fname.length}")

                txtHelloUser.text = "Hello ${user.fname.ifBlank { "there" }}"
                txtHelloUser.visibility = View.VISIBLE

            } catch (e: Exception) {
                Log.e("LEADERBOARD", "getCurrentUser failed", e)
                txtHelloUser.visibility = View.GONE
                // TEMP so you can see it during testing:
                 Toast.makeText(this@LeaderboardActivity, "Not logged in (${e.message})", Toast.LENGTH_SHORT).show()
            }
        }


    }
}
