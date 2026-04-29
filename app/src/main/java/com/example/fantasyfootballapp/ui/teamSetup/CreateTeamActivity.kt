package com.example.fantasyfootballapp.ui.teamSetup

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.data.TokenStore
import com.example.fantasyfootballapp.model.RegistrationDraft
import com.example.fantasyfootballapp.navigation.NavKeys
import com.example.fantasyfootballapp.network.ApiClient
import com.example.fantasyfootballapp.ui.pickTeam.PickTeamActivity
import com.example.fantasyfootballapp.ui.transfers.TransfersActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateTeamActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_team)

        val edtTeamName = findViewById<EditText>(R.id.edtTeamName)
        val btnContinue = findViewById<Button>(R.id.btnContinueToPickTeam)

        val draft = getRegistrationDraft() ?: run {
            finish()
            return
        }

        btnContinue.setOnClickListener {
            val newName = edtTeamName.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(this, "Please enter a Team Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedDraft = draft.copy(teamName = newName)

            startActivity(
                Intent(this, TransfersActivity::class.java)
                    .putExtra(NavKeys.REG_DRAFT, updatedDraft)
            )
            finish()
        }
        }

    //Can't use Tiramisu on my phone
    @Suppress("DEPRECATION")
    private fun getRegistrationDraft(): RegistrationDraft? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NavKeys.REG_DRAFT, RegistrationDraft::class.java)
        } else {
            intent.getParcelableExtra(NavKeys.REG_DRAFT)
        }
    }
    }
