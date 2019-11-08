package com.example.gorillagroove.dto

import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JsonSerialize
data class Playlist(val playlist: List<PlaylistSongDTO> = emptyList())

@JsonSerialize
data class PlaylistSongDTO(
    val id: Long = 0,
    val track: Track = Track()
)