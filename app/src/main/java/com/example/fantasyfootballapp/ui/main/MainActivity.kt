package com.example.fantasyfootballapp.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.data.TokenStore
import com.example.fantasyfootballapp.network.ApiClient
import com.example.fantasyfootballapp.ui.leaderboard.LeaderboardActivity
import com.example.fantasyfootballapp.ui.signup.SignupActivity
import com.example.fantasyfootballapp.ui.teamSetup.CreateTeamActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    //For development
    private val DEMO_MODE = true

    private val repo by lazy {
        val tokenStore = TokenStore(applicationContext)
        FantasyRepository(ApiClient.service, tokenStore)
    }

    private lateinit var loginContainer: View
    private lateinit var homeContainer: View
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnStartDemo: Button
    private lateinit var btnViewLeaderboard: Button
    private lateinit var joinNow: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ApiClient.init(applicationContext)
        setContentView(R.layout.activity_main)

        loginContainer = findViewById<View>(R.id.loginContainer)
        homeContainer = findViewById<View>(R.id.homeContainer)

        edtEmail = findViewById<EditText>(R.id.edtEmail)
        edtPassword = findViewById<EditText>(R.id.edtPassword)
        btnLogin = findViewById<Button>(R.id.btnLogin)

        btnStartDemo = findViewById<Button>(R.id.btnStartDemo)
        btnViewLeaderboard = findViewById<Button>(R.id.btnViewLeaderboard)
        joinNow = findViewById<TextView>(R.id.txtJoinNow)

        joinNow.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        loginContainer.visibility = View.VISIBLE
        homeContainer.visibility = View.VISIBLE

        //Demo buttons
        btnStartDemo.setOnClickListener {
            if (DEMO_MODE) {
                startActivity(Intent(this, CreateTeamActivity::class.java))
            } else {
                // In non-demo mode, optionally require login before allowing this
                lifecycleScope.launch {
                    val user = try { repo.getCurrentUser() } catch (e: Exception) { null }
                    if (user == null) {
                        Toast.makeText(this@MainActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    startActivity(Intent(this@MainActivity, CreateTeamActivity::class.java))
                }
            }
        }

        btnViewLeaderboard.setOnClickListener {
            if (DEMO_MODE) {
                startActivity(Intent(this, LeaderboardActivity::class.java))
            } else {
                lifecycleScope.launch {
                    val user = try { repo.getCurrentUser() } catch (e: Exception) { null }
                    if (user == null) {
                        Toast.makeText(this@MainActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    startActivity(Intent(this@MainActivity, LeaderboardActivity::class.java))
                }
            }
        }

        //Login button
        btnLogin.setOnClickListener {
            handleLogin()
        }
    }
    private fun validateLogin(emailRaw: String, passRaw: String): Pair<String, String>? {
        val email = emailRaw.trim()

        if (email.isBlank()) {
            Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show()
            return null
        }
        //If email matches current user
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Not a valid email address.", Toast.LENGTH_SHORT).show()
            return null
        }
        if (passRaw.isBlank()) {
            Toast.makeText(this, "Please enter your password.", Toast.LENGTH_SHORT).show()
            return null
        }
        if (passRaw.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
            return null
        }
        return email to passRaw
    }

    private fun handleLogin() {
        lifecycleScope.launch {
            val validated = validateLogin(
                edtEmail.text.toString(),
                edtPassword.text.toString()
            ) ?: return@launch

            val (email, password) = validated

            btnLogin.isEnabled = false
            try {
                repo.login(email, password)
                startActivity(
                    Intent(this@MainActivity, LeaderboardActivity::class.java)
                )
                finish()
            } catch (e: Exception) {
                Log.e("Login", "Login failed", e)

            }
            //Re-enables login
            finally {
                btnLogin.isEnabled = true
            }
        }
    }
        }
