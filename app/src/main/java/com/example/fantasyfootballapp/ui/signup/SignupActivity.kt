package com.example.fantasyfootballapp.ui.signup

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.data.TokenStore
import com.example.fantasyfootballapp.network.ApiClient
import com.example.fantasyfootballapp.util.RepoResult
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private val repo by lazy {
        val tokenStore = TokenStore(applicationContext)
        FantasyRepository(ApiClient.service, tokenStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val details = findViewById<View>(R.id.detailsContainer)
        val email = findViewById<TextInputEditText>(R.id.edtEmail)
        val password = findViewById<TextInputEditText>(R.id.edtPassword)
        val firstName = findViewById<TextInputEditText>(R.id.edtFirstName)
        val lastName = findViewById<TextInputEditText>(R.id.edtLastName)
        val btn = findViewById<MaterialButton>(R.id.btnContinue)

        findViewById<TextView>(R.id.txtSignIn).setOnClickListener { finish() }

        btn.setOnClickListener {
            val emailText = email.text?.toString()?.trim().orEmpty()

            // STEP 1: reveal extra fields after valid email
            if (details.visibility != View.VISIBLE) {
                if (!isValidEmail(emailText)) {
                    email.error = "Enter a valid Email"
                    return@setOnClickListener
                }
                email.error = null
                details.visibility = View.VISIBLE
                password.requestFocus()
                return@setOnClickListener
            }

            // STEP 2: validate all fields
            val passText = password.text?.toString().orEmpty()
            val fnText = firstName.text?.toString()?.trim().orEmpty()
            val lnText = lastName.text?.toString()?.trim().orEmpty()

            var ok = true
            if (!isValidEmail(emailText)) { email.error = "Enter a valid email"; ok = false } else email.error = null
            if (passText.length < 6) { password.error = "Password must be at least 6 characters"; ok = false } else password.error = null
            if (fnText.isBlank()) { firstName.error = "Required"; ok = false } else firstName.error = null
            if (lnText.isBlank()) { lastName.error = "Required"; ok = false } else lastName.error = null

            if (!ok) return@setOnClickListener

            // STEP 3: call backend register
            lifecycleScope.launch {
                btn.isEnabled = false

                when (val result = repo.registerSafe(
                    fname = fnText,
                    lname = lnText,
                    email = emailText,
                    password = passText
                )) {
                    is RepoResult.Success -> {
                        startActivity(
                            Intent(
                                this@SignupActivity,
                                com.example.fantasyfootballapp.ui.teamSetup.CreateTeamActivity::class.java
                            )
                        )
                        finish()
                    }

                    is RepoResult.Error -> {
                        btn.isEnabled = true
                        Toast.makeText(this@SignupActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}