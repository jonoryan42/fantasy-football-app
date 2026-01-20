package com.example.fantasyfootballapp.data

import android.content.Context
import androidx.core.content.edit

//For storing Tokens for users.
class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit { putString("token", token) }
    }

    fun getToken(): String? = prefs.getString("token", null)

    fun clearToken() {
        prefs.edit { remove("token") }
    }
}
