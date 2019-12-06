package com.example.gorillagroove.dto

import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JsonSerialize
data class Users(
    val id: Long = 0,
    val username: String = "",
    val email: String = ""
)