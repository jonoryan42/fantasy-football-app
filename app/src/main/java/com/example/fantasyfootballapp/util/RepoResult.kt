package com.example.fantasyfootballapp.util

//Used when confirming in Pick Team
sealed class RepoResult<out T> {
    data class Success<T>(val data: T) : RepoResult<T>()
    data class Error(val message: String) : RepoResult<Nothing>()
}