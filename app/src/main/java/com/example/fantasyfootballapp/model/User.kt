package com.example.fantasyfootballapp.model

data class User(
    val id: String,
    val email: String,
    val fname: String,
    val lname: String,
    val teamName: String? = null,
    val createdAt: String? = null
)
