package com.example.fantasyfootballapp.ui.teamSetup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.data.TokenStore
import com.example.fantasyfootballapp.network.ApiClient
import com.example.fantasyfootballapp.ui.pickTeam.PickTeamActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateTeamActivity : AppCompatActivity() {

    private val repo by lazy {
        val tokenStore = TokenStore(applicationContext)
        FantasyRepository(ApiClient.service, tokenStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_team)

        val edtTeamName = findViewById<EditText>(R.id.edtTeamName)
        val btnContinue = findViewById<Button>(R.id.btnContinueToPickTeam)

        btnContinue.setOnClickListener {
            val newName = edtTeamName.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(this, "Please enter a team name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update repository
            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        repo.updateCurrentUserTeamName(newName)
                    }

                    val intent = Intent(this@CreateTeamActivity, PickTeamActivity::class.java)
                    intent.putExtra("teamName", newName) // optional (helps UI immediately)
                    startActivity(intent)
                    finish()

                    // proceed to PickTeamActivity
                } catch (e: Exception) {
                    Toast.makeText(this@CreateTeamActivity, e.message ?: "Failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
