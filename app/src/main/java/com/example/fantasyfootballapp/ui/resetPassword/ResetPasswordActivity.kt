package com.example.fantasyfootballapp.ui.resetPassword

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.data.FantasyRepository
import com.example.fantasyfootballapp.data.TokenStore
import com.example.fantasyfootballapp.network.ApiClient

class ResetPasswordActivity : AppCompatActivity() {

    private val repo by lazy {
        val tokenStore = TokenStore(applicationContext)
        FantasyRepository(ApiClient.service, tokenStore)
    }

    private lateinit var form: LinearLayout
    private lateinit var sent: LinearLayout
    private lateinit var edtEmail: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        val btnSubmit = findViewById<Button>(R.id.btnSubmitReset)
        val btnCancel = findViewById<Button>(R.id.btnCancelReset)
        val btnBack = findViewById<Button>(R.id.btnBackToSignIn)
        val txtResend = findViewById<TextView>(R.id.txtResend)

        edtEmail = findViewById(R.id.edtResetEmail) // use your actual id

        form = findViewById(R.id.resetFormContainer)  // your actual id
        sent = findViewById(R.id.sentContainer)  // your actual id

        btnCancel.setOnClickListener { finish() }
        btnBack.setOnClickListener { finish() }

        btnSubmit.setOnClickListener {
            val rawEmail = edtEmail.text.toString()
            val error = validateEmail(rawEmail)

            if (error != null) {
                edtEmail.error = error
                edtEmail.requestFocus()
                return@setOnClickListener
            }

            // Normalised email (safe to send to backend later)
            val email = rawEmail.trim().lowercase()

            showSentState()
        }

        txtResend.setOnClickListener {
            Toast.makeText(this, "Resend requested.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateEmail(input: String): String? {
        val email = input.trim().lowercase()

        if (email.isBlank()) {
            return "Email is required."
        }

        if (email.contains(" ")) {
            return "Email cannot contain spaces."
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Enter a valid email address."
        }

        return null // valid
    }


    private fun showSentState() {
        form.visibility = android.view.View.GONE
        sent.visibility = android.view.View.VISIBLE
    }
}