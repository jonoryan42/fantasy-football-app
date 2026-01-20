package com.example.fantasyfootballapp.data.mappers

import com.example.fantasyfootballapp.model.User
import com.example.fantasyfootballapp.network.AuthUserDto
import com.example.fantasyfootballapp.network.MeResponse

//Model used to show different values for user.

fun MeResponse.toModel(): User = User(
    id = id,
    email = email,
    fname = "",
    lname = "",
    teamName = teamName,
    createdAt = createdAt
)

fun AuthUserDto.toModel(): User = User(
    id = _id,
    email = email,
    fname = fname,
    lname = lname
)

