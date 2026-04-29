package com.example.fantasyfootballapp.mappers

import com.example.fantasyfootballapp.model.User
import com.example.fantasyfootballapp.network.MeResponse
import com.example.fantasyfootballapp.network.AuthUserDto

fun MeResponse.toModel(): User =
    User(
        id = id,
        fname = fname,
        lname = lname,
        email = email,
        teamName = teamName
    )

fun AuthUserDto.toModel(): User =
    User(
        id = _id,
        fname = fname,
        lname = lname,
        email = email,
    )