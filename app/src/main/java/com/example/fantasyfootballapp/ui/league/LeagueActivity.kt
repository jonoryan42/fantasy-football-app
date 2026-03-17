package com.example.fantasyfootballapp.ui.league

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.model.LeagueRow
import com.example.fantasyfootballapp.ui.common.AppBottomNav
import com.example.fantasyfootballapp.ui.common.SystemBars

class LeagueActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_league)

        SystemBars.apply(this, R.color.screen_light_bg, lightIcons = true)


        recycler = findViewById(R.id.leagueRecycler)

        recycler.layoutManager = LinearLayoutManager(this)

        val rows = listOf(
            LeagueRow(1, "Ballinroad", 0, 0, 0, 0, 0),
            LeagueRow(2, "Bohemians", 0,0,0,0,0),
            LeagueRow(3, "Dungarvan", 0,0,0,0,0),
            LeagueRow(4, "Ferrybank", 0,0,0,0,0),
            LeagueRow(5, "Hibernians", 0,0,0,0,0),
            LeagueRow(6, "Portlaw", 0,0,0,0,0),
            LeagueRow(7, "Tramore", 0,0,0,0,0),
            LeagueRow(8, "Tycor", 0,0,0,0,0),
            LeagueRow(9, "Villa", 0,0,0,0,0),
            LeagueRow(10, "Waterford Crystal", 0,0,0,0,0)
        )

        recycler.adapter = LeagueAdapter(rows)

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)

        AppBottomNav.setup(
            activity = this,
            bottomNav = bottomNav,
            selectedItemId = R.id.nav_league
        )
    }
}