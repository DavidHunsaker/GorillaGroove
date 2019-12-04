package com.example.gorillagroove.dto

import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JsonSerialize
data class Track(
    val id: Long = 1,
    val name: String? = "",
    val artist: String? = "",
    val featuring: String? = "",
    val album: String? = "",
    val trackNumber: Long = 1,
    val fileName: String = "",
    val bitRate: Long = 0,
    val sampleRate: Long = 0,
    val length: Long = 0,
    val releaseYear: String? = "",
    val genre: String? = "",
    val playCount: Long = 0,
    val hidden: Boolean = false,
    val lastPlayed: String? = "",
    val updatedAt: String? = "",
    val createdAt: String? = "",
    val note: String? = ""
)