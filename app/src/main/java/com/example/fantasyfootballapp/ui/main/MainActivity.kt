package com.example.fantasyfootballapp.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.data.TokenStore
import com.example.fantasyfootballapp.network.ApiClient
import com.example.fantasyfootballapp.ui.leaderboard.LeaderboardActivity
import com.example.fantasyfootballapp.ui.teamSetup.CreateTeamActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val repo by lazy {
        val tokenStore = TokenStore(applicationContext)
        FantasyRepository(ApiClient.service, tokenStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ApiClient.init(applicationContext)

        setContentView(R.layout.activity_main)

        val loginContainer = findViewById<View>(R.id.loginContainer)
        val homeContainer = findViewById<View>(R.id.homeContainer)

        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        val btnStartDemo = findViewById<Button>(R.id.btnStartDemo)
        val btnViewLeaderboard = findViewById<Button>(R.id.btnViewLeaderboard)

        //Demo buttons
        btnStartDemo.setOnClickListener {
            startActivity(Intent(this, CreateTeamActivity::class.java))
        }

        btnViewLeaderboard.setOnClickListener {
            startActivity(Intent(this, LeaderboardActivity::class.java))
        }

        //Checks token
        lifecycleScope.launch {
            val user = try { repo.getCurrentUser() } catch (e: Exception) { null }

            if (user != null) {
                loginContainer.visibility = View.GONE
                homeContainer.visibility = View.VISIBLE
            } else {
                loginContainer.visibility = View.VISIBLE
                homeContainer.visibility = View.GONE
            }
        }

        //Login button
        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    repo.login(email, password)

                    loginContainer.visibility = View.GONE
                    homeContainer.visibility = View.VISIBLE
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Login failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
