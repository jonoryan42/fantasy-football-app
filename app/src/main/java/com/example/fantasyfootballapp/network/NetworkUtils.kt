package com.example.fantasyfootballapp.network

import retrofit2.HttpException

fun isUnauthorized(e: Throwable): Boolean =
    e is HttpException && e.code() == 401