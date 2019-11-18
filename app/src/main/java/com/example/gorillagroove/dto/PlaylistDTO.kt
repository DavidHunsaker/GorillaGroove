package com.example.gorillagroove.dto

import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JsonSerialize
data class PlaylistDTO(
    val id: Long = 0,
    val name: String = "",
    val createdAt: String = ""
)