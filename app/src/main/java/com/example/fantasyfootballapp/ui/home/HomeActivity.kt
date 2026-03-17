package com.example.fantasyfootballapp.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.data.TokenStore
import com.example.fantasyfootballapp.network.ApiClient
import com.example.fantasyfootballapp.ui.common.AppBottomNav
import com.example.fantasyfootballapp.ui.common.SystemBars
import com.example.fantasyfootballapp.ui.main.MainActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var txtWelcome: TextView

    private val repo by lazy {
        val tokenStore = TokenStore(applicationContext)
        FantasyRepository(ApiClient.service, tokenStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        SystemBars.apply(this, R.color.screen_light_bg, lightIcons = true)

        txtWelcome = findViewById(R.id.txtWelcome)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)

        AppBottomNav.setup(
            activity = this,
            bottomNav = bottomNav,
            selectedItemId = R.id.nav_home
        )

        btnLogout.setOnClickListener {
            lifecycleScope.launch {
                // Clear auth token
                repo.logout()

                // Go back to MainActivity and clear back stack
                val intent = Intent(this@HomeActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
        }

        //Show Welcome [User]
        lifecycleScope.launch {
            try {
                val user = repo.getCurrentUser()
                Log.d("LEADERBOARD", "fname='${user.fname}' len=${user.fname.length}")

                txtWelcome.text = "Hello ${user.fname.ifBlank { "there" }}"
                txtWelcome.visibility = View.VISIBLE

            } catch (e: Exception) {
                Log.e("LEADERBOARD", "getCurrentUser failed", e)
                txtWelcome.visibility = View.GONE
                // TEMP so you can see it during testing:
                Toast.makeText(this@HomeActivity, "Not logged in (${e.message})", Toast.LENGTH_SHORT).show()
            }
        }
    }
}