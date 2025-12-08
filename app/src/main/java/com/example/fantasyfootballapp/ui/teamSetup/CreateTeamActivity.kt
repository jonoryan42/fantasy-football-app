package com.example.fantasyfootballapp.ui.teamSetup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.ui.pickTeam.PickTeamActivity

class CreateTeamActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_team)

        val edtTeamName = findViewById<EditText>(R.id.edtTeamName)
        val btnContinue = findViewById<Button>(R.id.btnContinueToPickTeam)

        // Prefill with current team name if it exists
        val currentUser = FantasyRepository.getCurrentUser()
        edtTeamName.setText(currentUser.teamName)

        btnContinue.setOnClickListener {
            val newName = edtTeamName.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(this, "Please enter a team name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update repository
            FantasyRepository.updateCurrentUserTeamName(newName)

            // Go to Pick Team screen
            val intent = Intent(this, PickTeamActivity::class.java)
            startActivity(intent)
            finish() // optional: don’t come back here when pressing back
        }
    }
}
