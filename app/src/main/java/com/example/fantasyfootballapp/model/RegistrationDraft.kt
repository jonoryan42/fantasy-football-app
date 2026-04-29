package com.example.fantasyfootballapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

//Allows passing data through activities (creating new user)
@Parcelize
data class RegistrationDraft(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val teamName: String? = null
) : Parcelable