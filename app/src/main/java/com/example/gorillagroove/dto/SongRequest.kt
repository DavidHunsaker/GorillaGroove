package com.example.gorillagroove.dto

data class SongRequest(
    val title: String,
    val artist: String,
    val trackLink: String?,
    val imageUrl: String?
)