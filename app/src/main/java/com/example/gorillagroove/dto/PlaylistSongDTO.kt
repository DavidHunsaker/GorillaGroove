package com.example.gorillagroove.dto

import java.time.ZonedDateTime

data class PlaylistSongDTO(
    val playlistId: Long,
    val trackId: Long,
    val name: String?,
    val artist: String?,
    val featuring: String?,
    val album: String?,
    val trackNumber: Long?,
    val fileName: String?,
    val bitRate: Long?,
    val sampleRate: Long?,
    val length: Long,
    val releaseYear: Int?,
    val genre: String?,
    val playCount: Int?,
    val hidden: Boolean,
    val lastPlayed: ZonedDateTime?,
    val createdAt: ZonedDateTime,
    val note: String?
)