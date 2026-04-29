package com.example.fantasyfootballapp.ui.common

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.ui.fantasy.FantasyActivity
import com.example.fantasyfootballapp.ui.home.HomeActivity
import com.example.fantasyfootballapp.ui.leaderboard.LeaderboardActivity
import com.example.fantasyfootballapp.ui.league.LeagueActivity
import com.example.fantasyfootballapp.ui.main.MainActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

//For the bottom nav (used in most activities for navigation)
object AppBottomNav {

    fun setup(
        activity: AppCompatActivity,
        bottomNav: BottomNavigationView,
        selectedItemId: Int
    ) {
        bottomNav.selectedItemId = selectedItemId

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == selectedItemId) {
                return@setOnItemSelectedListener true
            }

            when (item.itemId) {
                R.id.nav_home -> {
                    activity.startActivity(Intent(activity, HomeActivity::class.java))
                    true
                }

                R.id.nav_fantasy -> {
                    activity.startActivity(Intent(activity, FantasyActivity::class.java))
                    true
                }

                R.id.nav_league -> {
                    activity.startActivity(Intent(activity, LeagueActivity::class.java))
                    true
                }

                R.id.nav_leaderboard -> {
                    activity.startActivity(Intent(activity, LeaderboardActivity::class.java))
                    true
                }

                else -> false
            }
        }
    }
}